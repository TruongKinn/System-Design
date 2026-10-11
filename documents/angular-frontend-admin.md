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
- **Quản lý Tài Khoản**: Bảng thông tin người dùng với Avatar, gán Roles, trạng thái Khóa/Mở khóa thông qua API `/api/users`.
- **Fine-Grained Permission Matrix**: Modal tích chọn ma trận quyền thao tác granular (`JOB_CREATE`, `JOB_EDIT`, `JOB_TRIGGER`, `JOB_PAUSE`, `USER_MANAGE`, `EXPORT_EXECUTE`, `IMPORT_EXECUTE`, `SYSTEM_CONFIG`) kết nối trực tiếp với backend qua API `PUT /api/roles/{id}/permissions`, tự động lưu bền vững vào cơ sở dữ liệu PostgreSQL.

### 3.7 Thiết Kế Đáp Ứng & Chống Méo Giao Diện (Responsive & Anti-distortion Table Design)
- **Tối ưu bảng dữ liệu lớn (`nz-table`)**: Toàn bộ các bảng danh sách (`/users`, `/jobs`, `/exports`, `/imports`) được cấu hình `[nzScroll]="{ x: '1150px - 1200px' }"` cùng độ rộng cố định theo từng cột (`nzWidth`).
- **Ngăn tràn & ngắt dòng chữ méo mó**: Áp dụng thuộc tính `white-space: nowrap !important;` cho thẻ họ tên, username, avatar cell, trạng thái và ngày giờ, tránh tình trạng họ tên người dùng bị bẻ đôi hoặc nút bấm bị dồn ép.
- **Chuẩn hóa hiển thị thời gian**: Dữ liệu ngày giờ ISO được format qua helper chuẩn `dd/MM/yyyy HH:mm`, ngắn gọn, trực quan và tiết kiệm không gian ngang.
- **Sticky Actions Column**: Cột hành động ("Khóa TK", "Mở khóa", "Chạy ngay", "Tải file") được cố định bên phải (`nzRight`) giúp thao tác thuận tiện trên mọi kích thước màn hình.
- **Tự động thu gọn Sidebar**: Layout tự động lắng nghe kích thước màn hình qua `HostListener('window:resize')`, tự động chuyển sang chế độ thu gọn (`isCollapsed = true`) khi màn hình < 1024px để ưu tiên tối đa không gian cho bảng dữ liệu.

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

## 6. Chuẩn Hóa CSS, UI Layout & Dark Mode (Update v1.1)

### 6.1. Header & Navigation
- **Dynamic Breadcrumbs**: Tự động lắng nghe `RouterNavigation` qua `admin-layout.component.ts` để cập nhật tiêu đề phân cấp phù hợp với từng màn hình (`Admin / Người dùng`, `Admin / Quản lý Job`, `Admin / Lịch sử Export`, `Admin / Nhập liệu Lô`, `Admin / Phân quyền & RBAC`).
- **Khắc phục vỡ nền Dark Mode ở Header**: Sửa ô tìm kiếm `header-search` tích hợp biến nền màu tối `--input-bg`, loại bỏ tình trạng hộp input bị viền trắng.
- **Biểu tượng Chuông Thông Báo & Chuyển Theme**: Sửa selector `::ng-deep` áp màu trực tiếp cho SVG/icon NG-ZORRO để hiển thị rõ nét trên nền tối thay vì bị tàng hình.

### 6.2. Chuẩn hóa Bảng Dữ Liệu (Users, Jobs, Exports, Imports)
- **Loại bỏ lỗi Sticky đè cột (`nzRight`)**: Đã gỡ bỏ thuộc tính `nzRight` đơn phương ở header khiến cột "Hành động" bị trôi nổi đè lên các cột kế bên khi cuộn bảng hoặc thay đổi kích cỡ màn hình.
- **Xử lý chuỗi dài (UUID / Filename)**: Tích hợp thuộc tính CSS `text-overflow: ellipsis; white-space: nowrap; max-width: ...px;` kèm tooltip `[title]` giúp ngăn chặn chuỗi UUID (36 ký tự) và tên file bị rớt dòng làm kéo giãn chiều cao hàng.
- **Phân bổ chiều rộng cột (`nzWidth`) & `nzScroll`**:
  - `Users`: Cột Thao tác 130px, scroll x: 1050px.
  - `Jobs`: Cột Thao tác 270px chứa đầy đủ bộ 3 nút Chạy ngay / Tạm dừng / Xóa, scroll x: 1300px.
  - `Exports`: Cột UUID thu gọn 130px, File Name 210px, scroll x: 1180px.
  - `Imports`: Cột Mã lô thu gọn 120px, Tên tệp 200px, scroll x: 1150px.
- **Format Thời gian chuẩn**: Đồng bộ định dạng ngày giờ hiển thị theo chuẩn `dd/MM/yyyy HH:mm`.

