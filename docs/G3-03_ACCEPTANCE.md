# G3-03 — Biên bản nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT phạm vi chức năng G3-03**, đóng R1–R3. Hai lượt sửa công nhân đã kết thúc; Codex tự xử lý phần còn lại, không giao vòng thứ ba.

## Bằng chứng cuối

- Backend: `mvnw.cmd -o -B -ntp verify`, **143 unit + 48 PostgreSQL integration tests**, không failures/errors/skips, BUILD SUCCESS và Spotless đạt. Log local `backend/target/review-g303-acceptance.log`.
- Frontend: **98/98 tests trong 16 files**, lint không error, production build gồm TypeScript đạt. Log local `frontend/review-g303-acceptance.log`.
- 8 reproductions R1/R2 từ hai lần review trước đã nằm trong suite chính và đạt. Bổ sung regression data router: lưu rồi rời khi note write lỗi phải ở lại và giữ chữ; mutation đang refetch phải khóa thao tác; lỗi refetch có recovery giữ draft. Bổ sung lookup ghi chú ngoài trang đầu và URL resource không an toàn.
- `node scripts/validate-openapi-examples.js`: smoke check văn bản đạt; không phải full OpenAPI schema validation.
- Tất cả integration tests dùng Testcontainers biệt lập. Không thay đổi tài khoản, secrets hoặc DB dev trong lượt nghiệm thu.

## Chức năng đã chốt

- Workspace đọc nội dung Markdown/IPA, instructions/resources và tiến độ theo enrollment/revision của chủ sở hữu; link Today/bài tiếp và focus mode giữ editor.
- Draft có enrollment expectedVersion; complete/undo/submit giữ chính sách G3-02; note có version riêng và scope theo enrollment/lesson. Markdown được sanitize, resources chỉ tạo link http/https.
- Note autosave 700ms serialize/coalesce, giữ local text khi lỗi/conflict, generation bảo vệ scope; recovery dùng version đọc thật. Điều hướng nội bộ dùng data router blocker chung với draft activity và note.
- R3: PRACTICE manual checklist hoạt động, response có undo, submit giữ whitespace; RECORDING/DICTATION chưa hỗ trợ thì không có mutation thủ công.

## Phần Codex tự sửa sau lượt 2

1. Repro bổ sung xác nhận lưu note lỗi nhưng nút Lưu rồi rời vẫn đưa sang trang khác. Sửa để kết quả save thất bại luôn chặn rời; không hiển thị lựa chọn này khi còn response draft chưa được lưu thủ công hoặc activity mutation đang chạy.
2. Activity callback chờ reconcile; khóa khi workspace/detail đang refetch hoặc lỗi. Refetch lỗi phải được truyền lên, hiện retry; không gỡ khóa hoặc xóa overrides như đã đồng bộ. Kiểm tra read version bắt kịp mutation version trước khi kết thúc sync.
3. Workspace được key theo enrollment/lesson để không mang editor/override của scope cũ sang scope mới. Background reads không đè câu trả lời local chưa lưu, kể cả sau lỗi.
4. Note không reset chỉ vì initialNote đổi id sau create/refetch; đọc hết các trang để tìm đúng note lesson. Guard phím tắt khi note chưa tải/lỗi; xóa note không làm snapshot cũ hồi sinh; conflict sau POST timeout có thể dùng bản server đã đọc để phục hồi đúng id/version.
5. No-op lưu draft rỗng trả responseText="" thay vì null, đúng contract; regression unit xác nhận không ghi progress/version cho thao tác này.
6. Bỏ version kỹ thuật khỏi giao diện người học và chặn resource URL ngoài http/https.

## Giới hạn và bước kế tiếp

- Chưa thực hiện kiểm tra trực quan trên browser thật ở 375px/1440px hoặc E2E toàn hành trình. Đây là công việc bắt buộc của G3-04; unit/component và data router tests không thay thế E2E.
- Warning còn lại: form.watch tại EnrollmentDialog; bundle khoảng 684 KB (208 KB gzip). Không chặn chức năng G3-03; theo dõi trong hoàn thiện M1.
- Bước tiếp: G3-04 — E2E vòng học cốt lõi, kiểm tra hai tài khoản, reload, mạng lỗi/conflict, mobile/desktop và hoàn thiện mốc M1. Chưa triển khai G3-04 trong lượt này.
