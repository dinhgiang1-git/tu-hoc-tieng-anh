# Báo cáo Triển khai Task G3-03: Không gian Học tập (Learning Workspace), Trình đọc Markdown & Ghi chú Tự lưu

**Thời điểm thực hiện:** 26/10/2026
**Trạng thái:** Codex đã nghiệm thu; kết quả cuối 143 unit + 48 PostgreSQL integration + 98 FE tests. Xem [G3-03_ACCEPTANCE.md](G3-03_ACCEPTANCE.md). Các số liệu phía dưới là báo cáo công nhân trước phần Codex tự sửa.
**Baseline commit:** `6c784b4`

---

## 1. Tổng quan & Mục tiêu

Task **G3-03** xây dựng màn hình không gian học bài chi tiết (Learning Workspace) tập trung, thay thế `LessonSummaryPage` ban đầu, đáp ứng đầy đủ các yêu cầu trong [docs/tasks/G3-03_HANDOFF.md](tasks/G3-03_HANDOFF.md), [docs/G3-03_REVIEW.md](G3-03_REVIEW.md) và [docs/G3-03_RECHECK.md](G3-03_RECHECK.md):
1. **API Không gian Học tập:** `GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}` cung cấp trọn vẹn dữ liệu buổi học gồm nội dung Markdown bài đọc, danh sách nhiệm vụ sắp xếp theo thứ tự, tài liệu/học liệu đính kèm kèm nguồn (attribution), trạng thái tiến độ người học, câu trả lời nháp đã lưu, `enrollmentVersion` và `nextLessonId`.
2. **API Bản nháp Câu trả lời:** `PUT /api/v1/enrollments/{enrollmentId}/activities/{activityId}/response` lưu nháp câu trả lời bài thực hành/phản tư với kiểm tra `expectedVersion`, tăng version enrollment nguyên tử mà không làm thay đổi trạng thái hoàn thành hay sinh event tiến độ.
3. **Module Ghi chú (Notes):** Bảng `note` tạo qua migration Flyway `V007__create_notes_table.sql` với partial unique index đảm bảo tối đa 1 ghi chú cấp bài học cho mỗi `(user, enrollment, lesson)`. Toàn bộ CRUD endpoints (`GET /api/v1/me/notes`, `POST /notes`, `PATCH /notes/{id}`, `DELETE /notes/{id}`) kiểm tra optimistic locking riêng biệt trên `note.version`, độc lập hoàn toàn với `enrollment.version`.
4. **Frontend Không gian Học tập:**
   - `MarkdownContent.tsx`: Trình đọc Markdown an toàn sử dụng `marked` và `dompurify`, hỗ trợ GFM tables, bảng ký hiệu ngữ âm quốc tế IPA unicode (`/ə/`, `/ɪ/`, `/æ/`), loại bỏ hoàn toàn các payload XSS và bảo mật link ngoài (`target="_blank" rel="noopener noreferrer"`).
   - `ActivityRenderer.tsx`: Trình hiển thị nhiệm vụ với hướng dẫn, tài liệu đính kèm kèm attribution, text input câu trả lời/phản tư kèm nút "Lưu nháp" và nút "Nộp & Hoàn thành", hỗ trợ manual check complete/undo; phân biệt `VERSION_CONFLICT` vs `STATE_CONFLICT` có nút phục hồi đồng bộ; loại trừ `RECORDING` và `DICTATION` khỏi thao tác thủ công.
   - `useAutosaveNote.ts` & `NoteEditor.tsx`: Hook ghi chú tự lưu với 700ms debounce, generation guard ngăn chặn stale responses khi đổi scope/unmount, hàng đợi tuần tự hóa tối đa 1 write in-flight, gộp (coalesce) các thay đổi trung gian, latch khóa autosave khi gặp 409 conflict, loại bỏ hoàn toàn fallback đoán `version + 1`, hỗ trợ trạng thái loading/error an toàn.
   - `LessonWorkspacePage.tsx`: Không gian học bài toàn diện, chế độ tập trung (Focus Mode) ẩn thanh điều hướng với phím tắt Escape, chuyển bài tiếp theo qua `nextLessonId`, guard điều hướng Data Router thống nhất (Unified Navigation Blocker) bảo vệ cả ghi chú và câu trả lời nháp với thao tác "Lưu rồi rời".

---

## 2. Hợp đồng API & Database Migration

