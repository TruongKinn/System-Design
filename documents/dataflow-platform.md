# System Architecture: DataFlow Platform
**Enterprise Job & Data Processing Platform**

## 1. Tổng quan Hệ thống (System Overview)

DataFlow Platform là một hệ thống phân tán cấp doanh nghiệp (Enterprise Distributed System) phục vụ các bài toán:
- **Job Scheduling & Distributed Execution**: Lập lịch và chạy các tác vụ nền định kỳ hoặc theo yêu cầu với cơ chế Distributed Lock chống race-condition.
- **Large Dataset Export & Import**: Xử lý xuất/nhập dữ liệu khối lượng lớn (1,000,000+ bản ghi) bất đồng bộ (Asynchronous processing) qua Kafka & Workers.
- **Real-time Progress & Notification**: Theo dõi tiến độ xử lý realtime qua Redis & WebSocket/Polling.
- **Reliability & Resilience**: Tự động retry, Dead Letter Queue (DLQ), Transactional Outbox Pattern, Circuit Breaker, Idempotency.
- **Enterprise Security**: API Gateway tập trung, Authentication (JWT), Authorization (RBAC).
- **Full Observability**: Tập trung Log, Distributed Tracing (OpenTelemetry) và Metrics (Prometheus + Grafana).

---

## 2. Kiến trúc Tổng thể (High-Level Architecture)

```
                                  Angular Client
                                        |
                                        v
                                ┌──────────────┐
                                │ API Gateway  │
                                └──────┬───────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         v                             v                             v
  ┌──────────────┐              ┌──────────────┐              ┌──────────────┐
  │ Auth Service │              │ Job Service  │              │ Data Service │
  └──────┬───────┘              └──────┬───────┘              └──────┬───────┘
         │                             │                             │
         v                             v                             v
      MySQL 1                       MySQL 2                       MySQL 3
                                       │
                                       v
                                 Apache Kafka
                                ┌──────┼──────┐
                                │      │      │
                                v      v      v
                             Worker Worker Worker
                                │      │      │
                                └──────┼──────┘
                                       │
                                ┌──────┴──────┐
                                v             v
                              Redis         MinIO
                                │               
                                v               
                          Progress/Lock         

                   ┌─────────────────────────────┐
                   │ Prometheus + Grafana        │
                   │ OpenTelemetry               │
                   └─────────────────────────────┘
```

---

## 3. Danh sách các Dịch vụ (Service Inventory)

| Tên Service / Tài liệu | Đường dẫn tài liệu chi tiết | Trách nhiệm chính | Stack Công nghệ |
| :--- | :--- | :--- | :--- |
| **DataFlow Platform** | [dataflow-platform.md](file:///d:/AI-AGENT/System%20Design/documents/dataflow-platform.md) | Tài liệu kiến trúc tổng thể toàn bộ hệ thống | Markdown / Architecture Spec |
| **Operations Runbook** | [dataflow-operations.md](file:///d:/AI-AGENT/System%20Design/documents/dataflow-operations.md) | **Tài liệu hướng dẫn Vận hành, Startup, K8s & Runbook Khắc phục sự cố** | DevOps Runbook / Markdown |
| **System Design Q&A Guide** | [interview-guide-dataflow.md](file:///d:/AI-AGENT/System%20Design/documents/interview-guide-dataflow.md) | **Bộ câu hỏi & Trả lời thực chiến Phỏng vấn Senior System Design** | Interview Spec / Q&A |
| **API Gateway** | [api-gateway.md](file:///d:/AI-AGENT/System%20Design/documents/api-gateway.md) | Entry point tập trung, Routing, CORS, Circuit Breaker, Trace ID | Java 17, Spring Cloud Gateway |
| **Auth Service** | [auth-service.md](file:///d:/AI-AGENT/System%20Design/documents/auth-service.md) | Quản lý người dùng, Đăng nhập, Cấp JWT Token, Phân quyền RBAC | Java 17, Spring Boot, MySQL, Spring Security |
| **Job Service** | [job-service.md](file:///d:/AI-AGENT/System%20Design/documents/job-service.md) | Quản lý Cron Jobs, Trigger Job, Redis Distributed Lock | Java 17, Spring Boot, Redis |
| **Export Service** | [export-service.md](file:///d:/AI-AGENT/System%20Design/documents/export-service.md) | Export 1M+ records, Outbox Pattern, Kafka, MinIO | Java 17, Spring Boot, Kafka, MinIO |
| **Import Service** | [import-service.md](file:///d:/AI-AGENT/System%20Design/documents/import-service.md) | Import Excel 1M+ records, Streaming Read, Batch Insert MySQL | Java 17, Spring Boot, MySQL |

---

## 4. Mô hình Dữ liệu & Lưu trữ (Data & Storage Layer)

1. **PostgreSQL Databases (Relational DB)**:
   - Database per service pattern: `auth_db`, `job_db`, `export_db`, `import_db` (PostgreSQL 16).
   - Lưu trữ thông tin tài khoản, RBAC, định nghĩa Job, Lịch sử Job Execution, Trạng thái Export/Import.
2. **Redis Cache & Distributed Lock**:
   - **Distributed Lock**: Đảm bảo tại một thời điểm chỉ có 1 instance của Job Service kích hoạt một Cron Job.
   - **Realtime Progress Tracking**: Lưu tiến độ xử lý Export/Import (Processed/Total rows).
3. **Apache Kafka (Message Broker)**:
   - Chứa các Topics: `job.execute`, `job.status`, `data.export.request`, `data.import.request`, `job.dlq`.
   - Phân chia Partitions để scale horizontal cho Worker nodes.
4. **MinIO (S3-compatible Object Storage)**:
   - Lưu các tệp tin xuất ra (XLSX, CSV) và tệp tin tải lên (Import files).
   - Truy xuất thông qua Presigned URLs an toàn.

---

## 5. Lộ trình Triển khai (Phase-by-Phase Roadmap)

- **Phase 0**: Khởi tạo Repository Monorepo, Docker Compose (MySQL, Redis, Kafka, MinIO) & Tài liệu thiết kế trong `documents/`.
- **Phase 1**: Authentication Service + RBAC + API Gateway Integration.
- **Phase 2**: Job Management Service (CRUD Job, Trigger, Log execution history).
- **Phase 3**: Redis Distributed Lock for Cron Scheduler (Chống race-condition khi scale-out).
- **Phase 4**: Event-Driven Architecture với Apache Kafka, Consumer Group, Retry & DLQ.
- **Phase 5**: Large Data Export Service (1,000,000 records) xử lý phân đoạn bất đồng bộ + MinIO + Progress Tracking.
- **Phase 6**: Large Excel Import Service (Streaming Read + Batch Insert + Progress Tracking).
- **Phase 7**: Transactional Outbox Pattern (Đảm bảo tính nhất quán giữa DB và Kafka Event).
- **Phase 8**: Resilience & Fault Tolerance (Resilience4j Circuit Breaker, Timeout, Rate Limiting, Retry).
- **Phase 9**: Full Observability (OpenTelemetry Tracing, Prometheus Metrics, Grafana Dashboard).
- **Phase 10**: Load Testing & Tuning Performance (JMeter / k6 - Benchmark 100K - 1M ops).
- **Phase 11**: Orchestration với Kubernetes (Deployment, Service, ConfigMap, Secret, HPA).
- **Phase 12**: Chaos Engineering & Final Architecture Review.
