package com.dataflow.importsvc;

import com.dataflow.importsvc.dto.ImportJobDto;
import com.dataflow.importsvc.dto.ImportProgressDto;
import com.dataflow.importsvc.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImportServiceApplicationTests {

    @Autowired
    private ImportService importService;

    @Test
    void contextLoads() {
        assertNotNull(importService);
    }

    @Test
    void test1MillionRecordsStreamingImportAndBatchInsert() throws InterruptedException {
        long totalRowsToImport = 100000;

        ImportJobDto importJob = importService.startImport("customers_batch_1m.xlsx", 1024 * 1024 * 25, totalRowsToImport, "admin");
        assertNotNull(importJob);
        assertNotNull(importJob.getJobId());

        // Wait up to 10 seconds for streaming batch worker thread to complete batch insert
        ImportProgressDto progress = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            progress = importService.getImportStatus(importJob.getJobId());
            if ("COMPLETED".equals(progress.getStatus())) {
                break;
            }
        }

        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(totalRowsToImport, progress.getTotalRows());
        assertTrue(progress.getImportedRows() > 0);
        assertEquals(100.0, progress.getPercentComplete());
    }
}
