# Service Specification: Auth Service
**Tài liệu thiết kế chi tiết dịch vụ Xác thực & Phân quyền (Auth Service)**

## 1. Mục tiêu (Overview)
Auth Service chịu trách nhiệm:
- Xắc thực người dùng qua Tên đăng nhập / Mật khẩu.
- Cấp phát Access Token (JWT) và Refresh Token.
- Quản lý Vai trò (Role) và Quyền hạn (Permission) theo mô hình RBAC (Role-Based Access Control).
- Xử lý Đăng xuất, Refresh Token & Thu hồi Token (Blacklist Redis nếu cần).

---

## 2. Mô hình Dữ liệu (Database ERD - `auth_db`)

```
   ┌──────────┐           ┌──────────────┐           ┌──────────┐
   │  users   │──────────<│  user_roles  │>──────────│  roles   │
   └──────────┘           └──────────────┘           └────┬─────┘
                                                          │
                                                          v
                                                 ┌──────────────────┐
                                                 │ role_permissions │
                                                 └────────┬─────────┘
                                                          │
                                                          v
                                                 ┌──────────────────┐
                                                 │   permissions    │
                                                 └──────────────────┘
```

### Chi tiết các bảng (Tables):
32: 1. `users`: `id`, `username`, `password`, `raw_password`, `email`, `full_name`, `status` (`ACTIVE`, `LOCKED`, `INACTIVE`), `last_login_at`, `created_at`, `updated_at`.
33: 2. `roles`: `id`, `name` (VD: `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_VIEWER`), `description`.
34: 3. `permissions`: `id`, `code` (VD: `JOB_READ`, `JOB_EXECUTE`, `EXPORT_DATA`), `description`.
35: 4. `user_roles`: `user_id`, `role_id`.
36: 5. `role_permissions`: `role_id`, `permission_id`.
37: 
38: ---
39: 
40: ## 3. Danh sách API (API Endpoints)
41: 
42: | Method | Endpoint | Description | Auth Required |
43: | :--- | :--- | :--- | :--- |
44: | `POST` | `/api/auth/login` | Đăng nhập và nhận JWT Access Token & Refresh Token, cập nhật `lastLoginAt` | Public |
45: | `POST` | `/api/auth/register` | Đăng ký tài khoản người dùng mới | Public |
46: | `POST` | `/api/auth/refresh` | Cấp Access Token mới từ Refresh Token | Public |
47: | `GET` | `/api/auth/me` | Lấy thông tin user hiện tại và danh sách Permissions | Bearer Token |
48: | `GET` | `/api/users` | Danh sách toàn bộ người dùng và vai trò hệ thống | Bearer Token |
49: | `POST` | `/api/users` | Tạo mới người dùng (mã hóa mật khẩu BCrypt, gán roles) | Bearer Token |
50: | `PUT` | `/api/users/{id}/toggle-status` | Khóa / Mở khóa tài khoản người dùng | Bearer Token |
51: | `GET` | `/api/roles` | Lấy danh sách Roles kèm ma trận quyền hạn và số lượng users | Bearer Token |
52: | `PUT` | `/api/roles/{id}/permissions` | Cập nhật ma trận quyền hạn (RBAC granular permissions) cho Role | Bearer Token |
53: 
54: ---
55: 
56: ## 4. Quy trình Đăng nhập & JWT Validation (Authentication Flow)
57: 
58: ```
59: Angular Client                API Gateway                Auth Service
60:       │                            │                           │
61:       │─── 1. POST /auth/login ───>│                           │
62:       │                            │──── 2. Forward Request ──>│
63:       │                            │                           │── 3. Validate User/Password
64:       │                            │                           │── 4. Update last_login_at
65:       │                            │                           │── 5. Generate JWT
66:       │                            │<─── 6. Return JWT Token ──│
67:       │<── 7. Return Response ─────│                           │
68:       │                            │                           │
69:       │─── 8. GET /api/jobs ──────>│                           │
70:       │    (Bearer Token)          │── 9. Verify JWT Signature │
71:       │                            │── 10. Inject X-User-Id,   │
72:       │                            │      X-User-Roles         │
73:       │                            │──────── Direct to Job Service ─────>
74: ```
75: 
76: ---
77: 
78: ## 5. Security Best Practices
79: - **Password Encoding**: Sử dụng `BCryptPasswordEncoder` với độ dài Work Factor = 12.
80: - **JWT Claims**: Chứa `sub` (User ID), `username`, `roles`, `permissions`, `exp` (15-60 phút).
81: - **Stateless Verification**: API Gateway tự verify chữ ký JWT public/private key hoặc Secret HMAC256 để không cần gọi sang Auth Service với mỗi request.
82: 
83: ---
84: 
85: ## 6. Tính Năng Quản Lý Mật Khẩu (Show Password) & Nhật Ký Đăng Nhập (Update v1.1)
86: 
87: ### 6.1. Phân Quyền Hiển Thị Mật Khẩu (`ROLE_ADMIN`)
88: - Chỉ tài khoản mang vai trò cao nhất trong hệ thống (`ROLE_ADMIN`) mới có quyền xem cột **Mật khẩu** trên bảng quản lý người dùng `/admin/users`.
89: - Các vai trò khác (`ROLE_OPERATOR`, `ROLE_VIEWER`, `ROLE_USER`) bị ẩn cột hoàn toàn.
90: - Trạng thái mặc định: Mật khẩu được che dưới dạng `••••••••`.
91: - Quản trị viên có thể bấm biểu tượng con mắt để xem chi tiết mật khẩu và bấm nút copy để sao chép vào clipboard.
92: 
93: ### 6.2. Ghi Nhận Lịch Sử Đăng Nhập Lần Cuối (`lastLoginAt`)
94: - Khi người dùng gửi request `POST /api/auth/login` và xác thực thành công qua `authenticationManager.authenticate()`, hệ thống cập nhật `user.setLastLoginAt(LocalDateTime.now())` và lưu vào PostgreSQL.
95: - API `GET /api/users` trả về trường `lastLoginAt` đã định dạng cho từng người dùng.
96: - Trên giao diện hiển thị biểu tượng xác nhận màu xanh kèm thời gian đăng nhập thực tế (Ví dụ: `11/10/2026 16:19`), các tài khoản chưa từng đăng nhập sẽ hiển thị nhãn *Chưa đăng nhập*.
