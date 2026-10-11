package com.dataflow.auth.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service mã hóa và giải mã dữ liệu nhạy cảm sử dụng chuẩn AES-256-GCM.
 * Chuẩn Galois/Counter Mode (GCM) cung cấp cả tính bảo mật (Confidentiality)
 * và tính toàn vẹn (Integrity / Authenticated Encryption).
 */
@Component
@Slf4j
public class AesGcmEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // 16 bytes auth tag
    private static final int IV_LENGTH_BYTE = 12;  // 12 bytes IV chuẩn khuyến nghị cho GCM

    @Value("${app.security.aes-gcm.secret-key:DataFlowPlatformAES256GCMSecretKey2026!}")
    private String secretKeyString;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        byte[] validKey256 = new byte[32]; // 256 bits = 32 bytes
        System.arraycopy(keyBytes, 0, validKey256, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(validKey256, "AES");
    }

    /**
     * Mã hóa chuỗi plaintext sang định dạng Base64 chứa: [12-byte IV] + [Ciphertext + AuthTag].
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Ghép [IV (12 bytes)] + [Ciphertext + Tag]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return "ENC:" + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Lỗi khi mã hóa dữ liệu với AES-GCM: {}", e.getMessage());
            throw new RuntimeException("Mã hóa dữ liệu thất bại", e);
        }
    }

    /**
     * Giải mã chuỗi Base64 trả về plaintext gốc.
     * Hỗ trợ tương thích ngược nếu chuỗi chưa được mã hóa bằng tiền tố "ENC:".
     */
    public String decrypt(String cipherTextWithPrefix) {
        if (cipherTextWithPrefix == null || cipherTextWithPrefix.isEmpty()) {
            return cipherTextWithPrefix;
        }

        // Tương thích ngược: nếu không có tiền tố ENC: thì trả về trực tiếp
        if (!cipherTextWithPrefix.startsWith("ENC:")) {
            return cipherTextWithPrefix;
        }

        try {
            String base64Data = cipherTextWithPrefix.substring(4);
            byte[] decoded = Base64.getDecoder().decode(base64Data);

            if (decoded.length < IV_LENGTH_BYTE) {
                return cipherTextWithPrefix;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Lỗi khi giải mã dữ liệu AES-GCM: {}", e.getMessage());
            return cipherTextWithPrefix; // Fallback an toàn nếu giải mã không thành công
        }
    }
}
