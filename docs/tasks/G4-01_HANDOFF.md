# G4-01 — Nền tảng lịch học và màn hình lịch desktop

## 1. Mục tiêu và baseline

Làm phần đầu G4: chuẩn hóa bộ tính lịch, API đọc lịch theo khoảng ngày, ngày kết thúc theo lịch hiện hành và trang Lịch học dùng được trên desktop. Chưa triển khai đổi lịch hoặc pause/resume.

Baseline `67c44c7`: M1 desktop đã nghiệm thu; 143 backend unit + 48 PostgreSQL integration + 98 frontend tests; 14 E2E full và nhóm lặp 9 tests đạt. Đọc:

- `BE_CODE_CONVENTION.md`, `FE_CODE_CONVENTION.md`, `docs/product-rules.md` mục 5.
- `KE_HOACH_DU_AN.md` G4, `docs/api/openapi.yaml`, `docs/G3-04_ACCEPTANCE.md`.
- `CreateEnrollmentService`, `EnrollmentQueryService`, `LessonSchedule`, `Enrollment`, các repositories/projections và logic Today/ProgressPolicy.
- Router/AppShell, Today/enrollment APIs, `frontend/e2e`, `scripts/run-e2e.ps1`, `scripts/seed-e2e.sql`.

Hai file ngoài phạm vi từ trước: `docs/DAY2_REVIEW.md`, thư mục `backend/src/main/resources/META-INF/spring/`. Không sửa/commit chúng.

Ưu tiên vẫn là **web desktop 1440×900**, smoke 1280×800; chưa làm mobile. E2E được phép dùng trực tiếp app/DB dev với dữ liệu test xác định; không dựng hạ tầng biệt lập mới. JUnit integration giữ Testcontainers hiện có.

## 2. Lộ trình G4 và điểm dừng

| Task | Phần việc | Điều kiện chuyển bước |
|---|---|---|
| **G4-01 — task hiện tại** | Planner dùng chung, GET lịch, ngày kết thúc, UI lịch theo tuần/khoảng ngày | Codex nghiệm thu đọc lịch đúng, không phá tiến độ/lịch đang lưu |
| G4-02 — giao sau | Preview/commit đổi lịch, giữ lesson completed, expectedVersion và Idempotency-Key | Handoff riêng sau G4-01, không code trước |
| G4-03 — giao sau | Pause/resume + áp lịch mới nguyên tử, Today/lifecycle đồng bộ | Handoff riêng, không bật nút giả trong G4-01 |

Không làm calendar kéo thả, ngày lễ theo quốc gia, CRUD ngày nghỉ riêng, nhắc lịch/background jobs, dashboard thống kê mới, G5/phát âm/admin hoặc rework toàn bộ UI. “Ngày nghỉ” ở G4-01 là ngày không được chọn trong studyDays, không tự suy diễn thêm bảng holiday.

## 3. Quy tắc lịch và planner

### 3.1 Nguồn chuẩn

- Một lesson **bắt buộc** chiếm một ngày học được chọn; optional lesson không chiếm lịch. Thứ tự sư phạm ổn định phase → week → lesson → id tie-break, theo revision của enrollment.
- `startDate` inclusive. Nếu không thuộc studyDays, chọn ngày hợp lệ kế tiếp. Chu kỳ tuần chỉ quyết định ngày học; không ép số tuần nội dung thành cùng số tuần lịch.
- dailyTargetMinutes 10..180 là mục tiêu học, **không** tự chia nhỏ lesson hoặc nhồi nhiều lesson/ngày. Không thay luật này khi tách planner.
- Dùng LocalDate cho plannedDate/originalDate, Clock + timeZone enrollment khi cần ngày hôm nay. Không dùng ngày Windows/browser mặc định hoặc UTC date thay ngày lịch người học.
- Planner hàm thuần trong module scheduling (hoặc package domain phù hợp convention), không đọc repository/Clock/security bên trong. Input bài đã sắp thứ tự, startDate, tập DayOfWeek hợp lệ; output lessonId/plannedDate và ngày cuối nullable. Có thể để tham số exclusions nội bộ phục vụ bước sau, nhưng G4-01 truyền tập rỗng, chưa có API/UI lưu ngày nghỉ riêng.
- studyDays null/rỗng hoặc chứa null phải reject trước vòng lặp; normalize/deduplicate và có giới hạn đầu vào theo content validator hiện có. Danh sách lesson rỗng trả lịch rỗng/endDate=null; lỗi ngày vượt miền trả lỗi nghiệp vụ, không vòng lặp vô hạn.

### 3.2 Tích hợp không phá dữ liệu

- Thay vòng lặp tạo lịch ở `CreateEnrollmentService` bằng planner với hành vi tương đương. Giữ owner lock, revision publish, Idempotency-Key, transaction tạo enrollment/preferences/lịch và các giới hạn cũ.
- Chỉ dùng planner để tạo enrollment **mới**. Không chạy batch sửa lịch enrollment cũ khi startup, không regenerate khi GET, không xóa/insert lại lesson_schedule để “đồng bộ”.
- Lịch đã persist là nguồn đọc chuẩn: giữ plannedDate, originalDate, completedAt và timestamps. Ngày nghỉ mới tính không được tự dời các bài chưa học chỉ vì đã quá hạn.
- Chuẩn bị khả năng planner dùng lại cho preview G4-02, nhưng chưa gọi nó để mutate hay cập nhật enrollment studyDays trong task này.

