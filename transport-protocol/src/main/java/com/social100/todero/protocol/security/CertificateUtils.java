package com.social100.todero.protocol.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class CertificateUtils {

  public static Certificate createCertificate(String identity, KeyPair identityKeyPair, byte[] ephemeralPublicKey) throws Exception {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(identityKeyPair.getPrivate());
    sig.update(ephemeralPublicKey);
    byte[] signature = sig.sign();
    return new Certificate(identity, identityKeyPair.getPublic().getEncoded(), signature);
  }

  public static boolean verifyCertificate(Certificate cert, byte[] ephemeralPubKey) throws Exception {
    Signature sig = Signature.getInstance("SHA256withRSA");
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(cert.publicKey));
    sig.initVerify(pub);
    sig.update(ephemeralPubKey);
    return sig.verify(cert.signature);
  }

  public static KeyPair generateRsaKeyPair() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    return gen.generateKeyPair();
  }
}
