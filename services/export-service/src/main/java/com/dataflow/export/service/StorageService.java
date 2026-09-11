package com.dataflow.export.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class StorageService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.bucket:dataflow-exports}")
    private String bucketName;

    public String generateStoragePath(String jobId, String fileName) {
        int year = LocalDateTime.now().getYear();
        int month = LocalDateTime.now().getMonthValue();
        return String.format("%s/%d/%02d/%s_%s", bucketName, year, month, jobId, fileName);
    }

    public String generatePresignedUrl(String filePath) {
        log.info("Generating MinIO Presigned Download URL for file path: {}", filePath);
        return String.format("%s/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=3600", minioEndpoint, filePath);
    }
}