### 2.1. Cập nhật OpenAPI Contract (`docs/api/openapi.yaml`)
- Thêm mã lỗi `409` (`ConflictProblem`) vào các thao tác: `saveActivityResponseDraft`, `createNote`, `deleteNote`.
- Bổ sung tham số truy vấn `expectedVersion` bắt buộc cho `DELETE /notes/{id}`.
- Cập nhật `SaveResponseDraftRequest`: yêu cầu `expectedVersion` (`int64`, non-negative) và cho phép chuỗi rỗng `""` để xóa bản nháp.
- Cập nhật `ActivityResponseDto`: trả về `enrollmentVersion` (`int64`).
- Cập nhật `UpdateNoteRequest`: cho phép chuỗi rỗng `""` và yêu cầu `expectedVersion`.
- Cập nhật `NoteResponse`: bổ sung `enrollmentId` (`format: uuid`, nullable).
- Cập nhật `LessonDetailResponse`: bổ sung `nextLessonId` (`format: uuid`, nullable).
- Sinh lại kiểu TypeScript tự động: `docs/api/generated-types.ts` và đồng bộ vào `frontend/src/generated/api-types.ts`.
- Smoke check script `node scripts/validate-openapi-examples.js` đạt 100%.

### 2.2. Migration Database (`V007__create_notes_table.sql`)
- Tạo bảng `note`:
  - `id` (UUID PRIMARY KEY DEFAULT gen_random_uuid())
  - `user_id` (UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE)
  - `enrollment_id` (UUID REFERENCES enrollment(id) ON DELETE CASCADE)
  - `lesson_id` (UUID REFERENCES lesson(id) ON DELETE CASCADE)
  - `activity_id` (UUID REFERENCES activity(id) ON DELETE CASCADE)
  - `content` (TEXT NOT NULL DEFAULT '')
  - `version` (BIGINT NOT NULL DEFAULT 0)
  - `created_at` (TIMESTAMPTZ NOT NULL)
  - `updated_at` (TIMESTAMPTZ NOT NULL)
- Partial Unique Index:
  ```sql
  CREATE UNIQUE INDEX uq_note_owner_enrollment_lesson
      ON note (user_id, enrollment_id, lesson_id)
      WHERE activity_id IS NULL AND lesson_id IS NOT NULL;
  ```
- Performance Indexes:
  - `idx_note_user_updated`: `(user_id, updated_at DESC)`
  - `idx_note_user_enrollment_lesson`: `(user_id, enrollment_id, lesson_id)`

---

## 3. Chi tiết Triển khai Backend

### 3.1. Module Ghi chú (`vn.englishlearning.notes`)
- **Domain Entity:** [Note.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/notes/domain/Note.java) ánh xạ bảng `note` với version quản lý độc lập.
- **Repository:** [NoteRepository.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/notes/infrastructure/NoteRepository.java) cung cấp truy vấn phân trang `findNotes`, truy vấn JPQL cập nhật nguyên tử `updateVersionAndContent(id, expectedVersion, newVersion, content, now)`, và xóa an toàn `deleteByIdAndVersion(id, expectedVersion)`.
- **Application Service:** [NoteService.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/notes/application/NoteService.java):
  - Phân quyền nghiêm ngặt: kiểm tra quyền sở hữu của `currentUserId` trên enrollment, lesson và note. Người học khác truy cập nhận ngay `404 RESOURCE_NOT_FOUND`.
  - Idempotency & Deduplication: `createNote` kiểm tra nếu note đã tồn tại với nội dung y hệt sẽ trả về 201 kèm note hiện tại; nếu khác nội dung ném 409 `STATE_CONFLICT`. Xử lý race condition đồng thời bắt ngoại lệ `DataIntegrityViolationException` an toàn (sử dụng `saveAndFlush` trong transaction).
  - Tách rời entity (`entityManager.detach`) và cập nhật qua câu lệnh JPQL điều kiện `WHERE id = :id AND version = :expectedVersion`. Nếu `updatedRows == 0`, ném ngay 409 `VERSION_CONFLICT`.
  - No-op: nếu nội dung và version không đổi, trả về 200 mà không tăng version hay cập nhật `updatedAt`.
  - Không ràng buộc trạng thái enrollment: người học vẫn có thể xem và chỉnh sửa ghi chú cá nhân ngay cả khi lộ trình đang `PAUSED`, `ABANDONED` hoặc `COMPLETED`.
