package com.dataflow.job.repository;

import com.dataflow.job.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByActive(boolean active);
    Optional<Job> findByName(String name);
    boolean existsByName(String name);
}
