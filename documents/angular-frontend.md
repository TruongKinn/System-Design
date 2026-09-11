# Frontend Specification: Angular Admin Portal (Ant Design / BabySystem Layout)
**Tài liệu thiết kế chi tiết Giao diện Quản trị Angular + NG-ZORRO (Ant Design)**

## 1. Tổng quan Giao diện (UI Overview)

Giao diện Quản trị của hệ thống DataFlow Platform được xây dựng dựa trên nền tảng **Angular** và thư viện UI **NG-ZORRO (Ant Design)**, lấy cảm hứng từ cấu trúc trang Admin của hệ thống BabySystem:
- **Layout Cốt lõi (Enterprise Layout)**:
  - **Sider Navbar**: Sidebar bên trái có thể thu gọn (`nz-sider`), chứa Menu điều hướng phân cấp (Dashboard, Quản lý Jobs, Xuất/Nhập dữ liệu 1M, Quản lý Người dùng & RBAC, Monitoring).
  - **Header**: Thanh tiêu đề trên cùng chứa Breadcrumbs, Thông báo Realtime (`nz-badge`), Công tắc đổi Theme (Dark/Light Mode) và Avatar Admin.
  - **Content Area**: Khu vực hiển thị bảng dữ liệu (`nz-table`), thẻ thống kê (`nz-statistic`), thanh tiến độ (`nz-progress`), và các Hộp thoại Modal/Drawer (`nz-modal`, `nz-drawer`).
  - **Footer**: Bản quyền hệ thống & Trạng thái kết nối Microservices.

---

## 2. Cấu trúc Thư mục Dự án Angular (`frontend/angular-app`)

```
frontend/angular-app/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── guards/          # AuthGuard, RoleGuard
│   │   │   ├── interceptors/    # JwtInterceptor (Tự động gắn Bearer Token)
│   │   │   └── services/        # AuthService, JobService, ExportService, ImportService
│   │   ├── layout/
│   │   │   ├── admin-layout/    # BabySystem Admin Sidebar & Header Layout
│   │   │   └── auth-layout/     # Trang Đăng nhập (Login Component)
│   │   ├── pages/
│   │   │   ├── dashboard/       # Thống kê tổng quan & Sơ đồ hạ tầng
│   │   │   ├── jobs/            # Quản lý Cron Jobs (NZ-Table, Modal, Run Now)
│   │   │   ├── exports/         # Xuất dữ liệu lớn 1M bản ghi + Progress Bar + MinIO Link
│   │   │   └── imports/         # Import Excel Streaming + Batch Insert Status
│   │   ├── app.module.ts
│   │   └── app-routing.module.ts
│   ├── assets/
│   ├── styles.scss              # Ant Design Custom Scss Theme
│   └── index.html
└── package.json
```

---

## 3. Các Tính năng Màn hình Quản trị (Admin Screens Spec)

### 1. Màn hình Đăng nhập (Login Component)
- Form Đăng nhập Ant Design (`nz-form`, `nz-input-group`).
- Tự động lưu JWT Access Token & Refresh Token vào `localStorage`.
- Chuyển hướng tới trang Dashboard sau khi đăng nhập thành công.

### 2. Bảng điều khiển Quản lý Jobs (Jobs Management Component)
- Sử dụng `nz-table` hiển thị danh sách Cron Jobs, trạng thái Active/Pause, Biểu thức Cron, Endpoint dịch vụ đích.
- Nút bấm action bằng Ant Design buttons (`nz-button` type `primary`, `default`, `danger`):
  - **▶ Chạy ngay (Run Now)**: Gửi request `POST /api/jobs/{id}/execute` và hiển thị Toast thông báo (`nzMessageService.success()`).
  - **⏸ Tạm dừng (Pause)** / **▶ Khôi phục (Resume)**.
  - **➕ Tạo Job mới**: Mở Hộp thoại Modal (`nz-modal`) với Form validate.

### 3. Xuất dữ liệu Lớn 1M Bản ghi (Large Data Export Component)
- Form lựa chọn loại dữ liệu (Khách hàng, Giao dịch, Audit Log) và số lượng bản ghi (1,000,000).
- Thanh tiến độ Ant Design Progress (`nz-progress` type `line` status `active`).
- Polling tự động từ API `/api/exports/{id}/status` mỗi 1 giây. Khi đạt 100%, hiển thị nút tải file (`nz-button` type `primary` icon `download`) dẫn tới MinIO Presigned URL.

### 4. Nhập dữ liệu Lớn Excel Streaming (Large Data Import Component)
- Ant Design Upload Drag-and-Drop Area (`nz-upload` type `drag`).
- Hỗ trợ xem danh sách dòng dữ liệu bị lỗi nếu có.

---

## 4. Tích hợp JWT HttpInterceptor (Bearer Token Insertion)

Mọi HTTP request phát ra từ Angular App đều tự động được đính kèm Header:
```typescript
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const token = localStorage.getItem('access_token');
        if (token) {
            req = req.clone({
                setHeaders: {
                    Authorization: `Bearer ${token}`
                }
            });
        }
        return next.handle(req);
    }
}
```
