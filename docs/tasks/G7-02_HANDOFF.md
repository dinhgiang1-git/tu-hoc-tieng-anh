# G7-02 — Nhiệm vụ: Kiểm thử Hiệu năng API (p95 < 500ms) & Kiểm thử Trực quan Giao diện Desktop

## 1. Mục tiêu & Bối cảnh

Tiếp nối thành công của Mốc G6 và Task G7-01 (sao lưu & phục hồi dữ liệu), Task **G7-02** thực hiện đánh giá chất lượng toàn diện trước khi phát hành bản dùng ổn định (Mốc M2) theo `KE_HOACH_DU_AN.md` (Mục 11 - G7, Mục 12.1, 12.2):
1. **Kiểm thử hiệu năng API (API Benchmark):**
   - Đo lường độ trễ (latency: min, avg, p50, p90, p95, max) của toàn bộ các API cốt lõi phục vụ người học trên bộ dữ liệu thực tế đầy đủ của Lộ trình A (26 tuần, 130 lessons, 406 activities).
   - Ngân sách hiệu năng (Performance Budget): **p95 < 500ms** trên môi trường máy phát triển tiêu chuẩn.
2. **Kiểm thử trực quan (Visual & Accessibility Audit):**
   - Xác minh giao diện trực quan trên web desktop (1440×900 chuẩn và 1280×800 laptop).
   - Kiểm tra hiển thị ký tự phiên âm quốc tế IPA (không lỗi font/ô vuông), layout không tràn ngang, trạng thái tương phản, bàn phím và modal dialogs.
   - Chụp ảnh màn hình minh chứng cho các trang quan trọng nhất.

---

## 2. Danh mục API Cốt lõi cần Đo lường

Kiểm thử tự động lặp (20–50 requests mỗi endpoint) có phiên đăng nhập xác thực:
1. `GET /api/v1/curricula` — Danh mục lộ trình công khai
2. `GET /api/v1/curricula/roadmap-a` — Chi tiết lộ trình A
3. `GET /api/v1/revisions/{revisionId}/outline` — Đề cương toàn bộ 26 tuần (130 bài học)
4. `GET /api/v1/me/today` — Bảng điều khiển Hôm nay của người học
5. `GET /api/v1/me/enrollments` — Danh sách lộ trình đã đăng ký
6. `GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/summary` — Tóm tắt buổi học
7. `GET /api/v1/learning/workspace/{enrollmentId}/lessons/{lessonId}` — Không gian học tập chi tiết (Markdown + Activities)
8. `GET /api/v1/learning/enrollments/{enrollmentId}/analytics` — Thống kê phân tích & Biểu đồ học tập tuần
9. `GET /api/v1/learning/enrollments/{enrollmentId}/review/items` — Danh sách câu sai & mục cần ôn tập

---

## 3. Tiêu chí Nghiệm thu (DoD)

1. **Hiệu năng API Đạt Chuẩn:**
   - 100% các endpoint được kiểm tra có **p95 < 500ms**.
   - Tỷ lệ lỗi (Error rate) = 0.0%.
   - Không phát sinh hiện tượng nghẽn I/O hay N+1 query trên payload 26 tuần.
2. **Kiểm thử Trực quan Đạt Chuẩn (Desktop Web):**
   - Giao diện đáp ứng tốt trên 1440×900 và 1280×800.
   - Ký tự IPA hiển thị sắc nét, chuẩn Unicode.
   - Modal và dropdown hoạt động trơn tru.
   - Ảnh chụp minh chứng được lưu trữ làm bằng chứng nghiệm thu.
3. **An toàn Môi trường:**
   - Không làm thay đổi mật khẩu hoặc phá hủy dữ liệu học tập hiện có.
   - Không commit secrets, `.env`, `docs/DAY2_REVIEW.md`, hay `backend/src/main/resources/META-INF/spring/`.
