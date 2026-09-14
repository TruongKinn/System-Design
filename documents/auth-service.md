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
1. `users`: `id`, `username`, `password`, `email`, `full_name`, `status` (`ACTIVE`, `LOCKED`, `INACTIVE`), `created_at`, `updated_at`.
2. `roles`: `id`, `name` (VD: `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_VIEWER`), `description`.
3. `permissions`: `id`, `code` (VD: `JOB_READ`, `JOB_EXECUTE`, `EXPORT_DATA`), `description`.
4. `user_roles`: `user_id`, `role_id`.
5. `role_permissions`: `role_id`, `permission_id`.

---

## 3. Danh sách API (API Endpoints)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Đăng nhập và nhận JWT Access Token & Refresh Token | Public |
| `POST` | `/api/auth/register` | Đăng ký tài khoản người dùng mới | Public |
| `POST` | `/api/auth/refresh` | Cấp Access Token mới từ Refresh Token | Public |
| `GET` | `/api/auth/me` | Lấy thông tin user hiện tại và danh sách Permissions | Bearer Token |
| `GET` | `/api/users` | Danh sách toàn bộ người dùng và vai trò hệ thống | Bearer Token |
| `POST` | `/api/users` | Tạo mới người dùng (mã hóa mật khẩu BCrypt, gán roles) | Bearer Token |
| `PUT` | `/api/users/{id}/toggle-status` | Khóa / Mở khóa tài khoản người dùng | Bearer Token |

---

## 4. Quy trình Đăng nhập & JWT Validation (Authentication Flow)

```
Angular Client                API Gateway                Auth Service
      │                            │                           │
      │─── 1. POST /auth/login ───>│                           │
      │                            │──── 2. Forward Request ──>│
      │                            │                           │── 3. Validate User/Password
      │                            │                           │── 4. Generate JWT
      │                            │<─── 5. Return JWT Token ──│
      │<── 6. Return Response ─────│                           │
      │                            │                           │
      │─── 7. GET /api/jobs ──────>│                           │
      │    (Bearer Token)          │── 8. Verify JWT Signature │
      │                            │── 9. Inject X-User-Id,    │
      │                            │      X-User-Roles         │
      │                            │──────── Direct to Job Service ─────>
```

---

## 5. Security Best Practices
- **Password Encoding**: Sử dụng `BCryptPasswordEncoder` với độ dài Work Factor = 12.
- **JWT Claims**: Chứa `sub` (User ID), `username`, `roles`, `permissions`, `exp` (15-60 phút).
- **Stateless Verification**: API Gateway tự verify chữ ký JWT public/private key hoặc Secret HMAC256 để không cần gọi sang Auth Service với mỗi request.
