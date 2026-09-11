package com.dataflow.importsvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportProgressDto {
    private String jobId;
    private String fileName;
    private String status;
    private long totalRows;
    private long importedRows;
    private long errorRows;
    private double percentComplete;
    private String errorLogUrl;
}
