package com.dataflow.export.repository;

import com.dataflow.export.domain.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
    Optional<ExportJob> findByJobId(String jobId);
}
