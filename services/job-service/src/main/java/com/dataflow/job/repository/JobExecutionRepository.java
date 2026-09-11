package com.dataflow.job.repository;

import com.dataflow.job.domain.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobIdOrderByStartTimeDesc(Long jobId);
    Optional<JobExecution> findByExecutionId(String executionId);
}
