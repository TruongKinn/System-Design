package com.dataflow.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExportRequest {
    private String entityName; // e.g., CUSTOMER, TRANSACTION, AUDIT_LOG
    private long requestedRecords; // e.g., 1000000
    private String fileFormat; // XLSX, CSV
}
