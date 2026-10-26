# G4-02 — Xem trước và xác nhận đổi lịch

## 1. Mục tiêu và phạm vi

Người học chọn ngày tiếp tục/ngày học trong tuần, xem lịch cũ–mới, rồi xác nhận áp dụng cho các buổi **chưa hoàn thành**. Giữ nguyên kết quả học và lịch buổi đã xong.

Baseline `aca84d7`: G4-01 đã nghiệm thu luồng chính. Đọc `AGENTS.md`, hai code conventions, `docs/product-rules.md` mục 5, `docs/api/openapi.yaml`, `docs/G4-01_IMPLEMENTATION.md`; tái dùng SchedulePlanner, IdempotencyService, enrollment version và trang SchedulePage hiện có.

Chỉ desktop. E2E dùng app/DB dev và dữ liệu test đã định danh; giữ backend Testcontainers hiện có. Ưu tiên làm một luồng FE–BE–DB hoàn chỉnh, kiểm tra nhóm liên quan, không chạy full suites sau mỗi lượt.

**Chưa làm:** pause/resume G4-03, calendar kéo thả, chọn ngày nghỉ lễ riêng, đổi timezone enrollment, mobile, admin, G5. Chỉ cho đổi lịch enrollment ACTIVE; PAUSED/ABANDONED/COMPLETED trả STATE_CONFLICT. Revision của enrollment được giữ nguyên, kể cả đã archive sau khi bắt đầu học.

## 2. Luồng người học

1. Tại trang Lịch, chọn enrollment ACTIVE và bấm **Đổi lịch**.
2. Form gồm ngày tiếp tục, các thứ học trong tuần, mục tiêu phút/ngày; mặc định lấy cấu hình enrollment, ngày tiếp tục là hôm nay theo timezone enrollment.
3. Bấm **Xem trước**: bảng buổi học/ngày hiện tại/ngày đề xuất, ngày kết thúc mới và thông báo các buổi đã hoàn thành được giữ nguyên.
4. Chỉ bấm **Xác nhận đổi lịch** mới lưu. Đóng/hủy hoặc xem trước không sửa DB.
5. Thành công: cập nhật lịch, metadata và Today. Conflict: giữ form, thông báo cần xem trước lại, không tự commit bằng version mới.

Không yêu cầu thêm bước admin hoặc cấu hình môi trường. Không đổi UI ngoài phần cần cho luồng này.

## 3. Quy tắc lập phương án

- Ngày tiếp tục là LocalDate, không trước hôm nay theo `Clock` và timezone enrollment; ngày bắt đầu không thuộc studyDays thì chọn ngày hợp lệ tiếp theo. studyDays không null/rỗng/chứa null, normalize/deduplicate; dailyTargetMinutes 10..180.
- Một lesson bắt buộc mỗi ngày học, đúng thứ tự phase/week/lesson. dailyTargetMinutes chỉ là mục tiêu, không chia hoặc gộp lesson; optional không chiếm slot.
- Lấy lesson_schedule hiện có làm nguồn. Completed xác định bằng completedAt do G3 quản lý. Giữ **nguyên toàn bộ row** completed: plannedDate, originalDate, completedAt, timestamps. Các row chưa completed chỉ đổi plannedDate khi cần; không xóa/insert lại lịch, không đổi originalDate ban đầu.
- Lesson đang làm dở vẫn là chưa completed nên có thể dời ngày, nhưng không đổi activity status/response/note/history. Không thay enrollment.startDate, timezone, revision hoặc learner preferences toàn cục; chỉ cập nhật cấu hình lịch của enrollment này.
- End date đề xuất là MAX của ngày mới các row chưa xong **và** ngày giữ nguyên của các row completed. Không lấy MAX chỉ trong tuần đang xem. Nếu không có lịch thì null; dữ liệu thiếu row required phải báo lỗi rõ, không tự tái tạo hoặc bỏ sót âm thầm.
- Tái dùng một hàm dựng phương án cho preview và commit. Preview phải là snapshot nhất quán với enrollmentVersion trả về; có thể dùng read-only REPEATABLE_READ để không trộn completion/version của hai thời điểm. Không lưu preview vào DB hoặc dựng hệ thống token/job mới.

## 4. Backend và contract

### Preview

`POST /api/v1/enrollments/{id}/schedule/preview`

