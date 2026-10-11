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
- **`AesGcmEncryptionService`**: Cung cấp hàm `encrypt()` và `decrypt()`, nạp khóa bí mật 256-bit trực tiếp từ HashiCorp Vault thông qua `VaultSecretService` khi khởi động.
- **`AesGcmAttributeConverter`**: Hiện thực `AttributeConverter<String, String>` với `@Converter`.
  - Khi JPA thực thi `INSERT` hoặc `UPDATE`: Tự động gọi `convertToDatabaseColumn()` để mã hóa trước khi ghi vào Database.
  - Khi JPA thực thi `SELECT`: Tự động gọi `convertToEntityAttribute()` để giải mã trước khi trả về cho Java Entity.
- **Độ trong suốt (Transparency)**: Tầng Business Logic và Service không cần can thiệp thủ công, dữ liệu được bảo vệ an toàn ở tầng lưu trữ và giải mã tự động trong RAM khi xử lý.

---

## 8. Tích Hợp HashiCorp Vault Cho Khóa Mã Hóa AES-256-GCM (Production Grade)

### 8.1. Đặt Vấn Đề & Giải Pháp
- **Vấn đề trên môi trường Production**: Khóa bí mật (Master Key / Encryption Key) không được phép lưu trữ dưới dạng Hardcode trong source code hoặc file cấu hình plaintext trong Git repository nhằm tuân thủ các chuẩn an ninh PCI-DSS, SOC-2, ISO 27001.
- **Giải pháp**: Tích hợp **HashiCorp Vault** (Secrets Management Engine) để quản lý tập trung khóa bí mật, xoay vòng khóa (Key Rotation) và cấp phát bảo mật động theo kiến trúc Zero Trust.

```
┌─────────────────────────────────┐
│     HashiCorp Vault Server      │
│   (Engine: KV Version 2)        │
│ Path: secret/data/auth-service  │
│ Key:  aes_gcm_key               │
└────────────────┬────────────────┘
                 │
                 │ 1. HTTP GET /v1/secret/data/auth-service
                 │    Header: X-Vault-Token
                 ▼
┌────────────────────────────────────────────────────────┐
│                      Auth Service                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │               VaultSecretService                 │  │
│  │   - Gọi Vault REST API v1                        │  │
│  │   - Trích xuất: data.data.aes_gcm_key            │  │
│  └──────────────────────────┬───────────────────────┘  │
│                             │                          │
│                             ▼                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │             AesGcmEncryptionService              │  │
│  │   - Khởi tạo 256-bit SecretKeySpec               │  │
│  │   - Resilient Fallback nếu Vault offline         │  │
│  └──────────────────────────┬───────────────────────┘  │
│                             │                          │
│                             ▼                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │            AesGcmAttributeConverter              │  │
│  │   - Tự động mã hóa/giải mã Entity JPA            │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 8.2. Cấu Hình Dịch Vụ Vault trong Docker Compose (`infrastructure/docker/docker-compose.yml`)
```yaml
  vault:
    image: hashicorp/vault:latest
    container_name: dataflow-vault
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: 00000000-0000-0000-0000-000000000000
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    ports:
      - "8200:8200"
    cap_add:
      - IPC_LOCK
    networks:
      - dataflow-net
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### 8.3. Cấu Hình Kết Nối trong `application.yml`
```yaml
app:
  vault:
    enabled: ${VAULT_ENABLED:true}
    uri: ${VAULT_URI:http://localhost:8200}
    token: ${VAULT_TOKEN:00000000-0000-0000-0000-000000000000}
    secret-path: ${VAULT_SECRET_PATH:secret/data/auth-service}
  security:
    aes-gcm:
      secret-key: ${AES_GCM_SECRET_KEY:DataFlowPlatformAES256GCMSecretKey2026!} # Fallback key dự phòng
```

### 8.4. Mã Nguồn Cốt Lõi Tích Hợp Vault

#### 1. `VaultSecretService.java`
Giao tiếp trực tiếp với Vault REST API v1 sử dụng `RestTemplate` và `ObjectMapper`:
```java
@Service
@Slf4j
public class VaultSecretService {
    @Value("${app.vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${app.vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${app.vault.token:00000000-0000-0000-0000-000000000000}")
    private String vaultToken;

    @Value("${app.vault.secret-path:secret/data/auth-service}")
    private String secretPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<String> getSecretKey(String keyName) {
        if (!vaultEnabled) return Optional.empty();
        String fullUrl = String.format("%s/v1/%s", vaultUri, secretPath);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", vaultToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.path("data").path("data");
                if (dataNode.has(keyName)) {
                    return Optional.of(dataNode.get(keyName).asText());
                }
            }
        } catch (Exception e) {
            log.error("[Vault] Error connecting to Vault: {}. Will use fallback key.", e.getMessage());
        }
        return Optional.empty();
    }
}
```