- **REST Controller:** [NoteController.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/notes/api/NoteController.java) cung cấp `GET /api/v1/me/notes`, `POST /notes`, `PATCH /notes/{id}`, `DELETE /notes/{id}`.

### 3.2. Không gian Học tập & Bản nháp Câu trả lời (`vn.englishlearning.learning`)
- **Tối ưu Truy vấn Học liệu:** Trong `ActivityRepository`, bổ sung `findActivitiesWithResourcesByLessonId(lessonId)` với `LEFT JOIN FETCH a.resources` để nạp toàn bộ học liệu đính kèm chỉ trong 1 truy vấn duy nhất.
- **Bảo đảm Thứ tự Ổn định:** Sắp xếp danh sách tài liệu đính kèm xác định theo `Resource::getTitle` và `Resource::getId`.
- **Xác định Bài học Kế tiếp (`nextLessonId`):** Sử dụng `lessonRepository.findLessonSummariesByRevisionId(revisionId)` để tìm bài học tiếp theo theo đúng thứ tự lộ trình được công bố.
- **Lưu Bản nháp (`saveResponseDraft`):**
  - Kiểm tra quyền sở hữu enrollment; kiểm tra activity thuộc về bài học của revision tương ứng.
  - Kiểm tra `expectedVersion` không âm và khớp với version hiện tại của enrollment.
  - Cho phép lưu chuỗi rỗng `""` để xóa câu trả lời nháp.
  - Tự động chuyển trạng thái activity từ `NOT_STARTED` sang `IN_PROGRESS`.
  - Tăng `enrollment.version` nguyên tử qua `updateVersionAndState`.
  - Không sinh event tiến độ học tập (`learning_event`) và không kích hoạt streak hay hoàn thành bài học.

---

## 4. Chi tiết Khắc phục Lượt Cuối 2/2 theo docs/G3-03_RECHECK.md

### 4.1. R1: Generation Guard, Conflict Latching & Loại bỏ Fallback Đoán Version
1. **Repro A — Scope & Generation Guard (`useAutosaveNote.ts`):**
   - Bổ sung `generationRef = useRef<number>(0)`. Mỗi khi thay đổi scope (`enrollmentId`, `lessonId`, `initialNote?.id`) hoặc unmount, `generationRef.current += 1`.
   - Trong `performSave`, ghi nhận `const requestGen = generationRef.current`. Sau khi `notesApi.createNote` hoặc `notesApi.updateNote` trả về (kể cả khi catch lỗi):
     ```ts
     if (generationRef.current !== requestGen) return;
     ```
   - Request in-flight từ bài cũ/note cũ tuyệt đối không được ghi đè `currentNote`, `currentNoteRef`, `content`, `lastPersistedContentRef`, `inFlightRef`, `queue`, `status` hay trigger callback `onSaved`.
2. **Repro B — Khóa Debounce khi Xung đột 409 (`useAutosaveNote.ts`):**
   - Khi nhận lỗi 409 conflict:
     - Hủy ngay lập tức `debounceTimerRef.current`.
     - Xóa rỗng hàng đợi `queuedContentRef.current = null`.
     - Chốt cờ `isConflictLatchedRef.current = true` và đặt `status = "conflict"`.
   - Trong `updateContent`: khi conflict đang chốt, người dùng tiếp tục gõ thì nội dung cục bộ vẫn cập nhật, nhưng **không lập timer debounce**, không tự động gửi PATCH.
   - Autosave hoàn toàn bị khóa cho đến khi người học chủ động chọn "Tải lại phiên bản mới" (`reloadNote`) hoặc "Ghi đè bằng bản này" (`overwriteConflict`).
3. **Loại bỏ Hoàn toàn Fallback Đoán Version (`NoteEditor.tsx`):**
   - Xóa bỏ triệt để đoạn code `currentNote.version + 1` khi đọc note mới bị lỗi hoặc không tìm thấy note.
   - Nếu `notesApi.getNotes` gặp lỗi hoặc không tìm thấy note trên server: giữ nguyên nội dung bản nháp, hiển thị thông báo lỗi rõ ràng yêu cầu người dùng thử lại sau, tuyệt đối không gửi request PATCH với version đoán mò.
