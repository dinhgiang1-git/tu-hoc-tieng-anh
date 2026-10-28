# Sổ Tay Vận Hành Hệ Thống (Operations Runbook)

**Dự án:** WebApp Tự Học Tiếng Anh Theo Lộ Trình (English Self-Study Platform)  
**Phiên bản phát hành:** Mốc M2 — Bản Dùng Ổn Định (Milestone M2 Stable Release)  
**Ngày cập nhật:** 28/10/2026  
**Môi trường khuyến nghị:** Windows 11 / WSL2 Linux, Docker Desktop hoặc Docker Engine.

---

## 1. Tổng quan Kiến trúc Hệ thống

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        TRÌNH DUYỆT NGƯỜI DÙNG                          │
│         (Giao diện Web Desktop: 1440x900 chuẩn, 1280x800 laptop)        │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ HTTP/JSON (Cùng Origin / Proxy)
                                     ▼
┌────────────────────────────────────┬───────────────────────────────────┐
│           FRONTEND LAYER           │           BACKEND LAYER           │
│     React 19 + TypeScript (TSX)    │       Java 21 + Spring Boot 3     │
│        Vite Dev Server (:5173)     │      Spring Security (Session)    │
│       Tailwind CSS + Lucide Icons  │       Spring Data JPA / Flyway    │
└────────────────────────────────────┴─────────────────┬─────────────────┘
                                                       │ JDBC / SQL (:5432)
                                                       ▼
                                     ┌───────────────────────────────────┐
                                     │          DATABASE LAYER           │
                                     │     PostgreSQL 16 (Container)     │
                                     │     23 Bảng, Immutability Trigger │
                                     └───────────────────────────────────┘
```

- **Frontend:** Single Page Application (SPA) phát triển bằng React 19, TypeScript, Tailwind CSS, TanStack Query, React Router v6.
- **Backend:** Spring Boot 3.4.3 trên nền Java 21 LTS, quản lý phiên qua cookie `SESSION` (HttpOnly), bảo vệ CSRF qua `X-CSRF-TOKEN`, băm mật khẩu `BCrypt`.
- **Database:** PostgreSQL 16 chạy trong container Docker, quản lý schema tự động bằng 9 Flyway migrations (`V001` đến `V009`), bảo vệ tính toàn vẹn bằng database triggers (immutability của revision đã xuất bản).
- **Gói nội dung giáo trình:** Định dạng chuẩn hóa (JSON + Markdown) biên soạn tại `content/roadmap-a/v1` và `content/pilot-curriculum/v1`.

---

## 2. Yêu cầu Tiên quyết (Prerequisites)

Trước khi khởi động hệ thống trên máy mới (clean machine), đảm bảo đã cài đặt:

| Thành phần | Phiên bản tối thiểu | Ghi chú kiểm tra |
|:---|:---|:---|
| **Java JDK** | 21 LTS | Chạy `java -version` và `javac -version` |
| **Node.js** | 20.x hoặc 24.x LTS | Chạy `node -v` (khuyến nghị Node 20+) |
| **npm** | 10.x+ | Đi kèm với Node.js (`npm -v`) |
| **Docker Engine** | 24.x+ hoặc Docker Desktop | Chạy `docker ps` |
| **PowerShell** | 5.1 hoặc PowerShell 7+ | Môi trường thực thi script trên Windows |

---

## 3. Khởi tạo và Thiết lập Môi trường từ Đầu (Clean Setup)

### Bước 3.1: Khởi động Cơ sở dữ liệu PostgreSQL
Ứng dụng sử dụng container PostgreSQL 16. Khởi động container nếu chưa chạy:
```powershell
docker run -d --name postgresql-postgres-1 `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=english_learning_db `
  -p 5432:5432 `
  postgres:16-alpine
```
*Lưu ý: Nếu container `postgresql-postgres-1` đã tồn tại từ trước:*
```powershell
docker start postgresql-postgres-1
```

### Bước 3.2: Thiết lập Biến môi trường Backend
Sao chép tệp mẫu biến môi trường vào thư mục `backend/`:
```powershell
Copy-Item backend/.env.example backend/.env
```
Nội dung tệp `backend/.env` cơ bản:
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=english_learning_db
DB_USER=postgres
DB_PASSWORD=postgres

BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=AdminPassword123!
```

### Bước 3.3: Khởi chạy Backend và Áp dụng Flyway Migrations
Khởi động ứng dụng backend (Spring Boot sẽ tự động áp dụng 9 migrations `V001`–`V009`):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Khi thấy log `Started EnglishLearningApplication in X.XXX seconds`, backend đã sẵn sàng phục vụ tại cổng `8080`.

Kiểm tra trạng thái sức khỏe (Healthcheck):
```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
# Kết quả mong đợi: {"status":"UP"}
```

### Bước 3.4: Khởi tạo Tài khoản Quản trị Ban đầu (Bootstrap Admin)
Mở cửa sổ PowerShell mới tại thư mục gốc dự án:
```powershell
.\scripts\initialize-admin.ps1
```
Script sẽ tạo tài khoản quản trị đầu tiên (`admin`) từ thông tin cấu hình trong `backend/.env`.

---

## 4. Quản lý Nội dung Giáo trình (Content Operations)

Giáo trình được nhập và xuất bản thông qua công cụ dòng lệnh (Backend CLI) với quy trình 4 bước nghiêm ngặt:

### Bước 4.1: Kiểm tra Tính hợp lệ (Validate Schema & Links)
Kiểm tra cú pháp JSON schema Draft 2020-12, đường dẫn tệp Markdown, ký tự phiên âm và không có liên kết traversal độc hại:
```powershell
java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content validate content/roadmap-a/v1
```

### Bước 4.2: Chạy Thử nghiệm (Dry-run Report)
Tính toán mã băm SHA-256 gói nội dung và thống kê số lượng phases, weeks, lessons, activities:
```powershell
java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content dry-run content/roadmap-a/v1
```

### Bước 4.3: Nhập Dữ liệu vào Cơ sở dữ liệu (Import Draft)
Nhập gói nội dung vào database ở trạng thái bản nháp (`DRAFT`) trong một transaction nguyên tử:
```powershell
java -Dspring.datasource.url=jdbc:postgresql://localhost:5432/english_learning_db `
     -Dspring.datasource.username=postgres `
     -Dspring.datasource.password=postgres `
     -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content import content/roadmap-a/v1
```

### Bước 4.4: Xuất bản Giáo trình Bất biến (Publish Revision)
Chuyển trạng thái bản nháp sang `PUBLISHED`. Sau khi xuất bản, database trigger sẽ khóa bất biến, ngăn chặn mọi hành vi sửa đổi trực tiếp:
```powershell
java -Dspring.datasource.url=jdbc:postgresql://localhost:5432/english_learning_db `
     -Dspring.datasource.username=postgres `
     -Dspring.datasource.password=postgres `
     -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content publish roadmap-a 1.0.0
```

---

## 5. Khởi chạy Giao diện Người học (Frontend Client)

### Chạy Môi trường Phát triển (Development Mode):
```powershell
cd frontend
npm install
npm run dev
```
Truy cập ứng dụng tại: **`http://localhost:5173`**  
Vite dev server được cấu hình sẵn reverse proxy chuyển tiếp tự động các yêu cầu `/api/*` và `/actuator/*` sang `http://localhost:8080`, đảm bảo chia sẻ session cookie và CSRF hoàn toàn trong suốt.

### Đóng gói Môi trường Production (Production Build):
```powershell
cd frontend
npm run build
```
Thư mục xuất bản tĩnh `frontend/dist/` sẵn sàng để phục vụ qua Nginx, Caddy hoặc bất kỳ Static Web Server nào.

---

## 6. Sao lưu & Phục hồi Thảm họa (Backup & Disaster Recovery)

Hệ thống cung cấp bộ công cụ tự động hóa đã được nghiệm thu (Task G7-01), vận hành an toàn nhị phân qua Docker:

### 6.1. Thực hiện Sao lưu Định kỳ (Backup)
Tạo bản sao lưu nén định dạng custom (`-Fc`) và tự động tính toán mã băm SHA-256:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/backup-db.ps1
```
- Bản sao lưu được lưu tại: `backups/english_learning_db_YYYYMMDD_HHMMSS.dump`
- Tệp mã băm SHA-256 kèm theo: `backups/english_learning_db_YYYYMMDD_HHMMSS.dump.sha256`
- *Khuyến nghị:* Thiết lập Windows Task Scheduler hoặc cronjob chạy hằng ngày vào 02:00 sáng.

### 6.2. Phục hồi Cơ sở dữ liệu (Restore)
Phục hồi dữ liệu từ tệp dump vào database chỉ định:
```powershell
# Phục hồi vào database kiểm thử an toàn (mặc định):
powershell -ExecutionPolicy Bypass -File scripts/restore-db.ps1 -BackupFile backups/<ten_file>.dump

