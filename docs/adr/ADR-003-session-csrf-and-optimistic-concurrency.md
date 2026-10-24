# ADR-003: Xác thực Session Cookie, Bảo vệ CSRF và Kiểm soát Đồng thời Lạc quan

**Ngày:** 24/10/2026  
**Trạng thái:** Accepted (Đã duyệt)  
**Người quyết định:** AI Specialist & Project Coordinator  
**Tài liệu liên quan:** [BE_CODE_CONVENTION.md](../../BE_CODE_CONVENTION.md), [FE_CODE_CONVENTION.md](../../FE_CODE_CONVENTION.md), [openapi.yaml](../api/openapi.yaml)

---

## 1. Bối cảnh (Context)
Các ứng dụng web Single Page Application (SPA) thường đứng trước hai lựa chọn quản lý phiên đăng nhập:
1. Lưu JWT Access Token trong `localStorage` / `sessionStorage`.
2. Sử dụng cookie session `HttpOnly` kết hợp bảo vệ CSRF.

Đồng thời, trong quá trình tự học, người dùng có thể mở ứng dụng trên nhiều tab trình duyệt hoặc vừa học bài vừa đổi lịch ở hai cửa sổ khác nhau. Cần giải pháp ngăn chặn việc ghi đè dữ liệu cũ (lost updates) và gửi trùng lặp yêu cầu mạng khi mạng chập chờn.

## 2. Quyết định (Decision)

### 2.1. Quản lý Phiên bằng HttpOnly Session Cookie và Token CSRF
- **Không dùng localStorage để lưu Access Token:** Giảm thiểu triệt để nguy cơ đánh cắp token qua lỗ hổng XSS.
- **Session Cookie:** Thiết lập cookie session chuẩn từ Spring Security với thuộc tính `HttpOnly`, `SameSite=Lax` (hoặc `Strict`), và `Secure` khi chạy trên môi trường HTTPS.
- **CSRF Token cho SPA:**
  * Endpoint `GET /api/v1/auth/csrf` trả về `{ headerName: "X-CSRF-TOKEN", token: "..." }` với `Cache-Control: no-store`.
  * Frontend lưu token trong bộ nhớ (memory variable) và gửi kèm header `X-CSRF-TOKEN` cho mọi HTTP mutation (`POST`, `PUT`, `PATCH`, `DELETE`), bao gồm cả request Login và Logout.
  * Khi logout hoặc gặp lỗi 401: Frontend hủy cache, xóa token khỏi memory.

### 2.2. Kiểm soát Đồng thời Lạc quan (Optimistic Concurrency Control)
- Mọi mutation cập nhật tiến độ học tập (`PUT /progress`), dời lịch học (`POST /schedule/commit`), hoặc cập nhật ghi chú (`PATCH /notes/{id}`) bắt buộc phải gửi trường `expectedVersion`.
- Trong đó, tiến độ học tập và lịch học sử dụng chung trường `enrollmentVersion` của thực thể `Enrollment` làm mốc đồng bộ duy nhất.
- Nếu `expectedVersion` gửi lên không khớp với version hiện hành trong cơ sở dữ liệu:
  * Backend rollback transaction và trả về HTTP 409 Conflict với error code `VERSION_CONFLICT` theo chuẩn RFC 7807.
  * Frontend bắt mã lỗi này, refetch dữ liệu mới nhất từ server và giữ lại bản nháp cục bộ (local draft) để người dùng quyết định, tuyệt đối không tự động ghi đè.

### 2.3. Khóa Chống Lặp Thao tác (Idempotency Key)
- Các tác vụ tạo tài nguyên quan trọng hoặc giao dịch phức tạp: Tạo enrollment mới (`POST /enrollments`), Commit đổi lịch (`POST /schedule/commit`), Nộp quiz (`POST /attempts`):
  * Yêu cầu header `Idempotency-Key: <UUID>`.
  * Scope: `(userId, operation, key)`.
  * Backend lưu hash payload và kết quả. Nếu client bị ngắt kết nối mạng nhưng server đã thực thi xong, client gửi lại cùng key và payload sẽ nhận ngay kết quả cũ mà không bị trừ điểm hay ghi lặp bản ghi. Khác payload sẽ nhận 409 `IDEMPOTENCY_CONFLICT`.

### 2.4. Hợp đồng Đọc phục vụ Tải lại trang (Read Contract for Reload - xử lý R4)
- Để đảm bảo sau khi reload trang hoặc mở lại bài học, client luôn có `expectedVersion` hợp lệ mà không cần hardcode hay đoán mò:
  * Mọi endpoint đọc dữ liệu không gian học (`GET /lessons/{id}`), trang Hôm nay (`GET /today`), tóm tắt tiến độ (`GET /summary`), và chi tiết enrollment (`GET /enrollments/{id}`) đều trả về `enrollmentVersion`.
  * Khai báo chính thức `securitySchemes.CookieAuth` trong OpenAPI (cookie `SESSION`).
  * `Idempotency-Key` được đặt bắt buộc (`required: true`) tại các endpoint mutation quan trọng theo đúng cam kết ADR-003.

## 3. Hệ quả (Consequences)
- **Tích cực:**
  * Bảo mật cao nhất cho ứng dụng web, tuân thủ các chuẩn an ninh OWASP.
  * Loại bỏ hoàn toàn tình trạng mất mát dữ liệu do mở nhiều tab hoặc click đúp nút hoàn thành.
  * Trải nghiệm mượt mà khi mạng bị rớt tạm thời.
  * Luồng tải lại trang (reload) có đầy đủ phiên bản đồng bộ để thực hiện mutation an toàn.
- **Thách thức:**
  * Frontend cần có adapter CSRF bootstrap cẩn thận để không tạo vòng lặp vô tận khi gọi API khởi động.
  * Phải đảm bảo mọi mutation cập nhật tiến độ đều tăng atomic `version` của thực thể Enrollment.
