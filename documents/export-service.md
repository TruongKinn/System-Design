# Service Specification: Export Service
**Tài liệu thiết kế chi tiết dịch vụ Xuất Dữ Liệu Lớn (Export Service - 1M+ Records)**

## 1. Mục tiêu (Overview)
Export Service giải quyết bài toán xuất dữ liệu khối lượng cực lớn (lên tới 1,000,000+ bản ghi) ra file Excel/CSV:
- **Tránh Out Of Memory (OOM)**: Không nạp 1M bản ghi vào Heap Memory cùng lúc; áp dụng Streaming Read / Cursor Fetch / Range Chunking.
- **Asynchronous Chunk Processing**: Chia 1M bản ghi thành các Chunks (mỗi chunk 250k bản ghi) và phân phối qua Kafka cho nhiều Worker Nodes xử lý song song.
- **MinIO Storage**: Tổng hợp file xuất ra Object Storage (MinIO/S3) và cấp Presigned URL download an toàn cho Angular Client.
- **Real-time Progress Tracking**: Lưu tiến độ xử lý (% hoàn thành, số bản ghi đã xuất, số bản ghi lỗi) vào Redis.

---

## 2. Kiến trúc Luồng Xử lý Export (Export Flow Architecture)

```
Angular Client               Export API Gateway           Kafka Topic            Worker Nodes           MinIO S3
      │                              │                         │                       │                   │
      │── 1. POST /exports/create ──>│                         │                       │                   │
      │   (Export 1M records)        │── 2. Push Export Event ─>│                       │                   │
      │<── 3. Return Job ID #1001 ───│                          │── 4. Consume Chunk ──>│                   │
      │                              │                          │   (Range 0 - 250K)   │── 5. Append XLSX ─>│
      │                              │                          │── 6. Consume Chunk ──>│                   │
      │                              │                          │   (Range 250K - 500K)│── 7. Append XLSX ─>│
      │                              │                          │                       │                   │
      │── 8. Poll Progress (Redis) ─>│                          │                       │                   │
      │<── 9. Status: 100% COMPLETE ─│                          │                       │                   │
      │                              │                          │                       │                   │
      │── 10. GET Download URL ─────>│                          │                       │                   │
      │<── 11. Presigned MinIO URL ──│                          │                       │                   │
```

---

## 3. Redis Data Structure cho Progress Tracking

Key Format: `export:progress:{exportJobId}` (Hash Structure)
- `totalRecords`: `1000000`
- `processedRecords`: `650000`
- `successRecords`: `648000`
- `errorRecords`: `2000`
- `status`: `PROCESSING` | `COMPLETED` | `FAILED`

## 5. Transactional Outbox Pattern (Senior Distributed Systems Pattern)

Để khắc phục sự cố **Dual-Write Failure** (khi DB Commit thành công nhưng Kafka Broker bị treo/ngắt kết nối làm mất tin nhắn), hệ thống sử dụng **Transactional Outbox Pattern**:

```
 ┌────────────────────────────────────────────────────────┐
 │                   Export Service DB                    │
 │                                                        │
 │   BEGIN TRANSACTION                                    │
 │    ├── 1. INSERT INTO export_jobs                      │
 │    └── 2. INSERT INTO outbox_events (Status: PENDING)  │
 │   COMMIT TRANSACTION                                   │
 └───────────────────────────┬────────────────────────────┘
                             │
                             v
                Outbox Poller / Scheduler
                             │
              (3. Read PENDING Outbox Events)
                             │
                             v
                       Apache Kafka
                             │
            (4. Mark Outbox Event: PUBLISHED)
```

### Cấu trúc bảng `outbox_events`:
- `id`: BigInt (Primary Key)
- `aggregate_type`: `EXPORT_JOB`
- `aggregate_id`: `jobId` (UUID)
- `event_type`: `EXPORT_REQUESTED`
- `payload`: JSON Event String
- `status`: `PENDING` | `PUBLISHED` | `FAILED`
- `created_at`: Timestamp
- `processed_at`: Timestamp

---

## 6. Cấu hình MinIO Object Storage & Cơ chế Download File

### 6.1. Nguyên nhân lỗi AccessDenied từ MinIO
Mặc định theo chuẩn AWS S3 API, mọi bucket mới tạo trên MinIO đều ở trạng thái **Private**. Khi client hoặc trình duyệt truy cập đường dẫn trực tiếp (ví dụ: `http://localhost:9000/exports/filename.csv`) mà không có chữ ký xác thực (S3 Signature/Presigned URL) hoặc bucket chưa cấu hình Anonymous Access, MinIO sẽ phản hồi:
```xml
<Error>
  <Code>AccessDenied</Code>
  <Message>Access Denied.</Message>
</Error>
```

### 6.2. Giải pháp triển khai trong Hệ thống

1. **Chế độ Public Read Bucket (`exports`)**:
   - Sử dụng MinIO Client (`mc`) để cấp quyền ẩn danh `download`:
     ```bash
     mc anonymous set download myminio/exports
     ```
   - Tự động hóa qua container `dataflow-minio-init` trong `docker-compose.yml`: tự động khởi tạo bucket `exports`, set policy `download` và nạp sẵn dữ liệu mẫu.

2. **Chế độ Presigned Download URL (Môi trường Production bảo mật cao)**:
   - Export Service sinh link download có thời hạn (TTL) thông qua MinIO SDK:
     ```java
     GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
         .method(Method.GET)
         .bucket("exports")
         .object(filePath)
         .expiry(60 * 60) // Hết hạn sau 1 giờ
         .build();
     String presignedUrl = minioClient.getPresignedObjectUrl(args);
     ```
   - Bảo vệ file khỏi truy cập trái phép, chỉ người dùng sở hữu job export mới có link tải hợp lệ.

