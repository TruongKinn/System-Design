package com.dataflow.export.controller;

import com.dataflow.export.dto.*;
import com.dataflow.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/create")
    public ResponseEntity<ExportJobDto> createExportJob(
            @RequestBody CreateExportRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        return ResponseEntity.ok(exportService.createExportJob(request, userName));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ExportProgressDto> getExportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(exportService.getExportProgress(jobId));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable String jobId) {
        String url = exportService.getDownloadUrl(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "downloadUrl", url));
    }
}
