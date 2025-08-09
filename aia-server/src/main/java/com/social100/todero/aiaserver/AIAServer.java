package com.social100.todero.aiaserver;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.MessageContainerUtils;
import com.social100.todero.console.base.CliCommandManager;
import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.core.ResponderRegistry;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;
import com.social100.todero.server.RawServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AIAServer implements RawServer {
    private final CliCommandManager commandManager;
    private ProtocolEngine engine;
    private final Integer port;
    Map<String, ResponderRegistry.Responder> profileResponderMap = new HashMap<>();

    public AIAServer(AppConfig appConfig, ServerType type) {
        ServerConfig server = ServerType.AI.equals(type) ?
            appConfig.getApp().getAi_server() : appConfig.getApp().getAia_server();
        port = server.getPort();
        commandManager = new CliCommandManager(appConfig, type, (eventName, message) -> {
            ResponderRegistry.Responder responder = engine.getResponder(message.getResponderId());
            try {
                responder.sendMessage(MessageContainerUtils.serialize(message).getBytes(StandardCharsets.UTF_8), true);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    @Override
    public void start() {

        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            byte[] message = receivedMessage.getPayload();
            String line = new String(message);
            MessageContainer receivedMessageContainer = MessageContainerUtils.deserialize(line);
            MessageContainer messageContainer = MessageContainer.builder()
                    .responderId(receivedMessage.getResponderId())
                    .addAllMessages(receivedMessageContainer.getMessages())
                    .build();
            commandManager.process(messageContainer);
        });

        try {
            // Data transport listens on port 'port'
            UdpTransport dataTraffic = new UdpTransport(port);

            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            engine = new ProtocolEngine(dataTraffic, pipeline);

            engine.startServer(receiveMessageCallback);

            System.out.println("Server is running and ready to receive messages...");

            // Keep the server running indefinitely
            //Thread.currentThread().join();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
