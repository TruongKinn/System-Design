# Service Specification: Auth Service
**Tài liệu thiết kế chi tiết dịch vụ Xác thực & Phân quyền (Auth Service)**

## 1. Mục tiêu (Overview)
Auth Service chịu trách nhiệm:
- Xác thực người dùng qua Tên đăng nhập / Mật khẩu.
- Cấp phát Access Token (JWT) và Refresh Token.
- Quản lý Vai trò (Role) và Quyền hạn (Permission) theo mô hình RBAC (Role-Based Access Control).
- Xử lý Đổi Mật Khẩu (Cá nhân tự đổi mật khẩu & Quản trị viên cấp lại mật khẩu cho tài khoản).
- Mã hóa dữ liệu nhạy cảm lưu trong Database (Encryption at Rest) bằng thuật toán **AES-256-GCM**.
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
1. `users`: `id`, `username`, `password`, `raw_password` (Mã hóa AES-256-GCM), `email`, `full_name`, `status` (`ACTIVE`, `LOCKED`, `INACTIVE`), `last_login_at`, `created_at`, `updated_at`.
2. `roles`: `id`, `name` (VD: `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_VIEWER`), `description`.
3. `permissions`: `id`, `code` (VD: `JOB_READ`, `JOB_EXECUTE`, `EXPORT_DATA`), `description`.
4. `user_roles`: `user_id`, `role_id`.
5. `role_permissions`: `role_id`, `permission_id`.

---

## 3. Danh sách API (API Endpoints)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Đăng nhập và nhận JWT Access Token & Refresh Token, cập nhật `lastLoginAt` | Public |
| `POST` | `/api/auth/register` | Đăng ký tài khoản người dùng mới | Public |
| `POST` | `/api/auth/refresh` | Cấp Access Token mới từ Refresh Token | Public |
| `GET` | `/api/auth/me` | Lấy thông tin user hiện tại và danh sách Permissions | Bearer Token |
| `POST` | `/api/auth/change-password` | Người dùng tự đổi mật khẩu cá nhân (yêu cầu mật khẩu cũ) | Bearer Token |
| `GET` | `/api/users` | Danh sách toàn bộ người dùng và vai trò hệ thống | Bearer Token |
| `POST` | `/api/users` | Tạo mới người dùng (mã hóa mật khẩu BCrypt, gán roles) | Bearer Token |
| `PUT` | `/api/users/{id}/toggle-status` | Khóa / Mở khóa tài khoản người dùng | Bearer Token |
| `PUT` | `/api/users/{id}/reset-password` | Quản trị viên (`ROLE_ADMIN`) thiết lập lại mật khẩu cho người dùng | Bearer Token |
| `GET` | `/api/roles` | Lấy danh sách Roles kèm ma trận quyền hạn và số lượng users | Bearer Token |
| `PUT` | `/api/roles/{id}/permissions` | Cập nhật ma trận quyền hạn (RBAC granular permissions) cho Role | Bearer Token |

---

## 4. Quy trình Đăng nhập & JWT Validation (Authentication Flow)

```
Angular Client                API Gateway                Auth Service
      │                            │                           │
      │─── 1. POST /auth/login ───>│                           │
      │                            │──── 2. Forward Request ──>│
      │                            │                           │── 3. Validate User/Password
      │                            │                           │── 4. Update last_login_at
      │                            │                           │── 5. Generate JWT
      │                            │<─── 6. Return JWT Token ──│
      │<── 7. Return Response ─────│                           │
      │                            │                           │
      │─── 8. GET /api/jobs ──────>│                           │
      │    (Bearer Token)          │── 9. Verify JWT Signature │
      │                            │── 10. Inject X-User-Id,   │
      │                            │      X-User-Roles         │
      │                            │──────── Direct to Job Service ─────>
```

---

## 5. Security Best Practices
- **Password Encoding**: Sử dụng `BCryptPasswordEncoder` với độ dài Work Factor = 12.
- **Mã Hóa Dữ Liệu Trong DB (Encryption at Rest - AES-256-GCM)**: Dữ liệu nhạy cảm lưu trữ trong Database được mã hóa theo chuẩn Authenticated Encryption.
- **JWT Claims**: Chứa `sub` (User ID), `username`, `roles`, `permissions`, `exp` (15-60 phút).
- **Stateless Verification**: API Gateway tự verify chữ ký JWT public/private key hoặc Secret HMAC256 để không cần gọi sang Auth Service với mỗi request.

---

## 6. Tính Năng Quản Lý Mật Khẩu (Show Password & Đổi Password) & Nhật Ký Đăng Nhập