## 4. Backend đọc lịch

### 4.1 GET schedule

Triển khai endpoint đã có trong OpenAPI:

`GET /api/v1/enrollments/{enrollmentId}/schedule?from=YYYY-MM-DD&to=YYYY-MM-DD`

- Giữ response **array LessonScheduleResponse** hiện có; không đổi root response thành object gây lệch contract.
- Owner từ CurrentUser; enrollment không tồn tại/không thuộc user → 404. Cho đọc lịch PAUSED/ABANDONED/COMPLETED và revision archived của enrollment. Không có lịch trong khoảng →200 `[]`.
- from/to inclusive. Không truyền cả hai: tuần hiện tại từ thứ Hai đến Chủ nhật theo timezone enrollment. Chỉ from: to=from+6; chỉ to: from=to−6. Cho phép xem quá khứ/tương lai. Khoảng đảo, date sai, khoảng >366 ngày inclusive →400 theo ProblemDetail hiện có.
- DTO có lessonId, lessonTitle, plannedDate, originalDate, isCompleted; bổ sung `isOverdue` boolean nếu cần cho UI. isCompleted đọc completedAt của schedule do G3-02 quản lý. isOverdue chỉ true khi enrollment ACTIVE, chưa completed và plannedDate < localToday; ngày hôm nay không overdue. PAUSED/ABANDONED không tô cảnh báo nợ bài.
- Query giới hạn theo enrollmentId + range tại DB; order plannedDate rồi thứ tự sư phạm/id ổn định. Dùng projection/join title, không kéo Markdown/user_response/quiz payload hoặc tạo N+1. Nếu cần index mới dùng migration tiếp theo (V008 hoặc số thực tế), không chỉnh V001–V007.
- Các GET không ghi DB, không tăng version, không tạo event hoặc sửa planned/original dates. Không tính lại completion theo cách khác G3-02.

### 4.2 Ngày kết thúc theo lịch hiện tại

- Bổ sung `estimatedEndDate` nullable vào **GET enrollment detail** và OpenAPI/generated types. Tính MAX(plannedDate) của toàn bộ schedule enrollment, không chỉ khoảng ngày UI đang xem; không có schedule →null.
- Đây là ngày cuối của lịch đang lưu, **không phải dự đoán người dùng chắc chắn sẽ hoàn thành**. Không cộng lại “remaining lessons tính từ hôm nay” mỗi lần GET; không suy từ weekNumber hoặc ngày completedAt.
- Giữ endDate khi học trước/undo vì G4-01 không đổi lịch. UI dùng nhãn “Kết thúc theo lịch hiện tại”; nếu PAUSED/ABANDONED hiển thị trạng thái riêng, không hứa ngày hoàn thành mới.
- Cập nhật generated types bằng generator hiện có và sửa fixtures/types cần thiết. Tránh copy DTO handwritten hoặc dùng any để che lệch contract. Không đổi shape Today/list chỉ để tiện trang lịch.

## 5. Frontend desktop

- Thay placeholder `/schedule` bằng trang Lịch học thật, giữ sidebar trái/AppShell/auth guard hiện có.
- Select enrollment từ `/me/enrollments`, ưu tiên focusedEnrollment nếu hợp lệ, nếu không chọn enrollment hợp lệ đầu tiên. Đổi lựa chọn **chỉ đổi lịch đang xem**, không tự đổi focusedEnrollment hay reschedule. Danh sách rỗng có CTA danh mục; enrollment đã mất quyền phải có lỗi/đường quay lại, không fallback âm thầm sang dữ liệu cũ.
- Header: tên lộ trình, timezone, studyDays, trạng thái và ngày kết thúc theo detail. Không UUID/version kỹ thuật trong nội dung người học.
- View mặc định theo tuần, có Trước/Sau/Hôm nay và khoảng ngày rõ ràng. Có thể nhóm theo ngày bằng danh sách/table gọn; chưa cần thư viện calendar nặng. Ngày nghỉ hiển thị trạng thái không có buổi theo lịch, không tạo lesson giả. Range/date formatting giữ local date đúng timezone.
- Mỗi buổi có title/ngày/Đã hoàn thành–Quá hạn–Theo lịch, link workspace thật. Có thể xem originalDate khi khác plannedDate để dùng được sau G4-02. Không tự hoàn thành khi click/mở link.
- PAUSED/ABANDONED đọc lịch nhưng không hiện nút mutation chưa làm. COMPLETED vẫn mở lại nội dung học theo guard G3-03. Không có nút Đổi lịch/Pause/Resume vô tác dụng hoặc text G4 placeholder.
- Loading/empty/error/retry đầy đủ; query keys theo enrollment và from/to. Đổi enrollment/range không flash rows cũ; response cũ không ghi sang scope mới. Không dùng thiếu dữ liệu như lịch rỗng thành công.
- Khi complete/undo/submit từ workspace rồi quay lại lịch, trạng thái lịch phải cập nhật. Extend invalidation hiện có đúng key; bảo toàn sync/error/draft guard của G3-03, không copy thêm cơ chế mutation yếu hơn.
- Kiểm tra desktop 1440×900 và 1280×800, bàn phím select/nút/link, focus rõ và không overflow ngang toàn trang. Mobile vẫn deferred.

