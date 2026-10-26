# G3-03 — Review lần đầu: chưa nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). Baseline: `6c784b4`; phạm vi theo `docs/tasks/G3-03_HANDOFF.md`.

## Bằng chứng độc lập

- `mvnw.cmd -o -B -ntp verify`: **142 unit + 45 PostgreSQL integration tests**, BUILD SUCCESS, không failure/error/skip; Spotless đạt. Log local `backend/target/review-g303.log`. Không dùng/chỉnh DB dev.
- FE suite hiện có **80/80 tests**; lint không error, 2 warnings; build gồm TypeScript đạt. Bundle khoảng 672 KB. Log local `frontend/review-g303.log`.
- Reviewer bổ sung 5 FE reproductions, **5/5 fail** với code hiện tại. Log `frontend/review-g303-repro.log`. Source giữ tại `docs/reviews/G303Review.test.tsx`; copy về `frontend/src/features/learning/__tests__/G303Review.test.tsx` để chạy `npm test -- src/features/learning/__tests__/G303Review.test.tsx`.
- Repro chỉ mock API/router blocker, dùng hook/component/page thật. Chưa kiểm chứng browser navigation blocker thật hoặc viewport desktop/mobile; không coi test mock useBlocker là bằng chứng điều hướng an toàn.
- Đã chuyển repro ra ngoài suite chính sau chạy; không sửa production code trong lượt review.

## R1 — P1: ghi chú mất nội dung, lùi version và báo lưu sai

Files: `notes/hooks/useAutosaveNote.ts`, `notes/components/NoteEditor.tsx`, `learning/pages/LessonWorkspacePage.tsx`.

Ba repro fail:

1. Note có initial content=original/version=0; PATCH updated trả version=1. `performSave` đặt hasUnsavedChanges=false, effect đồng bộ initialNote chạy lại với snapshot cũ, kéo textarea về **original** và currentNote về version cũ. Workspace không cập nhật notes cache sau save nên đây là luồng dùng thực tế.
2. A đang gửi, gõ B nhưng debounce B chưa chạy; A trả về, queuedContentRef vẫn null nên hook đặt saved/clean dù **B chưa gửi**. Test expected hasUnsavedChanges=true nhận false. Không chỉ là nhãn: effect có thể đè B và navigation guard được gỡ sớm.
3. Gõ note chưa lưu rồi bật Focus Mode: page đổi cấu trúc root, remount editors; textarea trở thành rỗng. Ẩn note cũng unmount NoteEditor và xóa timer/draft; refetch workspace lỗi cũng thay cả page bằng ErrorState.

Các nhánh cùng vòng đời cần sửa trong R1:

- NoteEditor không được mở ở trạng thái note query chưa biết/lỗi rồi coi là note mới. `items[0]` không đảm bảo note cấp lesson (list có cả activity notes); chọn đúng owner/enrollment/lesson và activityId=null, kể cả phân trang. Không ghi đè note activity bằng lesson editor.
- Hook chưa reset/đổi generation theo scope/user và không bảo vệ callback in-flight của bài cũ. Đổi lesson sau chọn bỏ thay đổi có thể giữ currentNote/draft bài trước; response cũ không được cập nhật state/cache bài mới.
- `overwriteConflict(currentNote.version + 1)` đang **đoán** version server. Phải đọc bản mới thật và cho xác nhận, không tự tăng expectedVersion. Khi conflict phải dừng queue/timer cho đến khi người học quyết định; gõ tiếp không tự gửi lại vô hạn.
- Delete đang xóa server rồi `updateContent("")` nhưng giữ id/version note đã xóa, 700ms sau PATCH note không còn. Bỏ nút delete khỏi workspace (handoff không yêu cầu) hoặc xử lý reset/cancel queue/caches/error đúng; không bắt lỗi rồi im lặng.
- Route blocker cần cùng state dirty/pending/error tin cậy, có lưu rồi rời/ở lại/bỏ. `saveImmediately` khi in-flight hiện trả về trước khi hàng đợi lưu xong: không dùng promise đó để cho rời trang sớm. Đừng remount editor để làm sạch dirty.

DoD R1: snapshot server đã xác nhận và draft mới nhất độc lập; saved chỉ khi chúng khớp và không queued/in-flight. A pending + B, fixed note initial snapshot, focus/thu panel, query error/retry, đổi lesson, logout và conflict đều giữ đúng nội dung/scope. Tests deferred/fake timers bắt buộc, cộng ít nhất một test dùng data router thật cho block/lưu rồi rời. Không thêm lưu offline để né vấn đề.

## R2 — P1: workspace bỏ cơ chế đồng bộ enrollment đã nghiệm thu

Files: `learning/components/ActivityRenderer.tsx`, `learning/pages/LessonWorkspacePage.tsx`.

Repro: complete activity A để request pending, nút complete B vẫn enabled. Mỗi ActivityRenderer dùng isSubmitting riêng; draftStatus không khóa submit cùng card. Draft/progress/submit có thể cùng dùng version cũ và tự gây conflict.

