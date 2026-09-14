package com.dataflow.importsvc.controller;

import com.dataflow.importsvc.dto.ImportJobDto;
import com.dataflow.importsvc.dto.ImportProgressDto;
import com.dataflow.importsvc.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({"/api/imports", "/api/v1/imports"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImportController {

    private final ImportService importService;

    @GetMapping
    public ResponseEntity<List<ImportJobDto>> getAllImportJobs() {
        return ResponseEntity.ok(importService.getAllImportJobs());
    }

    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ImportJobDto> uploadAndStartImportFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        ImportJobDto job = importService.startImportFromUpload(file, userName);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/simulate")
    public ResponseEntity<ImportJobDto> simulateImport(
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

    @GetMapping("/{jobId}/progress")
    public ResponseEntity<ImportProgressDto> getImportProgress(@PathVariable String jobId) {
        return ResponseEntity.ok(importService.getImportStatus(jobId));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadSampleTemplate(@RequestParam(value = "records", defaultValue = "1000") int records) {
        StringBuilder sb = new StringBuilder();
        sb.append("\ufeffMã Khách Hàng,Họ và Tên,Email,Số Điện Thoại\n");

        String[] sampleNames = {
                "Nguyễn Văn An", "Trần Thị Bình", "Lê Hoàng Nam", "Phạm Minh Đức", 
                "Vũ Thị Mai", "Đặng Quốc Huy", "Hoàng Thu Trang", "Bùi Thanh Tùng", 
                "Đỗ Hải Yến", "Ngô Quang Minh", "Dương Bảo Ngọc", "Lý Gia Bảo"
        };

        int count = Math.min(Math.max(records, 10), 500000);
        for (int i = 1; i <= count; i++) {
            String name = sampleNames[(i - 1) % sampleNames.length] + " " + i;
            String code = String.format("CUST-%06d", 100000 + i);
            String email = String.format("khachhang%d@dataflow.vn", i);
            String phone = String.format("09%08d", (10000000 + (i * 137) % 90000000));
            sb.append(code).append(",").append(name).append(",").append(email).append(",").append(phone).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_customers_" + count + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/template/excel")
    public ResponseEntity<byte[]> downloadSampleExcelTemplate(@RequestParam(value = "records", defaultValue = "1000") int records) {
        int count = Math.min(Math.max(records, 10), 500000);
        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(100)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Khách Hàng");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã Khách Hàng", "Họ và Tên", "Email", "Số Điện Thoại"};

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[] sampleNames = {
                    "Nguyễn Văn An", "Trần Thị Bình", "Lê Hoàng Nam", "Phạm Minh Đức",
                    "Vũ Thị Mai", "Đặng Quốc Huy", "Hoàng Thu Trang", "Bùi Thanh Tùng",
                    "Đỗ Hải Yến", "Ngô Quang Minh", "Dương Bảo Ngọc", "Lý Gia Bảo"
            };

            for (int i = 1; i <= count; i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(i);
                String name = sampleNames[(i - 1) % sampleNames.length] + " " + i;
                String code = String.format("CUST-%06d", 100000 + i);
                String email = String.format("khachhang%d@dataflow.vn", i);
                String phone = String.format("09%08d", (10000000 + (i * 137) % 90000000));

                row.createCell(0).setCellValue(code);
                row.createCell(1).setCellValue(name);
                row.createCell(2).setCellValue(email);
                row.createCell(3).setCellValue(phone);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            byte[] bytes = out.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_customers_" + count + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
