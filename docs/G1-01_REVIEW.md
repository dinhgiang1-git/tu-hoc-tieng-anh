# Review G1-01

Ngày: 24/10/2026. Phạm vi: backend skeleton, cấu hình, migration identity/preferences, Clock, lỗi HTTP, Maven Wrapper và test nền tảng. Không review nghiệp vụ chưa được giao.

## Kết luận và cách tiến tiếp

**Khung triển khai đạt hướng kiến trúc và đã qua formatter + 3 unit test. Chưa nghiệm thu hoàn toàn vì test chưa tách khỏi DB phát triển và cấu hình chứa mật khẩu cụ thể.** Không cần viết lại backend hoặc mở lại G0.

Để tránh vòng review kéo dài, giao một lượt sửa nền tảng ngắn rồi tiếp tục G1-02 trong cùng kế hoạch giao việc. Tiêu chí đóng rõ bên dưới; không cần chờ duyệt lại từng thay đổi nhỏ nếu AI cung cấp đủ bằng chứng kiểm tra.

## Phát hiện quan trọng

### F1 — P1: Integration test ghi vào DB phát triển

Vị trí: `backend/src/test/resources/application-test.yml:3`; `backend/src/test/java/vn/englishlearning/EnglishLearningApplicationIT.java:25`, `:69`, `:75`.

Profile test dùng cùng DB_NAME/DB_USER với cấu hình dev, mặc định cùng database. Test khởi động Flyway và lưu app_user/learner_preferences, không rollback hoặc cleanup; vì vậy `mvn verify` có thể thay đổi database học tập của người dùng. Dependency Testcontainers đã khai báo nhưng chưa được sử dụng để tách datasource.

**Sửa tối thiểu:** dùng PostgreSQL Testcontainers cho IT, inject datasource qua DynamicPropertySource hoặc cơ chế tương thích Spring Boot đã chọn. Không kế thừa DB dev qua biến môi trường; database test phải có vòng đời riêng. Không có Docker phải báo rõ chưa chạy được IT, không fallback sang DB dev. Không vô hiệu IT mặc định để làm verify xanh.

**Đóng khi:** verify chạy trên DB/container test riêng; migration + persistence + health + 404 test đạt; chạy lặp không để lại user trong DB phát triển. Không đổi container/volume PostgreSQL của người dùng.

### F2 — P1: Mật khẩu cụ thể nằm trong file mẫu và cấu hình test

Vị trí: `.env.example:16`, `backend/.env.example:12`, `backend/src/test/resources/application-test.yml:5`.

Các file được dự kiến lưu vào source có mật khẩu cụ thể thay vì placeholder; test cũng có fallback password cố định. Báo cáo này không lặp lại giá trị đó.

**Sửa tối thiểu:** example dùng placeholder; cấu hình dev yêu cầu DB_PASSWORD từ môi trường, test nhận credential tạm của Testcontainers. Rà lại tất cả file nguồn/config/log artifact định đưa vào Git. Ghi cách nạp biến môi trường trong README; chỉ copy file thành `.env` không tự bảo đảm Spring Boot nạp nó.

**Đóng khi:** ứng dụng không kết nối bằng mật khẩu cụ thể cài sẵn trong source; mẫu không chứa credential thật; thiếu secret có hướng dẫn/fail rõ. Credential test do container tạo độc lập.

### F3 — P2: Pin Spring Boot cũ không khớp quyết định chọn bản còn được duy trì

Vị trí: `backend/pom.xml:10`.

