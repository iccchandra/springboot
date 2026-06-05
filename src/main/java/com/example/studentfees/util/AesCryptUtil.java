package com.example.studentfees.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * CCAvenue AES encryption helper.
 *
 * This is byte-for-byte compatible with CCAvenue's official Java integration kit
 * AND with the Node.js {@code crypto.service.ts} used in the hdma-backend:
 *   - key  = MD5(workingKey)            (16 bytes -> AES-128)
 *   - iv   = 0x00 0x01 ... 0x0f         (fixed 16-byte IV)
 *   - algo = AES/CBC/PKCS5Padding
 *   - wire format = lowercase hex string
 *
 * Because the algorithm is identical, the SAME working key produces the SAME
 * ciphertext as the existing backend, so existing CCAvenue credentials work as-is.
 */
public class AesCryptUtil {

    private static final byte[] IV = new byte[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
    };

    private final SecretKeySpec secretKey;
    private final IvParameterSpec ivSpec;

    public AesCryptUtil(String workingKey) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] keyBytes = md5.digest(workingKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            this.ivSpec = new IvParameterSpec(IV);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise CCAvenue cipher", e);
        }
    }

    /** Encrypt a plaintext (CCAvenue query string) -> lowercase hex. */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /** Decrypt a lowercase-hex CCAvenue response -> plaintext query string. */
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(hexToBytes(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed: " + e.getMessage(), e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        hex = hex.trim();
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
