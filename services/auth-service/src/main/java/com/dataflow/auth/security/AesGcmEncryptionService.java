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

    private final org.springframework.beans.factory.ObjectProvider<VaultSecretService> vaultSecretServiceProvider;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private boolean vaultSource = false;

    public AesGcmEncryptionService(org.springframework.beans.factory.ObjectProvider<VaultSecretService> vaultSecretServiceProvider) {
        this.vaultSecretServiceProvider = vaultSecretServiceProvider;
    }

    @PostConstruct
    public void init() {
        String activeKey = null;
        VaultSecretService vaultSecretService = vaultSecretServiceProvider.getIfAvailable();
        if (vaultSecretService != null) {
            java.util.Optional<String> vaultKey = vaultSecretService.getSecretKey("aes_gcm_key");
            if (vaultKey.isPresent() && !vaultKey.get().trim().isEmpty()) {
                activeKey = vaultKey.get().trim();
                this.vaultSource = true;
                log.info("[AesGcmEncryptionService] Loaded AES-256-GCM master key directly from HashiCorp Vault!");
            }
        }

        if (activeKey == null) {
            log.info("[AesGcmEncryptionService] Using fallback AES-256-GCM secret key from application configuration.");
            activeKey = secretKeyString;
            this.vaultSource = false;
        }

        byte[] keyBytes = activeKey.getBytes(StandardCharsets.UTF_8);
        byte[] validKey256 = new byte[32]; // 256 bits = 32 bytes
        System.arraycopy(keyBytes, 0, validKey256, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(validKey256, "AES");
    }

    public boolean isVaultSource() {
        return this.vaultSource;
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

    /**
     * Mã hóa chi tiết trả về các thành phần mật mã phục vụ hiển thị trực quan.
     */
    public com.dataflow.auth.dto.CryptoDto.EncryptResult encryptDetailed(String plainText) {
        if (plainText == null) plainText = "";

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Trong GCM của SunJCE: 16 bytes cuối là Auth Tag (128-bit)
            int tagLen = 16;
            int payloadLen = cipherTextWithTag.length - tagLen;
            byte[] payload = new byte[payloadLen];
            byte[] authTag = new byte[tagLen];
            System.arraycopy(cipherTextWithTag, 0, payload, 0, payloadLen);
            System.arraycopy(cipherTextWithTag, payloadLen, authTag, 0, tagLen);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherTextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherTextWithTag);

            String formattedEnc = "ENC:" + Base64.getEncoder().encodeToString(byteBuffer.array());

            return com.dataflow.auth.dto.CryptoDto.EncryptResult.builder()
                    .cipherText(formattedEnc)
                    .ivHex(bytesToHex(iv))
                    .authTagHex(bytesToHex(authTag))
                    .cipherPayloadHex(bytesToHex(payload))
                    .algorithm("AES-256-GCM")
                    .keySizeBits(256)
                    .vaultSource(this.vaultSource)
                    .keyStatus(this.vaultSource ? "HashiCorp Vault (Online)" : "Fallback Configuration Key")
                    .build();
        } catch (Exception e) {
            log.error("Chi tiết mã hóa thất bại: {}", e.getMessage());
            throw new RuntimeException("Mã hóa chi tiết thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Giải mã chi tiết và kiểm tra tính toàn vẹn (phát hiện giả mạo qua GCM Auth Tag).
     */
    public com.dataflow.auth.dto.CryptoDto.DecryptResult decryptDetailed(String cipherTextWithPrefix) {
        if (cipherTextWithPrefix == null || cipherTextWithPrefix.isBlank()) {
            return com.dataflow.auth.dto.CryptoDto.DecryptResult.builder()
                    .success(false)
                    .message("Chuỗi mã hóa không được để trống!")
                    .build();
        }

        String dataToDecode = cipherTextWithPrefix.trim();
        if (dataToDecode.startsWith("ENC:")) {
            dataToDecode = dataToDecode.substring(4);
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dataToDecode);
            if (decoded.length < IV_LENGTH_BYTE + 16) {
                return com.dataflow.auth.dto.CryptoDto.DecryptResult.builder()
                        .success(false)
                        .message("Độ dài dữ liệu không hợp lệ (nhỏ hơn 28 bytes cho IV + AuthTag).")
                        .build();
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherTextWithTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherTextWithTag);

            int tagLen = 16;
            int payloadLen = cipherTextWithTag.length - tagLen;
            byte[] authTag = new byte[tagLen];
            System.arraycopy(cipherTextWithTag, payloadLen, authTag, 0, tagLen);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherTextWithTag);
            String plainText = new String(plainTextBytes, StandardCharsets.UTF_8);

            return com.dataflow.auth.dto.CryptoDto.DecryptResult.builder()
                    .success(true)
                    .plainText(plainText)
                    .tampered(false)
                    .ivHex(bytesToHex(iv))
                    .authTagHex(bytesToHex(authTag))
                    .algorithm("AES-256-GCM")
                    .vaultSource(this.vaultSource)
                    .message("Giải mã thành công! Xác thực toàn vẹn Auth Tag (AEAD) hợp lệ 100%.")
                    .build();
        } catch (javax.crypto.AEADBadTagException e) {
            log.warn("Phát hiện dữ liệu mã hóa bị giả mạo / Auth Tag mismatch: {}", e.getMessage());
            return com.dataflow.auth.dto.CryptoDto.DecryptResult.builder()
                    .success(false)
                    .tampered(true)
                    .algorithm("AES-256-GCM")
                    .vaultSource(this.vaultSource)
                    .message("CẢNH BÁO BẢO MẬT: Dữ liệu đã bị thay đổi hoặc giả mạo! Xác thực Authentication Tag thất bại (AEADBadTagException / Tag Mismatch).")
                    .build();
        } catch (Exception e) {
            log.warn("Lỗi giải mã: {}", e.getMessage());
            return com.dataflow.auth.dto.CryptoDto.DecryptResult.builder()
                    .success(false)
                    .tampered(true)
                    .algorithm("AES-256-GCM")
                    .vaultSource(this.vaultSource)
                    .message("Giải mã thất bại (" + e.getClass().getSimpleName() + "): " + e.getMessage())
                    .build();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
