package com.dataflow.importsvc.service;

import com.dataflow.importsvc.domain.ImportJob;
import com.dataflow.importsvc.domain.ImportedCustomer;
import com.dataflow.importsvc.repository.ImportJobRepository;
import com.dataflow.importsvc.repository.ImportedCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchImporterService {

    private final ImportJobRepository importJobRepository;
    private final ImportedCustomerRepository customerRepository;
    private final RedisImportProgressService progressService;
    private final JdbcTemplate jdbcTemplate;

    public void processRealFileImport(String jobId, File file, String originalFileName) {
        ImportJob job = importJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ImportJob not found: " + jobId));

        job.setStatus("PROCESSING");
        importJobRepository.save(job);

        long imported = 0;
        long errors = 0;
        long totalRows = 0;

        try {
            log.info("Starting processing real file import for Job [{}] from file: {} (Size: {} bytes)",
                    jobId, originalFileName, file.length());

            if (originalFileName.toLowerCase().endsWith(".csv")) {
                totalRows = processCsvFile(jobId, job.getFileName(), file);
            } else {
                totalRows = processExcelFile(jobId, job.getFileName(), file);
            }

            job = importJobRepository.findByJobId(jobId).orElse(job);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);

            log.info("Real File Import Job [{}] COMPLETED! Total: {}, Imported: {}, Errors: {}",
                    jobId, job.getTotalRows(), job.getImportedRows(), job.getErrorRows());

        } catch (Exception e) {
            log.error("Fatal error importing file for Job [{}]: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            importJobRepository.save(job);
            progressService.updateProgress(jobId, originalFileName, "FAILED", totalRows, imported, errors);
        } finally {
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                log.debug("Temp uploaded file deleted: {}", deleted);
            }
        }
    }

    private long processCsvFile(String jobId, String fileName, File file) throws IOException {
        long imported = 0;
        long errors = 0;
        long totalRows = 0;
        int batchSize = 1000;
        List<ImportedCustomer> batchList = new ArrayList<>(batchSize);

        // First pass: quick line count for accurate total progress
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                totalRows++;
            }
            if (totalRows > 0) totalRows--; // subtract header row
        }

        progressService.updateProgress(jobId, fileName, "PROCESSING", totalRows, 0, 0);

        try (Reader in = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(in)) {

            for (CSVRecord record : parser) {
                try {
                    String customerCode = getRecordField(record, "Mã Khách Hàng", "customer_code", 0);
                    String fullName = getRecordField(record, "Họ và Tên", "full_name", 1);
                    String email = getRecordField(record, "Email", "email", 2);
                    String phone = getRecordField(record, "Số Điện Thoại", "phone_number", 3);

                    if (customerCode == null || customerCode.isBlank() || email == null || !email.contains("@")) {
                        errors++;
                    } else {
                        batchList.add(ImportedCustomer.builder()
                                .importJobId(jobId)
                                .customerCode(customerCode.trim())
                                .fullName(fullName != null ? fullName.trim() : "Customer")
                                .email(email.trim())
                                .phoneNumber(phone != null ? phone.trim() : "")
                                .createdAt(LocalDateTime.now())
                                .build());
                        imported++;
                    }

                    if (batchList.size() >= batchSize) {
                        saveBatchInTransaction(batchList);
                        batchList.clear();
                        progressService.updateProgress(jobId, fileName, "PROCESSING", totalRows, imported, errors);
                    }
                } catch (Exception e) {
                    errors++;
                }
            }

            if (!batchList.isEmpty()) {
                saveBatchInTransaction(batchList);
                batchList.clear();
            }

            updateJobRecordCounts(jobId, totalRows, imported, errors);
            progressService.updateProgress(jobId, fileName, "COMPLETED", totalRows, imported, errors);
        }

        return totalRows;
    }

    private long processExcelFile(String jobId, String fileName, File file) throws Exception {
        if (fileName.toLowerCase().endsWith(".xls") && !fileName.toLowerCase().endsWith(".xlsx")) {
            return processLegacyExcelFile(jobId, fileName, file);
        }
        return processStreamingXlsxFile(jobId, fileName, file);
    }

    /**
     * Ultra-fast SAX Streaming Excel Parser:
     * Constant memory (< 15MB) regardless of row count (100k or 1M rows).
     */
    private long processStreamingXlsxFile(String jobId, String fileName, File file) throws Exception {
        long[] counts = new long[3]; // [0] = total, [1] = imported, [2] = errors
        int batchSize = 2000;
        List<ImportedCustomer> batchList = new ArrayList<>(batchSize);

        progressService.updateProgress(jobId, fileName, "PROCESSING", 0, 0, 0);

        try (OPCPackage pkg = OPCPackage.open(file, org.apache.poi.openxml4j.opc.PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();
            StylesTable styles = reader.getStylesTable();
            XMLReader parser = XMLHelper.newXMLReader();

            DataFormatter formatter = new DataFormatter();

            XSSFSheetXMLHandler.SheetContentsHandler sheetHandler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                private final Map<Integer, String> rowValues = new HashMap<>();

                @Override
                public void startRow(int rowNum) {
                    rowValues.clear();
                }

                @Override
                public void endRow(int rowNum) {
                    if (rowNum == 0) {
                        return; // Skip header
                    }

                    counts[0]++; // totalRows count

                    try {
                        String col0 = rowValues.get(0);
                        String col1 = rowValues.get(1);
                        String customerCode;
                        String fullName;
                        String email;
                        String phone;

                        // Check if col0 is numeric row index (1, 2, 3...)
                        if (col0 != null && col0.matches("^\\d+$") && col1 != null && !col1.isBlank()) {
                            customerCode = col1;
                            fullName = rowValues.get(2);
                            email = rowValues.get(3);
                            phone = rowValues.get(4);
                        } else {
                            customerCode = col0;
                            fullName = col1;
                            email = rowValues.get(2);
                            phone = rowValues.get(3);
                        }

                        if (customerCode == null || customerCode.isBlank() || email == null || !email.contains("@")) {
                            counts[2]++; // error
                        } else {
                            batchList.add(ImportedCustomer.builder()
                                    .importJobId(jobId)
                                    .customerCode(customerCode.trim())
                                    .fullName(fullName != null ? fullName.trim() : "Khách Hàng " + counts[0])
                                    .email(email.trim())
                                    .phoneNumber(phone != null ? phone.trim() : "")
                                    .createdAt(LocalDateTime.now())
                                    .build());
                            counts[1]++; // imported
                        }

                        if (batchList.size() >= batchSize) {
                            saveBatchInTransaction(batchList);
                            batchList.clear();
                            progressService.updateProgress(jobId, fileName, "PROCESSING", counts[0], counts[1], counts[2]);
                        }
                    } catch (Exception e) {
                        counts[2]++;
                    }
                }

                @Override
                public void cell(String cellReference, String formattedValue, XSSFComment comment) {
                    if (cellReference != null && formattedValue != null) {
                        int col = getColumnIndex(cellReference);
                        rowValues.put(col, formattedValue.trim());
                    }
                }
            };

            XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(styles, sst, sheetHandler, formatter, false);
            parser.setContentHandler(handler);

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    InputSource sheetSource = new InputSource(sheetStream);
                    parser.parse(sheetSource);
                }
            }

            if (!batchList.isEmpty()) {
                saveBatchInTransaction(batchList);
                batchList.clear();
            }

            updateJobRecordCounts(jobId, counts[0], counts[1], counts[2]);
            progressService.updateProgress(jobId, fileName, "COMPLETED", counts[0], counts[1], counts[2]);
            return counts[0];
        }
    }

    private int getColumnIndex(String cellReference) {
        int col = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char c = cellReference.charAt(i);
            if (Character.isLetter(c)) {
                col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
            } else {
                break;
            }
        }
        return Math.max(0, col - 1);
    }

    private long processLegacyExcelFile(String jobId, String fileName, File file) throws Exception {
        long imported = 0;
        long errors = 0;
        int batchSize = 2000;
        List<ImportedCustomer> batchList = new ArrayList<>(batchSize);

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            long totalRows = Math.max(0, lastRowNum);

            progressService.updateProgress(jobId, fileName, "PROCESSING", totalRows, 0, 0);

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                try {
                    String col0 = getCellValue(row.getCell(0));
                    String col1 = getCellValue(row.getCell(1));
                    String customerCode;
                    String fullName;
                    String email;
                    String phone;

                    if (col0 != null && col0.matches("^\\d+$") && col1 != null && !col1.isBlank()) {
                        customerCode = col1;
                        fullName = getCellValue(row.getCell(2));
                        email = getCellValue(row.getCell(3));
                        phone = getCellValue(row.getCell(4));
                    } else {
                        customerCode = col0;
                        fullName = col1;
                        email = getCellValue(row.getCell(2));
                        phone = getCellValue(row.getCell(3));
                    }

                    if (customerCode == null || customerCode.isBlank() || email == null || !email.contains("@")) {
                        errors++;
                    } else {
                        batchList.add(ImportedCustomer.builder()
                                .importJobId(jobId)
                                .customerCode(customerCode.trim())
                                .fullName(fullName != null ? fullName.trim() : "Customer")
                                .email(email.trim())
                                .phoneNumber(phone != null ? phone.trim() : "")
                                .createdAt(LocalDateTime.now())
                                .build());
                        imported++;
                    }

                    if (batchList.size() >= batchSize) {
                        saveBatchInTransaction(batchList);
                        batchList.clear();
                        progressService.updateProgress(jobId, fileName, "PROCESSING", totalRows, imported, errors);
                    }
                } catch (Exception e) {
                    errors++;
                }
            }

            if (!batchList.isEmpty()) {
                saveBatchInTransaction(batchList);
                batchList.clear();
            }

            updateJobRecordCounts(jobId, totalRows, imported, errors);
            progressService.updateProgress(jobId, fileName, "COMPLETED", totalRows, imported, errors);

            return totalRows;
        }
    }

    private String getRecordField(CSVRecord record, String header1, String header2, int defaultIdx) {
        if (record.isMapped(header1)) return record.get(header1);
        if (record.isMapped("\ufeff" + header1)) return record.get("\ufeff" + header1);
        if (record.isMapped(header2)) return record.get(header2);
        if (record.size() > defaultIdx) return record.get(defaultIdx);
        return null;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public void processStreamingImport(String jobId, long totalRecordsToSimulate) {
        ImportJob job = importJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ImportJob not found: " + jobId));

        job.setStatus("PROCESSING");
        importJobRepository.save(job);

        long batchSize = 1000;
        long processed = 0;
        long imported = 0;
        long errors = 0;

        log.info("Starting Streaming Batch Simulation for Job [{}] (Total: {} records)", jobId, totalRecordsToSimulate);

        while (processed < totalRecordsToSimulate) {
            long currentBatchCount = Math.min(batchSize, totalRecordsToSimulate - processed);
            List<ImportedCustomer> batchList = new ArrayList<>();

            for (int i = 0; i < currentBatchCount; i++) {
                long currentIdx = processed + i + 1;
                if (currentIdx % 1000 == 0) {
                    errors++;
                } else {
                    batchList.add(ImportedCustomer.builder()
                            .importJobId(jobId)
                            .customerCode("CUST-" + String.format("%07d", currentIdx))
                            .fullName("Khách Hàng " + currentIdx)
                            .email("customer" + currentIdx + "@dataflow.vn")
                            .phoneNumber("09" + String.format("%08d", (currentIdx * 31) % 100000000))
                            .createdAt(LocalDateTime.now())
                            .build());
                    imported++;
                }
            }

            saveBatchInTransaction(batchList);
            processed += currentBatchCount;
            progressService.updateProgress(jobId, job.getFileName(), "PROCESSING", totalRecordsToSimulate, imported, errors);
        }

        updateJobRecordCounts(jobId, totalRecordsToSimulate, imported, errors);
        job = importJobRepository.findByJobId(jobId).orElse(job);
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);

        progressService.updateProgress(jobId, job.getFileName(), "COMPLETED", totalRecordsToSimulate, imported, errors);
        log.info("Import Simulation Job [{}] COMPLETED! Total Imported: {}, Errors: {}", jobId, imported, errors);
    }

    private void updateJobRecordCounts(String jobId, long total, long imported, long errors) {
        importJobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setTotalRows(total);
            job.setImportedRows(imported);
            job.setErrorRows(errors);
            importJobRepository.save(job);
        });
    }

    @Transactional
    public void saveBatchInTransaction(List<ImportedCustomer> customers) {
        if (customers == null || customers.isEmpty()) return;

        String sql = "INSERT INTO imported_customers (import_job_id, customer_code, full_name, email, phone_number, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ImportedCustomer c = customers.get(i);
                ps.setString(1, c.getImportJobId());
                ps.setString(2, c.getCustomerCode());
                ps.setString(3, c.getFullName());
                ps.setString(4, c.getEmail());
                ps.setString(5, c.getPhoneNumber());
                ps.setTimestamp(6, Timestamp.valueOf(c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now()));
            }

            @Override
            public int getBatchSize() {
                return customers.size();
            }
        });
    }
}
