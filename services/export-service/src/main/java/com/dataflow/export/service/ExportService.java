package com.dataflow.export.service;

import com.dataflow.export.domain.ExportJob;
import com.dataflow.export.domain.OutboxEvent;
import com.dataflow.export.dto.*;
import com.dataflow.export.event.ExportRequestEvent;
import com.dataflow.export.messaging.ExportKafkaConsumer;
import com.dataflow.export.messaging.ExportKafkaProducer;
import com.dataflow.export.repository.ExportJobRepository;
import com.dataflow.export.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportJobRepository exportJobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedisProgressService progressService;
    private final StorageService storageService;
    private final ExportKafkaProducer kafkaProducer;
    private final ExportKafkaConsumer kafkaConsumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ExportJobDto createExportJob(CreateExportRequest request, String createdBy) {
        String jobId = UUID.randomUUID().toString();
        String format = request.getFileFormat() != null ? request.getFileFormat().toLowerCase() : "xlsx";
        String fileName = String.format("export_%s_%s.%s", request.getEntityName().toLowerCase(), jobId.substring(0, 8), format);
        String storagePath = storageService.generateStoragePath(jobId, fileName);

        long totalRecords = request.getRequestedRecords() > 0 ? request.getRequestedRecords() : 1000000;

        ExportJob job = ExportJob.builder()
                .jobId(jobId)
                .fileName(fileName)
                .filePath(storagePath)
                .status("PENDING")
                .totalRecords(totalRecords)
                .processedRecords(0)
                .successRecords(0)
                .errorRecords(0)
                .createdBy(createdBy != null ? createdBy : "SYSTEM")
                .build();

        ExportJob savedJob = exportJobRepository.save(job);
        progressService.updateProgress(jobId, fileName, "PENDING", totalRecords, 0, 0, 0);

        ExportRequestEvent event = ExportRequestEvent.builder()
                .jobId(jobId)
                .entityName(request.getEntityName())
                .startRow(0)
                .endRow(totalRecords)
                .fileFormat(format)
                .build();

        // Transactional Outbox Pattern: Save Event in SAME Database Transaction
        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("EXPORT_JOB")
                    .aggregateId(jobId)
                    .eventType("EXPORT_REQUESTED")
                    .payload(payloadJson)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Outbox Event", e);
        }

        // Direct Worker execution fallback for unit/local tests
        new Thread(() -> kafkaConsumer.processExportChunk(event)).start();

        return mapToDto(savedJob);
    }

    public ExportProgressDto getExportProgress(String jobId) {
        ExportProgressDto progress = progressService.getProgress(jobId);

        ExportJob job = exportJobRepository.findByJobId(jobId).orElse(null);
        if (job != null && "COMPLETED".equalsIgnoreCase(job.getStatus())) {
            progress.setDownloadUrl(storageService.generatePresignedUrl(job.getFilePath()));
        }

        return progress;
    }

    public String getDownloadUrl(String jobId) {
        ExportJob job = exportJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ExportJob not found with jobId: " + jobId));

        if (!"COMPLETED".equalsIgnoreCase(job.getStatus())) {
            throw new IllegalStateException("ExportJob is not completed yet. Current status: " + job.getStatus());
        }

        return storageService.generatePresignedUrl(job.getFilePath());
    }

    private ExportJobDto mapToDto(ExportJob job) {
        return ExportJobDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .filePath(job.getFilePath())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .successRecords(job.getSuccessRecords())
                .errorRecords(job.getErrorRecords())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
