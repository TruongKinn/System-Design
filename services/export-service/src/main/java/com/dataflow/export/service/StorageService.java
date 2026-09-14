package com.dataflow.export.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StorageService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:exports}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO Bucket [{}] created successfully", bucketName);
            } else {
                log.info("MinIO Bucket [{}] is ready", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO initialization warning (will retry on operation): {}", e.getMessage());
        }
    }

    public String generateStoragePath(String jobId, String fileName) {
        int year = LocalDateTime.now().getYear();
        int month = LocalDateTime.now().getMonthValue();
        return String.format("%d/%02d/%s_%s", year, month, jobId.substring(0, 8), fileName);
    }

    public void uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            log.info("Uploading file to MinIO: Bucket [{}], Object [{}] (Size: {} bytes)", bucketName, objectName, size);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded object [{}] to MinIO successfully!", objectName);
        } catch (Exception e) {
            log.error("Failed to upload object [{}] to MinIO: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String objectName) {
        try {
            log.info("Generating authentic MinIO Presigned Download URL for object: [{}]", objectName);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Could not generate MinIO presigned URL with SDK: {}, falling back to direct URL", e.getMessage());
            return String.format("%s/%s/%s", minioEndpoint, bucketName, objectName);
        }
    }
}
