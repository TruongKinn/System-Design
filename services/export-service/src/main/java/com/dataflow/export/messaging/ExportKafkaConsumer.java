package com.dataflow.export.messaging;

import com.dataflow.export.domain.ExportJob;
import com.dataflow.export.event.ExportRequestEvent;
import com.dataflow.export.repository.ExportJobRepository;
import com.dataflow.export.service.RedisProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportKafkaConsumer {

    private final ExportJobRepository exportJobRepository;
    private final RedisProgressService progressService;

    @KafkaListener(topics = "data.export.request", groupId = "export-worker-group", autoStartup = "false")
    public void consumeExportEvent(ExportRequestEvent event) {
        log.info("Worker consumed ExportRequestEvent from Kafka: {}", event);
        processExportChunk(event);
    }

    public void processExportChunk(ExportRequestEvent event) {
        String jobId = event.getJobId();
        Optional<ExportJob> optionalJob = exportJobRepository.findByJobId(jobId);
        if (optionalJob.isEmpty()) {
            log.error("ExportJob not found for jobId: {}", jobId);
            return;
        }

        ExportJob job = optionalJob.get();
        job.setStatus("PROCESSING");
        exportJobRepository.save(job);

        long total = event.getEndRow() - event.getStartRow();
        long chunkSize = 250000;
        long processed = 0;
        long success = 0;
        long error = 0;

        while (processed < total) {
            long currentBatch = Math.min(chunkSize, total - processed);
            processed += currentBatch;
            success += (long) (currentBatch * 0.998); // 99.8% success
            error += (currentBatch - (long) (currentBatch * 0.998));

            progressService.updateProgress(jobId, job.getFileName(), "PROCESSING", total, processed, success, error);
            log.info("Job [{}] Export Progress: {} / {} records ({}%)", jobId, processed, total, Math.round((double) processed / total * 100));
        }

        job.setStatus("COMPLETED");
        job.setProcessedRecords(processed);
        job.setSuccessRecords(success);
        job.setErrorRecords(error);
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        progressService.updateProgress(jobId, job.getFileName(), "COMPLETED", total, processed, success, error);
        log.info("Export Job [{}] COMPLETED successfully!", jobId);
    }
}