# Phục hồi vào database sản xuất (Bắt buộc cờ xác nhận):
powershell -ExecutionPolicy Bypass -File scripts/restore-db.ps1 -BackupFile backups/<ten_file>.dump -TargetDb english_learning_db -ForceProductionRestore
```
*Tính năng an toàn:* Tự động xác thực mã băm SHA-256 trước khi restore, ngắt toàn bộ kết nối đang hoạt động và dựng lại DB sạch sẽ.

### 6.3. Kiểm thử Tự động Đối soát Toàn vẹn (Integrity Verification)
Kiểm tra quy trình tạo backup, restore vào database cô lập, đối soát khớp 100% số hàng trên toàn bộ 23 bảng, kiểm tra phiên bản Flyway và kiểm tra database trigger:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backup-restore.ps1
```

---

## 7. Kiểm chuẩn Hiệu năng API (Performance Benchmarking)

Đo lường độ trễ thực tế và đảm bảo toàn bộ API đạt chuẩn **p95 < 500ms** (Task G7-02):
```powershell
node scripts/benchmark-api.mjs
```
Script sẽ chạy 30 chu kỳ đo (+ 3 lần warm-up) trên 12 endpoint cốt lõi với người dùng đã đăng ký Lộ trình A 26 tuần, xuất kết quả chi tiết ra màn hình và tệp JSON tại `reports/api-benchmark-report.json`.

---

## 8. Giám sát & Xử lý Sự cố Thường gặp (Troubleshooting)

### 8.1. Kiểm tra Sức khỏe Ứng dụng
- **Liveness & Readiness:** `GET /actuator/health` trả về `{"status":"UP"}`.
- **Log Backend:** Xem trực tiếp trên cửa sổ chạy `mvnw spring-boot:run` hoặc file log cấu hình.

### 8.2. Các Lỗi Thường Gặp & Cách Khắc Phục

| Triệu chứng lỗi | Nguyên nhân | Cách khắc phục |
|:---|:---|:---|
| **403 Forbidden / CSRF Token Missing** | Trình duyệt gửi request POST/PUT/DELETE nhưng thiếu header `X-CSRF-TOKEN` | Kiểm tra gọi `GET /api/v1/auth/csrf` trước khi thực hiện thao tác ghi; trong frontend đảm bảo sử dụng `api-client.ts`. |
| **409 Conflict: STATE_CONFLICT** | Người học đã có enrollment cho cùng lộ trình hoặc đang tạm dừng | Chuyển sang lộ trình hiện có qua combobox tại `/today` hoặc vào `/schedule` để bấm "Tiếp tục học" (Resume). |
| **409 Conflict: VERSION_CONFLICT** | Xung đột phiên bản (Optimistic Locking) do thao tác đồng thời trên hai tab | Bấm "Tải lại trạng thái mới nhất" hoặc "Đồng bộ tiến độ" trên giao diện để nạp phiên bản mới nhất từ server. |
| **Connection to localhost:5432 refused** | Container PostgreSQL chưa khởi động hoặc chưa mở cổng | Chạy `docker ps` kiểm tra container `postgresql-postgres-1`; nếu chưa chạy, gõ `docker start postgresql-postgres-1`. |
| **Flyway Schema Migration Checksum Mismatch** | Có người sửa trực tiếp các tệp SQL migration cũ (`V001`–`V009`) | Tuyệt đối không sửa các migration đã apply; tạo migration mới (ví dụ `V010__...`) khi cần thay đổi schema. |
| **Idempotency-Key header is required** | Request tạo enrollment hoặc đổi lịch thiếu header `Idempotency-Key` | Đảm bảo header `Idempotency-Key` có giá trị là một chuỗi UUID v4 hợp lệ. |

### 8.3. Quy trình Rollback Khẩn cấp (Emergency Rollback)
Nếu gặp sự cố nghiêm trọng sau khi triển khai phiên bản mới:
1. Dừng backend service.
2. Sử dụng lệnh phục hồi database từ bản sao lưu gần nhất:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/restore-db.ps1 -BackupFile backups/<backup_on_dinh>.dump -TargetDb english_learning_db -ForceProductionRestore
   ```
3. Checkout mã nguồn Git về tag/commit ổn định trước đó:
   ```powershell
   git checkout <commit_on_dinh>
   ```
4. Khởi động lại backend và frontend.
