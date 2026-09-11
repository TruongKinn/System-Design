package com.dataflow.job.repository;

import com.dataflow.job.domain.JobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobLogRepository extends JpaRepository<JobLog, Long> {
    List<JobLog> findByExecutionIdOrderByTimestampAsc(String executionId);
}
