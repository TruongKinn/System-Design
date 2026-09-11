package com.dataflow.importsvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobDto {
    private Long id;
    private String jobId;
    private String fileName;
    private long fileSize;
    private String status;
    private long totalRows;
    private long importedRows;
    private long errorRows;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
