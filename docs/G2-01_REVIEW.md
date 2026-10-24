# Review G2-01 — nội dung, importer và publish

## Kết luận

**Chưa chốt G2-01.** Code đã có migration, domain/repository, JSON Schema validator thực sự, checksum, CLI và import/publish service. Validate và dry-run chạy được trên pilot. Có bốn vấn đề chức năng dưới đây; ba vấn đề được tái hiện bằng kiểm thử bổ sung, vấn đề bất biến được xác định từ SQL. Không mở lại auth, FE hoặc Agent Desk.

## R1 — P1: DB được chỉ định cho CLI bị cấu hình dev ghi đè

Vị trí: `backend/src/main/java/vn/englishlearning/contentops/cli/CurriculumCli.java:298`.

`extractExplicitDbProps` nhận `--db-url`/`CONTENT_DB_URL` nhưng sau đó truyền vào `SpringApplicationBuilder.properties(...)`. Đây là default properties, có ưu tiên thấp hơn `application-dev.yml`; profile mặc định hiện là dev. URL/username/password đích có thể bị cấu hình dev ghi đè. Guard đã xác nhận người dùng chỉ định DB nhưng context lại chọn DB khác, có nguy cơ Flyway/JPA chạm DB dev trước cả khi service chạy.

Đã tái hiện bằng Spring builder với cùng cách thiết lập properties, tải cấu hình thật của ứng dụng và dừng trong initializer trước khi tạo bean/kết nối DB: URL thử nghiệm `jdbc:postgresql://127.0.0.1:1/review_no_database_connection` bị thay thành `jdbc:postgresql://localhost:5432/english_learning_db`. **Không mở kết nối DB nào trong test này.**

Sửa: áp dụng DB target tường minh ở property source có ưu tiên đúng, không dùng defaults cho tham số bắt buộc phải thắng. Nên tách profile CLI khỏi mặc định dev. Regression phải kiểm tra effective URL/user/password khi cấu hình dev hoặc environment khác cũng tồn tại. Kiểm tra khởi động CLI import/publish thật với DB biệt lập, không chỉ test guard từ chối thiếu tham số; không in password vào log.

## R2 — P1: Pilot không import được vì mọi activity có order_index=0

Vị trí: `backend/src/main/java/vn/englishlearning/contentops/application/ImportCurriculumPackageService.java:408`; constraint tại `V003__create_curriculum_tables.sql:98`.

21 activities trong gói pilot không có `orderIndex`. Validator cho phép trường này vắng và mặc định theo vị trí `aIdx + 1`, nhưng importer gọi `.path("orderIndex").asInt()` nên nhận 0 cho tất cả. Với `UNIQUE(lesson_id, order_index)`, activity thứ hai trong cùng lesson sẽ vi phạm constraint. Vì test hiện mock repository.save nên vẫn báo nhập thành công.

Đã capture entity thật được importer gửi đến mock repository: bốn activity đầu có thứ tự `[0,0,0,0]`, thay vì `[1,2,3,4]`. Đây là lỗi trong luồng pilot đang nghiệm thu, không phải dữ liệu giả định.

Sửa: thống nhất normalization thứ tự giữa validator và importer. Regression phải dùng nguyên gói pilot hiện có, không sửa fixture thêm trường để che lỗi. Trên PostgreSQL biệt lập phải nhập đủ 6 lessons/21 activities và thứ tự đúng; import lặp không nhân đôi.

## R3 — P1: Published revision chưa bảo vệ nội dung con khỏi sửa/xóa

Vị trí: `backend/src/main/resources/db/migration/V003__create_curriculum_tables.sql:164–167`.

Trigger duy nhất chỉ chạy `BEFORE UPDATE ON curriculum_revision`. Không có guard cho `curriculum_phase`, `curriculum_week`, `lesson`, `activity`, `resource`, `activity_resource`, hoặc DELETE revision. Vì vậy sửa `lesson.content_markdown`, xóa activity, đổi resource/link của revision published vẫn hợp lệ với các constraint hiện tại. Xóa curriculum/revision còn có cascade xóa nội dung. Trigger cũng kiểm tra metadata chỉ khi OLD và NEW đều PUBLISHED, nên đổi sang ARCHIVED cùng lúc có thể né kiểm tra.

Đây là nhận xét từ SQL, **chưa chạy SQL regression trên PostgreSQL trong lượt review vì Docker chưa sẵn sàng**. Không mô tả bất biến nội dung là đã được xác nhận chỉ vì re-import/publish lần hai bị service từ chối.

