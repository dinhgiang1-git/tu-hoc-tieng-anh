# G3-02 — Nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). Kết luận: **ĐẠT**, đóng R1/R2; có thể chuyển G3-03.

## Kết quả kiểm tra độc lập

- Backend: `mvnw.cmd -o -B -ntp verify` BUILD SUCCESS, **113 unit + 42 integration tests**, không failure/error/skip; Spotless đạt. PostgreSQL Testcontainers biệt lập, không dùng DB dev. Log: `backend/target/review-g302-round1.log`.
- `LearningProgressIT`: 11 tests, gồm Clock đứng yên, no-op, submit text mới/trùng, hai activities cùng expectedVersion, rollback và race tạo enrollment mới với undo completed cũ.
- Frontend sau sửa cuối: **68/68 tests** trong 10 files; lint không error, build gồm TypeScript đạt. Logs local: `frontend/review-g302-tests.log`, `frontend/review-g302-residual.log`.
- `node scripts/validate-openapi-examples.js`: smoke check văn bản đạt. Script này không phải full schema validation.

## Các finding đã đóng

**R1:** CAS update version/state có điều kiện expectedVersion trong cùng transaction; không dựa vào updatedAt dirty checking. Entity được detach trước cập nhật để tránh Hibernate tăng version lần nữa. No-op giữ nguyên version/event; request stale bị từ chối. Test PostgreSQL kiểm chứng concurrency và rollback.

**R2:** Cache nhận version từ mutation response, giữ mutation pending đến khi invalidation hoàn tất; khóa thao tác khi đang tải lại. Phân biệt VERSION_CONFLICT/STATE_CONFLICT; giữ draft và không tự gửi lại. Detail chưa tải hoặc lỗi ban đầu không mở thao tác; PAUSED/ABANDONED chỉ đọc, COMPLETED cho undo.

Reviewer tìm thấy phần còn sót sau lượt sửa 1: refresh thất bại vẫn mở nút, và conflict flag bị xóa trước khi refresh thành công. Hai assertion bổ sung fail trước sửa (log residual). Codex sửa trực tiếp: khóa khi query lỗi và chỉ gỡ conflict khi refresh thành công; cả hai nút retry dùng chung luồng phục hồi.

Codex đồng thời giữ ActivityProgressPanel được mount khi background detail refresh lỗi, bảo toàn draft; khóa thao tác và hiển thị retry cho đến khi lifecycle đọc lại thành công. Có regression mô phỏng lỗi mạng và retry, xác nhận text còn nguyên. Không cần thêm lượt giao công nhân.

## Giới hạn và bước tiếp

- Chưa kiểm tra trực quan tương tác trên viewport 375px/desktop; chưa thay cho nghiệm thu E2E G3-04.
- Warning hiện có: EnrollmentDialog dùng form.watch, bundle khoảng 581 KB. Không chặn chức năng G3-02.
- Tài khoản local và secrets không đưa vào commit. Không chỉnh DB dev trong lượt nghiệm thu.
- Bước tiếp theo: G3-03 — Learning Workspace, Markdown an toàn, lưu nháp phản hồi và ghi chú tự lưu theo backlog; chưa triển khai trong lượt review này.
