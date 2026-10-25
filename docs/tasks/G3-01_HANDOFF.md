# G3-01 — Enrollment, lịch ban đầu và trang Hôm nay

Ngày: 25/10/2026 (Asia/Ho_Chi_Minh).

## Đầu ra cần có

Người học đăng nhập, chọn lộ trình đã xuất bản, cấu hình ngày bắt đầu/thứ học/mục tiêu phút, đăng ký một lần và thấy bài cần học trên trang Hôm nay. Lịch và enrollment tồn tại sau reload. Không triển khai thao tác hoàn thành bài trong task này.

Đầu vào đã nghiệm thu: G2-02 tại commit `610b739`; backend 48 unit + 21 integration tests, FE 33 tests. Đọc BE_CODE_CONVENTION.md, FE_CODE_CONVENTION.md, product-rules.md mục 2/4/5/7, ADR-002/003 và OpenAPI trước khi code. Giữ thay đổi đang có; không commit/push, không đọc secrets hoặc chỉnh Docker/DB dùng chung.

## Phạm vi backend

### Schema và ràng buộc

- Migration kế tiếp là **V005**, không dùng lại V004 và không sửa V001–V004 đã áp dụng.
- Tạo enrollment, lesson_schedule, activity_progress, learning_event và bảng idempotency dùng chung nếu chưa có. Chỉ tạo phần schema learning cần cho truy vấn G3-01 và mở rộng G3-02; chưa tạo API ghi progress/event.
- Enrollment lưu owner, curriculum, revision cố định, status, start_date, time_zone IANA, study_days, daily_target_minutes, version và timestamp UTC. Dùng @Version; version ban đầu phải nhất quán với response, FE đọc từ server.
- Partial unique index `(user_id, curriculum_id) WHERE status IN ('ACTIVE','PAUSED')`. Ràng buộc DB phải bảo vệ cả hai request đồng thời, không chỉ exists-check trong Java.
- Mỗi `(enrollment_id, lesson_id)` có tối đa một schedule. Không tạo schedule cho lesson ngoài revision đã đăng ký. Dùng LocalDate/SQL DATE cho planned_date/original_date, không đổi thành UTC timestamp.
- Progress có unique `(enrollment_id, activity_id)`; chưa có record được hiểu là NOT_STARTED. Không tạo hàng trăm record progress rỗng nếu không cần. Không ghi learning_event COMPLETED khi mới đăng ký.
- Có FK/index phục vụ query theo owner, revision, planned_date. Kiểm tra curriculumId và revisionId cùng một lộ trình ở service, cùng transaction; ràng buộc quan hệ nhất quán ở DB khi phù hợp.

### API

Triển khai đúng OpenAPI và generated types hiện có:

| API | Hành vi |
|---|---|
| POST /api/v1/enrollments | Session + CSRF + Idempotency-Key bắt buộc, tạo enrollment/lịch nguyên tử, trả 201 EnrollmentResponse |
| GET /api/v1/enrollments/{id} | EnrollmentDetailResponse, gồm version và cấu hình lịch, chỉ owner đọc được |
| GET /api/v1/me/enrollments | Danh sách riêng của user, gồm version và tiến độ theo ProgressPolicy |
| GET /api/v1/me/today?enrollmentId=... | TodayResponse đủ 8 state, version và dữ liệu nullable nhất quán |

- Owner lấy từ principal; không nhận userId do FE truyền. Chưa login trả 401, thiếu CSRF trả 403. UUID của người khác hoặc không tồn tại trả 404 Problem Details, kể cả query enrollmentId của Today.
- Chỉ revision PUBLISHED được đăng ký mới, phải khớp curriculumId. Existing enrollment trên revision ARCHIVED vẫn đọc được; không dùng bộ lọc catalog PUBLISHED để vô tình chặn người đang học bản cũ.
- Mục tiêu phút: 10–180. studyDays không rỗng, tên thứ hợp lệ, chuẩn hóa tập ngày theo thứ tự ổn định. ZoneId hợp lệ; date đúng định dạng. Không thêm quy tắc cấm ngày quá khứ khi contract chưa quy định.
- Nếu đã ACTIVE/PAUSED cùng curriculum: key mới trả 409 STATE_CONFLICT. Không lộ 500 từ unique constraint.
- POST thành công đặt focusedEnrollmentId của chính user vào enrollment mới trong cùng transaction; không tự đổi các sở thích khác. Khi PATCH preferences đổi focusedEnrollmentId, xác minh enrollment thuộc user. Focus thiếu/null thì Today trả NO_ENROLLMENT theo product-rules; không tự chọn ngẫu nhiên enrollment của người khác hoặc mục đầu danh sách.
- Điều chỉnh mô tả OpenAPI của Today đang liệt kê thiếu UPCOMING; enum đã có 8 state. Nếu cần bổ sung schema để UI dùng field, sửa contract trước và regenerate cả hai file types, không tự ép kiểu `any`.

