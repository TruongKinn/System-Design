package com.dataflow.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecutionDto {
    private Long id;
    private Long jobId;
    private String executionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String resultSummary;
}
