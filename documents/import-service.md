# Service Specification: Import Service
**Tài liệu thiết kế chi tiết dịch vụ Nhập Dữ Liệu Lớn (Import Service - Batch Streaming Data Ingestion)**

## 1. Mục tiêu (Overview)
Import Service (chạy trên cổng `8084`) đảm nhiệm tiếp nhận và xử lý nạp các tập tin dữ liệu lớn (CSV, XLSX) vào hệ thống cơ sở dữ liệu:
- **Streaming Parsing**: Đọc luồng dữ liệu tuần tự từng dòng (Streaming Read) qua `Apache Commons CSV` và `Apache POI` để tối ưu bộ nhớ RAM, chống lỗi Out-Of-Memory khi nạp các file dung lượng hàng trăm MB.
- **Batch Insertion**: Gom nhóm dữ liệu thành từng lô 1,000 bản ghi/lượt thực thi `saveAll()` vào PostgreSQL thay vì ghi từng câu lệnh `INSERT` đơn lẻ.
- **Data Validation**: Tự động kiểm tra tính hợp lệ của dữ liệu (Mã khách hàng, định dạng Email). Các dòng lỗi được đếm riêng và ghi nhận vào `errorRows`.
- **Real-time Progress Tracking**: Cập nhật liên tục số dòng đã nạp và tiến độ % vào Redis (`import:progress:{jobId}`).

---

## 2. Kiến trúc Luồng Dữ liệu (Batch Insert Pipeline)

```
       File Upload (Multipart/Form-Data)
                     │
                     v
             Streaming Parser
                     │
                     v
         Data Validation Filter
            ├── Valid Rows ───> Batch Collector (1,000 rows) ──> PostgreSQL (imported_customers)
            └── Invalid Rows ─> Error Counter & Redis Progress (import:progress:{jobId})
```

---

## 3. Cấu Trúc Bảng Dữ Liệu PostgreSQL (`imported_customers`)

| Cột | Kiểu dữ liệu | Ý nghĩa |
| :--- | :--- | :--- |
| `id` | `BIGSERIAL` (PK) | Định danh duy nhất bản ghi |
| `import_job_id` | `VARCHAR(255)` | Mã UUID của đợt nạp dữ liệu |
| `customer_code` | `VARCHAR(255)` | Mã khách hàng (VD: `CUST-000001`) |
| `full_name` | `VARCHAR(255)` | Họ và tên khách hàng |
| `email` | `VARCHAR(255)` | Địa chỉ email |
| `phone_number` | `VARCHAR(255)` | Số điện thoại |
| `created_at` | `TIMESTAMP` | Thời điểm nạp vào database |

---

## 4. Đặc Tả REST API

| Method | Endpoint | Tham số / Body | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/imports` | Không | Lấy danh sách lịch sử tất cả các đợt nạp dữ liệu |
| `POST` | `/api/imports/upload` | `multipart/form-data` (`file`) | Tải lên file CSV/Excel thật và khởi chạy tiến trình nạp |
| `POST` | `/api/imports/simulate` | `fileName`, `totalRows` | Chạy giả lập benchmark nạp dữ liệu khối lượng lớn (1M dòng) |
| `GET` | `/api/imports/{jobId}/status` | Path variable `jobId` | Lấy tiến độ nạp thời gian thực từ Redis (`importedRows`, `totalRows`, `%`) |
| `GET` | `/api/imports/template` | Không | Tải file mẫu `.csv` chuẩn có sẵn cấu trúc cột để người dùng nạp thử |

---

## 5. Vòng đời Trạng Thái & Cơ Chế Auto-Polling Frontend

### Vòng đời trạng thái Job (State Machine):
```
[File Uploaded] ──> UPLOADED (Lưu metadata vào DB & Redis)
                         │
                         ▼
                    PROCESSING (Streaming batch insert 1,000 rows/lượt)
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
    COMPLETED                           FAILED
 (Tất cả bản ghi hợp lệ            (Gặp lỗi định dạng nghiêm trọng
  được lưu thành công)             hoặc IO file bị lỗi)
```

### Cơ chế Auto-Polling phía Frontend:
- Sau khi người dùng upload file thành công, giao diện kích hoạt bộ đếm **active polling** (chu kỳ 2 giây/lần liên tục trong tối thiểu 30 giây hoặc cho đến khi tất cả các job chuyển sang `COMPLETED`/`FAILED`).
- Điều kiện kiểm tra trạng thái hoạt động bao gồm cả `UPLOADED`, `PROCESSING`, `UPLOADING`, `PARSING`. Khi job kết thúc, thanh tiến độ tự động chuyển sang `100% (Success)` màu xanh mà không cần người dùng phải bấm nút "Làm mới" thủ công.

---

## 6. Hướng Dẫn Kiểm Thử Thực Tế Qua cURL / PowerShell

```powershell
# 1. Tải file CSV mẫu (1,000 dòng) từ hệ thống
curl.exe -s http://localhost:8084/api/imports/template -o template.csv

# 2. Tải file Excel mẫu (1,000 dòng) từ hệ thống
curl.exe -s http://localhost:8084/api/imports/template/excel -o template.xlsx

# 3. Upload file nạp dữ liệu thật vào DB
curl.exe -s -X POST http://localhost:8084/api/imports/upload -F "file=@template.xlsx"

# 4. Kiểm tra số lượng bản ghi đã vào DB PostgreSQL
docker exec dataflow-postgres psql -U postgres -d dataflow_db -c "SELECT count(*) FROM imported_customers;"
```

