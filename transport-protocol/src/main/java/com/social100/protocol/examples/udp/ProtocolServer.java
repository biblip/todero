package com.social100.protocol.examples.udp;

import com.social100.protocol.core.ProtocolEngine;
import com.social100.protocol.core.ProtocolMessage;
import com.social100.protocol.core.ReceiveMessageCallback;
import com.social100.protocol.core.Responder;
import com.social100.protocol.pipeline.ChecksumStage;
import com.social100.protocol.pipeline.CompressionStage;
import com.social100.protocol.pipeline.EncryptionStage;
import com.social100.protocol.pipeline.Pipeline;
import com.social100.protocol.transport.UdpTransport;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProtocolServer {

    public static void main(String[] args) {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            // Respond back to the sender
            try {
                System.out.println("Server Receive Message : packetId: " + receivedMessage.getPacketId() + " > " + receivedMessage.getPayload());
                int packetId = responder.sendMessage("Echo............... 1", true);
                System.out.println("Sending Message to Client packetId: " + packetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        BiConsumer<ProtocolMessage, Responder> receiverCallback = (receivedMessage, responder) -> {
            // Respond back to the sender
            try {
                System.out.println("Server Receive Message : packetId: " + receivedMessage.getPacketId() + " > " + receivedMessage.getPayload());
                int packetId = responder.sendMessage("Echo............... 1", true);
                System.out.println("Sending Message to Client packetId: " + packetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Consumer<Integer> ackSendMessageCallback = (packetId) -> {
            System.out.println("Server Confirmed Message packetId: " + packetId);
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