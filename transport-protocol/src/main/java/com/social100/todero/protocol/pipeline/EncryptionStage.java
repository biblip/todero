package com.social100.todero.protocol.pipeline;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionStage implements PipelineStage {
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES key size (128, 192, or 256 bits)
    private static final int IV_SIZE = 12;   // Recommended IV size for GCM (12 bytes)
    private static final int TAG_LENGTH = 128; // Authentication tag length (in bits)

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    // Default constructor: generate a new AES key
    public EncryptionStage() {
        this.secretKey = generateKey();
    }

    // Constructor to initialize with a pre-shared Base64-encoded key
    public EncryptionStage(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    @Override
    public byte[] processToSend(byte[] message) {
        if (message == null) {
            message = new byte[0];
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            // Generate a random IV
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(message);

            // Combine IV + ciphertext
            byte[] encryptedMessage = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedMessage, iv.length, ciphertext.length);

            // Return encrypted bytes.
            // (If you want them Base64-encoded, you'd do that externally, or store raw bytes.)
            return encryptedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public byte[] processToReceive(byte[] message) {
        if (message == null || message.length < IV_SIZE) {
            throw new IllegalArgumentException("Invalid ciphertext");
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            // Extract IV (first 12 bytes)
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(message, 0, iv, 0, IV_SIZE);

            // The rest is the actual ciphertext
            byte[] ciphertext = new byte[message.length - IV_SIZE];
            System.arraycopy(message, IV_SIZE, ciphertext, 0, ciphertext.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // Generate a new AES key
    private static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, new SecureRandom());
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }

    // Return the Base64-encoded version of the key for external storage/sharing
    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}
