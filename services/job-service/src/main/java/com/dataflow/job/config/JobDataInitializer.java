package com.dataflow.job.config;

import com.dataflow.job.domain.Job;
import com.dataflow.job.domain.JobExecution;
import com.dataflow.job.repository.JobExecutionRepository;
import com.dataflow.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobDataInitializer implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;

    @Override
    public void run(String... args) {
        if (jobRepository.count() == 0) {
            Job job1 = jobRepository.save(Job.builder()
                    .name("daily-analytics-aggregation")
                    .cronExpression("0 0 1 * * ?")
                    .targetService("analytics-service")
                    .targetEndpoint("/api/analytics/daily-aggregate")
                    .httpMethod("POST")
                    .active(true)
                    .maxRetries(3)
                    .build());

            Job job2 = jobRepository.save(Job.builder()
                    .name("sync-user-profiles-kafka")
                    .cronExpression("*/30 * * * * ?")
                    .targetService("auth-service")
                    .targetEndpoint("/api/users/sync")
                    .httpMethod("POST")
                    .active(true)
                    .maxRetries(3)
                    .build());

            Job job3 = jobRepository.save(Job.builder()
                    .name("cleanup-expired-export-files")
                    .cronExpression("0 0 3 * * ?")
                    .targetService("export-service")
                    .targetEndpoint("/api/exports/cleanup")
                    .httpMethod("DELETE")
                    .active(false)
                    .maxRetries(2)
                    .build());

            Job job4 = jobRepository.save(Job.builder()
                    .name("batch-reconcile-transactions")
                    .cronExpression("0 30 23 * * ?")
                    .targetService("finance-service")
                    .targetEndpoint("/api/finance/reconcile")
                    .httpMethod("POST")
                    .active(true)
                    .maxRetries(5)
                    .build());

            // Seed real execution histories
            executionRepository.save(JobExecution.builder()
                    .jobId(job2.getId())
                    .executionId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .startTime(LocalDateTime.now().minusMinutes(2))
                    .endTime(LocalDateTime.now().minusMinutes(2).plusSeconds(2))
                    .resultSummary("Synced 450 user profile updates via Kafka topic user-events.")
                    .build());

            executionRepository.save(JobExecution.builder()
                    .jobId(job1.getId())
                    .executionId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .startTime(LocalDateTime.now().minusHours(6))
                    .endTime(LocalDateTime.now().minusHours(6).plusMinutes(5))
                    .resultSummary("Processed 1,250,400 analytics events into PostgreSQL.")
                    .build());

            executionRepository.save(JobExecution.builder()
                    .jobId(job4.getId())
                    .executionId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .startTime(LocalDateTime.now().minusHours(24))
                    .endTime(LocalDateTime.now().minusHours(24).plusMinutes(2))
                    .resultSummary("Reconciled 12,500 transactions successfully.")
                    .build());
        }
    }
}
