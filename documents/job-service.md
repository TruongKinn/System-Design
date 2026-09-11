# Service Specification: Job Service
**Tài liệu thiết kế chi tiết dịch vụ Quản lý & Lập lịch Tác vụ (Job Service)**

## 1. Mục tiêu (Overview)
Job Service đảm nhận:
- Quản lý định nghĩa tác vụ (CRUD Jobs: Sync Data, Cleanup Logs, Export Report, API Health Check, v.v.).
- Lập lịch kích hoạt tự động theo biểu thức Cron (Cron Expression).
- **Redis Distributed Lock**: Đảm bảo an toàn concurrency khi hệ thống scale out chạy nhiều instance của Job Service đồng thời.
- **Publish Events to Kafka**: Đẩy thông điệp yêu cầu thực thi sang Kafka Topic `job.execute` để các Worker Nodes tiêu thụ bất đồng bộ.
- Lưu trữ nhật ký chạy tác vụ (`job_executions`, `job_logs`).

---

## 2. Mô hình Dữ liệu (Database ERD - `job_db`)

```
   ┌──────────┐           ┌────────────────┐           ┌──────────┐
   │   jobs   │──────────<│ job_executions │──────────<│ job_logs │
   └──────────┘           └────────────────┘           └──────────┘
```

### Chi tiết các bảng (Tables):
1. `jobs`: `id`, `name`, `cron_expression`, `target_service`, `target_endpoint`, `http_method`, `active`, `max_retries`, `created_at`, `updated_at`.
2. `job_executions`: `id`, `job_id`, `execution_id` (UUID), `status` (PENDING, RUNNING, SUCCESS, FAILED, RETRYING), `start_time`, `end_time`, `result_summary`.
3. `job_logs`: `id`, `execution_id`, `log_level` (INFO, WARN, ERROR), `message`, `timestamp`.

---

## 3. Danh sách API (API Endpoints)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/jobs` | Tạo mới tác vụ Job | Required: `JOB_MANAGE` |
| `GET` | `/api/jobs` | Danh sách các tác vụ | Required: `JOB_READ` |
| `GET` | `/api/jobs/{id}` | Chi tiết thông tin Job | Required: `JOB_READ` |
| `PUT` | `/api/jobs/{id}` | Cập nhật cấu hình Job | Required: `JOB_MANAGE` |
| `DELETE` | `/api/jobs/{id}` | Xóa Job | Required: `JOB_MANAGE` |
| `POST` | `/api/jobs/{id}/execute` | Kích hoạt chạy Job thủ công ngay lập tức | Required: `JOB_EXECUTE` |
| `POST` | `/api/jobs/{id}/pause` | Tạm dừng Cron Schedule của Job | Required: `JOB_MANAGE` |
| `POST` | `/api/jobs/{id}/resume` | Tiếp tục kích hoạt Cron Schedule | Required: `JOB_MANAGE` |
| `GET` | `/api/jobs/{id}/executions` | Xem lịch sử thực thi tác vụ | Required: `JOB_READ` |

---

## 4. Redis Distributed Lock & Scheduling Pattern

```
                       ┌─────────────────────────┐
                       │   Redis Cluster / Lock  │
                       └────────────┬────────────┘
                                    │
                                tryLock()
                                    │
           ┌────────────────────────┼────────────────────────┐
           │                        │                        │
           v                        v                        v
    Job Instance 1           Job Instance 2           Job Instance 3
    (Acquires Lock)          (Fails to Lock)          (Fails to Lock)
           │                        │                        │
           v                        v                        v
    Publish to Kafka             Bypass                   Bypass
```

### Mã giả thuật toán Lock (Pseudo-code):
```java
boolean isLocked = redisTemplate.opsForValue()
    .setIfAbsent("lock:job:" + jobId, instanceId, Duration.ofMinutes(5));

if (isLocked) {
    try {
        publishJobToKafka(jobExecution);
    } finally {
        releaseRedisLock("lock:job:" + jobId, instanceId);
    }
}
```
