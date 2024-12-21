package com.social100.todero.aiaserver;

import com.social100.todero.common.config.AppConfig;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AIAServer {
    private final CliCommandManager commandManager;
    private ProtocolEngine engine;
    Map<String, ResponderRegistry.Responder> profileResponderMap = new HashMap<>();

    public AIAServer(AppConfig appConfig) {
        commandManager = new CliCommandManager(appConfig, (eventName, message) -> {
            ResponderRegistry.Responder responder = engine.getResponder(message.getResponderId());
            try {
                responder.sendMessage(MessageContainerUtils.serialize(message).getBytes(StandardCharsets.UTF_8), true);
                System.out.println("CliCommandManager RESPONSE: >" + eventName + " --> " + MessageContainerUtils.serialize(message));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public void start() {

        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            byte[] message = receivedMessage.getPayload();
            String line = new String(message);
            System.out.println(line);
            MessageContainer receivedMessageContainer = MessageContainerUtils.deserialize(line);
            MessageContainer messageContainer = MessageContainer.builder()
                    .responderId(receivedMessage.getResponderId())
                    .addAllMessages(receivedMessageContainer.getMessages())
                    .build();
            // TODO: el siguiente response puede cambiar a una respuesta en linea, ya que solo retorna Booleano.

            // TODO: el messageContainer debe contener el receivedMessage.getResponderId()

            commandManager.process(messageContainer, response -> {
                if (!response.isEmpty()) {
                    try {
                        responder.sendMessage(response.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8), true);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        });

        Consumer<Integer> ackSendMessageCallback = (packetId) -> {
            //System.out.println("Server Confirmed Message packetId: " + packetId);
        };

        try {
            // Data transport listens on port 9876
            UdpTransport dataTraffic = new UdpTransport(9876);

            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            engine = new ProtocolEngine(dataTraffic, pipeline);

            engine.startServer(receiveMessageCallback, ackSendMessageCallback);

            System.out.println("Server is running and ready to receive messages...");

            // Keep the server running indefinitely
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
