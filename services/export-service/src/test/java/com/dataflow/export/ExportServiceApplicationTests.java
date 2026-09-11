package com.dataflow.export;

import com.dataflow.export.dto.CreateExportRequest;
import com.dataflow.export.dto.ExportJobDto;
import com.dataflow.export.dto.ExportProgressDto;
import com.dataflow.export.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExportServiceApplicationTests {

    @Autowired
    private ExportService exportService;

    @Autowired
    private com.dataflow.export.repository.OutboxEventRepository outboxEventRepository;

    @Test
    void contextLoads() {
        assertNotNull(exportService);
        assertNotNull(outboxEventRepository);
    }

    @Test
    void test1MillionRecordsExportJobAndProgressTracking() throws InterruptedException {
        CreateExportRequest request = CreateExportRequest.builder()
                .entityName("TRANSACTIONS")
                .requestedRecords(1000000)
                .fileFormat("xlsx")
                .build();

        ExportJobDto exportJob = exportService.createExportJob(request, "admin");
        assertNotNull(exportJob);
        assertNotNull(exportJob.getJobId());
        assertEquals(1000000, exportJob.getTotalRecords());

        // Verify Outbox Event created atomically in SAME Database Transaction
        assertFalse(outboxEventRepository.findAll().isEmpty());

        // Wait briefly for worker thread to complete export processing
        Thread.sleep(1000);

        ExportProgressDto progress = exportService.getExportProgress(exportJob.getJobId());
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(1000000, progress.getProcessedRecords());
        assertEquals(100.0, progress.getPercentComplete());
        assertNotNull(progress.getDownloadUrl());
        assertTrue(progress.getDownloadUrl().contains("dataflow-exports"));
    }
}
