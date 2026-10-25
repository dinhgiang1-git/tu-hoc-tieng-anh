# G2-01 — kiểm tra bản sửa sau review

## Kết luận mới nhất — Đã đạt (25/10/2026)

Codex đã đọc bản sửa và chạy độc lập `mvnw.cmd -o -B -ntp verify` với JDK 21.0.10: **BUILD SUCCESS, 38 unit tests + 15 integration tests, 0 failures/errors/skips**, Spotless đạt. Log local: `backend/target/review-g2-verify.log`.

PostgreSQL 16.15 chạy trong Testcontainers biệt lập. Lượt này không chạy import/publish hoặc sửa dữ liệu trong container PostgreSQL dùng chung của người dùng.

- R1/R2/R4: regression DB target CLI, thứ tự activity và đường dẫn gói nội dung đều đạt trong suite.
- R3: trigger kiểm tra cả OLD và NEW, bảo vệ published_at/created_at bằng so sánh null-safe. Integration kiểm tra chuyển phase/lesson published sang draft, chuyển week draft sang published, sửa timestamp/metadata, xóa nội dung/link/revision và xác nhận draft vẫn sửa/xóa được.
- Rollback: trigger thử nghiệm gây lỗi khi insert activity ở lesson thứ hai, sau các lần ghi trước đó. Test xác nhận revision mới không tồn tại sau lỗi; trigger thử nghiệm được dọn trong finally.
- Pilot import đủ 6 lessons/21 activities, import lặp không nhân đôi, publish thành công; CLI import dùng DB Testcontainers được chỉ định tường minh.

**Chốt G2-01, không yêu cầu thêm vòng sửa cho các finding bên dưới.** G2-02 catalog/outline đã có mã ở workspace nhưng chưa được nghiệm thu trong kết luận này. Các mục phía dưới là lịch sử review trước khi sửa.

Ngày kiểm tra: 25/10/2026 (Asia/Ho_Chi_Minh).

## Kết quả

- `mvnw.cmd -o -B -ntp test` với Java 21: BUILD SUCCESS, 32 tests, 0 failures/errors; Spotless đạt. Log local: `backend/target/review-g2-recheck.log`.
- R1: DB props được nâng ưu tiên bằng initializer và EnvironmentPostProcessor; hai regression precedence đạt. Chưa chạy CLI với DB biệt lập.
- R2: importer dùng vị trí `aIdx + 1` khi thiếu orderIndex; regression pilot đạt.
- R4: validator và checksum kiểm tra real path của manifest/resources trước đọc. Hai test symlink đã thêm, nhưng catch exception rồi return khiến không tạo được symlink cũng bị tính là pass; chưa xem con số unit pass là bằng chứng symlink thực thi trên mọi máy.
- Docker Desktop Linux Engine chưa chạy; cổng 5432/8080/5173 không có listener lúc kiểm tra. Chưa chạy integration assertions, không kết nối hoặc thay đổi DB dev.

## R3 vẫn chưa đóng — P1

`V004__enforce_published_revision_immutability.sql` chỉ kiểm tra parent được chọn bằng `COALESCE(NEW.parent_id, OLD.parent_id)`. Khi UPDATE đổi parent của một hàng từ published sang draft, NEW khác null nên chỉ kiểm tra draft: có thể lấy nội dung khỏi revision published. Áp dụng cho phase/week/lesson/activity/resource và activity_resource. Phải kiểm tra cả quan hệ OLD và NEW khi UPDATE.

Trigger revision cũng chưa bảo vệ `published_at`; có thể sửa thời điểm publish của revision đã published. Cần đưa trường này vào policy bất biến, dùng so sánh null-safe.

Đây là kết luận từ SQL, chưa tái hiện trên PostgreSQL vì Docker chưa sẵn sàng. Regression cần có UPDATE chuyển parent published -> draft, sửa published_at, và chứng minh draft hợp lệ vẫn sửa được.

Test `rollbackOnFailure_rollsBackEntirely` xóa Markdown trước validate; validator từ chối trước khi bắt đầu insert nên chưa chứng minh rollback giữa transaction. Cần lỗi xảy ra sau một số lần ghi DB và xác minh không còn dữ liệu dở dang. Giữ phạm vi sửa ở R3 và bằng chứng integration; chưa mở G2-02.