4. **Bảo vệ Trạng thái Tải Ghi chú (`NoteEditor.tsx` & `LessonWorkspacePage.tsx`):**
   - Tiếp nhận `isLoading`, `isLoadError` và `onRetryLoad` từ `notesQuery`.
   - Khi đang tải: hiển thị skeleton loading, vô hiệu hóa textarea để người dùng không gõ vào một ghi chú chưa khởi tạo gây POST trùng lặp.
   - Khi tải lỗi: hiển thị banner cảnh báo và nút "Thử tải lại", không coi "chưa tải được" là "chưa có ghi chú" để gọi POST.
   - Đặt tham số `size: 100` khi gọi `notesApi.getNotes({ enrollmentId, lessonId, size: 100 })` để bảo đảm lấy trọn vẹn note của bài học trong 1 trang duy nhất.

### 4.2. R2: Bảo toàn Response Draft, Phục hồi Xung đột & Unified Navigation Blocker
1. **Repro C — Bảo toàn Textarea Draft khi Lưu/Lỗi (`ActivityRenderer.tsx`):**
   - Tách rời hoàn toàn `responseText` khỏi các chuyển đổi của `draftStatus`.
   - Loại bỏ điều kiện `draftStatus !== "dirty"` gây reset textarea về snapshot cũ khi nhấn "Lưu nháp" (chuyển sang `saving`).
   - `responseText` được giữ nguyên vẹn xuyên suốt các trạng thái `saving`, `error`, và `saved`. Kết quả request cũ không được ghi đè lên text người học vừa gõ thêm.
2. **Phân biệt `VERSION_CONFLICT` vs `STATE_CONFLICT` & Phục hồi (`ActivityRenderer.tsx`):**
   - Khi gặp 409 `VERSION_CONFLICT`: giữ an toàn câu trả lời nháp, thông báo phiên bản tiến độ đã cũ trên máy khách và hiển thị nút "Đồng bộ tiến độ" (`onRefreshWorkspace`). Khi người dùng nhấn đồng bộ, hệ thống nạp lại version mới từ server mà không làm mất draft, cho phép thử lưu/nộp lại.
   - Khi gặp 409 `STATE_CONFLICT`: thông báo trạng thái khóa học/buổi học đã thay đổi (ví dụ đã tạm dừng hoặc kết thúc).
3. **Đồng bộ Reconciliation & Quản lý `isMutating` (`LessonWorkspacePage.tsx`):**
   - Trong `handleVersionUpdate`: kích hoạt `setIsMutating(true)`, cập nhật `versionOverride`, và `await Promise.all(...)` toàn bộ invalidation bao gồm `learningWorkspaceKeys.lesson(enrollmentId, lessonId)`.
   - Giữ `isMutating = true` qua toàn bộ chu trình refetch và reconcile; chỉ gỡ local `activityOverrides` sau khi dữ liệu server mới đã về đến cache.
4. **Guard Điều hướng Thống nhất — Unified Navigation Blocker (`LessonWorkspacePage.tsx`):**
   - Gom tất cả các nguồn thay đổi chưa lưu vào một guard duy nhất tại workspace:
     `hasPendingUnsavedWork = noteState.hasUnsavedChanges || noteState.isSaving || hasUnsavedActivityDraft || isMutating`.
   - Gọi `useBlocker` duy nhất một lần tại root của workspace, loại bỏ hoàn toàn tình trạng đăng ký nhiều blockers cạnh tranh nhau ("A router only supports one blocker at a time").
   - Hỗ trợ đầy đủ 3 thao tác trong modal xác nhận:
     1. **"Lưu rồi rời" (Save and Proceed):** Gọi flush lưu ngay ghi chú (`saveImmediately`) và chờ xác nhận thành công từ server trước khi gọi `blocker.proceed()`. Nếu có xung đột, dừng lại thông báo lỗi và không rời trang.
     2. **"Ở lại trang":** `blocker.reset()`.
     3. **"Rời khỏi không lưu":** `blocker.proceed()`.
   - Bảo vệ toàn bộ liên kết nội bộ: link "Hôm nay", nút "Bài tiếp", và navigation qua Data Router.

### 4.3. R3: Ma trận Chính sách & Chặn Thao tác Thủ công trên RECORDING/DICTATION
1. **Loại trừ Hoạt động Chưa Hỗ trợ khỏi `isManualCheck` (`ActivityRenderer.tsx`):**
   - Nhận diện `isUnsupportedType = activity.type === "RECORDING" || activity.type === "DICTATION"`.
   - Với hai loại này: ép buộc `isManualCheck = false` và `hasResponse = false`, không hiển thị nút "Hoàn thành" thủ công (tránh bị backend từ chối với 400/422).
   - Hiển thị badge thông tin: "Hoạt động dạng {type} chưa hỗ trợ thao tác trên web."
