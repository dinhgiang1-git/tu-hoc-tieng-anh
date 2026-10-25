# Giao việc tiếp theo — G2-02 Catalog và Outline

Ngày: 25/10/2026 (Asia/Ho_Chi_Minh).

## Quy tắc điều phối

Codex giao việc, review mã và tự chạy kiểm thử. Antigravity là công nhân triển khai; mỗi việc có tối đa hai lượt sửa. Nếu lượt thứ hai vẫn chưa đạt, Codex tự sửa. Không mở thêm vòng yêu cầu Anti sửa cùng một việc.

Chỉ gọi Antigravity sau khi quyền gửi mã nguồn/tài liệu dự án qua dịch vụ Antigravity được xác nhận. Loại trừ `.env`, credentials, secret và dữ liệu người dùng. Không commit/push hoặc thay đổi container/volume hiện có.

## Prompt triển khai

```text
Triển khai G2-02 trong D:/WorkSpace/tu-hoc-tieng-anh.
Giữ nguyên thay đổi đang có. Đọc quy chuẩn BE/FE, backlog G2-02,
docs/api/openapi.yaml, docs/wireframes.md và product-rules.md.

Backend: GET /api/v1/curricula, /api/v1/curricula/{slug},
/api/v1/revisions/{revisionId}/outline. Trả DTO đúng OpenAPI, chỉ catalog
revision PUBLISHED; không lộ draft, Markdown, activity payload hoặc đáp án.
Outline sắp thứ tự phase/week/lesson, tránh tải nội dung bài viết nặng.
Lỗi không tìm thấy phải dùng Problem Details hiện có.

Frontend: /curricula và /curricula/:slug, liên kết từ sidebar;
giữ tương thích đường dẫn /curriculum cũ bằng redirect. Dùng API client,
TanStack Query và generated types hiện có. Có loading, empty, error/retry,
accordion phase/week, thời lượng và nhãn bắt buộc/tùy chọn; thể hiện chuẩn
Anh-Anh và nguồn gốc trung thực, không mô tả nguồn UNVERIFIED là đã xác minh.

Kiểm thử bằng PostgreSQL Testcontainers biệt lập, không dùng DB dev:
draft không xuất hiện, pilot published có 1 phase/1 tuần/6 lessons,
outline không có Markdown/đáp án, lỗi 404, thứ tự ổn định và query không N+1.
FE kiểm tra loading/empty/error/retry và nội dung pilot.
Chạy Maven verify, FE test/typecheck/lint/build; báo lệnh/kết quả thực tế.
Không triển khai enrollment, lịch hoặc tiến độ G3. Không đọc .env hay secrets.
Không tự import/publish vào DB dev. Dừng sau mã, tests và báo cáo để Codex review.
```
