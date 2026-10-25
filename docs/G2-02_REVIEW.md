# Nghiệm thu G2-02 — Catalog và Outline

Ngày: 25/10/2026, Asia/Ho_Chi_Minh.

## Kết luận: Đạt

Codex đã đọc controller/service/repository, security, OpenAPI, generated types, router, sidebar và các trang catalog/detail. Không còn finding chặn nghiệm thu trong phạm vi G2-02. Chưa triển khai enrollment/lịch/tiến độ G3.

## Bằng chứng

- Java 21: `mvnw.cmd -o -B -ntp spotless:apply verify` đạt **48 unit tests + 21 integration tests**, 0 failures/errors/skips. Log local: `backend/target/review-g202-final.log`.
- PostgreSQL 16.15 Testcontainers biệt lập: 6 integration tests mới cho catalog; draft không xuất hiện và trả 404 ở detail/outline, pilot published đúng 1 phase/1 tuần/6 lessons (5 bắt buộc, 1 tùy chọn, 135 phút), metadata đúng gói pilot, JSON không chứa Markdown/activity payload/đáp án, thứ tự ổn định.
- Outline đếm thật 4 lần gọi query tham số hóa của JdbcTemplate: 1 metadata + 3 truy vấn phẳng cho phase/week/lesson. Truy vấn dùng projection, không tải Markdown và không truy vấn theo từng lesson.
- FE: `npm test` đạt **33/33**; `npm run lint` và `npm run build` (bao gồm `tsc -b`) đạt. Các test mới phủ loading/empty/error/retry, metadata Anh-Anh/nguồn chưa xác minh, thời lượng, nhãn bắt buộc/tùy chọn và redirect đường dẫn cũ.
- API public đúng contract; repository lọc PUBLISHED trước khi trả danh sách/detail/outline. Link nguồn trên FE giới hạn HTTP/HTTPS. UI dùng accordion details/summary và API client/Query hiện có.

## Sửa trực tiếp trong lượt nghiệm thu

- Test backend kỳ vọng nhầm nguồn BBC; sửa theo nguồn sheet tham khảo trong manifest pilot. Fixture FE cũng được cập nhật nguồn/license/URL tương ứng, không thay đổi dữ liệu giáo trình để làm test qua.
- Thay test N+1 chỉ đo thời gian dưới 5 giây bằng đếm truy vấn thật.
- Loại dependency `ci` được thêm nhưng không sử dụng; không nâng phiên bản package.

## Giới hạn

- Không import/publish hoặc thay đổi DB phát triển của người dùng.
- Chưa kiểm tra trực quan qua trình duyệt ở các kích thước màn hình; các trạng thái UI được kiểm tra bằng React Testing Library.
- Vite cảnh báo bundle khoảng 535 KB sau minify, chưa chặn G2-02. Có thể tách bundle theo route khi mở rộng tính năng.

G2-02 đã chốt; bước tiếp theo là G3-01 theo backlog. Không cần thêm vòng sửa tài liệu/style để nghiệm thu lại phần này.
