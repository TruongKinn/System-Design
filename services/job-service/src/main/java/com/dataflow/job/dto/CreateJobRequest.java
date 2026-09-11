package com.dataflow.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {
    private String name;
    private String cronExpression;
    private String targetService;
    private String targetEndpoint;
    private String httpMethod;
    private boolean active;
    private int maxRetries;
}
