package com.dataflow.job.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "execution_id", nullable = false, unique = true)
    private String executionId; // UUID

    @Column(nullable = false)
    private String status; // PENDING, RUNNING, SUCCESS, FAILED, RETRYING

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "result_summary", length = 2000)
    private String resultSummary;
}
