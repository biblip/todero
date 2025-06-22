package com.social100.todero.protocol.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HandshakeUtils {

  private HandshakeUtils() {
  }

  public static byte[] serializeHandshake(HandshakeMessage m) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeLong(m.timestamp);
    out.writeUTF(m.nonce);
    out.writeInt(m.ephemeralPublicKey.length);
    out.write(m.ephemeralPublicKey);
    if (m.certificate != null) {
      out.writeBoolean(true);
      out.writeUTF(m.certificate.identity);
      out.writeInt(m.certificate.publicKey.length);
      out.write(m.certificate.publicKey);
      out.writeInt(m.certificate.signature.length);
      out.write(m.certificate.signature);
    } else {
      out.writeBoolean(false);
    }
    //out.writeUTF(m.peerId);
    return baos.toByteArray();
  }

  public static HandshakeMessage deserializeHandshake(byte[] data) throws IOException {
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    long ts = in.readLong();
    String nonce = in.readUTF();
    byte[] epk = new byte[in.readInt()];
    in.readFully(epk);
    Certificate cert = null;
    if (in.readBoolean()) {
      String id = in.readUTF();
      byte[] pk = new byte[in.readInt()];
      in.readFully(pk);
      byte[] sig = new byte[in.readInt()];
      in.readFully(sig);
      cert = new Certificate(id, pk, sig);
    }
    //String peerId = in.readUTF();
    return new HandshakeMessage(ts, nonce, epk, cert);
  }
}
