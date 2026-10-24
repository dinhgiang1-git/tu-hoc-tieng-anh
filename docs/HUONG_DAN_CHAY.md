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
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DB_HOST = 'localhost'
$env:DB_PORT = '5432'
$env:DB_NAME = 'english_learning_db'
$env:DB_USER = 'postgres'
$dbSecret = Read-Host 'Mật khẩu PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbSecret).Password
$env:BOOTSTRAP_ADMIN_USERNAME = 'admin'
$adminSecret = Read-Host 'Mật khẩu tài khoản admin lần đầu' -AsSecureString
$env:BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password
.\mvnw.cmd spring-boot:run
```

Sửa đường dẫn JDK nếu máy dùng bản cài khác. Mật khẩu DB phải khớp container hiện có. Mật khẩu admin là mật khẩu mới bạn tự chọn; chỉ tạo tài khoản khi bảng người dùng còn trống. Nếu đã có tài khoản, biến bootstrap không đặt lại mật khẩu đó.

Hiện dự án không tự nạp `.env`; chỉ copy `.env.example` chưa đủ. Các biến trên có hiệu lực trong terminal này và các tiến trình con.

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

Hiện có giao diện shell, đăng nhập/đăng xuất và trang chủ nền tảng. Chưa có toàn bộ chức năng học theo lộ trình. Không cần import/publish nội dung để xem giao diện. G2-01 còn chờ hoàn tất kiểm tra bất biến nội dung và integration trên PostgreSQL biệt lập.

Dừng backend/frontend bằng `Ctrl+C` trong từng terminal. Không cần dừng PostgreSQL nếu ứng dụng khác đang dùng chung container.

## 5. Lỗi thường gặp

- Docker named pipe không tồn tại: Docker Engine chưa chạy hoặc chọn sai context.
- `Connection refused`: kiểm tra container, host và cổng publish.
- `password authentication failed`: kiểm tra DB_USER/DB_PASSWORD của container hiện có.
- `database ... does not exist`: tạo database riêng trước khi chạy backend.
- Không đăng nhập được ở DB mới: kiểm tra đã đặt BOOTSTRAP_ADMIN_PASSWORD trước khi khởi động và có log bootstrap thành công.
- Flyway báo lỗi checksum/migration: giữ nguyên DB và gửi log để kiểm tra; không tự xóa lịch sử Flyway.