Sửa: bảo vệ update/delete/insert vào cây nội dung và link của published revision theo policy nhất quán; bảo vệ cả chuyển quan hệ giữa draft và published. Kiểm tra transition trạng thái không trở thành đường vòng để sửa nội dung. Nếu V003 đã áp dụng ở một DB được giữ lại, thêm migration tiếp theo; không tự sửa checksum migration đã áp dụng. Regression SQL/Repository cần chứng minh update/delete nội dung và thay resource link bị chặn, draft hợp lệ vẫn ghi được.

## R4 — P2: Manifest/resources ở gốc bỏ qua kiểm tra symlink

Vị trí: `backend/src/main/java/vn/englishlearning/contentops/application/ContentPackageValidator.java:73–104`.

Path containment chỉ được áp dụng cho file tuần và Markdown. `manifest.json` và `resources.json` được resolve rồi readTree trực tiếp. Nếu một trong hai là symlink ra ngoài package, validator vẫn đọc và có thể trả valid, trái yêu cầu không đọc file ngoài package.

Đã tái hiện trên Windows bằng package fixture riêng: `resources.json` trỏ tới bản JSON hợp lệ nằm ngoài package; `validator.validate(...).hasErrors()` trả false. Test chỉ đọc fixture tự tạo, không đọc file riêng của người dùng.

Sửa: kiểm tra real path containment cho cả hai file JSON gốc trước mọi thao tác đọc; áp dụng cùng quy tắc cho các entry point checksum/import liên quan. Regression gồm symlink manifest và resources, ngoài trường hợp tuần/Markdown đã có.

## Kiểm tra đã chạy và giới hạn

- JDK 21.0.10, `mvnw.cmd -o -B -ntp test` ngoài sandbox: **27 unit tests đạt**, Spotless đạt. Có cảnh báo JVM agent của Mockito; không phải lỗi test.
- Ba test review `G201ReviewTest#review*`: **3/3 fail**, tương ứng R1/R2/R4. Test dùng fixture và mock repository; test property precedence dừng trước bean creation. Chỉ tắt Spotless cho file test tạm trong lần tái hiện, không tính đó là bằng chứng formatter đạt. Bản nguồn tại `docs/reviews/G201ReviewTest.java`.
- Đã chuyển test tạm khỏi `backend/src/test`, dọn đúng các class tạm `G201ReviewTest*.class`; không thay code ứng dụng. Repro này kế thừa fixture từ `ImportAndPublishServiceTest`.
- Package lại bằng `mvnw.cmd -o -B -ntp -DskipTests=true package`: đạt; sau đó chạy **jar vừa build** với `content validate` và `content dry-run`: đạt.
- Dry-run: 1 phase, 1 tuần, 6 lessons (5 bắt buộc/1 tùy chọn), 21 activities, 19 bắt buộc hiệu lực, 5 resources chưa xác minh. Checksum `98b297866781af20cb6800835618b233a8b73328de6a9e6edc4009526e65c961`.
- Docker Desktop Linux Engine named pipe vẫn không tồn tại. Chưa chạy integration assertions/rollback/DB triggers hoặc CLI import/publish với DB thật. Không chạy import/publish vào DB dev, không khởi động/chỉnh Docker/WSL.
- Log trong `backend/target/`: `review-g2-01-unit-elevated.log`, `review-g2-01-repro.log`, `review-g2-01-package.log`, `review-g2-01-validate.log`, `review-g2-01-dry-run.log`.

## Prompt giao AI sửa — một lượt có điểm dừng

```text
Đọc docs/G2-01_REVIEW.md. Chỉ sửa R1–R4 trong G2-01:
DB target CLI phải thắng cấu hình dev; thứ tự activity thiếu orderIndex phải
được chuẩn hóa đúng; published revision và nội dung/link con bất biến;
manifest/resources symlink ngoài package phải bị chặn trước khi đọc.

Dùng docs/reviews/G201ReviewTest.java làm tái hiện, đưa regression đúng vào
suite chính. Không chỉ sửa mock/fixture cho pass. Có kiểm tra PostgreSQL thật
qua Testcontainers cho import pilot, rollback, immutable update/delete/link,
và có smoke test khởi động CLI import/publish với DB biệt lập.

Docker chưa chạy thì báo phần chưa xác minh, không fallback DB dev hoặc
chỉnh môi trường. Không chạy import/publish vào DB dev. Không sửa migration
đã áp dụng; kiểm tra lịch sử để chọn migration kế tiếp khi cần.

Không làm G2-02, không mở lại G1/auth/FE, không nâng dependency hay chỉnh style
ngoài yêu cầu formatter. Chạy Spotless, unit và verify khi Docker sẵn sàng;
cập nhật backlog theo bằng chứng thật. Không commit/push.

Điểm dừng: bốn regression đạt, import/publish biệt lập và các check đạt thì
chốt G2-01, chuyển G2-02; không tạo thêm vòng sửa tài liệu/style. Nếu Docker
còn thiếu, tách rõ phần code đã đạt và runtime chưa xác minh.
```