### 6.3. Màn hình Phân quyền (RBAC Matrix)
- Cân bằng chiều cao các thẻ vai trò (Roles) với bố cục flexbox `flex-direction: column`.
- Ghim nút thao tác "Chỉnh sửa Ma trận Phân quyền" xuống chân thẻ với `margin-top: auto` để giao diện luôn thẳng hàng ngay ngắn dù mô tả của mỗi vai trò có độ dài khác nhau.

## 7. Triển Khai Tính Năng Nâng Cao (Update v1.2)

### 7.1. Chức năng Xóa Cron Job (`DELETE /api/jobs/{id}`)
- Bổ sung phương thức `deleteJob(id: string)` vào `JobService`.
- Tích hợp nút **Xóa** màu đỏ kèm hộp thoại xác nhận an toàn `nz-popconfirm` trực tiếp trên bảng Jobs.
- Tối ưu kích thước và nhãn nút hành động (Chạy, Dừng/Bật, Lịch sử, Xóa) giúp hiển thị trọn vẹn 1 hàng mà không bị cắt hoặc ép cuộn ngang.

### 7.2. Modal Hồ Sơ Cá Nhân & Cài Đặt Hệ Thống
- **Hồ Sơ Cá Nhân**: Click vào menu người dùng góc phải trên Header hiển thị modal gồm avatar, Username, Tên hiển thị, Vai trò phân quyền, Trạng thái tài khoản (ACTIVE) và cơ chế xác thực JWT Bearer.
- **Cài Đặt Hệ Thống**: Cho phép người dùng chuyển đổi theme Sáng/Tối, cấu hình tần suất làm mới dữ liệu realtime (3s, 5s, 10s, 30s) và âm thanh thông báo.

### 7.3. Xử Lý Thông Báo & Tìm Kiếm Nhanh
- Tích hợp tính năng **Đánh dấu tất cả đã đọc**: Reset badge số lượng thông báo về 0 khi người dùng xem xong.
- Tích hợp **Tìm kiếm nhanh** trên Header: Nhập từ khóa (vd: `job`, `user`, `export`, `import`, `rbac`) và nhấn Enter để điều hướng tức thì tới trang nghiệp vụ tương ứng.

## 8. Thiết Kế Form Đăng Nhập & Đăng Ký V7 (Update v1.3 - Luxury Dark Glassmorphism)

### 8.1. Phong cách Thiết kế (UI/UX)
- **3D Luxury Golden Waves**: Nền đen sâu thẳm kết hợp dải sóng vàng ánh kim 3D lượn sóng tạo chiều sâu thị giác đẳng cấp.
- **Futuristic Corner Accents**: Hai góc đối xứng (góc trên bên trái và góc dưới bên phải) sở hữu chi tiết vát góc kính hình học phản quang viền vàng nổi bật.
- **Glassmorphism Card**: Khung kính mờ `backdrop-filter: blur(28px) saturate(180%)`, đường viền phản sáng vi tế `border: 1px solid rgba(255, 255, 255, 0.12)`.
- **Màu sắc chủ đạo**: Nền đen Obsidian (`#07070a`), điểm nhấn vàng chanh Neon Gold (`#facc15` / `#fbbf24`), text trắng sáng và xám bạc.

### 8.2. Chức năng Form & Kết nối API Thật
- **Form Đăng ký (Create Account)**:
  - Các trường: `Full Name`, `Email Address`, `Username`, `Password` & `Confirm Password` (bố trí 2 cột cân xứng), Checkbox điều khoản "I agree to the Terms & Privacy Policy".
  - Nút Submit: `Create Account →` màu vàng phát sáng.
  - Kết nối API backend thật: `POST /api/auth/register`, tự động đăng nhập và điều hướng ngay khi đăng ký thành công.
- **Form Đăng nhập (Welcome Back)**:
  - Các trường: `Username or Email`, `Password`, Checkbox `Remember me`, Link `Forgot password?`.
  - Nút Submit: `Sign In →` màu vàng phát sáng.
  - Kết nối API backend thật: `POST /api/auth/login`, cấp phát JWT Token.
- **Chuyển đổi Tab mượt mà**:
  - Chuyển đổi giữa Đăng nhập và Đăng ký ngay trên 1 màn hình thông qua link ở chân card (`Already have an account? Sign In` / `Don't have an account? Create Account`).
- **Nút Đăng nhập Nhanh Mạng xã hội**:
  - Hỗ trợ nút `Google` (vector SVG chuẩn đa màu) và `Apple` (vector SVG chuẩn trắng).

### 8.3. Hiệu Ứng Animation Co Giãn (Fluid Accordion Interaction)
- **Cơ chế Co Ra / Đóng Vào Mượt Mà (Smooth Spring Expand & Collapse)**:
  - Khung Card kính áp dụng curve chuyển động vật lý `cubic-bezier(0.16, 1, 0.3, 1)` giúp thay đổi chiều cao êm ái, không bị khựng hay giật khung hình.
  - Các trường chỉ có ở Sign Up (`Full Name`, `Email Address`, `Confirm Password`) được cấu hình dạng `collapsible-field` với `max-height`, `opacity` và `transform: translateY(...)` trượt mượt mà từ trên xuống khi mở ra và thu gọn biến mất khi đóng lại.
  - **Hàng Mật Khẩu Co Giãn 1 Cột ↔ 2 Cột**: Cột `Confirm Password` sử dụng `transform: scaleX(...)` và `max-width: 50%` mở rộng ngang trơn tru bên cạnh cột `Password`.
  - **Morphing Chữ Tiêu Đề & Nút Bấm**: Tiêu đề (*Welcome Back* ↔ *Create Account*) và nút CTA (*Sign In →* ↔ *Create Account →*) chuyển đổi trạng thái bằng hiệu ứng cross-fade trượt nhẹ vị trí.

