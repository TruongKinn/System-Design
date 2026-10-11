package com.dataflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CryptoDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncryptRequest {
        private String plainText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptRequest {
        private String cipherText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncryptResult {
        private String cipherText;
        private String ivHex;
        private String authTagHex;
        private String cipherPayloadHex;
        private String algorithm;
        private int keySizeBits;
        private boolean vaultSource;
        private String keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptResult {
        private boolean success;
        private String plainText;
        private boolean tampered;
        private String ivHex;
        private String authTagHex;
        private String algorithm;
        private String message;
        private boolean vaultSource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VaultStatus {
        private boolean vaultEnabled;
        private boolean vaultConnected;
        private String vaultUri;
        private String secretPath;
        private String keyName;
        private String algorithm;
        private int keySizeBits;
        private String statusMessage;
    }
}
