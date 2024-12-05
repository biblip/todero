package com.social100.todero.aiaserver;

import com.social100.todero.console.base.CliCommandManager;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.io.IOException;
import java.util.function.Consumer;

public class AIAServer {
    private final CliCommandManager commandManager;

    public AIAServer(AppConfig appConfig) {
        commandManager = new CliCommandManager(appConfig);
    }

    public void start() {

        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            String line = receivedMessage.getPayload();

            String outputLine = commandManager.execute(line);
            try {
                if (!outputLine.isEmpty()) {
                    responder.sendMessage(outputLine.replace("\n", "\r\n"), true);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Consumer<Integer> ackSendMessageCallback = (packetId) -> {
            //System.out.println("Server Confirmed Message packetId: " + packetId);
        };

        try {
            // Data transport listens on port 9876
            UdpTransport dataTraffic = new UdpTransport(9876);

            // ACK transport uses any available port (0)
            UdpTransport transportTraffic = new UdpTransport(0);

            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            ProtocolEngine engine = new ProtocolEngine(dataTraffic, transportTraffic, pipeline);

            engine.startServer(receiveMessageCallback, ackSendMessageCallback);

            System.out.println("Server is running and ready to receive messages...");

            // Keep the server running indefinitely
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
