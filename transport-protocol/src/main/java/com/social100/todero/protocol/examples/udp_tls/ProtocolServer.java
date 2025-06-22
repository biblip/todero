package com.social100.todero.protocol.examples.udp_tls;

import com.social100.todero.protocol.core.ProtocolEngine;
import com.social100.todero.protocol.core.ReceiveMessageCallback;
import com.social100.todero.protocol.pipeline.ChecksumStage;
import com.social100.todero.protocol.pipeline.CompressionStage;
import com.social100.todero.protocol.pipeline.EncryptionStage;
import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.security.CertificateUtils;
import com.social100.todero.protocol.transport.UdpTransport;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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
      // Generate or load long-term identity keypair
      KeyPair serverIdentity = CertificateUtils.generateRsaKeyPair();

      // Build pipeline without key (TLS will inject it)
      Pipeline pipeline = new Pipeline();
      pipeline.addStage(new CompressionStage());
      pipeline.addStage(new EncryptionStage());  // no key yet
      pipeline.addStage(new ChecksumStage());

      UdpTransport transport = new UdpTransport(9877);

      ProtocolEngine engine = new ProtocolEngine(
          transport,
          pipeline,
          true,                          // enable TLS-like security
          "server-1",                    // identity label
          serverIdentity                 // identity keypair
      );

      engine.startServer(receiveMessageCallback);

      System.out.println("Server ready.");
      Thread.currentThread().join();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
