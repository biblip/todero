package com.social100.todero.protocol.security;

public class HandshakeMessage {
  public long timestamp;
  public String nonce;
  public byte[] ephemeralPublicKey;
  public Certificate certificate;

  public HandshakeMessage(long timestamp, String nonce, byte[] ephemeralPublicKey, Certificate cert) {
    this.timestamp = timestamp;
    this.nonce = nonce;
    this.ephemeralPublicKey = ephemeralPublicKey;
    this.certificate = cert;
  }
}
