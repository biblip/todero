package com.social100.todero.cli.base;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AiaCommandProcessor implements CommandProcessor {
    private final CommandManager commandManager;
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
        this.commandManager = new CommandManager(appConfig);
    }

    ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage) -> {
        System.out.println(receivedMessage.getPayload());
    });

    Consumer<Integer> ackSendMessageCallback = (packetId) -> {
        //System.out.println("Server Confirmed Message packetId: " + packetId);
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
    public void process(Pattern pattern, String line) {
        try {
            int packetId = engine.sendMessage(serverAddress, line, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        this.commandManager.terminate();
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
