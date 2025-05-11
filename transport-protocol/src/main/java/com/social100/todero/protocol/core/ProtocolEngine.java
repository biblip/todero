package com.social100.todero.protocol.core;

import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.TransportInterface;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolEngine {
    private final TransportInterface transport;
    private final ResponderRegistry responderRegistry = new ResponderRegistry();
    private final Pipeline pipeline;
    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ProtocolEngine(TransportInterface transport, Pipeline pipeline) {
        this.transport = transport;
        this.pipeline = pipeline;
    }

    public void startClient(ReceiveMessageCallback messageCallback) throws IOException {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((protocolMessage, responder) -> {
            messageCallback.consume(protocolMessage);
        });
        startServer( receiveMessageCallback);
    }

    public void startServer(ReceiveMessageCallback messageCallback) throws IOException {
        transport.startReceiving((source, data) -> executor.submit(() -> {
            try {
                ResponderRegistry.Responder responder = responderRegistry.useResponder(source, this);
                ProtocolFrameManager.FrameMessage frame = ProtocolFrameManager.deserialize(
                        data, responder.getId(), this::processReceivedMessage);

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
        int packetId = packetIdGenerator.getAndIncrement();
        ProtocolFrameManager.FrameMessage frame = ProtocolFrameManager.FrameMessage.builder()
                .messageId(packetId)
                .payload(prepareMessageForSending(message))
                .ackRequested(ackRequested)
                .build();

        transport.sendMessage(frame, destination);
        return packetId;
    }

    public byte[] prepareMessageForSending(byte[] message) {
        return pipeline != null ? pipeline.processToSend(message) : message;
    }

    public byte[] processReceivedMessage(byte[] message) {
        return pipeline != null ? pipeline.processToReceive(message) : message;
    }

    public ResponderRegistry.Responder getResponder(String id) {
        return responderRegistry.getResponder(id);
    }

    public void close() throws IOException {
        executor.shutdownNow();
        transport.close();
    }
}
