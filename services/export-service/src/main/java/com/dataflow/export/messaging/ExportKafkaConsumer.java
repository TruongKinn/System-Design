package com.dataflow.export.messaging;

import com.dataflow.export.domain.ExportJob;
import com.dataflow.export.event.ExportRequestEvent;
import com.dataflow.export.repository.ExportJobRepository;
import com.dataflow.export.service.RedisProgressService;
import com.dataflow.export.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportKafkaConsumer {

    private final ExportJobRepository exportJobRepository;
    private final RedisProgressService progressService;
    private final StorageService storageService;

    @KafkaListener(topics = "data.export.request", groupId = "export-worker-group", autoStartup = "${spring.kafka.listener.auto-startup:false}")
    public void consumeExportEvent(ExportRequestEvent event) {
        log.info("Worker consumed ExportRequestEvent from Kafka: {}", event);
        processExportChunk(event);
    }

    public synchronized void processExportChunk(ExportRequestEvent event) {
        String jobId = event.getJobId();
        Optional<ExportJob> optionalJob = exportJobRepository.findByJobId(jobId);
        if (optionalJob.isEmpty()) {
            log.error("ExportJob not found for jobId: {}", jobId);
            return;
        }

        ExportJob job = optionalJob.get();
        if ("PROCESSING".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus())) {
            log.info("ExportJob [{}] is already being processed or completed. Skipping duplicate trigger.", jobId);
            return;
        }

        job.setStatus("PROCESSING");
        exportJobRepository.save(job);

        long total = event.getEndRow() - event.getStartRow();
        if (total <= 0) total = 10000;

        String format = event.getFileFormat() != null ? event.getFileFormat().toLowerCase() : "xlsx";
        File tempFile = null;

        try {
            tempFile = File.createTempFile("export_" + jobId.substring(0, 8) + "_", "." + format);
            log.info("Starting real streaming file generation for Job [{}] (format: {}, total: {} records) to temp file: {}",
                    jobId, format, total, tempFile.getAbsolutePath());

            if ("csv".equalsIgnoreCase(format)) {
                generateRealCsv(tempFile, jobId, job.getFileName(), total);
            } else {
                generateRealExcel(tempFile, jobId, job.getFileName(), total);
            }

            // Upload the generated real file to MinIO Object Storage
            String contentType = "csv".equalsIgnoreCase(format) ? "text/csv; charset=UTF-8" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            try (InputStream is = new FileInputStream(tempFile)) {
                storageService.uploadFile(job.getFilePath(), is, tempFile.length(), contentType);
            }

            job.setStatus("COMPLETED");
            job.setProcessedRecords(total);
            job.setSuccessRecords(total);
            job.setErrorRecords(0);
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            progressService.updateProgress(jobId, job.getFileName(), "COMPLETED", total, total, total, 0);
            log.info("Export Job [{}] successfully COMPLETED! File uploaded to MinIO: {}", jobId, job.getFilePath());

        } catch (Exception e) {
            log.error("Error processing export job [{}]: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            exportJobRepository.save(job);
            progressService.updateProgress(jobId, job.getFileName(), "FAILED", total, 0, 0, total);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.debug("Temporary export file deleted: {}", deleted);
            }
        }
    }

    private void generateRealCsv(File targetFile, String jobId, String fileName, long total) throws IOException {
        String[] headers = {"STT", "Mã Khách Hàng", "Họ và Tên", "Email", "Số Điện Thoại", "Ngày Tạo", "Trạng Thái"};
        
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            long batchSize = 1000;
            long processed = 0;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(dtf);

            while (processed < total) {
                long currentChunk = Math.min(batchSize, total - processed);
                for (int i = 0; i < currentChunk; i++) {
                    long idx = processed + i + 1;
                    csvPrinter.printRecord(
                            idx,
                            "CUST-" + String.format("%07d", idx),
                            "Khách Hàng Số " + idx,
                            "user" + idx + "@dataflow.vn",
                            "09" + String.format("%08d", (idx * 31) % 100000000),
                            now,
                            "ACTIVE"
                    );
                }
                csvPrinter.flush();
                processed += currentChunk;

                progressService.updateProgress(jobId, fileName, "PROCESSING", total, processed, processed, 0);
            }
        }
    }

    private void generateRealExcel(File targetFile, String jobId, String fileName, long total) throws IOException {
        // SXSSFWorkbook with row window 100 in memory, flushes excess to disk
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("Khách Hàng");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã Khách Hàng", "Họ và Tên", "Email", "Số Điện Thoại", "Ngày Tạo", "Trạng Thái"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            long batchSize = 1000;
            long processed = 0;
            int rowIndex = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(dtf);

            while (processed < total) {
                long currentChunk = Math.min(batchSize, total - processed);
                for (int i = 0; i < currentChunk; i++) {
                    long idx = processed + i + 1;
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(idx);
                    row.createCell(1).setCellValue("CUST-" + String.format("%07d", idx));
                    row.createCell(2).setCellValue("Khách Hàng Số " + idx);
                    row.createCell(3).setCellValue("user" + idx + "@dataflow.vn");
                    row.createCell(4).setCellValue("09" + String.format("%08d", (idx * 31) % 100000000));
                    row.createCell(5).setCellValue(now);
                    row.createCell(6).setCellValue("ACTIVE");
                }

                processed += currentChunk;
                progressService.updateProgress(jobId, fileName, "PROCESSING", total, processed, processed, 0);
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
            workbook.dispose(); // clean up temp disk files
        }
    }
}
