# G7-01 — Thử nghiệm sao lưu và phục hồi dữ liệu PostgreSQL (pg_dump / pg_restore)

## 1. Mục tiêu và Phạm vi

Xây dựng và kiểm chứng thực tế quy trình sao lưu (**Backup**) và phục hồi (**Restore**) toàn diện cho cơ sở dữ liệu PostgreSQL của ứng dụng Tự học Tiếng Anh theo yêu cầu tại `KE_HOACH_DU_AN.md` (Mục 11 - G7, Mục 12.1):
1. **Kịch bản sao lưu tự động (`scripts/backup-db.ps1`):**
   - Sử dụng công cụ chuẩn `pg_dump` từ container PostgreSQL (`postgresql-postgres-1`).
   - Sử dụng định dạng nén tối ưu `custom` (`-Fc`), hỗ trợ nén dữ liệu, bảo toàn metadata (blob, OIDs, triggers, schema, ownership), cho phép `pg_restore` linh hoạt và khôi phục song song nếu cần.
   - Tự động đặt tên tệp theo mốc thời gian ISO/timestamp chuẩn (ví dụ: `english_learning_db_20261028_192000.dump`).
   - Tạo mã băm toàn vẹn SHA-256 đi kèm cho mỗi tệp sao lưu (`.dump.sha256`).
2. **Kịch bản phục hồi an toàn (`scripts/restore-db.ps1`):**
   - Nhận diện tệp backup nguồn và kiểm tra checksum trước khi thực hiện.
   - Cho phép chỉ định database đích (mặc định hoặc database kiểm thử biệt lập như `english_learning_db_restore_test`).
   - Tự động ngắt các kết nối đang hoạt động tới database đích trước khi drop/recreate.
   - Thực thi `pg_restore --clean --if-exists --no-owner --no-privileges` hoặc nạp vào database mới sạch sẽ.
3. **Kịch bản kiểm chứng tính toàn vẹn độc lập (`scripts/verify-backup-restore.ps1`):**
   - Tự động tạo bản sao lưu từ `english_learning_db`.
   - Khôi phục sang cơ sở dữ liệu kiểm thử `english_learning_db_restore_test`.
   - Đối chiếu số lượng bảng (đủ 23 bảng) và số dòng dữ liệu (`COUNT(*)`) giữa DB nguồn và DB phục hồi. Khớp chính xác 100%.
   - Kiểm tra tính hợp lệ của lịch sử migration Flyway (`flyway_schema_history` đạt version 009).
   - Kiểm tra chức năng bảo vệ toàn vẹn: triggers (immutability trigger của revision `PUBLISHED`), foreign keys, constraints.
   - Thử nghiệm kết nối ứng dụng với database phục hồi để đảm bảo backend đọc/ghi bình thường.
   - Dọn dẹp an toàn cơ sở dữ liệu kiểm thử sau khi hoàn thành.
4. **Biên bản nghiệm thu:** Ghi nhận bằng chứng thực tế tại `docs/G7-01_ACCEPTANCE.md`.

---

## 2. Tiêu chí Nghiệm thu (Definition of Done)

1. **Khả năng Thực thi Độc lập:**
   - Script chạy trơn tru qua PowerShell (`pwsh`) trên Windows kết nối Docker container `postgresql-postgres-1`.
   - Không đòi hỏi cài đặt thêm client PostgreSQL riêng trên Windows Host (chạy trực tiếp thông qua `docker exec`).
2. **Toàn vẹn Dữ liệu 100%:**
   - 23/23 bảng cơ sở dữ liệu được sao lưu và phục hồi đầy đủ.
   - Dữ liệu người dùng cá nhân, ghi danh, lịch học, tiến độ, ghi chú, trắc nghiệm và ôn tập không bị mất mát hay sai lệch.
   - Toàn bộ 26 tuần của Lộ trình A và giáo trình thử nghiệm nguyên vẹn.
3. **Bảo toàn Quy tắc Nghiệp vụ:**
   - Migration Flyway không bị chạy lại hoặc lỗi checksum.
   - Các trigger ràng buộc tính bất biến của revision `PUBLISHED` vẫn hoạt động chuẩn xác sau khi restore.
4. **An toàn Hệ thống:**
   - Không bao giờ ghi đè lên database chính `english_learning_db` trừ khi có cờ xác nhận nguy hiểm tường minh (`--ForceProductionRestore`).
   - Mặc định kiểm thử phục hồi trên database tạm `english_learning_db_restore_test` và dọn dẹp sạch sẽ sau khi kiểm tra xong.