## 9. Cấu Trúc Menu Cha - Con Trực Quan & Tối Ưu UX/UI (Update v1.4)

### 9.1. Tinh Gọn Cấu Trúc: Menu Cha & Con (Loại Bỏ Tiêu Đề Nhóm Ngoài Thừa)
- **Loại bỏ các tiêu đề nhóm ngoài cùng thừa (`ng-reflect-ng-if`)**: Không còn các khối header chữ in hoa rườm rà (`TỔNG QUAN & GIÁM SÁT`, `TÁC VỤ & LẬP LỊCH`, v.v.) và các comment bindings thừa làm rối DOM.
- **Giữ trọn cấu trúc Menu Cha - Con chuyên nghiệp**:
  - 📊 **Bảng Điều Khiển** (Menu đơn trực tiếp): Bảng điều khiển tổng quan và đồ thị giám sát Microservices Realtime.
  - ⏱️ **Quản Lý Tác Vụ** (Menu cha đa cấp):
    - `Danh Sách Cron Jobs`: Quản lý 4 tác vụ nền (Job #1 - #4) kèm badge đếm số lượng.
  - ☁️ **Xử Lý Dữ Liệu Lớn** (Menu cha đa cấp):
    - `Xuất Dữ Liệu (1M Rows)`: Xuất file Excel 1,000,000 dòng bất đồng bộ qua RabbitMQ/MinIO.
    - `Nhập Dữ Liệu (Streaming)`: Nạp dữ liệu Excel lớn qua streaming SAX/SXSSF.
  - ⚙️ **Quản Trị & Bảo Mật** (Menu cha đa cấp):
    - `Quản Lý Người Dùng`: Quản trị tài khoản, reset mật khẩu, trạng thái hoạt động.
    - `Phân Quyền RBAC`: Phân quyền vai trò và quyền hạn chi tiết.
- **Visual & Micro-interactions nâng cấp**:
  - **Submenu Mở/Đóng Mượt Mà**: Menu cha có mũi tên chỉ hướng xoay mượt, click mở/đóng các menu con tức thì.
  - **Pill Hover & Slide Transition**: Khi hover vào từng mục menu, xuất hiện hiệu ứng trượt nhẹ (`transform: translateX(4px)`) cùng màu nền cyan trong suốt `rgba(56, 189, 248, 0.08)`.
  - **Active State Neon Glow**: Mục menu con/đơn đang chọn có viền phát sáng 3.5px màu Neon Blue (`#38bdf8`), background gradient mờ và icon phát sáng với `drop-shadow`.

### 9.2. Cơ Chế Đóng/Mở Sidebar Chuẩn Qua Nút Bấm Header & Icon Chuẩn Ant Design
- **Bỏ cơ chế tự động mở rộng khi hover (Remove Hover-to-Expand)**:
  - Sidebar không còn tự động bung ra khi người dùng rê chuột qua, loại bỏ cảm giác nhấp nháy hoặc vô tình chạm phải menu.
  - Sidebar mặc định mở rộng cố định ở độ rộng `260px` giúp người dùng bao quát đầy đủ tên các menu cha và menu con.
  - Thu gọn về `72px` (hoặc mở rộng ra `260px`) chủ động thông qua nút bấm toggle trên Header.
- **Nút Bấm Toggle Header Chuẩn Ant Design**:
  - Icon chuyển đổi linh hoạt: `menu-fold` khi đang mở rộng và `menu-unfold` khi đang thu gọn.
  - Toàn bộ nội dung trang chính (`main-layout`) tự động co giãn đồng bộ (`margin-left: 260px` khi mở, `margin-left: 72px` khi thu gọn) với hiệu ứng trượt êm mượt `0.3s cubic-bezier(0.16, 1, 0.3, 1)`.
- **Chuẩn Hóa Toàn Diện Bộ Icon Ant Design**:
  - Nút chuyển đổi Giao diện Tối / Sáng: Sử dụng icon chuẩn Ant Design `bulb` / `highlight`.
  - Nút Thông báo Hệ thống: Sử dụng icon chuông `bell` chuẩn Ant Design, tự động đổi màu sắc tương thích hoàn hảo giữa nền Sáng (`#0f172a`) và nền Tối (`#f8fafc`).
  - Menu navigation: Toàn bộ sử dụng icon vector chuẩn của hệ sinh thái Ng-Zorro / Ant Design (`dashboard`, `schedule`, `clock-circle`, `cloud-server`, `export`, `import`, `setting`, `team`, `safety-certificate`).