#### 2. `AesGcmEncryptionService.java` (Khởi tạo Khóa từ Vault với Graceful Fallback)
```java
@PostConstruct
public void init() {
    String activeKey = null;
    VaultSecretService vaultSecretService = vaultSecretServiceProvider.getIfAvailable();
    if (vaultSecretService != null) {
        Optional<String> vaultKey = vaultSecretService.getSecretKey("aes_gcm_key");
        if (vaultKey.isPresent() && !vaultKey.get().trim().isEmpty()) {
            activeKey = vaultKey.get().trim();
            log.info("[AesGcmEncryptionService] Loaded AES-256-GCM master key directly from HashiCorp Vault!");
        }
    }

    if (activeKey == null) {
        log.info("[AesGcmEncryptionService] Using fallback AES-256-GCM secret key from application configuration.");
        activeKey = secretKeyString;
    }

    byte[] keyBytes = activeKey.getBytes(StandardCharsets.UTF_8);
    byte[] validKey256 = new byte[32]; // 256-bit key
    System.arraycopy(keyBytes, 0, validKey256, 0, Math.min(keyBytes.length, 32));
    this.secretKey = new SecretKeySpec(validKey256, "AES");
}
```

### 8.5. Kết Quả Xác Thực Kiểm Thử
1. **Khởi động dịch vụ**:
   Log hệ thống xác nhận tải key trực tiếp từ Vault:
   ```
   [Vault] Fetching secret 'aes_gcm_key' from HashiCorp Vault: http://localhost:8200/v1/secret/data/auth-service
   [Vault] Successfully retrieved key 'aes_gcm_key' from HashiCorp Vault! (Length: 32 chars)
   [AesGcmEncryptionService] Loaded AES-256-GCM master key directly from HashiCorp Vault!
   ```
2. **Kiểm tra lưu trữ trong Database PostgreSQL**:
   Dữ liệu được mã hóa dạng `ENC:<Base64>`:
   ```sql
   SELECT id, username, raw_password FROM users WHERE username = 'admin';
   -- raw_password: ENC:GEQx0wR5JyTs581bIGMcFxJYMpUqeOpI3rq83/CrXrIGThLEjg==
   ```
3. **Kiểm tra API giải mã**:
   - Khi gọi API `GET /api/users` với tài khoản `ROLE_ADMIN`, hệ thống tự động giải mã `raw_password` sử dụng khóa master từ Vault và hiển thị mật khẩu gốc `"Admin@123"` cho quản trị viên xem.

---

## 9. Màn Hình Trực Quan: Crypto Studio & Vault Management (UI / UX)

### 9.1. Giới thiệu Màn Hình (`/admin/crypto`)
Cung cấp môi trường kiểm thử trực quan (Interactive Cryptography Studio) cho Quản trị viên và Đội ngũ An ninh hệ thống:
- **Đường dẫn truy cập**: Menu *Quản Trị & Bảo Mật* ➔ **Mã Hóa & Giải Mã (Vault)** (`/admin/crypto`).
- **Giao diện hiện đại**: Thiết kế theo phong cách Ultra-Modern Fintech / Cyber Security Dashboard với hiệu ứng Glassmorphism, viền ánh sáng tinh tế và Terminal Code Blocks chuyên nghiệp.

### 9.2. Các Phân Hệ Chức Năng Chính

```
┌────────────────────────────────────────────────────────────────────────┐
│             Crypto Studio & Vault Key Management Header                │
│    [AEAD AES-256-GCM]  [ZERO TRUST PRODUCTION]  [Làm mới trạng thái]   │
└────────────────────────────────────────────────────────────────────────┘
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Trạng Thái  │  │  Thuật Toán  │  │ Đường Dẫn    │  │ Độ Dài Khóa  │
│    Vault     │  │  AES-256-GCM │  │  Vault Path  │  │ 256-bit      │
│   (Online)   │  │    (AEAD)    │  │ secret/auth  │  │ (32 bytes)   │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│     CỘT 1: MÃ HÓA (ENCRYPT)     │  │     CỘT 2: GIẢI MÃ (DECRYPT)    │
│  - Nhập Plaintext / Mẫu JSON    │  │  - Nhập chuỗi ENC:Base64(...)   │
│  - Nút "Mã hóa bằng Vault Key"  │  │  - Nút "Thử nghiệm giả mạo"     │
│  - Hiển thị Terminal chuỗi ENC  │  │  - Nút "Giải mã & Verify Auth"  │
│  - Phân tích chi tiết AEAD:     │  │  - Kết quả 1: XÁC THỰC 100% OK  │
│    + 12-byte IV ngẫu nhiên      │  │  - Kết quả 2: CẢNH BÁO BẢO MẬT: │
│    + 16-byte Auth Tag (128-bit) │  │    Phát hiện giả mạo/tampering  │
│    + Cipher Payload Hex         │  │    (AEADBadTagException)        │
└─────────────────────────────────┘  └─────────────────────────────────┘
```

### 9.3. Danh Sách REST API Mới

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/crypto/status` | Lấy trạng thái kết nối HashiCorp Vault, URI, Secret Path, Key Length | Bearer Token |
| `POST` | `/api/security/crypto/encrypt` | Mã hóa văn bản với AES-256-GCM, bóc tách cấu trúc Nonce, Auth Tag, Payload | Bearer Token |
| `POST` | `/api/security/crypto/decrypt` | Giải mã dữ liệu và xác thực tính toàn vẹn 100% bằng Auth Tag (AEAD) | Bearer Token |