## 6. Tests và nghiệm thu

### Backend

1. Planner: Mon/Wed/Fri; một ngày/tuần; bắt đầu ngày nghỉ; qua tháng/năm/năm nhuận; optional không chiếm slot; thứ tự bài; empty lessons/endDate null; empty/null days reject; dailyTarget không làm đổi số lesson/ngày.
2. Create enrollment giữ ngày/lịch/idempotency/owner-lock và rollback như trước; existing regression G3-01 còn đạt. Không sinh lịch cho optional.
3. HTTP schedule: owner A/B, 401/404, from/to defaults/inclusive/boundary/>366/invalid date, empty range, thứ tự ổn định. Clock fixed tại ranh giới UTC/local date, London DST và Ho_Chi_Minh để xác minh overdue ngày thực.
4. PAUSED/ABANDONED/COMPLETED/archived revision đọc được đúng scope, không phát cảnh báo overdue sai. Complete/undo làm schedule isCompleted đổi, endDate/planned/original không đổi.
5. GET không mutation: kiểm tra version/timestamps/events trước/sau không đổi. EndDate lấy toàn enrollment ngay khi query lịch một tuần rỗng.
6. Query/projection: fixture nhiều lessons/resources không N+1, không select Markdown/user_response. Chỉ thêm index khi query mới cần.

### Frontend / browser thật

7. Loading/error/retry/empty, select enrollment, chuyển tuần, Hôm nay, ngày nghỉ và endDate. Deferred reads/range change không hiển thị dữ liệu enrollment cũ.
8. Browser E2E trên app/DB dev: login → enroll với lịch 3 ngày/tuần → mở Lịch → kiểm tra ngày và link workspace → complete → quay lại/reload lịch có completed → undo → lịch cập nhật, planned/original/endDate giữ nguyên. Optional không xuất hiện như buổi bắt buộc.
9. Account B không xem lịch A khi thay UUID; desktop screenshots hai viewport, keyboard. Giữ 14 E2E M1 đang có, nhất là notes/conflict/navigation và modal focus.

Dùng dữ liệu test có IDs/names xác định trong DB dev; không truncate/reset dữ liệu cá nhân, không tắt trigger/ràng buộc hoặc xóa curriculum published. Có thể mở rộng allowlist fixture script nếu tạo account mới; không đổi mật khẩu/tiến độ `admin`/`dinhgiang1`. Backend integration vẫn dùng Testcontainers hiện có.

Chạy Maven verify, FE tests/typecheck/lint/build; E2E lịch mới và regression M1. Với thay đổi nhỏ không liên quan một case, tránh lặp mọi suite nhiều lần vô ích; báo lệnh và số tests thực chạy, không thay discovery thành pass.

## 7. Tài liệu / bàn giao

- `docs/G4-01_IMPLEMENTATION.md`: file/API/schema đổi, quyết định planner/timezone/default range/endDate, query checks, bugs sửa, test commands/results, screenshots và phần chưa kiểm chứng.
- Cập nhật OpenAPI/hai generated types và backlog **chờ Codex nghiệm thu**, không tự chốt task.
- Nếu phát hiện contract cũ mâu thuẫn quyết định trên, cập nhật mô tả đúng phạm vi G4-01 và nêu rõ; không tự mở G4-02/03.
- Không commit/push; Codex nghiệm thu và quản lý Git. Công nhân tối đa 2 lượt sửa cùng finding, Codex tự xử lý phần còn lại.

## 8. Prompt gửi công nhân

```text
Triển khai G4-01 theo docs/tasks/G4-01_HANDOFF.md trong
D:/WorkSpace/tu-hoc-tieng-anh. Baseline M1 desktop đã nghiệm thu 67c44c7.
Làm planner dùng chung tương đương lịch enrollment hiện có, GET schedule
owner/range/timezone đúng, estimatedEndDate theo lịch persisted và trang
Lịch học desktop thật. Không regenerate lịch cũ, không phá progress/note.
Chưa làm preview/commit đổi lịch, pause/resume, calendar kéo thả hoặc mobile.
Tái dùng app/DB dev cho E2E với dữ liệu test; JUnit giữ Testcontainers hiện có.
Chạy checks, cập nhật contract/generated types, docs/G4-01_IMPLEMENTATION.md
và backlog chờ Codex review rồi dừng. Không sửa secrets, dữ liệu cá nhân,
compose dùng chung, migration cũ; không commit/push. Tối đa 2 lượt sửa cùng
finding, còn lại Codex tự xử lý. Không bắt đầu G4-02 trước nghiệm thu.
```
