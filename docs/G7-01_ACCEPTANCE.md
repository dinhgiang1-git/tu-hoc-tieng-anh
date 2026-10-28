# G7-01 — Biên bản nghiệm thu Sao lưu & Phục hồi Dữ liệu PostgreSQL

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT tiêu chuẩn nghiệm thu G7-01**, hoàn thành thử nghiệm và tự động hóa quy trình sao lưu (`pg_dump`) và phục hồi (`pg_restore`) dữ liệu PostgreSQL.

---

## 1. Kết quả kiểm chứng Tự động hóa (Verification Report)

Quy trình kiểm thử trọn vẹn đã được thực thi và xác thực tự động bằng script [verify-backup-restore.ps1](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/verify-backup-restore.ps1):
- Lệnh chạy: `powershell -ExecutionPolicy Bypass -File scripts/verify-backup-restore.ps1`
- Trạng thái kết thúc: **Exit code 0 (100% PARITY PASSED)**
- Thời gian thực thi: **~2.2 giây**

### Báo cáo đối soát 23/23 Bảng dữ liệu (Data Parity Matrix):

| STT | Tên bảng (Table Name) | Bản gốc (Source Rows) | Bản phục hồi (Restored Rows) | Trạng thái đối soát |
|:---:|:---|:---:|:---:|:---:|
| 1 | `activity` | 430 | 430 | **OK (100% Khớp)** |
| 2 | `activity_progress` | 1 | 1 | **OK (100% Khớp)** |
| 3 | `activity_resource` | 21 | 21 | **OK (100% Khớp)** |
| 4 | `app_user` | 24 | 24 | **OK (100% Khớp)** |
| 5 | `curriculum` | 4 | 4 | **OK (100% Khớp)** |
| 6 | `curriculum_phase` | 6 | 6 | **OK (100% Khớp)** |
| 7 | `curriculum_revision` | 4 | 4 | **OK (100% Khớp)** |
| 8 | `curriculum_week` | 29 | 29 | **OK (100% Khớp)** |
| 9 | `enrollment` | 1 | 1 | **OK (100% Khớp)** |
| 10 | `flyway_schema_history` | 9 | 9 | **OK (100% Khớp)** |
| 11 | `idempotency_record` | 1 | 1 | **OK (100% Khớp)** |
| 12 | `learner_preferences` | 24 | 24 | **OK (100% Khớp)** |
| 13 | `learning_event` | 1 | 1 | **OK (100% Khớp)** |
| 14 | `lesson` | 138 | 138 | **OK (100% Khớp)** |
| 15 | `lesson_schedule` | 1 | 1 | **OK (100% Khớp)** |
| 16 | `note` | 0 | 0 | **OK (100% Khớp)** |
| 17 | `quiz_attempt` | 1 | 1 | **OK (100% Khớp)** |
| 18 | `quiz_attempt_answer` | 4 | 4 | **OK (100% Khớp)** |
| 19 | `quiz_option` | 14 | 14 | **OK (100% Khớp)** |
| 20 | `quiz_question` | 4 | 4 | **OK (100% Khớp)** |
| 21 | `resource` | 15 | 15 | **OK (100% Khớp)** |
| 22 | `review_bookmark` | 1 | 1 | **OK (100% Khớp)** |
| 23 | `review_resolution` | 1 | 1 | **OK (100% Khớp)** |

---

## 2. Kiểm chứng Toàn vẹn Schema & Ràng buộc Nghiệp vụ (Integrity Checks)

1. **Flyway Migration Schema:**
   - Phiên bản schema phục hồi: **`009`** (khớp chính xác 9 migrations từ `V001` đến `V009`).
   - Kiểm tra: `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;` $\rightarrow$ `009`.
2. **Business Trigger & Immutability:**
   - Đã kiểm tra trigger nghiệp vụ `trg_check_revision_immutability` trên database phục hồi:
   - Thử nghiệm cập nhật trái phép: `UPDATE curriculum_revision SET title = 'Blocked' WHERE status = 'PUBLISHED';`
   - Kết quả: Bị PostgreSQL chặn hoàn toàn với lỗi chuẩn xác:
     ```
     ERROR: PUBLISHED curriculum revision is immutable and its metadata/timestamps cannot be modified
     ```
   - Xác nhận: Toàn bộ triggers, functions, procedures và constraints được phục hồi nguyên vẹn và vận hành chuẩn mực.
3. **Mã băm toàn vẹn SHA-256:**
   - Tệp dump sinh ra định dạng custom nén (`-Fc`, dung lượng ~135 KB).
   - Tự động sinh tệp `.sha256` đi kèm. Quá trình restore xác thực mã băm SHA-256 trước khi chạm vào database, lập tức hủy thao tác nếu mã băm sai lệch.

---

## 3. Các thành phần và Script đã triển khai

| Tệp / Script | Vai trò & Đặc điểm kỹ thuật |
|:---|:---|
| [scripts/backup-db.ps1](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/backup-db.ps1) | Tự động tạo bản sao lưu `pg_dump -Fc` bên trong container Docker `postgresql-postgres-1`, xuất tệp an toàn nhị phân ra thư mục `backups/`, tính toán và ghi mã băm SHA-256. |
| [scripts/restore-db.ps1](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/restore-db.ps1) | Tự động kiểm tra SHA-256, kiểm tra an toàn chặn vô tình đè DB dev/production (`-ForceProductionRestore`), ngắt kết nối đang hoạt động, tái tạo DB đích sạch sẽ và gọi `pg_restore --no-owner --no-privileges`. |
| [scripts/verify-backup-restore.ps1](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/verify-backup-restore.ps1) | Kịch bản kiểm thử tích hợp đầu cuối: đọc hàng 23 bảng nguồn $\rightarrow$ tạo backup $\rightarrow$ khôi phục vào DB cô lập `english_learning_db_restore_test` $\rightarrow$ so sánh từng bảng $\rightarrow$ kiểm tra Flyway & Trigger $\rightarrow$ tự động dọn dẹp sạch sẽ. |
| [docs/tasks/G7-01_HANDOFF.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/tasks/G7-01_HANDOFF.md) | Tài liệu bàn giao chi tiết các quy chuẩn vận hành, tham số CLI và kịch bản phục hồi thảm họa. |
| [.gitignore](file:///d:/WorkSpace/tu-hoc-tieng-anh/.gitignore) | Đã cập nhật chặn tuyệt đối `backups/`, `*.dump`, `*.dump.sha256`, bảo đảm không bao giờ đẩy dữ liệu nhị phân hoặc dữ liệu cá nhân lên Git. |

---

## 4. Ràng buộc & An toàn Môi trường

- **Không rò rỉ Secrets:** Không in mật khẩu hoặc connection string chứa credential vào terminal hay file logs.
- **Không thay đổi DB phát triển:** Quy trình kiểm thử phục hồi chạy trên database cô lập (`english_learning_db_restore_test`) và dọn dẹp ngay sau khi kiểm tra xong; DB `english_learning_db` đang chạy không bị gián đoạn hay sửa đổi dữ liệu.
- **Tập tin được bảo vệ:** Tuyệt đối không chạm vào `docs/DAY2_REVIEW.md` và `backend/src/main/resources/META-INF/spring/`.
- **Chuyển tiếp:** Đạt tiêu chuẩn nghiệm thu G7-01. Sẵn sàng chuyển tiếp sang **Task G7-02** ("Kiểm thử hiệu năng API (p95 < 500ms) và kiểm thử trực quan trên thiết bị thật").
