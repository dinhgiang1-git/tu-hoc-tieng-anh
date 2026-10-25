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

Hiện có giao diện shell, đăng nhập/đăng xuất và trang chủ nền tảng. Chưa có toàn bộ chức năng học theo lộ trình. Không cần import/publish nội dung để xem giao diện. G2-01 đã qua kiểm tra bất biến nội dung và integration trên PostgreSQL biệt lập; G2-02 catalog/outline đang chờ nghiệm thu.

Dừng backend/frontend bằng `Ctrl+C` trong từng terminal. Không cần dừng PostgreSQL nếu ứng dụng khác đang dùng chung container.

## 5. Lỗi thường gặp

- Docker named pipe không tồn tại: Docker Engine chưa chạy hoặc chọn sai context.
- `Connection refused`: kiểm tra container, host và cổng publish.
- `password authentication failed`: kiểm tra DB_USER/DB_PASSWORD của container hiện có.
- `database ... does not exist`: tạo database riêng trước khi chạy backend.
- Không đăng nhập được ở DB mới: kiểm tra đã đặt BOOTSTRAP_ADMIN_PASSWORD trước khi khởi động và có log bootstrap thành công.
- Flyway báo lỗi checksum/migration: giữ nguyên DB và gửi log để kiểm tra; không tự xóa lịch sử Flyway.
