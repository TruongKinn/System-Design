package com.dataflow.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Service tích hợp trực tiếp với HashiCorp Vault REST API v1.
 * Đảm nhiệm việc tải các Secret và Encryption Keys (như AES-GCM master key)
 * động từ Vault KV Engine theo kiến trúc Zero Trust / Production chuẩn bảo mật.
 */
@Service
@Slf4j
public class VaultSecretService {

    @Value("${app.vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${app.vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${app.vault.token:00000000-0000-0000-0000-000000000000}")
    private String vaultToken;

    @Value("${app.vault.secret-path:secret/data/auth-service}")
    private String secretPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lấy giá trị secret key theo key name từ HashiCorp Vault KV v2.
     *
     * @param keyName Tên key cần lấy (ví dụ: aes_gcm_key)
     * @return Optional chứa giá trị key nếu lấy thành công
     */
    public Optional<String> getSecretKey(String keyName) {
        if (!vaultEnabled) {
            log.warn("[Vault] HashiCorp Vault integration is disabled via configuration.");
            return Optional.empty();
        }

        String fullUrl = String.format("%s/v1/%s", vaultUri.replaceAll("/+$", ""), secretPath.replaceAll("^/+", ""));
        log.info("[Vault] Fetching secret '{}' from HashiCorp Vault: {}", keyName, fullUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", vaultToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                // KV v2 format: data.data.<keyName>
                JsonNode dataNode = root.path("data").path("data");
                if (dataNode.has(keyName)) {
                    String secretValue = dataNode.get(keyName).asText();
                    log.info("[Vault] Successfully retrieved key '{}' from HashiCorp Vault! (Length: {} chars)",
                            keyName, secretValue.length());
                    return Optional.of(secretValue);
                } else {
                    log.warn("[Vault] Key '{}' not found in Vault path '{}'. Available keys: {}",
                            keyName, secretPath, dataNode.fieldNames());
                }
            } else {
                log.warn("[Vault] Received non-2xx response from Vault: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[Vault] Error connecting to HashiCorp Vault at {}: {}. Will use fallback secret key.",
                    fullUrl, e.getMessage());
        }

        return Optional.empty();
    }
}
