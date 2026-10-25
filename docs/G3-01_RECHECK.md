# G3-01 — thẩm định sau lượt sửa thứ nhất

## Kết luận cuối — Đạt sau lượt sửa thứ hai (25/10/2026)

Codex đã đọc bản sửa và chạy độc lập `mvnw.cmd -o -B -ntp verify`: **BUILD SUCCESS, 81 unit tests + 31 integration tests**, 0 failures/errors/skips; Spotless đạt. Log local: `backend/target/review-g301-final.log`. FE: **54/54 tests**, TypeScript (trong build), lint không error và production build đạt.

**R1–R5 đã đóng.** R4 đã dùng LessonSummaryDto/ActivitySummaryDto/ActivityProgressDto/RevisionInfoDto cho projections; my-enrollments batch theo tập enrollment/revision, không query trong vòng lặp từng enrollment. Lesson summary cũng dùng projection.

Integration ghi SQL qua Hibernate StatementInspector trong HTTP request thật trên PostgreSQL Testcontainers: so sánh Today ở pilot và bộ 10 lessons; so sánh danh sách 1 và 2 enrollments thuộc các revisions khác nhau. Query-count giữ nguyên giữa hai kích thước dữ liệu; SQL không chứa content_markdown, instructions, skill_tags hoặc user_response. Màn hình summary cũng được kiểm tra không chọn các cột này.

Các test đã xác nhận lịch 5 buổi bắt buộc, replay và xung đột idempotency, owner isolation, hai request đồng thời, rollback khi ghi lịch lỗi, revision archived vẫn đọc, trạng thái Today và chọn lại focus. Không thay đổi DB dev trong lượt nghiệm thu cuối.

Không còn finding chặn G3-01; không giao công nhân vòng sửa thứ ba. Chỉ dọn một dòng trống EOF trước commit, không đổi chức năng. Chưa kiểm tra trực quan bằng trình duyệt ở 375px/desktop; UI được kiểm tra qua React Testing Library. Warning form.watch và bundle khoảng 569 KB không chặn mốc này. Bước tiếp theo: G3-02.

Các phần bên dưới là lịch sử lượt thẩm định thứ nhất.

Ngày: 25/10/2026 (Asia/Ho_Chi_Minh).

## Kết luận

Chưa chốt G3-01. **R1, R2, R3, R5 đã xử lý trong phạm vi được kiểm tra; chỉ R4 còn thiếu.** Không mở thêm danh sách style/tính năng ngoài các finding cũ.

## Kiểm chứng độc lập

- `mvnw.cmd -o -B -ntp verify`: BUILD SUCCESS, **80 unit tests + 30 PostgreSQL Testcontainers integration tests**, 0 failures/errors/skips. Spotless đạt. Log local: `backend/target/review-g301-recheck.log`.
- FE: **54/54 tests**, lint không error (1 warning form.watch), `npm run build` gồm TypeScript đạt. Bundle khoảng 569 KB có warning.
- R1: test optional không giữ buổi completed ở IN_PROGRESS và PAUSED ưu tiên trước progress counts đạt.
- R2: selector/CTA chọn lại enrollment khi có một item + null focus; hai FE regressions được đưa vào suite và đạt.
- R3: API lesson summary có kiểm tra owner và lesson thuộc revision, trả title/thời lượng/plannedDate; FE dùng API mới, không hiển thị UUID/giai đoạn kỹ thuật. Test lesson mismatch và existing ARCHIVED đọc summary đạt.
- R5: integration mới gửi hai request cùng key/cùng payload, hai key khác nhau cùng curriculum, rollback khi schedule save lỗi, chặn đăng ký draft/archived, đọc enrollment archived, lịch London qua DST/cuối tháng đều đạt. Rollback test dùng spy gây lỗi tại saveAll, còn enrollment/idempotency/focus được kiểm tra trên PostgreSQL thật. Không coi đây là test ghi thành công một phần schedule rồi mới lỗi.
- Lượt verify dùng DB tạm. Truy vấn kiểm tra tài khoản riêng chỉ đọc username/is_active trong DB dev; xác nhận admin đang hoạt động, không đổi mật khẩu.

## R4 vẫn mở — giữ nguyên P2

Today đã đổi query activity theo từng lesson thành `findByLessonWeekPhaseRevisionId`, nên phần N+1 theo lesson đã giảm. Tuy nhiên vẫn query **entity đầy đủ**:

- TodayQueryService dùng `List<Lesson>` và `List<Activity>`; Lesson có content_markdown, Activity có instructions/skill_tags được load như các basic columns thông thường.
- EnrollmentQueryService dùng cùng entity; ActivityProgress còn có user_response. `getMyEnrollments` vẫn lặp qua enrollments, query revision và progress mỗi lần, thêm lesson/activity theo revision riêng. Cache Map chỉ giảm lặp khi nhiều enrollment trùng revision, chưa phải truy vấn batch độc lập số enrollment.
- Chưa có integration đếm query trên tập dữ liệu lớn hơn pilot hoặc xác nhận SQL không chọn các cột nặng. Test `never().findByLessonId...` chỉ chứng minh không gọi method cũ, không chứng minh đã đạt yêu cầu projection và query-count.

Đây chính là phần còn thiếu của R4 cũ, không phải yêu cầu mới. Handoff yêu cầu Today/list chỉ đọc projections/counts, tránh Markdown/payload và N+1.

## Giao sửa lần hai — chỉ R4

```text
Đọc docs/G3-01_RECHECK.md. R1/R2/R3/R5 đã qua; chỉ hoàn tất R4.
Đổi Today và my-enrollments sang projection/count-query/batch không SELECT
content_markdown, instructions, skill_tags hoặc user_response. List enrollment
không query riêng theo từng enrollment/revision; batch hoặc aggregate qua SQL.
Giữ nguyên API và logic trạng thái/owner/idempotency đã qua test.

Bổ sung integration đo SQL/query-count thật, so sánh dữ liệu nhỏ và lớn hơn
pilot để phát hiện query tăng theo số lessons/enrollments. Kiểm chứng SQL không
chọn cột nặng. Không chỉ sửa Mockito verify hoặc đổi tên repository method.
Chạy Maven verify và FE checks theo phạm vi thay đổi, ghi kết quả rồi dừng.
Không sửa secrets/DB dev/migration cũ, không mở G3-02/G3-03, không commit/push.
Đây là lượt sửa thứ hai của R4; nếu còn lỗi Codex sẽ tự xử lý, không tạo thêm
vòng giao lại cùng finding.
```
