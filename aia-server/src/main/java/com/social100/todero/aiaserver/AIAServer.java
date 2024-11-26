package com.social100.todero.aiaserver;

import com.social100.todero.aiaserver.config.AIAServerConfig;
import com.social100.todero.cli.base.CommandManager;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIAServer {
    private final AIAServerConfig tcpServerConfig;
    private final CommandManager commandManager;

    public AIAServer(AppConfig appConfig) {
        tcpServerConfig = new AIAServerConfig(appConfig);
        commandManager = new CommandManager(appConfig);
    }

    public void start() {

        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            String line;
            Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

            line = receivedMessage.getPayload();

            Matcher matcher = pattern.matcher(line);
            ArrayList<String> arguments = new ArrayList<>();

            while (matcher.find()) {
                arguments.add(matcher.group(1).replace("\"", ""));
            }

            if (!arguments.isEmpty()) {
                String firstParam = arguments.remove(0);
                String secondParam = null;
                String[] commandArgs = {};
                if (!arguments.isEmpty()) {
                    secondParam = arguments.remove(0);
                }
                if (!arguments.isEmpty()) {
                    commandArgs = arguments.toArray(new String[0]);
                }
                String outputLine = commandManager.execute(firstParam, secondParam, commandArgs);
                try {
                    responder.sendMessage(outputLine.replace("\n", "\r\n"), true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
