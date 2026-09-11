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
public class JobDto {
    private Long id;
    private String name;
    private String cronExpression;
    private String targetService;
    private String targetEndpoint;
    private String httpMethod;
    private boolean active;
    private int maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
