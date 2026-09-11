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
1. `users`: `id`, `username`, `password_hash`, `email`, `status`, `created_at`, `updated_at`.
2. `roles`: `id`, `name` (VD: `ROLE_ADMIN`, `ROLE_OPERATOR`), `description`.
3. `permissions`: `id`, `code` (VD: `JOB_READ`, `JOB_EXECUTE`, `EXPORT_DATA`), `description`.
4. `user_roles`: `user_id`, `role_id`.
5. `role_permissions`: `role_id`, `permission_id`.

---

## 3. Danh sách API (API Endpoints)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Đăng nhập và nhận JWT Access Token & Refresh Token | Public |
| `POST` | `/api/auth/refresh` | Cấp Access Token mới từ Refresh Token | Public |
| `POST` | `/api/auth/logout` | Đăng xuất và vô hiệu hóa Refresh Token | Bearer Token |
| `GET` | `/api/users/me` | Lấy thông tin user hiện tại và danh sách Permissions | Bearer Token |
| `GET` | `/api/users` | Danh sách người dùng (Dành cho Admin) | Required: `USER_READ` |

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
