package com.social100.todero.protocol.core;

import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.security.Certificate;
import com.social100.todero.protocol.security.CertificateUtils;
import com.social100.todero.protocol.security.HandshakeManager;
import com.social100.todero.protocol.security.HandshakeMessage;
import com.social100.todero.protocol.security.ReplayProtectionManager;
import com.social100.todero.protocol.transport.TransportInterface;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolEngine {
    private final TransportInterface transport;
    private final ResponderRegistry responderRegistry = new ResponderRegistry();
    private final Pipeline pipeline;
    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final boolean enableTlsSecurity;
    private final String identity;
    private final KeyPair identityKeyPair;

    private final ReplayProtectionManager replayProtection = new ReplayProtectionManager();

    private final Map<SocketAddress, Boolean> completedHandshakes = new ConcurrentHashMap<>();
    private final ConcurrentMap<SocketAddress, CompletableFuture<HandshakeMessage>> pendingHandshakes = new ConcurrentHashMap<>();
    private final ConcurrentMap<SocketAddress, CompletableFuture<Void>> handshakeFutures = new ConcurrentHashMap<>();

    public ProtocolEngine(TransportInterface transport, Pipeline pipeline) {
        this(transport, pipeline, false, null, null);
    }

    public ProtocolEngine(TransportInterface transport, Pipeline pipeline, boolean enableTlsSecurity,
                          String identity, KeyPair identityKeyPair) {
        this.transport = transport;
        this.pipeline = pipeline;
        this.enableTlsSecurity = enableTlsSecurity;
        this.identity = identity;
        this.identityKeyPair = identityKeyPair;
    }

    public void startClient(ReceiveMessageCallback messageCallback) throws IOException {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((protocolMessage, responder) -> {
            messageCallback.consume(protocolMessage);
        });
        startServer(receiveMessageCallback);
    }

    public void startServer(ReceiveMessageCallback messageCallback) throws IOException {
        transport.startReceiving((source, data) -> executor.submit(() -> {
            try {
                if (enableTlsSecurity && !completedHandshakes.containsKey(source)) {
                    HandshakeMessage handshake = deserializeHandshake(data);

                    CompletableFuture<HandshakeMessage> future = pendingHandshakes.get(source);
                    if (future != null && !future.isDone()) {
                        future.complete(handshake);
                        return;
                    }

                    if (!replayProtection.isValid(handshake.timestamp, handshake.nonce)) return;

                    if (handshake.certificate != null && !CertificateUtils.verifyCertificate(handshake.certificate, handshake.ephemeralPublicKey))
                        return;

                    HandshakeManager serverHandshake = new HandshakeManager();

                    serverHandshake.receivePeerPublicKey(handshake.ephemeralPublicKey);
                    SecretKey sharedSecret = serverHandshake.getSharedSecret();
                    Certificate responseCert = identityKeyPair != null
                        ? CertificateUtils.createCertificate(identity, identityKeyPair, serverHandshake.getEphemeralPublicKey())
                        : null;

                    HandshakeMessage serverHello = new HandshakeMessage(
                        System.currentTimeMillis(), UUID.randomUUID().toString(),
                        serverHandshake.getEphemeralPublicKey(), responseCert);

                    byte[] serverRaw = serializeHandshake(serverHello);
                    transport.sendMessageRaw(serverRaw, source);

                    // ✔ FIX: associate key with string-based source ID
                    String peerId = normalizePeerId(source);
                    updateEncryptionKey(peerId, sharedSecret);
                    completedHandshakes.put(source, true);
                    handshakeFutures.computeIfAbsent(source, k -> CompletableFuture.completedFuture(null));
                    return;
                }

                ResponderRegistry.Responder responder = responderRegistry.useResponder(source, this);
                String peerId = normalizePeerId(source);
                ProtocolFrameManager.FrameMessage frame = ProtocolFrameManager.deserialize(
                    data, responder.getId(), (message) -> this.processReceivedMessage(message, peerId));

                if (frame == null) return;

                if (messageCallback != null) {
                    messageCallback.consume(frame, responder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public int sendMessage(InetSocketAddress destination, byte[] message, boolean ackRequested) throws IOException {
        try {
            if (!handshakeFutures.containsKey(destination)) {
                // Only initiate handshake if no handshake is expected from peer
                CompletableFuture<Void> future = new CompletableFuture<>();
                handshakeFutures.put(destination, future);

                executor.submit(() -> {
                    try {
                        performHandshakeAsync(destination); // This sends a ClientHello
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                        handshakeFutures.remove(destination); // allow retry
                    }
                });
            }

            handshakeFutures.get(destination).get(5, TimeUnit.SECONDS);

            int packetId = packetIdGenerator.getAndIncrement();
            ProtocolFrameManager.FrameMessage frame = ProtocolFrameManager.FrameMessage.builder()
                .messageId(packetId)
                .payload(prepareMessageForSending(message, normalizePeerId(destination)))
                .ackRequested(ackRequested)
                .build();

            transport.sendMessage(frame, destination);
            return packetId;
        } catch (Exception e) {
            throw new IOException("Failed to send message due to handshake error", e);
        }
    }

    private void performHandshakeAsync(InetSocketAddress destination) throws Exception {
        HandshakeManager handshake = new HandshakeManager();
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        Certificate cert = identityKeyPair != null
            ? CertificateUtils.createCertificate(identity, identityKeyPair, handshake.getEphemeralPublicKey())
            : null;

        HandshakeMessage clientHello = new HandshakeMessage(timestamp, nonce, handshake.getEphemeralPublicKey(), cert);
        byte[] raw = serializeHandshake(clientHello);
        transport.sendMessageRaw(raw, destination);

        CompletableFuture<HandshakeMessage> future = new CompletableFuture<>();
        pendingHandshakes.put(destination, future);
        HandshakeMessage serverHello = future.get(5, TimeUnit.SECONDS);

        if (!replayProtection.isValid(serverHello.timestamp, serverHello.nonce)) {
            throw new SecurityException("Replay detected!");
        }

        if (serverHello.certificate != null && !CertificateUtils.verifyCertificate(serverHello.certificate, serverHello.ephemeralPublicKey)) {
            throw new SecurityException("Invalid server certificate!");
        }

        handshake.receivePeerPublicKey(serverHello.ephemeralPublicKey);
        SecretKey sharedSecret = handshake.getSharedSecret();
        String peerId = normalizePeerId(destination);
        updateEncryptionKey(peerId, sharedSecret);
        completedHandshakes.put(destination, true);
        pendingHandshakes.remove(destination);
    }

    private void updateEncryptionKey(String peerId, SecretKey key) {
        pipeline.updateStage(EncryptionStage.class, enc -> enc.setKeyForPeer(peerId, key));
    }

    public byte[] prepareMessageForSending(byte[] message, String destinationId) {
        return pipeline != null ? pipeline.processToSend(message, destinationId) : message;
    }

    public byte[] processReceivedMessage(byte[] message, String sourceId) {
        return pipeline != null ? pipeline.processToReceive(message, sourceId) : message;
    }

    public ResponderRegistry.Responder getResponder(String id) {
        return responderRegistry.getResponder(id);
    }

    public void close() throws IOException {
        executor.shutdownNow();
        transport.close();
    }

    private byte[] serializeHandshake(HandshakeMessage m) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeLong(m.timestamp);
        out.writeUTF(m.nonce);
        out.writeInt(m.ephemeralPublicKey.length);
        out.write(m.ephemeralPublicKey);
        if (m.certificate != null) {
            out.writeBoolean(true);
            out.writeUTF(m.certificate.identity);
            out.writeInt(m.certificate.publicKey.length);
            out.write(m.certificate.publicKey);
            out.writeInt(m.certificate.signature.length);
            out.write(m.certificate.signature);
        } else {
            out.writeBoolean(false);
        }
        return baos.toByteArray();
    }

    private HandshakeMessage deserializeHandshake(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        long ts = in.readLong();
        String nonce = in.readUTF();
        byte[] epk = new byte[in.readInt()];
        in.readFully(epk);
        Certificate cert = null;
        if (in.readBoolean()) {
            String id = in.readUTF();
            byte[] pk = new byte[in.readInt()];
            in.readFully(pk);
            byte[] sig = new byte[in.readInt()];
            in.readFully(sig);
            cert = new Certificate(id, pk, sig);
        }
        return new HandshakeMessage(ts, nonce, epk, cert);
    }

    private String normalizePeerId(SocketAddress address) {
        String fullNormalized = (address instanceof InetSocketAddress isa)
            ? isa.getAddress().toString() + ":" + isa.getPort()
            : address.toString();
        int slashIndex = fullNormalized.indexOf('/');
        if (slashIndex != -1) {
            fullNormalized = fullNormalized.substring(slashIndex + 1);
        }
        return fullNormalized;
    }
}
