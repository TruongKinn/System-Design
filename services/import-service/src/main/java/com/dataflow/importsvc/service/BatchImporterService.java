package com.dataflow.importsvc.service;

import com.dataflow.importsvc.domain.ImportJob;
import com.dataflow.importsvc.domain.ImportedCustomer;
import com.dataflow.importsvc.repository.ImportJobRepository;
import com.dataflow.importsvc.repository.ImportedCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchImporterService {

    private final ImportJobRepository importJobRepository;
    private final ImportedCustomerRepository customerRepository;
    private final RedisImportProgressService progressService;

    public void processStreamingImport(String jobId, long totalRecordsToSimulate) {
        ImportJob job = importJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ImportJob not found: " + jobId));

        job.setStatus("PROCESSING");
        importJobRepository.save(job);

        long batchSize = 1000;
        long processed = 0;
        long imported = 0;
        long errors = 0;

        log.info("Starting Streaming Batch Import pipeline for Job [{}] (Total: {} records)", jobId, totalRecordsToSimulate);

        while (processed < totalRecordsToSimulate) {
            long currentBatchCount = Math.min(batchSize, totalRecordsToSimulate - processed);
            List<ImportedCustomer> batchList = new ArrayList<>();

            for (int i = 0; i < currentBatchCount; i++) {
                long currentIdx = processed + i + 1;
                // Validate email pattern (simulate 0.1% error rate)
                if (currentIdx % 1000 == 0) {
                    errors++;
                } else {
                    batchList.add(ImportedCustomer.builder()
                            .importJobId(jobId)
                            .customerCode("CUST-" + currentIdx)
                            .fullName("Customer Name " + currentIdx)
                            .email("customer" + currentIdx + "@dataflow.com")
                            .phoneNumber("09" + String.format("%08d", currentIdx % 100000000))
                            .createdAt(LocalDateTime.now())
                            .build());
                    imported++;
                }
            }

            // Perform Batch Insert in DB
            saveBatchInTransaction(batchList);

            processed += currentBatchCount;
            progressService.updateProgress(jobId, job.getFileName(), "PROCESSING", totalRecordsToSimulate, imported, errors);

            if (processed % 100000 == 0 || processed == totalRecordsToSimulate) {
                log.info("Import Job [{}] Progress: {} / {} rows ({}%)", jobId, processed, totalRecordsToSimulate, Math.round((double) processed / totalRecordsToSimulate * 100));
            }
        }

        job.setStatus("COMPLETED");
        job.setTotalRows(totalRecordsToSimulate);
        job.setImportedRows(imported);
        job.setErrorRows(errors);
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);

        progressService.updateProgress(jobId, job.getFileName(), "COMPLETED", totalRecordsToSimulate, imported, errors);
        log.info("Import Job [{}] COMPLETED! Total Imported: {}, Errors: {}", jobId, imported, errors);
    }

    @Transactional
    public void saveBatchInTransaction(List<ImportedCustomer> customers) {
        customerRepository.saveAll(customers);
    }
}
