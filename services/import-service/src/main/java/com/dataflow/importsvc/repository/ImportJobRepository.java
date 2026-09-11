package com.dataflow.importsvc.repository;

import com.dataflow.importsvc.domain.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    Optional<ImportJob> findByJobId(String jobId);
}
