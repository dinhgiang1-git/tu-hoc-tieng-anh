# G3-02 — Tiến độ, hoàn tác, nộp phản tư và tóm tắt

Ngày: 25/10/2026 (Asia/Ho_Chi_Minh).

## Mục tiêu và đầu vào

Người học thao tác nhiệm vụ trên bài thuộc enrollment của mình; trạng thái/câu trả lời được lưu qua reload, Today và tiến độ cập nhật đúng. Hai tab không ghi đè nhau, hoàn tác không xóa lịch sử. Chỉ mở rộng luồng học tối thiểu, chưa làm workspace đầy đủ.

Baseline đã nghiệm thu: **G3-01 commit 3af9d1e**, 81 backend unit + 31 PostgreSQL integration + 54 FE tests. Đọc BE_CODE_CONVENTION.md, FE_CODE_CONVENTION.md, product-rules.md mục 2/3/4/6/7, ADR-003, OpenAPI và G3-01_RECHECK.md. Giữ các projections và regression cũ.

Codex review và commit/push. Công nhân không commit/push, không đọc .env/secrets, không thay DB/container/volume phát triển. Tối đa **hai lượt sửa cho cùng finding**; sau đó Codex tự sửa, không giao vòng thứ ba.

## Phạm vi API và schema

| Endpoint | Hành vi |
|---|---|
| PUT /api/v1/enrollments/{id}/activities/{activityId}/progress | UpdateProgressRequest: status + expectedVersion; trả ProgressResponse |
| POST /api/v1/enrollments/{id}/activities/{activityId}/response/submit | SubmitResponseRequest: responseText + expectedVersion; lưu text/hoàn thành nguyên tử; trả ProgressResponse |
| GET /api/v1/enrollments/{id}/summary | ProgressSummaryResponse gồm enrollmentVersion |
| GET /api/v1/enrollments/{id}/lessons/{lessonId}/activities | Read model nhẹ để UI thao tác nhiệm vụ trong G3-02; thêm contract/types trước code |

Endpoint đọc mới trả envelope `lessonId`, `enrollmentVersion`, `activities[]`; mỗi activity có id, title, type, required, completionPolicy, status và responseText nullable của chính owner. Thứ tự orderIndex ổn định. Không trả Markdown, skill_tags, quiz answers/payload hoặc dữ liệu người khác. Nó khác endpoint workspace đầy đủ của G3-03; không dựng workspace giả hoặc gọi endpoint chưa tồn tại.

- Hoạt động đọc responseText ở endpoint này là cần thiết để reload form, không kéo text riêng tư vào Today/my-enrollments vốn phải nhẹ.
- Tái dùng enrollment/activity_progress/learning_event và unique keys của V005. Migration mới nếu cần là **V006 hoặc số kế tiếp thực tế**, không sửa V001–V005 đã áp dụng.
- LearningEvent hiện chưa có local_date/time_zone; bổ sung dữ liệu để lưu UTC occurredAt và ngày địa phương theo timezone enrollment tại thời điểm thao tác. Giữ lịch sử cũ, không tùy tiện đổi ngày theo timezone preferences hiện tại. Trường bổ sung cho dữ liệu cũ phải có kế hoạch backfill/nullability được ghi rõ.
- activity_progress đã có user_response; không tạo thêm nguồn sự thật cạnh tranh nếu chưa cần. Event payload JSONB phải persist thực sự bằng JDBC/JPA mapping đúng, không chỉ qua mock.
- Bổ sung ErrorCode RESPONSE_REQUIRED (400) vì contract/product-rules đã nhắc nhưng enum backend hiện chưa có. Các lỗi khác dùng code sẵn có và Problem Details.
- Nếu đổi mô tả/response schema, sửa docs/api/openapi.yaml và regenerate cả docs/api/generated-types.ts, frontend/src/generated/api-types.ts, giữ đồng bộ.

## Quy tắc mutation

### Owner, revision và lifecycle

