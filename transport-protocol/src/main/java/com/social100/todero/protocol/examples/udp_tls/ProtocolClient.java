package com.social100.todero.protocol.examples.udp_tls;


import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.security.CertificateUtils;
import com.social100.todero.protocol.transport.UdpTransport;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Scanner;

public class ProtocolClient {
  public static void main(String[] args) {
    try {
      // Generate or load long-term identity keypair
      KeyPair clientIdentity = CertificateUtils.generateRsaKeyPair();

      // Build pipeline without key (will be set after handshake)
      Pipeline pipeline = new Pipeline();
      pipeline.addStage(new CompressionStage());
      pipeline.addStage(new EncryptionStage());  // No key yet
      pipeline.addStage(new ChecksumStage());

      UdpTransport transport = new UdpTransport(0);

      ProtocolEngine engine = new ProtocolEngine(
          transport,
          pipeline,
          true,                         // enable TLS-like security
          "client-1",                   // identity label
          clientIdentity                // identity keypair
      );

      ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((receivedMessage) -> {
        System.out.println("Client Receive Message: " + new String(receivedMessage.getPayload()));
      });

      engine.startClient(receiveMessageCallback);

      InetSocketAddress serverAddr = new InetSocketAddress("localhost", 9877);

      Scanner scanner = new Scanner(System.in);
      while (true) {
        String input = "Hello Server!";
        int id = engine.sendMessage(serverAddr, input.getBytes(StandardCharsets.UTF_8), true);
        System.out.println("Sent packetId: " + id);
        scanner.nextLine(); // wait for user input to send again
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
