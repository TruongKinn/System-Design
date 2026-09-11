# Service Specification: Import Service
**Tài liệu thiết kế chi tiết dịch vụ Nhập Dữ Liệu Lớn (Import Service - 1M+ Records)**

## 1. Mục tiêu (Overview)
Import Service đảm nhiệm xử lý đọc và import các tập tin Excel/CSV cực lớn:
- **Streaming SAX/Event API Parsing**: Đọc file Excel dạng Streaming (sử dụng Apache POI SAX Event Model hoặc EasyExcel) để không bị xé nát RAM khi đọc file 500MB - 1GB.
- **Batch Insertion**: Gom nhóm dữ liệu 1,000 - 5,000 bản ghi/lượt (Batch Insert) thay vì ghi từng câu lệnh `INSERT` đơn lẻ.
- **Data Validation & Error Log**: Kiểm tra dữ liệu hợp lệ (Data Constraints, Email Format, Business Rules). Lưu danh sách dòng bị lỗi ra file log để user download.
- **Real-time Progress & Dead Letter Queue**: Cập nhật tiến độ vào Redis và đẩy bản ghi lỗi nghiêm trọng vào Kafka DLQ.

---

## 2. Dynamic Batch Insert Pipeline

```
     Excel File (MinIO)
             │
             v
   Streaming SAX Parser
             │
             v
   Data Validation Rule Filter
      ├── Valid Rows ──────> Batch Collector (1,000 rows) ──> JDBC Batch Insert (MySQL)
      └── Invalid Rows ────> Error Log File (MinIO) & Redis Progress Counter
```

---

## 3. API Specification

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/imports/upload` | Upload file Excel lên MinIO & khởi tạo Import Job | Required: `IMPORT_DATA` |
| `GET` | `/api/imports/{id}/status` | Lấy tiến độ Import realtime từ Redis | Required: `IMPORT_DATA` |
| `GET` | `/api/imports/{id}/errors` | Tải xuống file nhật ký dòng dữ liệu bị lỗi | Required: `IMPORT_DATA` |
