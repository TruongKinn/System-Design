package com.dataflow.job.service;

import com.dataflow.job.domain.Job;
import com.dataflow.job.domain.JobExecution;
import com.dataflow.job.domain.JobLog;
import com.dataflow.job.repository.JobExecutionRepository;
import com.dataflow.job.repository.JobLogRepository;
import com.dataflow.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobLogRepository logRepository;
    private final RedisLockService redisLockService;

    private static final String INSTANCE_ID = UUID.randomUUID().toString();

    @Scheduled(fixedRate = 30000) // Poll active scheduled jobs every 30s
    public void scheduleActiveJobs() {
        List<Job> activeJobs = jobRepository.findByActive(true);
        for (Job job : activeJobs) {
            triggerJobExecution(job.getId(), "CRON_SCHEDULED");
        }
    }

    @Transactional
    public JobExecution triggerJobExecution(Long jobId, String triggeredBy) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        String lockKey = "lock:job:" + jobId;
        Duration lockTimeout = Duration.ofMinutes(5);

        boolean acquired = redisLockService.tryLock(lockKey, INSTANCE_ID, lockTimeout);
        if (!acquired) {
            log.info("Job {} is currently locked by another instance. Skipping execution.", jobId);
            return null;
        }

        String executionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        JobExecution execution = JobExecution.builder()
                .jobId(job.getId())
                .executionId(executionId)
                .status("RUNNING")
                .startTime(startTime)
                .resultSummary("Triggered by: " + triggeredBy)
                .build();

        JobExecution savedExecution = executionRepository.save(execution);

        saveLog(executionId, "INFO", "Job execution started. Target: " + job.getTargetEndpoint());

        try {
            // Simulate task execution (In Phase 4, this publishes an event to Apache Kafka)
            log.info("Executing Job [{}] Target Endpoint: {}", job.getName(), job.getTargetEndpoint());
            
            savedExecution.setStatus("SUCCESS");
            savedExecution.setEndTime(LocalDateTime.now());
            savedExecution.setResultSummary("Execution completed successfully via " + triggeredBy);
            
            saveLog(executionId, "INFO", "Job executed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute Job [{}]", job.getName(), e);
            savedExecution.setStatus("FAILED");
            savedExecution.setEndTime(LocalDateTime.now());
            savedExecution.setResultSummary("Execution failed: " + e.getMessage());

            saveLog(executionId, "ERROR", "Job execution failed: " + e.getMessage());
        } finally {
            executionRepository.save(savedExecution);
            redisLockService.unlock(lockKey, INSTANCE_ID);
        }

        return savedExecution;
    }

    private void saveLog(String executionId, String level, String message) {
        logRepository.save(JobLog.builder()
                .executionId(executionId)
                .logLevel(level)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
