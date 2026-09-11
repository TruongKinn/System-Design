package com.dataflow.export.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequestEvent implements Serializable {
    private String jobId;
    private String entityName;
    private long startRow;
    private long endRow;
    private String fileFormat;
}
