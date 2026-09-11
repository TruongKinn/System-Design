package com.dataflow.importsvc.service;

import com.dataflow.importsvc.domain.ImportJob;
import com.dataflow.importsvc.dto.ImportJobDto;
import com.dataflow.importsvc.dto.ImportProgressDto;
import com.dataflow.importsvc.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final RedisImportProgressService progressService;
    private final BatchImporterService batchImporterService;

    @Transactional
    public ImportJobDto startImport(String originalFileName, long fileSize, long totalRowsToImport, String createdBy) {
        String jobId = UUID.randomUUID().toString();

        ImportJob job = ImportJob.builder()
                .jobId(jobId)
                .fileName(originalFileName)
                .fileSize(fileSize)
                .status("UPLOADED")
                .totalRows(totalRowsToImport)
                .importedRows(0)
                .errorRows(0)
                .createdBy(createdBy != null ? createdBy : "SYSTEM")
                .build();

        ImportJob savedJob = importJobRepository.save(job);
        progressService.updateProgress(jobId, originalFileName, "UPLOADED", totalRowsToImport, 0, 0);

        // Run Streaming Batch Processing in background thread
        new Thread(() -> batchImporterService.processStreamingImport(jobId, totalRowsToImport)).start();

        return mapToDto(savedJob);
    }

    public ImportProgressDto getImportStatus(String jobId) {
        return progressService.getProgress(jobId);
    }

    private ImportJobDto mapToDto(ImportJob job) {
        return ImportJobDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .fileSize(job.getFileSize())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .importedRows(job.getImportedRows())
                .errorRows(job.getErrorRows())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
