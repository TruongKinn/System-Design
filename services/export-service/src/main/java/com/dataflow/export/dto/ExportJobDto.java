package com.dataflow.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobDto {
    private Long id;
    private String jobId;
    private String fileName;
    private String filePath;
    private String status;
    private long totalRecords;
    private long processedRecords;
    private long successRecords;
    private long errorRecords;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
