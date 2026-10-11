package com.dataflow.auth.controller;

import com.dataflow.auth.dto.CryptoDto;
import com.dataflow.auth.security.AesGcmEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security/crypto")
@RequiredArgsConstructor
@Slf4j
public class CryptoController {

    private final AesGcmEncryptionService aesGcmEncryptionService;

    @Value("${app.vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${app.vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${app.vault.secret-path:secret/data/auth-service}")
    private String secretPath;

    /**
     * Lấy thông tin trạng thái thuật toán AES-256-GCM và kết nối HashiCorp Vault.
     */
    @GetMapping("/status")
    public ResponseEntity<CryptoDto.VaultStatus> getCryptoStatus() {
        boolean vaultConnected = aesGcmEncryptionService.isVaultSource();
        CryptoDto.VaultStatus status = CryptoDto.VaultStatus.builder()
                .vaultEnabled(vaultEnabled)
                .vaultConnected(vaultConnected)
                .vaultUri(vaultUri)
                .secretPath(secretPath)
                .keyName("aes_gcm_key")
                .algorithm("AES-256-GCM (Galois/Counter Mode)")
                .keySizeBits(256)
                .statusMessage(vaultConnected 
                        ? "Đang kết nối trực tiếp HashiCorp Vault KV Engine v2. Khóa Master Key được bảo vệ an toàn."
                        : "Đang sử dụng khóa dự phòng cục bộ (Fallback Configuration Key).")
                .build();
        return ResponseEntity.ok(status);
    }

    /**
     * Mã hóa dữ liệu sử dụng AES-256-GCM và phân tách thành phần (IV, Ciphertext, Auth Tag).
     */
    @PostMapping("/encrypt")
    public ResponseEntity<CryptoDto.EncryptResult> encryptData(@RequestBody CryptoDto.EncryptRequest request) {
        String input = request != null && request.getPlainText() != null ? request.getPlainText() : "";
        CryptoDto.EncryptResult result = aesGcmEncryptionService.encryptDetailed(input);
        return ResponseEntity.ok(result);
    }

    /**
     * Giải mã dữ liệu và kiểm tra tính toàn vẹn (chống giả mạo / phát hiện can thiệp).
     */
    @PostMapping("/decrypt")
    public ResponseEntity<CryptoDto.DecryptResult> decryptData(@RequestBody CryptoDto.DecryptRequest request) {
        String input = request != null && request.getCipherText() != null ? request.getCipherText() : "";
        CryptoDto.DecryptResult result = aesGcmEncryptionService.decryptDetailed(input);
        return ResponseEntity.ok(result);
    }
}