Parent đang là 3.3.5. Theo [wiki chính thức Spring Boot](https://github.com/spring-projects/spring-boot/wiki), các nhánh đang được duy trì tích cực tại thời điểm review là 4.1 và 4.0. Chưa có ADR giải thích lựa chọn nhánh 3.3 cho dự án mới.

**Xử lý trước khi mở rộng auth:** chọn patch ổn định của nhánh còn được hỗ trợ, kiểm tra Java 21 và các starter/Flyway/Testcontainers/test APIs tương thích rồi pin lại. Không chỉ đổi số parent mà bỏ qua compatibility. Nếu có ràng buộc bắt buộc dùng nhánh khác, ghi bằng chứng hỗ trợ và lý do cụ thể vào ADR.

## Gộp vào G1-02, không yêu cầu vòng sửa riêng

- `GlobalExceptionHandler` mới chuẩn hóa một số exception. Các lỗi MVC khác đi qua handler kế thừa có thể thiếu code/traceId/fieldErrors. Khi bổ sung SecurityProblemWriter, chuẩn hóa chung cả 400/404/405/415 và 401/403, giữ status/headers gốc, thêm HTTP tests. Không cần tạo business endpoint giả trong main source chỉ để kiểm tra lỗi.
- SecurityConfig hiện permitAll và tắt CSRF là trạng thái skeleton G1-01, chưa coi là auth đạt. G1-02 phải thay bằng session/CSRF và deny/protect routes theo contract trước khi có API cá nhân.
- AppUser hiện chưa có role; thêm mapping/schema phù hợp ở G1-02 khi triển khai principal/UserResponse. Daily goal 5–240 trong entity/schema khác API 10–180: chốt validation ở cùng giai đoạn thay vì triển khai các mức chấp nhận khác nhau.
- README còn thiếu hướng dẫn chạy backend/test và khảo sát môi trường có thể tái lập. Ghi Java path/JAVA_HOME, biến môi trường, port, command, phiên bản và kết quả; không ghi mật khẩu. Không tự reset Flyway/checksum để đổi schema trên DB đã dùng.

## Kiểm tra đã chạy

Reviewer chạy bằng JDK `C:/Program Files/Java/jdk-21.0.10`, JAVA_HOME/PATH chỉ đặt trong process của lệnh; không thay cấu hình hệ thống.

```text
cd backend
.\mvnw.cmd -o -B -ntp spotless:check test
```

Kết quả: BUILD SUCCESS; 3 unit tests, 0 failures/errors; Spotless qua. `-o` dùng dependency cache sẵn có; chưa chứng minh máy sạch tải/build được đầy đủ.

Có report cũ trong target ghi 4 integration tests đạt; reviewer không coi đó là kết quả do mình chạy lại. **Không chạy verify/khởi động ứng dụng trên datasource hiện tại**, để tránh Flyway và test ghi vào DB phát triển. Vì vậy chưa xác nhận lại startup, health và migration trên DB biệt lập trong lượt review này.

Java qua PATH mặc định trỏ Oracle javapath và lệnh bị treo trong lượt kiểm tra; đặt JAVA_HOME trỏ JDK thật giúp Maven Wrapper chạy thành công. Đây là lưu ý môi trường phát triển, không phải lỗi business code.

## Prompt giao AI tiếp theo

```text
Đọc docs/G1-01_REVIEW.md. Giữ backend skeleton hiện có.

Trước tiên xử lý F1/F2 và quyết định dependency F3:
- IT dùng PostgreSQL Testcontainers, tuyệt đối không fallback DB dev.
- Xóa credential cụ thể khỏi file mẫu/source; document env loading.
- Pin Spring Boot được hỗ trợ và dependency/test APIs tương thích.

Chạy formatter + unit + IT trên datasource biệt lập, ghi bằng chứng.
Không sửa container/volume/database hiện có của người dùng.
Nếu môi trường không cho chạy Docker, báo phần bị chặn, không báo IT đạt.

Khi các điều kiện này đạt, tiếp tục G1-02 trong cùng lượt:
session login/logout, CSRF, principal/role, /me/preferences,
bootstrap user từ secret và error body nhất quán MVC/security.
Đọc các mục được gộp G1-02 trong report trước khi triển khai.

Không dựng FE hoặc nghiệp vụ lộ trình, không mở lại G0.
Cuối lượt báo checks đã chạy, phần chưa xác minh và cách demo.
```
