# Chạy dự án local trên Windows

Cần PostgreSQL, backend Java 21 và frontend Node.js. Chạy backend và frontend ở hai terminal riêng. Các lệnh dưới đây dùng PowerShell.

## 1. PostgreSQL

Mở Docker Desktop, chờ engine sẵn sàng. Nếu Docker của bạn chạy trong WSL thì bật Docker ở distro đó. Kiểm tra ở terminal đang quản lý Docker:

```powershell
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Nếu container PostgreSQL đã có nhưng đang dừng, chạy `docker start TEN_CONTAINER` với đúng tên thực tế. Không tạo container mới nếu đã có sẵn. Kiểm tra cổng publish: cấu hình mặc định backend kết nối `localhost:5432`; nếu Docker publish cổng khác thì đổi `DB_PORT` tương ứng.

Cần database riêng tên `english_learning_db`. Nếu chưa có, có thể tạo qua công cụ quản lý DB, hoặc chạy với đúng tên container và user PostgreSQL:

```powershell
docker exec -it TEN_CONTAINER psql -U postgres -d postgres -c "CREATE DATABASE english_learning_db;"
```

Chỉ chạy lệnh tạo database một lần. Không trỏ backend vào database của dự án khác: Flyway tự áp dụng migration khi khởi động. Không xóa volume hoặc database để xử lý lỗi migration.

## 2. Backend — terminal thứ nhất

```powershell
cd D:\WorkSpace\tu-hoc-tieng-anh\backend
.\mvnw.cmd spring-boot:run
```

Máy cần cấu hình JDK 21 qua JAVA_HOME. Backend dùng profile `dev` mặc định và tự đọc `backend/.env` khi chạy từ thư mục `backend`. Lần đầu ở máy mới, copy `backend/.env.example` thành `backend/.env`, sửa DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD cho đúng PostgreSQL.

**Khởi tạo tài khoản lần đầu:** chạy từ thư mục gốc dự án trước khi khởi động backend:

```powershell
cd D:\WorkSpace\tu-hoc-tieng-anh
.\scripts\initialize-admin.ps1
```

Script tạo mật khẩu ngẫu nhiên và lưu `BOOTSTRAP_ADMIN_USERNAME=admin`, `BOOTSTRAP_ADMIN_PASSWORD` trong `backend/.env`; giữ nguyên cấu hình DB và mật khẩu bootstrap đã có. Mở file này để lấy thông tin đăng nhập. Script chỉ chuẩn bị cấu hình: backend tạo tài khoản và thiết lập học tập khi khởi động với bảng người dùng trống. Nếu backend đang chạy, dừng bằng `Ctrl+C` rồi chạy lại `mvnw.cmd spring-boot:run` từ `backend`. File `.env` ở thư mục gốc không được đọc khi chạy từ `backend`.

Để tạo admin ở database trống, thêm BOOTSTRAP_ADMIN_USERNAME=admin và BOOTSTRAP_ADMIN_PASSWORD=<mật khẩu tự chọn> vào `.env` trước khi khởi động. Nếu đã có người dùng, bootstrap không đặt lại mật khẩu. `.env` dùng định dạng KEY=value, không dùng `export` hoặc đặt SPRING_PROFILES_ACTIVE trong file này. Biến môi trường đã đặt trong terminal có ưu tiên cao hơn `.env`. File `.env` bị Git bỏ qua và không được push lên GitHub.

Chờ log `Started EnglishLearningApplication`. Kiểm tra [health](http://localhost:8080/actuator/health) có trạng thái `UP`.

## 3. Frontend — terminal thứ hai

```powershell
cd D:\WorkSpace\tu-hoc-tieng-anh\frontend
npm ci
npm run dev
```

`npm ci` cần khi cài lần đầu hoặc khi lockfile đổi; các lần sau chỉ cần `npm run dev`. Mở [ứng dụng](http://localhost:5173), đăng nhập bằng `admin` và mật khẩu bootstrap đã chọn. Nếu Vite báo cổng khác vì 5173 bị chiếm, dùng URL Vite in ra.

Frontend proxy `/api` và `/actuator` tới `http://localhost:8080`. Nếu đổi cổng backend cần sửa proxy trong `frontend/vite.config.ts`.