### 6.1. Phân Quyền Hiển Thị Mật Khẩu (`ROLE_ADMIN`)
- Chỉ tài khoản mang vai trò cao nhất trong hệ thống (`ROLE_ADMIN`) mới có quyền xem cột **Mật khẩu** trên bảng quản lý người dùng `/admin/users`.
- Các vai trò khác (`ROLE_OPERATOR`, `ROLE_VIEWER`, `ROLE_USER`) bị ẩn cột hoàn toàn.
- Trạng thái mặc định: Mật khẩu được che dưới dạng `••••••••`.
- Quản trị viên có thể bấm biểu tượng con mắt để xem chi tiết mật khẩu và bấm nút copy để sao chép vào clipboard.

### 6.2. Tính Năng Đổi Mật Khẩu Toàn Diện (Password Management)
1. **Quản trị viên đổi mật khẩu cho người dùng (`PUT /api/users/{id}/reset-password`)**:
   - Tại cột Hành động trên bảng người dùng, click nút **"Đổi Pass"** (icon chìa khóa).
   - Mở modal nhập mật khẩu mới và xác nhận mật khẩu.
   - Hệ thống mã hóa BCrypt và cập nhật `raw_password` mới trong DB ngay lập tức.
2. **Người dùng tự đổi mật khẩu cá nhân (`POST /api/auth/change-password`)**:
   - Truy cập từ menu avatar góc phải trên Header -> **"Hồ sơ cá nhân"** -> nút **"Đổi Mật Khẩu"**.
   - Yêu cầu cung cấp: Mật khẩu hiện tại, Mật khẩu mới (tối thiểu 6 ký tự), Xác nhận mật khẩu mới.
   - Xác thực đúng mật khẩu cũ trước khi cho phép cập nhật.

### 6.3. Ghi Nhận Lịch Sử Đăng Nhập Lần Cuối (`lastLoginAt`)
- Khi người dùng gửi request `POST /api/auth/login` và xác thực thành công qua `authenticationManager.authenticate()`, hệ thống cập nhật `user.setLastLoginAt(LocalDateTime.now())` và lưu vào PostgreSQL.
- API `GET /api/users` trả về trường `lastLoginAt` đã định dạng cho từng người dùng.
- Trên giao diện hiển thị biểu tượng xác nhận màu xanh kèm thời gian đăng nhập thực tế (Ví dụ: `11/10/2026 16:19`), các tài khoản chưa từng đăng nhập sẽ hiển thị nhãn *Chưa đăng nhập*.

---

## 7. Kiến Trúc Mã Hóa AES-256-GCM Trong Cơ Sở Dữ Liệu (Encryption at Rest - Update v1.3)

### 7.1. Tại sao lựa chọn AES-GCM (Galois/Counter Mode)?
- **Authenticated Encryption with Associated Data (AEAD)**: Khác với AES-CBC truyền thống cần thêm HMAC để kiểm tra tính toàn vẹn, AES-GCM tự động sinh ra một Authentication Tag (128-bit) song song với Ciphertext.
- **Chống Giả Mạo & Tấn Công Bit-Flipping**: Nếu bất kỳ byte nào của dữ liệu trong Database bị chỉnh sửa trái phép, hàm giải mã GCM sẽ ném ngoại lệ và từ chối nạp dữ liệu.
- **Random Initialization Vector (IV 12-byte)**: Mỗi lần mã hóa một chuỗi, hệ thống sinh ra một IV ngẫu nhiên 96-bit (`SecureRandom`). Cùng một mật khẩu nhưng hai lần lưu xuống DB sẽ sinh ra hai chuỗi Ciphertext hoàn toàn khác biệt.

### 7.2. Cấu trúc Chuỗi Mã Hóa Lưu Trữ
Định dạng chuỗi lưu trong PostgreSQL:
```
ENC:Base64( [12-byte IV] + [Ciphertext] + [16-byte Auth Tag] )
```
Ví dụ thực tế trong bảng `users`:
```sql
SELECT username, raw_password FROM users WHERE username = 'admin';
-- Kết quả:
-- ENC:XelntBiJokFWjMlgfuC+D2al53W+tZAzM64VqO4GA869INJ4iA==
```

### 7.3. Triển khai Kỹ Thuật (JPA AttributeConverter)
- **`AesGcmEncryptionService`**: Cung cấp hàm `encrypt()` và `decrypt()`, nạp khóa bí mật 256-bit từ cấu hình `app.security.aes-gcm.secret-key`.
- **`AesGcmAttributeConverter`**: Hiện thực `AttributeConverter<String, String>` với `@Converter`.
  - Khi JPA thực thi `INSERT` hoặc `UPDATE`: Tự động gọi `convertToDatabaseColumn()` để mã hóa trước khi ghi vào Database.
  - Khi JPA thực thi `SELECT`: Tự động gọi `convertToEntityAttribute()` để giải mã trước khi trả về cho Java Entity.
- **Độ trong suốt (Transparency)**: Tầng Business Logic và Service không cần can thiệp thủ công, dữ liệu được bảo vệ an toàn ở tầng lưu trữ và giải mã tự động trong RAM khi xử lý.