Ngoài ra handleVersionUpdate chỉ invalidate summary; Today/detail/my-enrollments/workspace không được đồng bộ. versionOverride và activityOverrides tồn tại qua refetch/đổi route, che bản server mới. Các 409 đều bị coi giống nhau và chỉ yêu cầu reload trang, không có recovery giữ draft. Response draft/submit có thể gán saved dù người học đã gõ text mới trong lúc request chạy; activity draft không tham gia guard rời trang.

Sửa bằng cơ chế dùng chung cấp workspace/enrollment cho draft/progress/submit, tái dùng và mở rộng logic G3-02, không duy trì bản copy với quy tắc yếu hơn. Gửi kế tiếp phải dùng version mới được xác nhận. Cập nhật/invalidate đầy đủ keys; không overlay vĩnh viễn lên data đọc lại. Chuyển scope reset state đúng chỗ; response cũ không được đè scope mới. Khi conflict/network, giữ text, refetch an toàn, chỉ mở thao tác sau biết state/version mới; phân biệt VERSION_CONFLICT/STATE_CONFLICT và không auto-resubmit. Không unmount editor vì background query lỗi.

DoD R2: deferred mutation khóa toàn bộ activity actions; draft và submit không chạy chồng; success đồng bộ caches/version, refetch pending/error vẫn khóa; conflict giữ chữ và có retry không reload toàn trang; đổi bài/enrollment không mang version/status/draft cũ. Có regression dùng page/real hooks thay vì chỉ test handler props. Guard rời trang phải xét cả response draft, không riêng note.

## R3 — P2: renderer chọn thao tác theo type quá rộng

Repro PRACTICE + MANUAL_CHECK không có nút Hoàn thành, chỉ có Lưu nháp/Nộp. Backend từ chối cả hai endpoint response vì policy không hỗ trợ. Đây là policy hợp lệ được G3-02 hỗ trợ; fixture workspace của công nhân cũng dùng đúng tổ hợp này nhưng chưa thử complete.

Completed REFLECTION/PRACTICE có response không có nút undo, trong khi draft endpoint yêu cầu undo trước khi lưu nháp mới. RECORDING/DICTATION cũng rơi vào nút complete thủ công dù backend từ chối. Submit đang trim text trước khi gửi thay vì chỉ trim để validate blank, làm nội dung nộp khác nội dung người học nhập.

DoD R3: renderer theo cả type và completionPolicy: PRACTICE MANUAL_CHECK dùng checklist; policy có response dùng draft/submit; completed có undo theo G3-02, giữ text; QUIZ/RECORDING/DICTATION chỉ đọc khi chưa hỗ trợ. Dùng status thật server trả, không tự biến draft rỗng/no-op thành IN_PROGRESS. Validate blank bằng trim nhưng gửi nguyên text. Không đưa G5/giai đoạn kỹ thuật vào nội dung UI. Test ma trận policy, undo response và whitespace preservation.

## Kiểm thử backend còn thiếu bằng chứng

45 IT hiện đạt nhưng 3 IT G3-03 mới chủ yếu là workflow tuần tự; tên `notes_crudAndConcurrency` không chứng minh race bằng hai request đồng thời. Hoàn thành các checks đã yêu cầu trong handoff: draft-vs-submit cùng expectedVersion, rollback sau text save, fixed Clock/no-op, note create trùng scope đồng thời và owner/revision isolation. `NoteService.createNote` bắt DataIntegrityViolationException rồi query trong cùng transaction, trong khi save có thể flush ở commit ngoài try; kiểm chứng PostgreSQL thật và sửa xử lý transaction nếu cần. Không tuyên bố race đã đạt chỉ từ Mockito hoặc hai request tuần tự.

## Prompt sửa và điểm dừng

Đây là **lượt sửa 1/2**, chưa chuyển G3-04. Chỉ sửa R1–R3 và bổ sung các bằng chứng backend đã có trong handoff; không mở thêm chức năng/style mới.

```text
Sửa G3-03 theo docs/G3-03_REVIEW.md, lượt 1/2.
Đọc và đưa 5 reproductions docs/reviews/G303Review.test.tsx vào suite chính,
giữ nguyên mục đích/kỳ vọng. Ưu tiên không mất note/draft và không báo saved
giả; giữ editor khi focus/thu panel/query lỗi; generation scope/user an toàn.
Tái dùng sync G3-02 để draft/progress/submit cùng enrollment không chạy chồng,
đồng bộ đủ cache/version, xử lý conflict giữ chữ; sửa renderer theo policy.
Thêm PostgreSQL races/rollback/fixed Clock còn thiếu theo handoff.
Chạy Maven verify + FE tests/typecheck/lint/build, cập nhật báo cáo bằng chứng
thực tế và backlog chờ Codex nghiệm thu rồi dừng. Không sửa secrets/DB dev,
migration cũ; không commit/push, không G3-04. Tối đa hai lượt sửa cùng finding;
phần còn lại Codex tự xử lý.
```
