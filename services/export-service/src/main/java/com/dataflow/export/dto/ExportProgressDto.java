package com.dataflow.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportProgressDto {
    private String jobId;
    private String fileName;
    private String status;
    private long totalRecords;
    private long processedRecords;
    private long successRecords;
    private long errorRecords;
    private double percentComplete;
    private String downloadUrl;
}
