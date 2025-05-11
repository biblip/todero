package com.social100.todero.protocol.examples.udp;

import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtocolServer {

    public static void main(String[] args) {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage, responder) -> {
            // Respond back to the sender
            try {
                System.out.println("Server Receive Message : packetId: " + receivedMessage.getMessageId() + " > " + new String(receivedMessage.getPayload()));
                int packetId = responder.sendMessage("Echo............... 1".getBytes(StandardCharsets.UTF_8), true);
                System.out.println("Sending Message to Client packetId: " + packetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            // Data transport listens on port 9876
            UdpTransport dataTraffic = new UdpTransport(9876);

            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());

            ProtocolEngine engine = new ProtocolEngine(dataTraffic, pipeline);

            engine.startServer(receiveMessageCallback);

            System.out.println("Server is running and ready to receive messages...");

            // Keep the server running indefinitely
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}