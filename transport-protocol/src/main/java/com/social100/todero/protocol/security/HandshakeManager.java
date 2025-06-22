package com.social100.todero.protocol.security;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class HandshakeManager {
  private final KeyPair ephemeralKeyPair;
  private final KeyAgreement keyAgreement;
  private SecretKey sharedSecret;

  public HandshakeManager() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
    kpg.initialize(2048);
    this.ephemeralKeyPair = kpg.generateKeyPair();

    this.keyAgreement = KeyAgreement.getInstance("DH");
    this.keyAgreement.init(ephemeralKeyPair.getPrivate());
  }

  public byte[] getEphemeralPublicKey() {
    return ephemeralKeyPair.getPublic().getEncoded();
  }

  public void receivePeerPublicKey(byte[] peerEncodedPubKey) throws Exception {
    KeyFactory kf = KeyFactory.getInstance("DH");
    PublicKey peerKey = kf.generatePublic(new X509EncodedKeySpec(peerEncodedPubKey));
    keyAgreement.doPhase(peerKey, true);

    byte[] secret = keyAgreement.generateSecret();
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] hashed = sha256.digest(secret);
    this.sharedSecret = new SecretKeySpec(hashed, 0, 32, "AES");
  }

  public SecretKey getSharedSecret() {
    return sharedSecret;
  }
}
