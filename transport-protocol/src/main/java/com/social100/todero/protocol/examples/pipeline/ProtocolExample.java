package com.social100.todero.protocol.examples.pipeline;

import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.UdpTransport;

import java.io.IOException;

public class ProtocolExample {
    public static void main(String[] args) {
        try {
            Pipeline pipeline = new Pipeline();
            pipeline.addStage(new CompressionStage());
            pipeline.addStage(new EncryptionStage());
            pipeline.addStage(new ChecksumStage());

            // Data transport uses any available port (0)
            UdpTransport dataTransport = new UdpTransport(0);

            // ACK transport binds to a specific port to receive ACKs
            UdpTransport ackTransport = new UdpTransport(9000);

            ProtocolEngine engine = new ProtocolEngine(dataTransport, pipeline);
            // engine.start(); // No need to start the engine's selector loop

            String originalMessage = "Hello, Secure World!";
            System.out.println("Original Message: " + originalMessage);
            String preparedMessage = engine.prepareMessageForSending(originalMessage);
            System.out.println("Prepared Message: " + preparedMessage);

            String receivedMessage = engine.processReceivedMessage(preparedMessage);
            System.out.println("Received Message: " + receivedMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

