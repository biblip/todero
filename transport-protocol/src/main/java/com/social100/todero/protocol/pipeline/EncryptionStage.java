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
    private static final int IV_SIZE = 12; // Recommended IV size for GCM (12 bytes)
    private static final int TAG_LENGTH = 128; // Authentication tag length (128 bits)

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    // Constructor to initialize with a securely generated key
    public EncryptionStage() {
        this.secretKey = generateKey();
    }

    // Constructor to initialize with a pre-shared key (Base64-encoded)
    public EncryptionStage(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Process message for sending: encrypts the plaintext.
     */
    @Override
    public String processToSend(String message) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv); // Generate a random IV
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(message.getBytes("UTF-8"));

            // Combine IV and ciphertext and encode in Base64
            byte[] encryptedMessage = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedMessage, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encryptedMessage);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Process message for receiving: decrypts the ciphertext.
     */
    @Override
    public String processToReceive(String message) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            byte[] encryptedMessage = Base64.getDecoder().decode(message);

            // Extract IV and ciphertext
            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[encryptedMessage.length - IV_SIZE];
            System.arraycopy(encryptedMessage, 0, iv, 0, IV_SIZE);
            System.arraycopy(encryptedMessage, IV_SIZE, ciphertext, 0, ciphertext.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a new AES key.
     */
    private static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, new SecureRandom());
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Get the Base64-encoded version of the key.
     */
    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}

