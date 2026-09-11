# Service Specification: API Gateway
**Tài liệu thiết kế chi tiết dịch vụ Cổng kết nối (API Gateway)**

## 1. Mục tiêu (Overview)
API Gateway là điểm truy cập duy nhất (Single Entry Point) cho toàn bộ ứng dụng DataFlow Platform. Gateway giải quyết các vấn đề liên dịch vụ:
- **Request Routing**: Định tuyến các request HTTP từ Client tới các Microservices phía sau.
- **Authentication & Claims Forwarding**: Xử lý giải mã JWT, kiểm tra tính hợp lệ và đính kèm `X-User-Id`, `X-User-Permissions` vào Header chuyển tiếp sang downstream services.
- **Rate Limiting**: Giới hạn số lượng request per IP / User bằng Redis Token Bucket Algorithm.
- **CORS Handling**: Cấu hình CORS cho phép ứng dụng Frontend Angular kết nối an toàn.
- **Circuit Breaker & Fallback**: Bảo vệ hệ thống khi dịch vụ phía sau bị sập hoặc phản hồi chậm.

---

## 2. Request Flow & Routing Rules

```
                      ┌─────────────────────────────────┐
                      │    Spring Cloud API Gateway     │
                      └────────────────┬────────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │ Path: /api/auth/**       │ Path: /api/jobs/**       │ Path: /api/exports/**
            v                          v                          v
     ┌──────────────┐           ┌──────────────┐           ┌──────────────┐
     │ Auth Service │           │ Job Service  │           │ Export Svc   │
     │  (Port 8081) │           │  (Port 8082) │           │  (Port 8083) │
     └──────────────┘           └──────────────┘           └──────────────┘
```

### Routing Configuration Spec (`application.yml` preview)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**, /api/users/**
        - id: job-service
          uri: lb://job-service
          predicates:
            - Path=/api/jobs/**
        - id: export-service
          uri: lb://export-service
          predicates:
            - Path=/api/exports/**
        - id: import-service
          uri: lb://import-service
          predicates:
            - Path=/api/imports/**
```

---

## 3. Dynamic Rate Limiting with Redis

- **Algorithim**: Token Bucket Filter via `RequestRateLimiterGatewayFilterFactory`.
- **Key Resolver**: 
  - Đã đăng nhập: Giới hạn theo User ID (`X-User-Id`). Max: 100 requests/phút.
  - Chưa đăng nhập: Giới hạn theo Client IP. Max: 20 requests/phút.

## 5. Circuit Breaker Fallback & Distributed Tracing

### Structured Fallback JSON Response (`GET /fallback`):
Khi dịch vụ downstream bị hỏng hoặc phản hồi quá 5000ms, API Gateway tự động kích hoạt Fallback:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Phản hồi dịch vụ bị chậm hoặc tạm thời ngắt kết nối. Vui lòng thử lại sau.",
  "timestamp": "2026-09-11T20:06:00"
}
```

### Correlation ID / Trace Header:
Mọi HTTP Request đi qua Gateway đều được gán hoặc duy trì `X-Correlation-ID` (UUID) để truy vết Log xuyên suốt các Microservices (Centralized Distributed Tracing).

