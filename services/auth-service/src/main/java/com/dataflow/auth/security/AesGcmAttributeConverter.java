package com.dataflow.auth.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter tự động mã hóa AES-256-GCM khi ghi xuống Database
 * và tự động giải mã khi đọc lên Entity.
 */
@Converter
@Component
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {

    private static AesGcmEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(AesGcmEncryptionService service) {
        AesGcmAttributeConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService != null) {
            return encryptionService.encrypt(attribute);
        }
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService != null) {
            return encryptionService.decrypt(dbData);
        }
        return dbData;
    }
}
