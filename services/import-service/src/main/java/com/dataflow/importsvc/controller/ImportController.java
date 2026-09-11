package com.dataflow.importsvc.controller;

import com.dataflow.importsvc.dto.ImportJobDto;
import com.dataflow.importsvc.dto.ImportProgressDto;
import com.dataflow.importsvc.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/upload")
    public ResponseEntity<ImportJobDto> uploadAndStartImport(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "totalRows", defaultValue = "1000000") long totalRows,
            @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        ImportJobDto job = importService.startImport(fileName, 1024 * 1024 * 50, totalRows, userName);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ImportProgressDto> getImportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(importService.getImportStatus(jobId));
    }
}
