package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.MessageContainerUtils;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.security.CertificateUtils;
import com.social100.todero.protocol.transport.UdpTransport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Optional;

public class AiaCommandProcessor implements CommandProcessor {
    private EventChannel.EventListener eventListener = null;
    // Data transport
    UdpTransport dataTraffic;
    // Pipeline
    Pipeline pipeline = new Pipeline();
    // Protocol Engine
    ProtocolEngine engine;
    // Server Address
    InetSocketAddress serverAddress;

    public AiaCommandProcessor(InetSocketAddress serverAddress, EventChannel.EventListener eventListener) {
        this.serverAddress = serverAddress;
        this.eventListener = eventListener;
    }

    ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback(frameMessage -> {
        byte[] data = frameMessage.getPayload();
        String json = new String(data);
        MessageContainer messageContainer = MessageContainerUtils.deserialize(json);

        this.eventListener.onEvent("command", messageContainer);

    });

    @Override
    public void open() {
        try {
            dataTraffic = new UdpTransport(0);

            pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage());
            pipeline.addStage(new ChecksumStage());

            KeyPair clientIdentity = CertificateUtils.generateRsaKeyPair();

            engine = new ProtocolEngine(
                dataTraffic,
                pipeline,
                true,
                "aia-client",
                clientIdentity);
            engine.startClient(receiveMessageCallback);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(MessageContainer messageContainer) {
        return Optional.ofNullable(MessageContainerUtils.serialize(messageContainer))
                .map(serializedMessage -> {
                    try {
                        engine.sendMessage(serverAddress,
                                serializedMessage.getBytes(StandardCharsets.UTF_8),
                                true);
                        return true;  // success
                    } catch (Exception e) {
                        return false; // failure
                    }
                })
                .orElse(false);
    }

    @Override
    public void close() throws IOException {
        this.engine.close();
    }

    @Override
    public CliCommandManager getCommandManager() {
        return null;
    }

}
