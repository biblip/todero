package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;
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
import java.util.Optional;
import java.util.function.Consumer;

public class AiaCommandProcessor implements CommandProcessor {
    private final AppConfig appConfig;
    private PipelineStreamBridge bridge;
    // Data transport
    UdpTransport dataTraffic;
    // ACK transport
    UdpTransport transportTraffic;
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

    ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage) -> {
        if (Optional.ofNullable(this.bridge).isPresent()) {
            this.bridge.writeAsync(receivedMessage.getPayload().getBytes());
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

            // ACK transport uses any available port for transport (0)
            transportTraffic = new UdpTransport(0);

            pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            engine = new ProtocolEngine(dataTraffic, transportTraffic, pipeline);
            engine.startClient(receiveMessageCallback, ackSendMessageCallback);

            serverAddress = new InetSocketAddress("localhost", 9876);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(String line) {
        try {
            engine.sendMessage(serverAddress, line, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        String line = new String(data);
        process(line); // Process the received data as if it were passed to `process(String line)`
    }
}