- Session + CSRF bắt buộc. Owner lấy từ CurrentUser, không nhận userId của FE. Foreign/missing enrollment/activity/lesson hoặc activity không thuộc revision đăng ký → 404; không kiểm tra payload tiến độ của người khác trước owner.
- Existing enrollment trên ARCHIVED vẫn hoạt động theo trạng thái enrollment; chỉ đăng ký mới bị chặn ở G3-01. Không thay revision gắn với enrollment.
- ACTIVE cho phép thao tác hợp lệ. PAUSED/ABANDONED không nhận mutations học tập: 409 STATE_CONFLICT, DB/version/event không thay đổi. Completed xử lý như phần hoàn tác bên dưới; không cho ghi tùy tiện mọi trạng thái.
- Không cho checklist hoàn thành/undo QUIZ, RECORDING/DICTATION hoặc các loại chưa được hỗ trợ. QUIZ thuộc assessment G5: trả 409 STATE_CONFLICT (hoặc 403 FORBIDDEN nếu policy đã thống nhất), không sửa score/result.

### Version, atomicity và hành vi lặp

- expectedVersion bắt buộc, số nguyên không âm, so khớp **version enrollment**. Owner/lifecycle/version-check và ghi progress/response/event/schedule/completion phải ở transaction nhất quán.
- Version cũ → 409 VERSION_CONFLICT, kể cả trạng thái gửi lại trùng trạng thái hiện tại. Không bypass optimistic concurrency để làm test replay qua.
- Version hiện hành + trạng thái đích đã có → no-op thành công, không tạo event trùng, không đổi completedAt/updatedAt hoặc tăng version vô ích. Request lặp dùng version mới nhất sau lần trước sẽ qua; request retry còn version cũ nhận 409 và client refetch.
- Submit lại cùng text và activity đã COMPLETED với version hiện hành cũng là no-op, không thêm event. Gửi text mới với version hiện hành được lưu và ghi SUBMITTED, vẫn chỉ đếm activity hoàn thành một lần.
- Mỗi thay đổi thực sự tăng version đúng một lần. Không tự cộng version rồi Hibernate @Version lại cộng lần nữa. Response phải trả version thực tế đã ghi, khớp GET detail/summary/Today ngay sau commit.
- Hai request đồng thời cùng expectedVersion: tối đa một thay đổi thành công, request còn lại 409 (không lộ constraint/optimistic-lock 500). Phải kiểm thử với PostgreSQL thật.
- Nếu lỗi ở event/schedule/completion update thì rollback toàn bộ, không có progress đã đổi nhưng event/version chưa đổi. Không tiếp tục query trong transaction PostgreSQL đã aborted.

### Complete và submit theo CompletionPolicy

- MANUAL_CHECK cho READING/EXTERNAL_RESOURCE/PRACTICE: checklist được đặt COMPLETED. Không tự hoàn thành khi mở link/mở trang.
- MANUAL_CHECK_WITH_RESPONSE: hoàn thành cần text đã lưu không blank; nếu chưa có trả 400 RESPONSE_REQUIRED. Endpoint submit có thể hỗ trợ PRACTICE với policy này để lưu text + hoàn thành nguyên tử; không buộc mở API lưu nháp G3-03 trong task này.
- REFLECTION + RESPONSE_SUBMIT phải dùng submit; không cho checklist COMPLETED lách quy trình, kể cả khi có text cũ. Khi chưa có text trả RESPONSE_REQUIRED; khi có text vẫn hướng caller dùng submit bằng lỗi nghiệp vụ rõ ràng. Làm rõ mô tả OpenAPI để không có hai cách hoàn thành trái policy.
- Submit chỉ nhận PRACTICE/REFLECTION với policy phù hợp, không dùng cho READING/QUIZ. responseText trim để kiểm tra blank, tối đa 5000 ký tự; null/blank/whitespace-only → 400, không mutate. Ghi text và trạng thái COMPLETED trong cùng transaction.
- Một submit hợp lệ ghi một sự kiện SUBMITTED cho thao tác đó; không đồng thời thêm COMPLETED chỉ để đếm hai event. Số activity hoàn thành luôn tính từ progress, không tính số event.
- Event chứa owner/enrollment/activity/type, UTC timestamp và local date/timezone tại thời điểm thao tác. Không log nội dung response hoặc ghi credentials vào event. Dùng Clock inject, không Instant.now()/LocalDate.now() trực tiếp.

### Undo và hoàn thành lesson/enrollment