### Idempotency thực sự qua DB

- Scope `(userId, operation, key)`; không dùng cache RAM. Key UUID bắt buộc, hash từ payload chuẩn hóa, lưu response/status để replay.
- Cùng key + cùng payload trả kết quả đã lưu, không thêm schedule/enrollment/event. Kiểm tra replay trước khi kiểm tra enrollment đang tồn tại.
- Cùng key + payload khác trả 409 IDEMPOTENCY_CONFLICT. Key của user A không ảnh hưởng user B.
- Test hai request đồng thời: không lộ unique-constraint/transaction-aborted 500, chỉ một enrollment và một bộ lịch. Không bắt lỗi constraint rồi tiếp tục query trong transaction PostgreSQL đã hỏng; thiết kế transaction/locking đúng.
- Nếu tạo lịch lỗi sau khi tạo enrollment thì rollback cả enrollment, schedules, focus và idempotency success response.

### Xếp lịch ban đầu

1. Lấy các lesson bắt buộc theo thứ tự phase.orderIndex → week.orderIndex → lesson.orderIndex, có tie-break ổn định.
2. Duyệt LocalDate từ startDate (bao gồm ngày bắt đầu), mỗi ngày trong studyDays nhận đúng một lesson.
3. dailyTargetMinutes là mục tiêu cá nhân, không dùng để ghép/tách lesson. Lesson tùy chọn không chiếm ngày học bắt buộc.
4. plannedDate = originalDate khi tạo. Không xếp lại lịch mỗi lần GET Today.
5. Case chuẩn: MON/WED/FRI, startDate=2026-10-26, pilot 5 buổi bắt buộc → 2026-10-26, 10-28, 10-30, 11-02, 11-04.

### Truy vấn Today

- Inject Clock, tính todayDate bằng timezone enrollment; chưa có focus dùng timezone preferences. Không LocalDate.now() theo timezone máy chủ; không hardcode ngày test.
- Chặn trạng thái trước: thiếu focus → NO_ENROLLMENT, PAUSED → PAUSED, hoàn thành toàn bộ yêu cầu → CURRICULUM_COMPLETED. ABANDONED không tạo bài cần học; nếu đang focus bản này trả NO_ENROLLMENT (hoặc từ chối focus theo policy được ghi rõ), không tự kích hoạt lại.
- Enrollment ACTIVE chưa xong: IN_PROGRESS > OVERDUE sớm nhất > DUE_TODAY > REST_DAY/UPCOMING. Khi có nhiều bài cùng nhóm, chọn ổn định theo plannedDate rồi thứ tự sư phạm. Bài đã hoàn thành không còn được chọn.
- Ngày nghỉ vẫn có thể là IN_PROGRESS/OVERDUE theo ưu tiên; isRestDay phản ánh lịch ngày đó độc lập với state. Khi không có bài dở/quá hạn/đến hạn, ngày nghỉ có state REST_DAY và có thể gợi ý primaryAction.type=UPCOMING cho bài sắp tới. Trước startDate, gợi ý UPCOMING/REST_DAY theo lịch, không tạo quá hạn giả.
- NO_ENROLLMENT: enrollmentId/revisionId/curriculumTitle/enrollmentVersion/primaryAction/progress null, overdueLessons=[], isRestDay=false. PAUSED: primaryAction=null, overdueLessons=[], không cảnh báo nợ mới. Completed action nếu có thì lessonId=null, không dựng link bài học null.
- Tiến độ dùng yêu cầu hiệu lực `lesson.required && activity.required`. Pilot tổng 19 activities bắt buộc hiệu lực; phần trăm theo ProgressPolicy, không gộp điểm phát âm/quiz. Không hardcode pilot vào service. Mẫu số 0 trả null, không NaN/100% giả.
- Các trạng thái progress và PAUSED/COMPLETED được test bằng fixture DB; chưa mở mutation pause/resume/complete/undo để tạo dữ liệu cho test.
- Query projections/batch tránh N+1, không tải Markdown/activity payload/đáp án để dựng Today.

## Phạm vi frontend

