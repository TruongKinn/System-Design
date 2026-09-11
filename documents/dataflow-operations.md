# Tài Liệu Vận Hành & Khắc Phục Sự Cố — DataFlow Platform
**DataFlow Platform Operations Guide & Incident Runbook**

Tài liệu này cung cấp quy trình chi tiết cho Đội ngũ Vận hành (DevOps / SRE / System Admin) để khởi chạy, giám sát, khắc phục sự cố và bảo trì hệ thống DataFlow Platform trên môi trường Local Docker và Kubernetes Production.

---

## 1. Thứ tự Khởi chạy Hệ thống (System Startup Sequence)

### Bước 1: Khởi chạy Hạ tầng cơ sở (Infrastructure Layer)
Chạy lệnh bên trong thư mục `infrastructure/docker/`:
```bash
docker-compose up -d
```

#### Kiểm tra Health Check Hạ tầng:
- **MySQL 8.0**: `docker exec -it dataflow-mysql mysqladmin -u root -prootpassword ping` -> Trả về `mysqld is alive`.
- **Redis 7**: `docker exec -it dataflow-redis redis-cli ping` -> Trả về `PONG`.
- **Apache Kafka**: `docker exec -it dataflow-kafka kafka-topics --bootstrap-server localhost:9092 --list`.
- **MinIO S3**: Mở trình duyệt tại `http://localhost:9001` (User: `minioadmin` / Pass: `minioadmin`).

---

### Bước 2: Khởi chạy Microservices theo Đúng Thứ tự (Service Layer)

```
        ┌────────────────────────────────────────────────────────┐
        │  1. Auth Service (Port 8081)                           │
        │     - Khởi tạo DB Schema & Seed Quyền RBAC             │
        └───────────────────────────┬────────────────────────────┘
                                    │
                                    v
        ┌────────────────────────────────────────────────────────┐
        │  2. Job Service (Port 8082), Export Svc (Port 8083),   │
        │     Import Svc (Port 8084)                             │
        └───────────────────────────┬────────────────────────────┘
                                    │
                                    v
        ┌────────────────────────────────────────────────────────┐
        │  3. API Gateway (Port 8080)                            │
        │     - Point of Entry, Route requests & JWT Validation  │
        └────────────────────────────────────────────────────────┘
```

#### Lệnh Maven Build & Launch từng Microservice:
```bash
# 1. Start Auth Service
mvn spring-boot:run -pl services/auth-service

# 2. Start Job Service
mvn spring-boot:run -pl services/job-service

# 3. Start Export Service
mvn spring-boot:run -pl services/export-service

# 4. Start Import Service
mvn spring-boot:run -pl services/import-service

# 5. Start API Gateway
mvn spring-boot:run -pl services/api-gateway
```

---

## 2. Quy trình Triển khai Kubernetes Production (K8s Deployment)

### 1. Thao tác Deploy toàn bộ Cluster:
```bash
# 1. Tạo Namespace & Configs
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/configmap.yaml

# 2. Deploy toàn bộ Microservices
kubectl apply -f infrastructure/k8s/auth-service-deployment.yaml
kubectl apply -f infrastructure/k8s/api-gateway-deployment.yaml
kubectl apply -f infrastructure/k8s/export-service-deployment.yaml
kubectl apply -f infrastructure/k8s/ingress.yaml
```

### 2. Kiểm tra Trạng thái Pods & Scaling:
```bash
# Xem danh sách Pods
kubectl get pods -n dataflow-platform -o wide

# Cấu hình Horizontal Pod Autoscaler (HPA) cho Export Workers:
kubectl autoscale deployment export-service -n dataflow-platform --cpu-percent=80 --min=4 --max=20
```

---

## 3. Quy trình Giám sát (Observability & Monitoring)

### Metrics quan trọng cần cài đặt Alerting trên Grafana:

| Metric Name | Ngưỡng Cảnh báo (Alert Threshold) | Hành động Vận hành |
| :--- | :--- | :--- |
| **HTTP Error Rate (5xx)** | `> 1%` trong 5 phút | Kiểm tra log Gateway & Fallback Controller |
| **Kafka Consumer Lag** | `> 5,000` messages | Tăng số lượng Replica Worker Nodes (`kubectl scale`) |
| **Redis Memory Usage** | `> 85%` Ram Limit | Kiểm tra TTL các keys progress & lock |
| **JVM Heap Usage** | `> 85%` Max Heap | Kiểm tra Memory Leak trong Export/Import Stream |
| **DB Hikari Connection Pool** | `Active == MaxConnections` | Tăng `maximum-pool-size` hoặc tối ưu slow query |

---

## 4. Runbook Xử lý Sự cố Thường gặp (Incident Runbooks)

### 🚨 Kịch bản 1: Consumer Lag tăng cao trên Kafka Topic `data.export.request`
- **Trieu chứng**: User phản ánh tiến độ Export dừng ở 25-50% không chạy tiếp.
- **Hành động ngay**:
  1. Kiểm tra Kafka lag: `kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group export-worker-group`.
  2. Scale thêm số lượng Worker Pods: `kubectl scale deployment/export-service --replicas=8 -n dataflow-platform`.
  3. Kiểm tra log của Worker: `kubectl logs -l app=export-service -n dataflow-platform --tail=200`.

### 🚨 Kịch bản 2: Sự cố Mất kết nối Redis (Redis Downtime / Failover)
- **Triệu chứng**: Xuất hiện Warning `Redis unreachable, falling back to local memory lock`.
- **Hành động ngay**:
  1. Hệ thống đã được thiết kế cơ chế **Local Memory Fallback**, dịch vụ vẫn hoạt động bình thường trên từng Node độc lập.
  2. Khởi động lại container Redis: `docker restart dataflow-redis` hoặc `kubectl rollout restart deployment/redis`.

### 🚨 Kịch bản 3: Sập dịch vụ Downstream (VD: Export Service quá tải)
- **Triệu chứng**: Client nhận được HTTP `503 Service Unavailable` từ API Gateway.
- **Hành động ngay**:
  1. API Gateway tự động kích hoạt [FallbackController.java](file:///d:/AI-AGENT/System%20Design/services/api-gateway/src/main/java/com/dataflow/gateway/controller/FallbackController.java) bảo vệ hệ thống không bị sập dây chuyền (Cascading Failure).
  2. Kiểm tra tài nguyên RAM/CPU của Export Service và nâng giới hạn limit trong Kubernetes manifest.

---

## 5. Quy trình Sao lưu & Phục hồi Dữ liệu (Backup & Disaster Recovery)

### 1. Sao lưu CSDL MySQL (Daily Backup):
```bash
docker exec dataflow-mysql mysqldump -u root -prootpassword --all-databases > backup_dataflow_$(date +%Y%m%d).sql
```

### 2. Phục hồi CSDL MySQL:
```bash
docker exec -i dataflow-mysql mysql -u root -prootpassword < backup_dataflow_20260911.sql
```

### 3. Sao lưu Object Storage MinIO Files:
```bash
mc mirror local/dataflow-exports /backup/minio/dataflow-exports
```