- Undo checklist được hỗ trợ với các loại thủ công/PRACTICE/REFLECTION phù hợp; không undo QUIZ. Với activity đã completed, chọn IN_PROGRESS nếu còn text, nếu không còn thì NOT_STARTED. Không xóa text đã lưu khi undo.
- Xóa completedAt của activity khi undo; ghi UNDONE khi có chuyển từ completed, không sửa/xóa event cũ. Với IN_PROGRESS/NOT_STARTED không được tạo event UNDONE giả nếu trước đó chưa completed.
- Lesson bắt buộc hoàn thành dựa trên **activity bắt buộc** của lesson, không đòi optional. Cập nhật lesson_schedule.completed_at khi buổi thực sự hoàn thành; undo hoạt động bắt buộc phải bỏ mốc hoàn thành khi buổi không còn đạt. Giữ planned_date/original_date, không tự reschedule.
- Lesson tùy chọn không đưa vào mẫu số lộ trình, không tự thêm lịch bắt buộc; vẫn có thể lưu tiến độ activity của nó.
- Enrollment chuyển COMPLETED khi toàn bộ yêu cầu hiệu lực xong (total > 0). completedAt gán theo Clock; no-op không đổi mốc này. Mẫu số 0 trả progressPercent=null, không tự gán 100% hoặc complete giả.
- Undo **yêu cầu hiệu lực** ở enrollment COMPLETED chỉ được mở lại ACTIVE nếu không có enrollment ACTIVE/PAUSED khác của cùng owner/curriculum; nếu có → 409 STATE_CONFLICT và rollback mọi thay đổi. Clear enrollment.completedAt khi mở lại.
- Undo optional không mở lại enrollment COMPLETED và không đổi phần trăm yêu cầu hiệu lực. Cập nhật nội dung phản tư bằng version hợp lệ có thể giữ COMPLETED nếu yêu cầu hiệu lực vẫn đầy đủ.
- Race mở enrollment mới và undo enrollment cũ phải tôn trọng partial unique index của V005, không trả 500. Thiết kế thứ tự lock nhất quán với CreateEnrollmentService (hiện có khóa hàng user). Không xóa ràng buộc DB để né concurrency.

## Tiến độ và tính nhất quán đọc

- Tái dùng ProgressPolicy: isEffectivelyRequired = lesson.required && activity.required. Pilot mẫu số 19, không hardcode 19 trong production code. Làm tròn một chữ số, BigDecimal; mẫu số 0 là null.
- Summary, mutation response, Today, my-enrollments dùng cùng định nghĩa; reload không mất status/responseText/version. Giữ R4 đã đạt: Today/list dùng projection, không kéo user_response hoặc Markdown vào dashboard.
- Summary và endpoint đọc activities owner-checked; tránh N+1; không lấy payload quiz để tính progress. Không sửa các task G3-01 đã chốt ngoài thay đổi cần thiết để dùng dữ liệu tiến độ thật.

## Frontend tối thiểu để dùng được

- Bổ sung ActivityProgressPanel trong LessonSummaryPage hiện tại, dùng endpoint nhẹ mới. Hiển thị title/type/nhãn bắt buộc, status, checkbox hoặc nút hoàn thành/undo theo policy, textarea + Nộp & Hoàn thành cho phản tư. Các component/hook phải tái dùng được trong workspace G3-03.
- Triển khai useUpdateProgress, useSubmitReflection, query summary/activities bằng generated types/API client hiện có. Không tự định nghĩa các DTO trùng generated types hoặc dùng any để bỏ qua lỗi contract.
- Hiển thị tiến độ do server trả; không tự đếm UI items để ra phần trăm lộ trình. Khi null hiển thị Chưa có nội dung bắt buộc.
- Serialize mutations trong cùng enrollment hoặc khóa các thao tác chung khi pending để không tự gửi nhiều request cùng version; disable submit/double click. Không auto-retry mutation mù, không hứa thành công trước response server.
- Success cập nhật/invalidate đúng activities, enrollment detail, summary, Today, my-enrollments; request sau dùng version mới từ server. Đảm bảo invalidate đúng key enrollment hiện tại, không làm rò cache giữa user A/B.
- 409 VERSION_CONFLICT: giữ nguyên text người học đang nhập, thông báo dữ liệu đã đổi, refetch version/state và yêu cầu xác nhận thao tác lại; không tự overwrite hoặc auto-submit draft bằng version mới.
- Khi network lỗi: giữ text, trạng thái hiển thị trung thực và có thử lại; refetch để biết thao tác trước đã commit chưa. Không tạo text đã lưu giả sau lỗi.
- PAUSED/ABANDONED chỉ đọc; COMPLETED vẫn có undo hợp lệ theo server. Không hiển thị nút Resume G4 hoặc bắt đăng ký lại để undo.
- Sau khi xong buổi, UI gợi ý quay về Today/buổi tiếp theo bằng thông tin thật; không điều hướng tới lessonId null. Responsive 375px/desktop, labels bàn phím rõ ràng.
- Chưa làm Markdown renderer, autosave nháp, ghi chú, focus mode, quiz grading, recording/pronunciation hoặc lịch G4. Không đưa tên giai đoạn/UUID kỹ thuật vào nội dung người học.

