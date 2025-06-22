package com.social100.todero.protocol.security;

public class Certificate {
  public String identity;
  public byte[] publicKey;     // long-term public key
  public byte[] signature;     // signature over ephemeralPublicKey

  public Certificate(String identity, byte[] publicKey, byte[] signature) {
    this.identity = identity;
    this.publicKey = publicKey;
    this.signature = signature;
  }
}
