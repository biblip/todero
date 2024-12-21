package com.social100.todero.protocol.examples.udp;

import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

public class ProtocolClient {
    public static void main(String[] args) {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage) -> {
            System.out.println("Client Receive Message: " + receivedMessage.getPayload());
        });

        Consumer<Integer> ackSendMessageCallback = (packetId) -> {
            System.out.println("Server Confirmed Message packetId: " + packetId);
        };

        try {
            // Data transport uses any available port (0)
            UdpTransport dataTraffic = new UdpTransport(0);

            // ACK transport uses any available port for transport (0)
            UdpTransport transportTraffic = new UdpTransport(0);

            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage("1tNXAlS+bFUZWyEpQI2fAUjKtyXHsUTgBVecFad98LY="));
            pipeline.addStage(new ChecksumStage());


            ProtocolEngine engine = new ProtocolEngine(dataTraffic, pipeline);
            engine.startClient(receiveMessageCallback, ackSendMessageCallback);

            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 9876);

            while (true) {
                int packetId = engine.sendMessage(serverAddress, "Hello, Server!".getBytes(StandardCharsets.UTF_8), true);
                System.out.println("Sending Message to Server packetId: " + packetId);

                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}