## Bộ kiểm thử và DoD

1. HTTP thật: login + CSRF, đọc activities/version → complete → reload → summary/Today thống nhất; optional không đổi mẫu số/không giữ buổi đã xong phần bắt buộc ở IN_PROGRESS.
2. Lặp cùng trạng thái với version hiện hành là no-op: một COMPLETED event, một mốc completedAt, không tăng version thêm. Gửi lại version cũ là 409, không mutate.
3. Hai request đồng thời cùng version trên cùng hoặc khác activities: chỉ một commit, còn lại 409; không count vượt total, không event duplicate, không 500.
4. Submit text blank/null/>5000 bị từ chối; submit hợp lệ persist response + completed + SUBMITTED + version nguyên tử. Inject lỗi **sau progress save** tại event/completion update để chứng minh rollback response/progress/version/schedule/status.
5. Undo required làm giảm progress, bỏ completedAt buổi/enrollment đúng; giữ responseText và lịch sử. Undo optional không mở lại lộ trình hoặc đổi mẫu số.
6. Completed cũ + enrollment mới ACTIVE/PAUSED cùng curriculum: undo required trả 409, DB không đổi. Có concurrency test create enrollment mới vs undo cũ với thứ tự lock/unique index thực tế.
7. Owner/revision isolation: user B không GET/mutate dữ liệu A; activity khác revision trả 404; PAUSED/ABANDONED trả STATE_CONFLICT; existing archived vẫn học; QUIZ không bị checklist sửa.
8. Clock cố định: occurredAt UTC, localDate Ho_Chi_Minh/London tại nửa đêm/DST; no-op không đổi timestamps; lịch planned/original không bị đổi.
9. FE: loading/empty/error/retry; pending chống double-click; complete/undo/reload; submit/validation; null progress; 409 giữ text và không tự gửi lại; refresh caches/version; state read-only.
10. Regression G3-01 + phép đo SQL giữ đạt; không thêm N+1 hoặc cột nặng vào Today/list. Kiểm tra schema/serialization JSONB event trên PostgreSQL, không chỉ Mockito.

Chạy backend `mvnw.cmd -o -B -ntp verify` (bỏ -o khi cần tải dependency), FE `npm test`, `npm run typecheck`, `npm run lint`, `npm run build`. Chỉ dùng PostgreSQL Testcontainers biệt lập. Không seed/modify DB dev để lấy bằng chứng. Kiểm tra màn hình 375px/desktop nếu công cụ cho phép; thiếu kiểm tra phải báo rõ.

## Điểm dừng

Code, tests và report theo phạm vi trên rồi dừng cho Codex nghiệm thu; không làm G3-03/G4/G5. Không nâng dependencies hoặc chỉnh style ngoài formatter cần thiết. Báo file/migration/API đổi, quyết định concurrency/no-op, số tests thực chạy, lỗi và phần chưa kiểm chứng. Hai lượt sửa cho cùng finding là giới hạn; phần còn lại Codex tự xử lý.

## Prompt gửi AI công nhân

```text
Triển khai G3-02 theo docs/tasks/G3-02_HANDOFF.md trong
D:/WorkSpace/tu-hoc-tieng-anh. Đọc file và tài liệu đầu vào trước khi code.
G3-01 đã nghiệm thu tại 3af9d1e. Làm complete/undo, submit phản tư,
summary và bảng nhiệm vụ tối thiểu trên trang tóm tắt bài.
Ưu tiên owner/revision, expectedVersion, transaction, no-op/event và luật
mở lại enrollment completed. Không sửa migration đã áp dụng; migration mới
từ V006 hoặc số kế tiếp thực tế. Không làm workspace G3-03/G4/G5,
không đọc secrets/chỉnh DB dev, không commit/push. Chạy tests PostgreSQL
biệt lập và FE checks, cập nhật backlog bằng kết quả thật rồi dừng review.
Tối đa hai lượt sửa cùng finding, sau đó Codex tự xử lý.
```
