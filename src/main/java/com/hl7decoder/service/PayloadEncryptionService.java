package com.hl7decoder.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PayloadEncryptionService {
    private static final String PREFIX = "aesgcm";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String activeKeyId;
    private final SecretKeySpec activeKey;

    public PayloadEncryptionService(@Value("${app.encryption-key:local-dev-key-change-me}") String keyMaterial,
                                    @Value("${app.encryption-key-id:v1}") String activeKeyId) {
        this.activeKeyId = activeKeyId == null || activeKeyId.isBlank() ? "v1" : activeKeyId.trim();
        this.activeKey = new SecretKeySpec(sha256(keyMaterial), "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal((plainText == null ? "" : plainText).getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + ":" + activeKeyId + ":" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Saved payload could not be encrypted.", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.startsWith(PREFIX + ":")) {
            return cipherText;
        }
        try {
            String[] parts = cipherText.split(":", 3);
            byte[] combined = Base64.getDecoder().decode(parts[2]);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, activeKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Saved payload could not be decrypted with the active managed key.", ex);
        }
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Encryption key could not be initialized.", ex);
        }
    }
}