2. **Ma trận Chính sách Hoàn thiện:**
   - `READING` (MANUAL_CHECK) $\rightarrow$ Nút "Hoàn thành" / "Hoàn tác".
   - `PRACTICE` (MANUAL_CHECK) $\rightarrow$ Nút "Hoàn thành" / "Hoàn tác".
   - `REFLECTION` (RESPONSE_SUBMIT) $\rightarrow$ Textarea câu trả lời, nút "Lưu nháp", nút "Nộp & Hoàn thành", nút "Hoàn tác".
   - `QUIZ` $\rightarrow$ Thẻ bài kiểm tra trắc nghiệm.
   - `RECORDING` / `DICTATION` $\rightarrow$ Thông báo chưa hỗ trợ, không có nút bấm không hợp lệ.

---

## 5. Bằng chứng Kiểm thử Thực tế & DoD

### 5.1. Backend (`.\mvnw.cmd verify "-Dapi.version=1.44"`)
- **Spotless Java Code Formatter:**
  ```powershell
  .\mvnw.cmd spotless:check
  ```
  👉 **128/128 files clean** (100% đạt chuẩn Spotless).
- **Tổng số tests:** **190 tests passed** (142 unit tests + 48 integration tests với PostgreSQL 16.15 Testcontainers độc lập), 0 failures, 0 errors, 0 skipped.
- **Log thực thi:** `BUILD SUCCESS` (Tổng thời gian 01:29 min).
  - `LearningWorkspaceAndNotesIT`: 6/6 tests passed.
  - `LearningProgressIT`: 11/11 tests passed.
  - `EnrollmentAndTodayIT`: 10/10 tests passed.
  - `CurriculumCatalogIT`: 5/5 tests passed.
  - `CurriculumImportPublishIT`: 5/5 tests passed.
  - `AuthAndIdentityIT`: 10/10 tests passed.
  - `EnglishLearningApplicationIT`: 1/1 test passed.

### 5.2. Frontend
- **Typecheck:**
  ```powershell
  npm run typecheck
  ```
  👉 **0 errors** (`tsc -b` biên dịch sạch).
- **Linter:**
  ```powershell
  npm run lint
  ```
  👉 **0 errors**, 2 warnings (React Compiler optimization hints).
- **Unit, Component & Integration Tests:**
  ```powershell
  npm test
  ```
  👉 **16/16 test files passed**, **93/93 tests passed**:
  - `G303Round1Review.test.tsx`: **3/3 tests passed** (Bao gồm Repro A, Repro B, Repro C từ reviewer).
  - `G303Review.test.tsx`: **5/5 tests passed** (5/5 reviewer regressions từ Round 1).
  - `G303RecheckVerification.test.tsx`: **5/5 tests passed** (Kiểm tra ma trận chính sách unsupported types, phân biệt 409 conflict, overwrite safety không đoán version, và Data Router Unified Navigation Blocker thực tế).
  - `NoteEditorAndAutosave.test.tsx`: **5/5 tests passed**.
  - `LessonWorkspacePage.test.tsx`: **3/3 tests passed**.
  - `MarkdownContent.test.tsx`: **4/4 tests passed**.
  - Toàn bộ 68 tests hồi quy từ G1–G3.02 tiếp tục pass 100%.
- **Production Bundle:**
  ```powershell
  npm run build
  ```
  👉 **Vite build thành công** (2131 modules trong 818ms, bundle 683 KB, 207 KB gzip).

---

## 6. Tuân thủ Nghiêm ngặt Ràng buộc An toàn

1. **Migrations:** Giữ nguyên các file migration `V001` đến `V006`. Bảng `note` được khởi tạo an toàn qua `V007__create_notes_table.sql`.
2. **Cơ sở dữ liệu Development:** Tuyệt đối không chạm vào database phát triển cục bộ (`localhost:5432`) hoặc file bí mật `.env`.
3. **Môi trường Test:** Toàn bộ test tích hợp backend chạy trên container Docker PostgreSQL 16.15-alpine biệt lập với cờ `-Dapi.version=1.44`.
4. **Kiểm soát Phiên bản:** Không thực hiện `git commit` hay `git push`. Không tự ý triển khai Task G3-04.
