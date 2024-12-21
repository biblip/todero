package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.MessageContainerUtils;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;
import com.social100.todero.stream.PipelineStreamBridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

public class AiaCommandProcessor implements CommandProcessor {
    private final AppConfig appConfig;
    private PipelineStreamBridge bridge;
    // Data transport
    UdpTransport dataTraffic;
    // Pipeline
    Pipeline pipeline = new Pipeline();
    // Protocol Engine
    ProtocolEngine engine;
    // Server Address
    InetSocketAddress serverAddress;

    public AiaCommandProcessor(AppConfig appConfig) {
        this.appConfig = appConfig;

        // Build the bridge with an internal onReceive handler
        try {
            this.bridge = new PipelineStreamBridge.Builder()
                    .onReceive(this::handleIncomingData)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize PipelineStreamBridge", e);
        }
    }

    ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback(frameMessage -> {
        System.out.println("receiveMessageCallback");
        if (Optional.ofNullable(this.bridge).isPresent()) {
            System.out.println("Llego algo en receiveMessageCallback: " + frameMessage.getPayload());
            this.bridge.writeAsync(frameMessage.getPayload());
        }
    });

    Consumer<Integer> ackSendMessageCallback = (packetId) -> {
        // Do nothing
    };

    @Override
    public void open() {
        try {
            // Data transport uses any available port (0)
            dataTraffic = new UdpTransport(0);

            pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            engine = new ProtocolEngine(dataTraffic, pipeline);
            engine.startClient(receiveMessageCallback, ackSendMessageCallback);

            serverAddress = new InetSocketAddress("localhost", 9876);
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
    public void close() {
        this.engine.close();
        this.bridge.close();
    }

    @Override
    public CliCommandManager getCommandManager() {
        return null;
    }

    @Override
    public PipelineStreamBridge.PipelineStreamBridgeShadow getBridge() {
        return this.bridge.getBridge();
    }

    @Override
    public void handleIncomingData(byte[] data) {
        // TODO: this data contains already a MessageContainer?
        String line = new String(data);
        System.out.println("TODO: this data contains already a MessageContainer?");
        System.out.println(line);
        MessageContainer messageContainer = MessageContainer.builder()
                .addChannelMessage(ChannelMessage.builder()
                        .channel(ChannelType.PUBLIC_DATA)
                        .payload(PublicDataPayload.builder()
                                .message(line)
                                .build())
                        .build())
                .build();
        process(messageContainer); // Process the received data as if it were passed to `process(String line)`
    }
}