## 4. Phạm vi hiện tại và dừng ứng dụng

Đã hoàn thành các chức năng:
- **G3-01 / G3-02**: Danh mục/chi tiết lộ trình, đăng ký lộ trình, trang Hôm nay (tính lịch, gợi ý bài kế, rest day, overdue, upcoming, hoàn thành lộ trình), chuyển đổi lộ trình ưu tiên, đánh dấu hoàn thành/undo hoạt động bắt buộc, lưu nháp câu trả lời phản tư, kiểm tra phân tách revision và mẫu số tiến độ.
- **G3-03**: Không gian học tập buổi học (`LessonWorkspacePage`), render nội dung Markdown bài học, panel danh sách hoạt động kèm tài liệu tham khảo nguồn mở, trình soạn thảo ghi chú bài học (`NoteEditor`) có autosave (700ms debounce) và giải quyết xung đột phiên bản (CAS conflict với lựa chọn Tải lại hoặc Ghi đè), Chế độ tập trung (Focus Mode, Esc để thoát), và Unified Navigation Blocker bảo vệ thay đổi chưa lưu khi rời trang.

Dừng backend/frontend bằng `Ctrl+C` trong từng terminal. Không cần dừng PostgreSQL nếu ứng dụng khác đang dùng chung container.

## 5. Chạy bộ kiểm thử E2E Playwright (G3-04)

Cài dependency bằng `npm ci` trong frontend; lần đầu cài Chromium bằng `npx playwright install chromium`. Mở FE/BE development trước khi chạy runner. Runner mặc định dùng container `postgresql-postgres-1`, không tự tạo/xóa container.

Bộ E2E tự động xác thực toàn bộ vòng lặp học tập M1 và các luồng đa tài khoản/đa tab/điều hướng trên trình duyệt Chromium thật:

```powershell
# Chạy toàn bộ 14 test E2E (tự động seed fixture an toàn và chạy Playwright)
cd D:\WorkSpace\tu-hoc-tieng-anh
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1

# Chạy riêng từng suite
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter e2e/m1-core-loop.e2e.ts
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter e2e/multi-account-and-sync.e2e.ts
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter e2e/navigation-and-desktop-ux.e2e.ts

# Hoặc lọc theo regex tên test; mặc định reset đúng các tài khoản E2E
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter '5\.2|5\.3|M1-5'
# Tái dùng trạng thái test hiện có (chỉ dùng khi case phù hợp)
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -SkipSeed -TestFilter '5\.4 Desktop'
# Container khác tên mặc định: thêm -ContainerName TEN_CONTAINER_THUC_TE
```

*Lưu ý an toàn:*
- Script tận dụng trực tiếp Backend (`localhost:8080`), Frontend (`localhost:5173`) và Database phát triển (`english_learning_db`).
- Seed/reset chỉ tác động allowlist 17 tài khoản `e2e_*` trong scripts/seed-e2e.sql; không reset các tài khoản cá nhân. SQL lỗi rollback transaction và dừng trước test. Không tắt trigger/ràng buộc, không xóa curriculum published để reset fixture.

## 6. Lỗi thường gặp

- Docker named pipe không tồn tại: Docker Engine chưa chạy hoặc chọn sai context.
- `Connection refused`: kiểm tra container, host và cổng publish.
- `password authentication failed`: kiểm tra DB_USER/DB_PASSWORD của container hiện có.
- `database ... does not exist`: tạo database riêng trước khi chạy backend.
- Không đăng nhập được ở DB mới: kiểm tra đã đặt BOOTSTRAP_ADMIN_PASSWORD trước khi khởi động và có log bootstrap thành công.
- Flyway báo lỗi checksum/migration: giữ nguyên DB và gửi log để kiểm tra; không tự xóa lịch sử Flyway.
