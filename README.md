# 🚀 DataFlow Platform

**Enterprise Job & Data Processing Platform** — Hệ thống Microservices xử lý dữ liệu quy mô lớn (1,000,000+ bản ghi).

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black)](https://kafka.apache.org)
[![Angular](https://img.shields.io/badge/Angular-17-red)](https://angular.io)

---

## 📐 Kiến Trúc Hệ Thống

```
                           Angular Admin Client (Port 4200)
                                        │
                                        ▼
                           ┌─────────────────────────┐
                           │  API Gateway  (Port 8080) │
                           │  JWT Auth · CORS · CB     │
                           └────────────┬────────────┘
                                        │
               ┌────────────────────────┼──────────────────────┐
               │                        │                       │
               ▼                        ▼                       ▼
    ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────────┐
    │  Auth Service    │   │  Job Service     │   │  Export / Import Svc │
    │  Port 8081       │   │  Port 8082       │   │  Port 8083 / 8084    │
    │  JWT · RBAC      │   │  Redis Lock ·    │   │  Kafka · MinIO ·     │
    └────────┬─────────┘   │  Kafka Publisher │   │  Outbox Pattern      │
             │             └────────┬─────────┘   └──────────┬───────────┘
             ▼                      ▼                         ▼
        PostgreSQL            PostgreSQL              PostgreSQL · Redis
                                    │                         │
                                    └──────────┬──────────────┘
                                               ▼
                                        Apache Kafka
                                               │
                                        ┌──────┴──────┐
                                        ▼             ▼
                                      Redis          MinIO
                                   (Progress/Lock) (File Storage)
```

---

## ⚡ Quick Start — Khởi Chạy Nhanh

### Yêu Cầu
- **Java 17+**, **Maven 3.8+**
- **Docker Desktop** (ít nhất 8GB RAM)
- **Node.js 18+** + **Angular CLI** (nếu muốn chạy frontend)

### Option 1: Chỉ Infrastructure (Phát Triển)

```bash
# 1. Khởi chạy tất cả infrastructure
cd infrastructure/docker
docker-compose up -d postgres redis zookeeper kafka minio minio-init

# 2. Kiểm tra health
docker-compose ps

# 3. Khởi chạy từng microservice (từ root)
mvn spring-boot:run -pl services/auth-service   # Port 8081
mvn spring-boot:run -pl services/job-service    # Port 8082
mvn spring-boot:run -pl services/export-service # Port 8083
mvn spring-boot:run -pl services/import-service # Port 8084
mvn spring-boot:run -pl services/api-gateway    # Port 8080
```

### Option 2: Full Stack Docker (Production-like)

```bash
# 1. Build tất cả JAR
mvn clean package -DskipTests

# 2. Khởi chạy toàn bộ stack
cd infrastructure/docker
docker-compose up --build -d

# 3. Xem log
docker-compose logs -f api-gateway
```

### Frontend Angular

```bash
cd frontend/angular-app
npm install
ng serve --port 4200
# Truy cập: http://localhost:4200
# Demo login: admin / admin123
```

---

## 🗂️ Cấu Trúc Project

```
dataflow-platform/
├── services/
│   ├── api-gateway/          # Spring Cloud Gateway (Port 8080)
│   ├── auth-service/         # JWT + RBAC (Port 8081)
│   ├── job-service/          # Cron Job + Redis Lock (Port 8082)
│   ├── export-service/       # Export 1M+ records + Kafka + MinIO (Port 8083)
│   └── import-service/       # Import Excel/CSV Streaming (Port 8084)
├── frontend/
│   ├── angular-app/          # Angular 17 + NG-ZORRO Admin UI
│   └── index.html            # Standalone HTML Demo UI
├── infrastructure/
│   ├── docker/
│   │   └── docker-compose.yml  # Full stack compose
│   └── k8s/                    # Kubernetes manifests
├── documents/                  # Tài liệu kiến trúc
└── pom.xml                     # Maven Multi-module Parent
```

---

## 🔌 API Endpoints

| Service | Port | Endpoint chính |
|---|---|---|
| **API Gateway** | `8080` | Tất cả `/api/**` |
| **Auth Service** | `8081` | `POST /api/auth/login`, `POST /api/auth/register` |
| **Job Service** | `8082` | `GET /api/jobs`, `POST /api/jobs/{id}/execute` |
| **Export Service** | `8083` | `POST /api/exports/create`, `GET /api/exports/{id}/status` |
| **Import Service** | `8084` | `POST /api/imports/upload`, `GET /api/imports/{id}/status` |

---

## 🏗️ Tech Stack

| Layer | Công nghệ |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2, Spring Cloud Gateway |
| **Security** | Spring Security, JWT (JJWT 0.12), BCrypt, RBAC |
| **Database** | PostgreSQL 16, Spring Data JPA, Hibernate |
| **Cache / Lock** | Redis 7, Spring Data Redis |
| **Message Broker** | Apache Kafka (Confluent 7.5) |
| **Object Storage** | MinIO (S3-compatible) |
| **Frontend** | Angular 17, NG-ZORRO (Ant Design) |
| **Infrastructure** | Docker Compose, Kubernetes |
| **Build** | Maven 3.8, Multi-module |

---

## 📚 Tài Liệu

| Tài liệu | Mô tả |
|---|---|
| [dataflow-platform.md](documents/dataflow-platform.md) | Kiến trúc tổng thể hệ thống |
| [dataflow-operations.md](documents/dataflow-operations.md) | Hướng dẫn vận hành & Runbook |
| [interview-guide-dataflow.md](documents/interview-guide-dataflow.md) | System Design Q&A Senior |
| [api-gateway.md](documents/api-gateway.md) | Đặc tả API Gateway |
| [auth-service.md](documents/auth-service.md) | Đặc tả Auth Service |
| [job-service.md](documents/job-service.md) | Đặc tả Job Service |
| [export-service.md](documents/export-service.md) | Đặc tả Export Service |
| [import-service.md](documents/import-service.md) | Đặc tả Import Service |
| [angular-frontend-admin.md](documents/angular-frontend-admin.md) | Đặc tả Angular Admin UI |

---

## 🧪 Kiểm Thử E2E

```powershell
# Chạy E2E test toàn bộ luồng Export + Import
.\test-e2e-dataflow.ps1
```

---

## ☸️ Kubernetes Deployment

```bash
# 1. Tạo Namespace & Configs
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/configmap.yaml

# 2. Deploy tất cả Microservices
kubectl apply -f infrastructure/k8s/auth-service-deployment.yaml
kubectl apply -f infrastructure/k8s/job-service-deployment.yaml
kubectl apply -f infrastructure/k8s/export-service-deployment.yaml
kubectl apply -f infrastructure/k8s/import-service-deployment.yaml
kubectl apply -f infrastructure/k8s/api-gateway-deployment.yaml
kubectl apply -f infrastructure/k8s/ingress.yaml

# 3. Cài đặt Auto-scaling
kubectl apply -f infrastructure/k8s/hpa.yaml

# 4. Kiểm tra
kubectl get pods -n dataflow-platform
```

---

## 📊 Deployment Status

| Phase | Mô tả | Trạng thái |
|---|---|---|
| Phase 0 | Repository, Docker Compose, Tài liệu thiết kế | ✅ Hoàn thành |
| Phase 1 | Auth Service + RBAC + API Gateway | ✅ Hoàn thành |
| Phase 2 | Job Management Service (CRUD + History) | ✅ Hoàn thành |
| Phase 3 | Redis Distributed Lock (Chống Race-condition) | ✅ Hoàn thành |
| Phase 4 | Event-Driven Architecture (Kafka + DLQ) | ✅ Hoàn thành |
| Phase 5 | Export Service 1M+ records + MinIO | ✅ Hoàn thành |
| Phase 6 | Import Service Streaming + Batch Insert | ✅ Hoàn thành |
| Phase 7 | Transactional Outbox Pattern | ✅ Hoàn thành |
| Phase 8 | Resilience4j Circuit Breaker | ✅ Hoàn thành (API Gateway) |
| Phase 9 | Observability (OpenTelemetry + Prometheus) | 🔄 Kế hoạch |
| Phase 10 | Load Testing (JMeter / k6) | 🔄 Kế hoạch |
| Phase 11 | Kubernetes Orchestration | ✅ Hoàn thành |
| Phase 12 | Chaos Engineering | 🔄 Kế hoạch |
