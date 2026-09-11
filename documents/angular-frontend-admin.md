# Đặc Tả Kiến Trúc Angular Admin Frontend - DataFlow Platform

## 1. Tổng Quan Kiến Trúc (Architecture Overview)

Giao diện **DataFlow Admin Frontend** được thiết kế dựa trên framework **Angular 17** kết hợp thư viện giao diện chuẩn doanh nghiệp **Ant Design (NG-ZORRO 17)**. Thiết kế tuân theo phong cách Admin của hệ thống **BabySystem** với tone màu tối cao cấp (Dark Mode `#0f172a`), hiệu ứng kính làm mờ (Glassmorphism), typography hiện đại (**Outfit** & **JetBrains Mono**), cùng các bảng dữ liệu tương tác thời gian thực.

---

## 2. Cấu Trúc Thư Mục Dự Án Angular (`frontend/angular-app/`)

```
frontend/angular-app/
├── src/
│   ├── index.html                  # Thẻ meta SEO & Nạp font Google (Outfit & JetBrains Mono)
│   ├── main.ts                     # Entrypoint Bootstrap AppModule
│   ├── styles.css                  # CSS Toàn cục & Override Ant Design Dark Theme
│   └── app/
│       ├── app.component.ts        # Root Component chứa <router-outlet>
│       ├── app.module.ts           # Root Module nạp NG-ZORRO Modules & Interceptors
│       ├── app-routing.module.ts   # Routing cấp cao với Lazy Loading cho 6 Module chính
│       │
│       ├── core/                   # Các dịch vụ cốt lõi dùng chung (Singleton)
│       │   ├── guards/
│       │   │   └── auth.guard.ts   # Bảo vệ Route, kiểm tra JWT Token
│       │   ├── interceptors/
│       │   │   └── jwt.interceptor.ts # Tự động gắn Bearer Header & Refresh Token
│       │   └── services/
│       │       ├── auth.service.ts    # Đăng nhập, đăng xuất, lưu JWT & User Profile
│       │       ├── job.service.ts     # CRUD Cron Jobs, trigger, pause/resume & history
│       │       ├── export.service.ts  # Khởi tạo và theo dõi tiến độ Export Big Data (1M+ dòng)
│       │       ├── import.service.ts  # Upload kéo thả & nạp dữ liệu batch từ CSV/Excel
│       │       └── user.service.ts    # Quản lý tài khoản người dùng & Ma trận RBAC
│       │
│       ├── layout/                 # Khung Giao Diện Admin (BabySystem Style)
│       │   └── admin-layout/
│       │       ├── admin-layout.component.ts   # Điều khiển Collapsible Sidebar & Notification
│       │       ├── admin-layout.component.html # Layout nz-sider, nz-header & nz-content
│       │       └── admin-layout.component.css  # Styling glassmorphic sticky header & active menu
│       │
│       └── pages/                  # Các Trang Tính Năng (Lazy Loaded Modules)
│           ├── login/              # Trang Đăng nhập với Demo Mode Fallback
│           ├── dashboard/          # Trang Tổng quan Giám sát Real-time & Sơ đồ Kiến trúc
│           ├── jobs/               # Trang Quản lý Cron Jobs (Quartz Scheduler)
│           ├── exports/            # Trang Quản lý Tác vụ Xuất dữ liệu lớn & Kho MinIO File
│           ├── imports/            # Trang Kéo thả Upload & Nạp dữ liệu Batch
│           ├── users/              # Trang Quản lý Tài khoản & Gán Vai Trò
│           └── rbac/               # Trang Ma Trận Phân Quyền Chi Tiết (Role Matrix)
```

---

## 3. Các Trang Tính Năng Chi Tiết (Module Specs)

### 3.1 Layout Admin (BabySystem Reference Style)
- **Sidebar Cố Định (Fixed Left Sider)**: Tích hợp logo DataFlow Platform hiệu ứng phát sáng, danh sách Menu biểu tượng Ant Design với chỉ báo Badge đếm thông báo real-time.
- **Header Dính (Sticky Header)**: Breadcrumbs phân cấp đường dẫn, nút Toggle thu gọn Sidebar, khu vực Chuông thông báo (Notifications Drawer) và Menu Thông tin cá nhân (User Avatar & Logout).