Request theo SchedulePreviewRequest: `{resumeDate, studyDays, dailyTargetMinutes}`. Trả SchedulePreviewResponse có `enrollmentVersion`, `proposedEndDate`, `modifiedSchedules`.

- Owner từ principal; không tồn tại/không thuộc user →404. Session/CSRF theo cơ chế hiện có. Kiểm tra lifecycle và input; preview không mutate bất kỳ row/version/event nào.
- `modifiedSchedules` chỉ gồm các buổi chưa completed có ngày thực sự thay đổi, theo thứ tự sư phạm. Mỗi item cần ngày trước đổi và ngày mới; bổ sung `previousPlannedDate` hoặc schema item chuyên biệt vào contract, giữ nghĩa `originalDate` là lịch gốc lúc đăng ký, không dùng nó giả làm ngày ngay trước lần đổi.
- Cấu hình studyDays/minutes có thể đổi dù không có row cần dời. UI vẫn xem trước được thay đổi cấu hình; không coi mảng modifiedSchedules rỗng là lỗi.

### Commit

`POST /api/v1/enrollments/{id}/schedule/commit`

Request ScheduleCommitRequest: cùng input preview + `expectedVersion` không âm; bắt buộc `Idempotency-Key` UUID. Response giữ EnrollmentResponse theo contract, trả version thực tế đã persist. Đồng bộ OpenAPI/hai generated types, không dùng any hoặc tự chép DTO.

Thứ tự xử lý:

1. Xác thực owner/CSRF/key và shape dữ liệu; normalize payload. Idempotency scope theo user + operation, hash gồm **enrollmentId, toàn bộ input, expectedVersion**; không replay nhầm enrollment khác. Tái dùng IdempotencyService và cơ chế serialize key/user của CreateEnrollmentService.
2. Lookup replay **trước** kiểm tra version/lifecycle/ngày hiện tại phụ thuộc thời gian. Cùng key + payload đã thành công trả kết quả cũ, kể cả version hiện tại đã tăng hoặc retry qua ngày mới. Cùng key khác payload → IDEMPOTENCY_CONFLICT.
3. Với request mới, kiểm tra ACTIVE và expectedVersion; stale → VERSION_CONFLICT. Dựng lại phương án từ DB + chính input đó, không tin danh sách ngày do client tự gửi. Chưa cần preview token bắt buộc, nhưng UI phải chỉ confirm snapshot vừa xem.
4. Ghi changed plannedDate, studyDays/dailyTargetMinutes enrollment, tăng version **đúng một lần** và lưu idempotency result trong một transaction. Không tạo learning COMPLETED/SUBMITTED event. No-op thật với version hiện hành không đổi lịch/config/timestamps/version, vẫn có thể lưu replay result.
5. Phối hợp thứ tự ghi/lock với progress/undo/submit hiện có. Có thể flush schedule updates rồi CAS enrollment/config cuối transaction, tương tự learning mutation, tránh một luồng giữ enrollment chờ schedule còn luồng kia giữ schedule chờ enrollment. Không gán config trên entity managed rồi để Hibernate tự tăng version trước CAS. Nếu chọn cách khác, phải có kiểm chứng race/rollback tương đương, không sửa auth hoặc bỏ concurrency.
6. CAS hoặc bước bất kỳ thất bại → rollback toàn bộ schedule/config/version/idempotency; không có lịch ghi nửa chừng, không tiếp tục query sau transaction PostgreSQL aborted. Race progress-vs-commit cùng version tối đa một thay đổi commit, phía còn lại 409, không 500/deadlock không xử lý.

Không đổi schema DB nếu không cần. Nếu cần migration mới thì dùng số kế tiếp thực tế; không chỉnh migrations đã áp dụng.

## 5. Frontend dùng lại trang Lịch

