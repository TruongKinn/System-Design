package com.dataflow.job.service;

import com.dataflow.job.domain.Job;
import com.dataflow.job.domain.JobExecution;
import com.dataflow.job.dto.*;
import com.dataflow.job.repository.JobExecutionRepository;
import com.dataflow.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobSchedulerService schedulerService;

    @Transactional
    public JobDto createJob(CreateJobRequest request) {
        if (jobRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Job name already exists: " + request.getName());
        }

        Job job = Job.builder()
                .name(request.getName())
                .cronExpression(request.getCronExpression())
                .targetService(request.getTargetService())
                .targetEndpoint(request.getTargetEndpoint())
                .httpMethod(request.getHttpMethod() != null ? request.getHttpMethod() : "POST")
                .active(request.isActive())
                .maxRetries(request.getMaxRetries() > 0 ? request.getMaxRetries() : 3)
                .build();

        return mapToDto(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobDto> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobDto getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + id));
        return mapToDto(job);
    }

    @Transactional
    public JobDto updateJob(Long id, UpdateJobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + id));

        if (request.getCronExpression() != null) job.setCronExpression(request.getCronExpression());
        if (request.getTargetService() != null) job.setTargetService(request.getTargetService());
        if (request.getTargetEndpoint() != null) job.setTargetEndpoint(request.getTargetEndpoint());
        if (request.getHttpMethod() != null) job.setHttpMethod(request.getHttpMethod());
        if (request.getActive() != null) job.setActive(request.getActive());
        if (request.getMaxRetries() != null) job.setMaxRetries(request.getMaxRetries());

        return mapToDto(jobRepository.save(job));
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    @Transactional
    public JobDto setJobActive(Long id, boolean active) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + id));
        job.setActive(active);
        return mapToDto(jobRepository.save(job));
    }

    public JobExecutionDto triggerJob(Long id) {
        JobExecution execution = schedulerService.triggerJobExecution(id, "MANUAL_TRIGGER");
        if (execution == null) {
            throw new IllegalStateException("Job is currently locked or running.");
        }
        return mapToExecutionDto(execution);
    }

    @Transactional(readOnly = true)
    public List<JobExecutionDto> getJobExecutions(Long jobId) {
        return executionRepository.findByJobIdOrderByStartTimeDesc(jobId).stream()
                .map(this::mapToExecutionDto)
                .collect(Collectors.toList());
    }

    private JobDto mapToDto(Job job) {
        return JobDto.builder()
                .id(job.getId())
                .name(job.getName())
                .cronExpression(job.getCronExpression())
                .targetService(job.getTargetService())
                .targetEndpoint(job.getTargetEndpoint())
                .httpMethod(job.getHttpMethod())
                .active(job.isActive())
                .maxRetries(job.getMaxRetries())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private JobExecutionDto mapToExecutionDto(JobExecution execution) {
        return JobExecutionDto.builder()
                .id(execution.getId())
                .jobId(execution.getJobId())
                .executionId(execution.getExecutionId())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .resultSummary(execution.getResultSummary())
                .build();
    }
}
