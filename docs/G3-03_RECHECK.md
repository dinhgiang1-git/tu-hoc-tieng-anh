# G3-03 — Kiểm tra sau lượt sửa 1: chưa nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). Tiếp nối `G3-03_REVIEW.md`, không mở scope mới.

## Bằng chứng

- 5 repro lần trước đã được đưa vào suite chính và đạt.
- Frontend: **85/85 tests**, lint không error (3 warnings), production build/TypeScript đạt, bundle khoảng 676 KB. Log `frontend/review-g303-round1.log`.
- Reviewer bổ sung 3 repro cho phần R1/R2 còn sót: **3/3 fail**. Log `frontend/review-g303-round1-repro.log`; source `docs/reviews/G303Round1Review.test.tsx`. Copy về `frontend/src/features/learning/__tests__/G303Round1Review.test.tsx` để chạy. Đã chuyển ra ngoài suite chính sau kiểm tra, không sửa code sản phẩm.
- Backend `mvnw.cmd -o -B -ntp verify`: **142 unit + 48 integration tests**, BUILD SUCCESS, không failure/error/skip; Spotless đạt. PostgreSQL Testcontainers biệt lập, log `backend/target/review-g303-round1.log`. Không sửa DB dev/secrets.

## R1 — P1: scope và conflict của autosave vẫn chưa an toàn

**Repro A:** note n1/bài l1 gửi PATCH pending; chuyển scope sang n2/bài l2; PATCH l1 trả về. Expected currentNote=n2, actual **n1**. Effect reset scope không vô hiệu hóa continuation cũ; continuation vẫn sửa currentNoteRef, queue, trạng thái và resolver chung. Lượt save kế tiếp ở l2 có nguy cơ PATCH note l1. Unmount/logout cũng thiếu generation guard. Hủy timer không ngăn response đã gửi.

**Repro B:** PATCH A pending, người học gõ B nên có timer; A nhận 409; sau 701ms hook vẫn gửi PATCH B, call count **2 thay vì 1**. catch và updateContent chưa khóa đường autosave ở trạng thái conflict. Không được tự gửi lại khi chưa có quyết định người học.

Phần còn thiếu trong cùng finding:

- NoteEditor.handleOverwrite vẫn dùng `currentNote.version + 1` khi đọc bản mới lỗi/không thấy note. Loại bỏ hoàn toàn fallback đoán version; lỗi đọc phải giữ draft, báo retry, không gửi PATCH.
- Workspace vẫn mount editor khi notesQuery pending/error và chỉ đọc trang đầu để tìm note lesson. Không được coi chưa tải được/hết trang đầu là chưa có note rồi POST. Phải xác định note đúng scope (activityId=null) chắc chắn, có loading/error/retry phù hợp.
- Guard hiện chưa có hành động lưu rồi rời đúng nghĩa; kiểm thử data router thật, không chỉ mock useBlocker. Một guard chung của workspace cần xét note dirty/pending/error và response draft, tránh đăng ký nhiều blockers cạnh tranh nhau.

Sửa: generation theo user/enrollment/lesson/lifetime, snapshot request và kiểm tra generation trước mọi write state/ref/cache/callback/queue/resolver khi resolve/reject; scope mới không bị old request unlock/overwrite. Conflict cancel timer/queue và latch đến khi người học chọn xử lý. Promise flush phải chờ bản mới nhất được xác nhận trước khi cho rời trang.

## R2 — P1: lưu response làm mất chữ, recovery/cache vẫn chưa xong

**Repro C:** REFLECTION có userResponse=null; nhập `My unsaved answer`; nhấn Lưu nháp, HTTP còn pending. Expected textarea giữ text, actual **rỗng ngay**. Effect ActivityRenderer phụ thuộc draftStatus, điều kiện `!== dirty` khiến chuyển sang saving reset về activity.userResponse cũ. Nếu request thất bại, bản nháp đã mất dù thông báo nói được giữ an toàn.

Giữ local draft độc lập snapshot server. Thay trạng thái saving/error/saved không được tự reset text; kết quả request cũ cũng không được ghi đè text gõ mới. Chỉ adopt server khi chưa có draft hoặc người học xác nhận, theo đúng scope.

Các phần R2 chưa được giải quyết:

- handleVersionUpdate vẫn `void invalidateQueries`, không invalidate workspace, vẫn giữ overrides đè lên bản đọc mới. `isMutating` gỡ ngay sau HTTP, không chờ đọc lại state/version; workspace lỗi nền không khóa activity. Giữ sync pending qua quá trình reconcile, có error/retry, bỏ overlay khi server đã cập nhật.
- Catch 409 chưa phân biệt VERSION_CONFLICT/STATE_CONFLICT, không refetch/refresh action; chỉ hiển thị câu nhắc. User không thể lấy version mới từ trang này sau conflict. Giữ draft, tải lại state/version, mở thao tác khi recovery thành công, không auto-submit.
- beforeunload mới chặn đóng tab; không chặn link Hôm nay/Bài tiếp/router nội bộ cho response draft. Đưa response dirty/pending vào guard chung, test navigation bằng data router thật.
- Đổi scope vẫn thiếu bảo vệ callback activity request cũ; old version/status/flags không được ghi vào workspace mới.

## R3 — P2: đã sửa một phần policy, còn chặn loại chưa hỗ trợ

PRACTICE MANUAL_CHECK đã có checklist; response completed đã có undo; submit giữ whitespace. Tuy nhiên isManualCheck không loại RECORDING/DICTATION nên vẫn có nút complete thủ công bị backend từ chối. Chặn các loại chưa hỗ trợ ở renderer; có test ma trận policy như review đầu. Không thêm tính năng audio/quiz.

## Phạm vi lượt sửa cuối

Lượt sửa **2/2** cho R1–R3. Chỉ hoàn tất các nhánh trên, không thêm tính năng, không đổi giao diện ngoài cần thiết. Sau lượt này Codex tự sửa phần còn lại, không giao thêm lượt cho cùng finding.

```text
Sửa lần cuối G3-03 theo docs/G3-03_RECHECK.md (lượt 2/2 của R1–R3).
Đưa 3 repro docs/reviews/G303Round1Review.test.tsx vào suite, không giảm kỳ vọng.
Ưu tiên giữ text response khi save/error, generation bảo vệ note/activity
request cũ, conflict dừng queue và không đoán version, recovery đồng bộ
workspace/caches thật. Guard chung phải chặn cả response draft và note khi
điều hướng nội bộ; thêm test data router thật và deferred responses.
Chặn RECORDING/DICTATION thao tác thủ công. Giữ các regression đã qua.
Chạy Maven verify với PostgreSQL Testcontainers và FE tests/typecheck/lint/build.
Cập nhật báo cáo/backlog bằng kết quả thực chạy, dừng chờ Codex.
Không commit/push, không sửa secrets/DB dev/migration đã áp dụng, không G3-04.
Sau lượt này Codex tự xử lý phần còn lại, không mở thêm lượt sửa cùng lỗi.
```