### 3.2 Dashboard Component (`/dashboard`)
- **Metric Cards**: Hiển thị chỉ số Jobs Active, CPU/Memory Utilization, Kafka Event Throughput (msg/sec), MinIO Storage Used.
- **Live System Topology Diagram**: Sơ đồ dòng chảy dữ liệu tương tác SVG/Mermaid trực quan hóa luồng: `Client -> API Gateway -> Job/Auth Service -> Kafka Cluster -> MinIO Storage`.
- **Live Event Log Feeds**: Giả lập log dòng dữ liệu real-time theo chu kỳ với hiệu ứng nhấp nháy status badge.

### 3.3 Jobs Component (`/jobs`)
- **Quản lý Quartz Cron Jobs**: Hiển thị bảng Ant Design (`nz-table`) danh sách tác vụ với status badges (`SCHEDULED`, `RUNNING`, `PAUSED`, `FAILED`).
- **Nút Thao Tác Chạy Nhanh**:
  - `Chạy ngay`: Kích hoạt job thủ công qua API `/api/v1/jobs/{id}/trigger`.
  - `Tạm dừng / Kích hoạt`: Chuyển đổi trạng thái Scheduler.
  - `Lịch sử`: Mở Drawer xem chi tiết các đợt thực thi (duration ms, processed records, error logs).
- **Modal Tạo Job Mới**: Nhập tên job, nhóm (ANALYTICS, SYNC, FINANCE, MAINTENANCE) và chuẩn Quartz Cron syntax.

### 3.4 Exports Component (`/exports`)
- **Tiến Độ Real-time 1M+ Records**: Hiển thị các ô Progress Bar (%) hoạt họa cập nhật số lượng dòng đã xử lý.
- **Tải File Từ MinIO Bucket**: Nút tải xuống trực tiếp file `.csv`, `.xlsx`, `.parquet` khi status chuyển sang `COMPLETED`.

### 3.5 Imports Component (`/imports`)
- **Drag & Drop Upload Zone**: Sử dụng `nz-upload` loại `drag` cho phép kéo thả file lớn up to 500MB.
- **Tiến Độ Batch Ingestion**: Bảng danh sách các lô nạp dữ liệu với thống kê chi tiết `Số dòng thành công`, `Số dòng lỗi`, `Tiến độ parse %`.

### 3.6 Users & RBAC Matrix (`/users`, `/rbac`)
- **Quản lý Tài Khoản**: Bảng thông tin người dùng với Avatar, gán Roles, trạng thái Khóa/Mở khóa.
- **Fine-Grained Permission Matrix**: Modal tích chọn ma trận quyền thao tác granular (JOB_CREATE, JOB_TRIGGER, EXPORT_EXECUTE, USER_MANAGE) áp dụng tức thì cho nhóm vai trò.

---

## 4. Cơ Chế Fallback & Chế Độ Chạy Demo (Offline Fallback)

Toàn bộ các Angular Services (`AuthService`, `JobService`, `ExportService`, `ImportService`, `UserService`) đều tích hợp toán tử RxJS `catchError`. Khi backend Spring Boot hoặc API Gateway offline/chưa khởi chạy:
1. Hệ thống tự động bắt lỗi HTTP và fallback về dữ liệu **Mock Data chuẩn**.
2. Giao diện frontend vẫn hoạt động mượt mà, đầy đủ animation và tương tác real-time mà không bị treo trang hay văng lỗi console.

---

## 5. Hướng Dẫn Khởi Chạy Frontend

1. **Cài đặt thư viện dependencies**:
   ```bash
   cd frontend/angular-app
   npm install
   ```

2. **Chạy Server Phát Triển (Dev Server)**:
   ```bash
   ng serve --port 4200
   ```
   Sau đó truy cập: `http://localhost:4200` (Tài khoản demo: `admin` / `admin123`).

3. **Build Sản Phẩm Production**:
   ```bash
   ng build --configuration production
   ```