- Một modal hoặc panel đơn giản trên SchedulePage, chỉ bật cho ACTIVE đã đọc đáng tin cậy. Form labels và lỗi inline, không UUID/version kỹ thuật. Modal có focus/Tab/Escape đúng theo mẫu đang có.
- Form thay đổi sau preview phải hủy hiệu lực preview và khóa confirm đến khi xem trước lại. Response preview cũ đến muộn không được mở confirm cho input mới hoặc enrollment mới; key/snapshot gồm enrollmentId và input.
- expectedVersion lấy từ preview, không tự lấy version mới hơn từ detail rồi commit mà không xem lại. Cần giữ snapshot input tương ứng khi confirm.
- Disable confirm khi pending; một Idempotency-Key cho một attempt/snapshot. Timeout không biết đã commit chưa: giữ nguyên key + payload để retry/replay. Xem preview mới/input mới tạo attempt mới; không sinh key mới mỗi lần retry request cũ.
- Khi VERSION_CONFLICT/STATE_CONFLICT, giữ form và cho tải trạng thái/xem trước lại; không tự resubmit. Preview lỗi không báo Đã lưu. Commit thành công nhưng refetch lỗi phải hiển thị trạng thái trung thực và retry đồng bộ, không tự commit lần hai.
- Đồng bộ schedule keys của enrollment, detail/endDate, summary/Today/my-enrollments và workspace/lesson-summary có ngày lịch liên quan. Không xóa draft/note của scope đang học, không cập nhật cache user khác.
- Giữ originalDate trong bảng lịch; cho thấy ngày hiện tại trước lần đổi trong preview. Bài completed không nằm trong danh sách bị dời. Không làm UI calendar mới hoặc thêm bước không cần thiết.

## 6. Thứ tự triển khai và kiểm chứng tối thiểu

1. Backend dựng phương án + preview, tái dùng planner hiện có.
2. Commit nguyên tử + idempotency/version; nối FE form/preview/confirm.
3. Chạy nhóm kiểm tra dưới đây một lần, sửa lỗi thực tế rồi chỉ chạy lại phần bị ảnh hưởng.

**Kiểm tra trọng tâm:**

- Unit/table-driven cho phương án: bài completed giữ nguyên, bài chưa xong đúng thứ tự/ngày, originalDate không đổi, input rỗng/ngày trước today bị từ chối, endDate lấy cả row giữ nguyên.
- Một nhóm PostgreSQL integration/API thật: preview không ghi; commit/replay đúng; cùng key khác payload/stale version/owner B bị từ chối; inject lỗi giữa thao tác để xác minh rollback; race progress-vs-commit. Gom fixture, không viết hàng loạt test chỉ kiểm tra getters/labels.
- FE build (gồm TypeScript) + lint. Một test deferred preview để bắt response cũ/input đã đổi và confirm key/version đúng nếu logic này cần test riêng.
- E2E trên DB dev: complete một buổi → preview đổi ngày → hủy chứng minh chưa đổi → confirm → reload lịch/Today → buổi đã xong và note còn nguyên, buổi chưa xong dời đúng. Thêm probe stale version hoặc hai tab trong cùng nhóm. Một smoke học/undo hiện có nếu bị ảnh hưởng.

Không chạy lại toàn bộ Maven verify/Vitest/Playwright ở mỗi lượt. JUnit IT từng vướng loopback Java trong lượt G4-01: nếu vẫn lỗi môi trường, dùng harness API/E2E thật với DB dev để chứng minh transaction/race cần thiết và báo rõ; không mất nhiều vòng chỉnh hạ tầng, không thay bằng Mockito-only. Full regression cuối G4 hoặc khi có rủi ro lan rộng mới.

Chỉ seed/reset tài khoản test allowlist hoặc IDs do test tạo. Không truncate/drop DB, sửa dữ liệu học cá nhân, tắt trigger, xóa curriculum published hoặc commit secrets.

## 7. Bàn giao và prompt

Ghi báo cáo ngắn `docs/G4-02_IMPLEMENTATION.md`: luồng đã làm, file/API đổi, lệnh kiểm tra/kết quả và lỗi/giới hạn còn lại. Cập nhật backlog chờ Codex nghiệm thu. Công nhân không commit/push, chưa làm G4-03. Codex tự sửa lỗi nhỏ; tối đa hai lượt trả công nhân cho cùng finding.

```text
Triển khai G4-02 theo docs/tasks/G4-02_HANDOFF.md, đọc AGENTS.md trước.
Baseline G4-01: aca84d7. Làm preview/confirm đổi lịch trên trang Lịch hiện có,
giữ buổi completed/originalDate/progress/note; commit nguyên tử dùng enrollment
expectedVersion và Idempotency-Key, retry không áp lịch hai lần.
Ưu tiên một luồng chạy được, kiểm chứng trọng tâm ở mục 6; không chạy full
suites lặp lại. Dùng app/DB dev, chỉ dữ liệu test. Báo cáo ngắn tại
docs/G4-02_IMPLEMENTATION.md rồi dừng cho Codex review.
Không sửa secrets/dữ liệu cá nhân/migration cũ, không commit/push, chưa G4-03.
```