- Nút Đăng ký trên CurriculumDetailPage mở dialog/form, dùng react-hook-form + zod và UI primitives hiện có. Nhập startDate, các thứ, dailyTargetMinutes và timezone (mặc định từ preferences, cho phép xem/chọn IANA hợp lệ).
- Có label, lỗi gần field, focus khi mở/đóng modal, đóng bằng Escape, dùng bàn phím; không nhồi chữ hướng dẫn nhỏ. Responsive 375px và desktop.
- Giữ Idempotency-Key cho cùng một lần submit khi mạng lỗi/chưa biết server đã commit. Đổi payload thì tạo operation/key mới. Disable submit lúc pending, không retry mutation mù bằng key mới.
- Success invalidate Today/my enrollments/preferences liên quan và chuyển /today. Hiển thị 409 STATE_CONFLICT/IDEMPOTENCY_CONFLICT cụ thể, giữ form khi lỗi; dùng API client/Problem Details hiện có.
- /today và trang gốc / dùng cùng TodayPage. Không để trang Home giả lập thay cho dữ liệu thực. Nếu /preferences hiện tái dùng HomePage, giữ route riêng cho phần chưa triển khai để không vô tình biến Preferences thành Today.
- Dùng query keys gồm enrollmentId khi có chọn focus; loading, empty/no enrollment, error/retry, các state 8 trạng thái đầy đủ; không hiển thị fake progress hoặc thành công khi API lỗi.
- Có lựa chọn enrollment từ danh sách của user nếu đã đăng ký nhiều lộ trình; cập nhật focus qua preferences và invalidate query liên quan. Không lưu owner/focus chỉ ở localStorage rồi mất sau reload.
- Hành động chính rõ ràng, danh sách quá hạn phụ thu gọn, ngày nghỉ nhẹ nhàng, thông điệp paused/completed phù hợp. Không thêm nút Resume gọi API G4 chưa triển khai; dùng thông điệp trạng thái thay cho nút chết.
- **Giới hạn G3-01:** CTA Bắt đầu/Tiếp tục/Học bù dẫn tới route `/enrollments/:enrollmentId/lessons/:lessonId` có màn hình tóm tắt chỉ đọc (title, thời lượng, lịch; lấy từ Today/outline và owner-checked enrollment). Thông báo ngắn phần học chi tiết sẽ có sau. Không redirect im lặng về trang chủ; không ghi bắt đầu/hoàn thành giả khi click. Không tạo Markdown renderer, checklist, quiz hoặc API lesson workspace đầy đủ (G3-03).

## Kiểm thử bắt buộc

- Unit Clock cố định: đủ 8 state và ưu tiên khi đồng thời có bài dở/quá hạn/đến hạn; timezone Ho_Chi_Minh/London, qua nửa đêm/cuối tháng/DST; không phụ thuộc ngày chạy.
- Lịch: case 26/10/2026 nêu trên, startDate vào ngày nghỉ, chỉ một thứ học/tuần, optional không chiếm ngày, thứ tự nhiều phase/week, tập ngày không hợp lệ.
- PostgreSQL Testcontainers biệt lập: migration, POST→GET→Today, pin revision, active/paused uniqueness, rollback giữa transaction, replay/khác payload/concurrent cùng key, cross-user key isolation, owner/CSRF/401/404, draft và archived không nhận đăng ký mới.
- Test Today với progress fixture: 19 yêu cầu hiệu lực, partial/complete, bỏ optional khỏi mẫu số, paused/completed, đọc enrollment archived vẫn được, reload version đúng.
- FE: form validation, pending/double submit, retry mạng giữ key, 409 có thể hiểu, success navigation/cache refresh, 8 Today states, selector focus/reload, không có CTA null hoặc route chết.
- Kiểm tra cả trang chưa enrollment và đã có enrollment ở 375px/desktop; không dùng dữ liệu người dùng để tạo screenshot fixture.
- Lệnh: backend `mvnw.cmd -o -B -ntp verify` (hoặc bỏ -o nếu thiếu cache), FE `npm test`, `npm run typecheck`, `npm run lint`, `npm run build`. Ghi kết quả thực tế, không viết “đạt” khi chưa chạy. Docker thiếu thì ghi phần chưa kiểm chứng, không fallback DB dev.

## Điểm dừng và báo cáo

Chỉ G3-01. Không mở progress mutations G3-02, workspace đầy đủ G3-03, reschedule/pause/resume G4 hoặc phát âm. Không seed/import/publish vào DB dev, không sửa migration cũ hoặc secrets, không commit/push. Tối đa hai lượt sửa cùng finding; sau đó Codex xử lý trực tiếp.

Báo cáo cuối: file đã thay đổi, migration, API/request/response, quyết định cho edge cases, số tests/lệnh thực tế, lỗi còn lại. Chờ Codex review trước khi chuyển G3-02.

## Prompt gửi AI triển khai

```text
Triển khai G3-01 theo docs/tasks/G3-01_HANDOFF.md trong
D:/WorkSpace/tu-hoc-tieng-anh. Đọc file đó và các tài liệu đầu vào trước khi
code. G2-02 đã nghiệm thu tại 610b739. Chỉ thực hiện phạm vi G3-01: enrollment,
lịch ban đầu, API đọc, Today và UI đăng ký. Migration kế tiếp V005.
Ưu tiên owner isolation, transaction/idempotency đồng thời, Clock/timezone,
8 trạng thái Today và lịch pilot chính xác. Không triển khai G3-02/G3-03/G4,
không chạm secrets/DB dev, không commit/push. Chạy kiểm thử biệt lập, cập nhật
backlog bằng kết quả thực tế rồi dừng để Codex nghiệm thu.
```
