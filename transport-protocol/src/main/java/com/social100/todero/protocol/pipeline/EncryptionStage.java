package com.social100.todero.protocol.pipeline;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptionStage implements PipelineStage {
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;       // AES key size (128, 192, 256 bits)
    private static final int IV_SIZE = 12;         // Recommended IV size for GCM
    private static final int TAG_LENGTH = 128;     // GCM tag length in bits

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> peerKeys = new ConcurrentHashMap<>();

    public EncryptionStage() {
    }

    public EncryptionStage(String base64Key, String defaultPeerId) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        SecretKey key = new SecretKeySpec(decodedKey, "AES");
        peerKeys.put(defaultPeerId, key);
    }

    public void setKeyForPeer(String peerId, SecretKey key) {
        peerKeys.put(peerId, key);
    }

    public String getEncodedKey(String peerId) {
        SecretKey key = peerKeys.get(peerId);
        return key != null ? Base64.getEncoder().encodeToString(key.getEncoded()) : null;
    }

    private SecretKey getKey(String peerId) {
        SecretKey key = peerKeys.get(peerId);
        if (key == null) {
            throw new IllegalStateException("No encryption key set for peer: " + peerId);
        }
        return key;
    }

    @Override
    public byte[] processToSend(byte[] message, String destinationId) {
        if (message == null) message = new byte[0];

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(destinationId), gcmSpec);

            byte[] ciphertext = cipher.doFinal(message);

            byte[] encryptedMessage = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedMessage, iv.length, ciphertext.length);

            return encryptedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed for peer: " + destinationId, e);
        }
    }

    @Override
    public byte[] processToReceive(byte[] message, String sourceId) {
        if (message == null || message.length < IV_SIZE) {
            throw new IllegalArgumentException("Invalid ciphertext from peer: " + sourceId);
        }

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(message, 0, iv, 0, IV_SIZE);

            byte[] ciphertext = new byte[message.length - IV_SIZE];
            System.arraycopy(message, IV_SIZE, ciphertext, 0, ciphertext.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(sourceId), gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed for peer: " + sourceId, e);
        }
    }
}

