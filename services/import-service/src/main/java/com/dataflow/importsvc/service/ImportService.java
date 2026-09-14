package com.dataflow.importsvc.service;

import com.dataflow.importsvc.domain.ImportJob;
import com.dataflow.importsvc.dto.ImportJobDto;
import com.dataflow.importsvc.dto.ImportProgressDto;
import com.dataflow.importsvc.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final RedisImportProgressService progressService;
    private final BatchImporterService batchImporterService;

    @Transactional
    public ImportJobDto startImportFromUpload(MultipartFile multipartFile, String createdBy) {
        String originalFileName = multipartFile.getOriginalFilename() != null ? multipartFile.getOriginalFilename() : "imported_data.csv";
        long fileSize = multipartFile.getSize();
        String jobId = UUID.randomUUID().toString();

        File tempFile;
        try {
            String suffix = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".csv";
            tempFile = File.createTempFile("upload_" + jobId.substring(0, 8) + "_", suffix);
            try (InputStream is = multipartFile.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            log.error("Failed to save uploaded multipart file for job [{}]: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Could not save uploaded file: " + e.getMessage(), e);
        }

        ImportJob job = ImportJob.builder()
                .jobId(jobId)
                .fileName(originalFileName)
                .fileSize(fileSize)
                .status("UPLOADED")
                .totalRows(0)
                .importedRows(0)
                .errorRows(0)
                .createdBy(createdBy != null ? createdBy : "ADMIN")
                .build();

        ImportJob savedJob = importJobRepository.save(job);
        progressService.updateProgress(jobId, originalFileName, "UPLOADED", 0, 0, 0);

        // Run real streaming batch import in background thread
        new Thread(() -> batchImporterService.processRealFileImport(jobId, tempFile, originalFileName)).start();

        return mapToDto(savedJob);
    }

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

    public List<ImportJobDto> getAllImportJobs() {
        return importJobRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt() != null && a.getCreatedAt() != null ? b.getCreatedAt().compareTo(a.getCreatedAt()) : 0)
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
