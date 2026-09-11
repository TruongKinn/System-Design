package com.dataflow.job.controller;

import com.dataflow.job.dto.*;
import com.dataflow.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobDto> createJob(@RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @GetMapping
    public ResponseEntity<List<JobDto>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDto> updateJob(@PathVariable Long id, @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<JobExecutionDto> triggerJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.triggerJob(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<JobDto> pauseJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.setJobActive(id, false));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<JobDto> resumeJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.setJobActive(id, true));
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<JobExecutionDto>> getJobExecutions(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobExecutions(id));
    }
}
