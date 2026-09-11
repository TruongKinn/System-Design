# System Design Interview Guide — DataFlow Platform
**Bộ câu hỏi & Trả lời Thực chiến phỏng vấn Senior System Design / Microservices**

## 1. Bài toán Xuất 1,000,000+ Bản ghi (Large Data Export)

### ❓ Câu hỏi:
> "Làm thế nào để xuất 1 triệu bản ghi từ Database ra file Excel mà không gây tràn bộ nhớ Heap (OutOfMemoryError) và không làm đứng ứng dụng?"

### 💡 Trả lời chuẩn Senior Architect:
1. **Không nạp tất cả bản ghi vào RAM**:
   - Thất bại: `List<User> users = userRepository.findAll();` (Gây OOM ngay lập tức với 1M bản ghi ~ 1GB RAM).
   - Giải pháp: Áp dụng **Streaming Cursor Fetch / JPA ScrollableResults** hoặc **Page Range Partitioning** (VD: đọc từng chunk 5,000 bản ghi).
2. **Xử lý Bất đồng bộ (Asynchronous Worker Pipeline)**:
   - Client gửi yêu cầu `POST /api/exports/create` -> API trả về HTTP `202 Accepted` kèm `jobId` ngay lập tức (< 100ms).
   - Hệ thống đẩy Event vào **Apache Kafka Topic `data.export.request`**.
   - Các **Worker Nodes** thuộc cùng một Consumer Group chia nhỏ khoảng (Range offset: 0-250k, 250k-500k, ...) để đọc và ghi tập tin bất đồng bộ.
3. **MinIO Object Storage & Presigned URL**:
   - File kết quả được ghi dạng Streaming XLSX vào **MinIO S3 Bucket**.
   - Trả về Presigned Download URL có thời hạn (VD: 60 phút) giúp Client tải trực tiếp từ S3 mà không đi qua Application Server.

---

## 2. Bài toán Nhất quán Dữ liệu (Transactional Outbox Pattern)

### ❓ Câu hỏi:
> "Khi tạo Job trong Database và phát Event tới Kafka, nếu Database Commit thành công nhưng Kafka bị sập (Dual-Write Failure) thì xử lý thế nào?"

### 💡 Trả lời chuẩn Senior Architect:
1. **Vấn đề Dual-Write Failure**:
   - Nếu thực hiện ghi DB rồi gọi `kafkaTemplate.send()`, nếu Kafka bị ngắt kết nối, DB đã saved nhưng Event không tới được Kafka -> Mất tính nhất quán.
2. **Giải pháp Transactional Outbox Pattern**:
   - Lưu vết Event dưới dạng bản ghi trong bảng `outbox_events` nằm trên **CÙNG DATABASE TRANSACTION** với dữ liệu chính (`export_jobs`).
   - Giao dịch tuân thủ **ACID**: Hoặc cả 2 cùng được lưu, hoặc cả 2 cùng Rollback.
3. **Outbox Poller / Debezium CDC**:
   - Sử dụng một background worker (`OutboxPublisherService`) hoặc **Debezium Change Data Capture (CDC)** đọc bảng `outbox_events` trạng thái `PENDING`, phát tin nhắn tới Kafka và đổi trạng thái thành `PUBLISHED`.
   - Đảm bảo cơ chế **At-Least-Once Delivery**.

---

## 3. Bài toán Khóa Phân tán (Redis Distributed Lock)

### ❓ Câu hỏi:
> "Hệ thống Job Service scale-out thành 5 instances. Làm sao đảm bảo một Cron Job chỉ được chạy bởi duy nhất 1 instance tại một thời điểm?"

### 💡 Trả lời chuẩn Senior Architect:
1. **Vấn đề Race Condition**:
   - Mỗi instance tự chạy `@Scheduled` độc lập sẽ dẫn tới 5 instances đồng thời thực thi cùng 1 Job -> Trùng lặp dữ liệu hoặc tài nguyên.
2. **Giải pháp Redis Distributed Lock**:
   - Trước khi thực thi Job, Instance phải chiếm khóa Redis bằng lệnh nguyên tử:
     `SET lock:job:{jobId} {instanceId} NX PX 300000` (`setIfAbsent`).
   - Nếu trả về `true` -> Instance đó chiếm được khóa và tiến hành chạy Job.
   - Nếu trả về `false` -> Instance khác đang giữ khóa, bỏ qua lượt chạy.
   - Sử dụng **UUID Instance ID** khi giải phóng khóa (`unlock`) để tránh trường hợp Instance A giải phóng nhầm khóa do Instance B đang nắm giữ.

---

## 4. Bài toán Đọc Streaming & Batch Insert (Large Excel Import)

### ❓ Câu hỏi:
> "Làm sao để import file Excel chứa 1,000,000 dòng vào MySQL trong thời gian ngắn nhất?"

### 💡 Trả lời chuẩn Senior Architect:
1. **Streaming SAX Parser**:
   - Dùng **Apache POI SAX Event Model** (hoặc EasyExcel) đọc tệp Excel dưới dạng luồng sự kiện XML thay vì nạp DOM Tree của file 500MB vào RAM.
2. **JDBC Batch Insert**:
   - Tránh câu lệnh `INSERT` đơn lẻ từng dòng (`INSERT INTO table VALUES (...)` x 1,000,000 lần -> Mất vài tiếng).
   - Gom nhóm 1,000 dòng/batch và thực thi `saveAll()` với cấu hình `rewriteBatchedStatements=true` trên JDBC Driver -> Đạt tốc độ xử lý hàng trăm ngàn bản ghi/phút.
3. **Tracking Tiến độ Realtime**:
   - Tiến độ (`importedRows`, `errorRows`, `% complete`) được ghi cập nhật liên tục vào **Redis Hash** (`import:progress:{jobId}`).
   - Angular Client sử dụng WebSocket hoặc Polling ngắn lấy tiến độ hiển thị thanh Progress Bar realtime cho người dùng.

---

## 5. Bảng Tính Toán Năng Lực (Capacity Estimation Model)

```
Target Metrics:
- Total Users: 1,000,000 users
- Daily Active Users (DAU): 100,000 users
- Peak Requests per Second (RPS): 2,000 RPS
- Average Request Payload: 2 KB
- Peak Network Bandwidth: 2,000 RPS * 2 KB = 4 MB/sec
- Database Storage Growth: 100,000 records/day * 1 KB = 100 MB/day (~36.5 GB/year)
```
