# Báo cáo Triển khai G4-02: Xem trước và Xác nhận Đổi lịch (Reschedule Preview & Commit)

**Mã công việc:** G4-02  
**Trạng thái:** [ ] Hoàn thành triển khai, sẵn sàng chờ Codex nghiệm thu  
**Baseline xuất phát:** `aca84d7`  
**Ngày hoàn thành triển khai:** 26/10/2026  
**Tài liệu liên quan:** [docs/tasks/G4-02_HANDOFF.md](tasks/G4-02_HANDOFF.md), [docs/tasks/backlog.md](tasks/backlog.md), [docs/api/openapi.yaml](api/openapi.yaml)

---

## 1. Tóm tắt Mục tiêu & Phạm vi Đã Hoàn Thành

Task G4-02 hoàn thành toàn bộ luồng đổi lịch cá nhân (Reschedule) cho lộ trình đang học (`ACTIVE`) với cơ chế xem trước an toàn và commit nguyên tử có kiểm soát phiên:

1. **API Xem trước Đổi lịch (`POST /api/v1/enrollments/{enrollmentId}/schedule/preview`):**
   - Thuần túy tính toán (pure calculation), không ghi DB, không đổi version, không sinh domain event.
   - Trả về `SchedulePreviewResponse` gồm `enrollmentVersion`, `proposedEndDate` (tính bằng max của ngày học lớn nhất giữa bài đã hoàn thành và bài chưa hoàn thành) và danh sách `modifiedSchedules` (`RescheduledLessonItemResponse`).
   - Danh sách `modifiedSchedules` chỉ chứa các bài chưa hoàn thành có `newPlannedDate != previousPlannedDate` (các bài giữ nguyên ngày không bị đưa vào danh sách trả về).
   - Kiểm tra chặt chẽ: `resumeDate >= today` theo múi giờ của enrollment; từ chối enrollment ở trạng thái không phải `ACTIVE` bằng 409 `STATE_CONFLICT`.

2. **API Xác nhận Đổi lịch (`POST /api/v1/enrollments/{enrollmentId}/schedule/commit`):**
   - Áp dụng ngày học mới cho các bài học chưa hoàn thành theo đúng thứ tự sư phạm.
   - Cập nhật cấu hình lịch trên `Enrollment` (`studyDays`, `dailyTargetMinutes`).
   - Tăng `enrollmentVersion` nguyên tử bằng câu lệnh CAS (`expectedVersion == currentVersion`); phát hiện xung đột và trả về 409 `VERSION_CONFLICT` kèm rollback sạch.
   - Bảo vệ Idempotency (`Idempotency-Key` scoped theo user + operation, payload hash kết hợp `enrollmentId`, `resumeDate`, `studyDays`, `dailyTargetMinutes`, `expectedVersion`). Lookup idempotency record được ưu tiên kiểm tra trước các kiểm tra trạng thái/thời gian để bảo đảm replay thành công ngay cả sau khi version đã tăng hoặc ngày đã chuyển qua hôm sau.
   - Xử lý No-Op: Nếu tham số cấu hình không đổi và không có bài học nào dời ngày, trả về 200 OK với `EnrollmentResponse` mà không tăng version và không ghi đè DB không cần thiết.

3. **Bảo tồn Tuyệt đối Bài học Đã Hoàn Thành:**
   - Các buổi học đã hoàn thành (`isCompleted == true`) được giữ nguyên 100%: `plannedDate`, `originalDate`, `completedAt`, timestamps không bị dời hoặc tạo lại.
   - `originalDate` của mọi bài học (kể cả bài dời ngày) được bảo toàn vĩnh viễn, không bao giờ bị ghi đè.

4. **Giao diện Màn hình Lịch Desktop (`SchedulePage` & `RescheduleDialog`):**
   - Nút "Đổi lịch" trên banner khi enrollment ở trạng thái `ACTIVE`.
   - Dialog đổi lịch với bẫy tiêu điểm (focus trap), hỗ trợ Tab/Shift-Tab và phím Escape.
   - Form nhập: Ngày bắt đầu áp dụng (`resumeDate`, mặc định hôm nay theo timezone enrollment, `min = today`), checkbox chọn ngày học trong tuần (Thứ Hai đến Chủ Nhật), thời lượng mục tiêu hàng ngày (10 đến 180 phút).
   - Thông báo minh bạch: "Tất cả bài học và hoạt động đã hoàn thành sẽ được giữ nguyên theo ngày học thực tế."
   - Nút "Xem trước lịch mới" gọi API preview. Bảng preview hiển thị danh sách các bài học sẽ thay đổi ngày (`previousPlannedDate` $\rightarrow$ `newPlannedDate`) và ngày kết thúc dự kiến mới.
   - Cơ chế tự hủy preview (Auto-invalidation): Bất kỳ thao tác chỉnh sửa nào trên form sau khi preview lập tức xóa kết quả xem trước và vô hiệu hóa nút xác nhận, ngăn chặn việc commit trạng thái không khớp với bản xem trước.
   - Nút "Xác nhận đổi lịch" gửi mutation kèm `expectedVersion` và `idempotencyKey`. Sau khi thành công, dialog đóng lại, tự động invalidate queries (`schedules`, `enrollment`, `today`) và làm mới giao diện lịch.
   - Khi gặp 409 `VERSION_CONFLICT`, dialog giữ nguyên dữ liệu form và nhắc người dùng nhấn "Xem trước lịch mới" để nạp snapshot mới nhất.

---

## 2. Chi tiết Triển khai Theo Từng Tầng

### 2.1 Hợp đồng API (OpenAPI & Types)
- **`docs/api/openapi.yaml`:**
  - Bổ sung response 409 `ConflictProblem` cho `POST /enrollments/{enrollmentId}/schedule/preview`.
  - Khai báo schema `RescheduledLessonItemResponse` (`lessonId`, `lessonTitle`, `previousPlannedDate`, `newPlannedDate`, `originalDate`).
  - Cập nhật schema `SchedulePreviewResponse` sử dụng `$ref: '#/components/schemas/RescheduledLessonItemResponse'` và `proposedEndDate` (format: date, nullable).
  - Chuẩn hóa schema `ScheduleCommitRequest` (`dailyTargetMinutes` min: 10, max: 180).
- **TypeScript Types:** Đồng bộ bằng `npx openapi-typescript`:
  - `docs/api/generated-types.ts`
  - `frontend/src/generated/api-types.ts`
- **Validation:** `node scripts/validate-openapi-examples.js` đạt.

### 2.2 Backend Repositories & DTOs
- **`LessonScheduleDetailDto`:** DTO nhẹ truy vấn thông tin chi tiết từng schedule: `scheduleId`, `lessonId`, `lessonTitle`, `plannedDate`, `originalDate`, `completedAt`, `lessonOrderIndex`, `weekOrderIndex`, `phaseOrderIndex`.
- **`LessonScheduleRepository`:**
  - `findAllScheduleDetailsByEnrollmentIdOrdered`: Truy vấn toàn bộ schedules của enrollment, sắp xếp theo thứ tự sư phạm `p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, s.id ASC`.
  - `updatePlannedDate`: `@Modifying @Query` cập nhật `plannedDate` và `updatedAt` cho một schedule cụ thể.
- **`EnrollmentRepository`:**
  - `updateScheduleConfigAndVersion`: `@Modifying @Query` cập nhật `studyDays`, `dailyTargetMinutes`, `version = :newVersion`, `updatedAt = :now` với điều kiện CAS `id = :id AND version = :expectedVersion`.

### 2.3 Backend Service & Controller
- **`RescheduleService`:**
  - `previewReschedule(UUID enrollmentId, SchedulePreviewRequest request, CurrentUser currentUser)`:
    * Kiểm tra quyền sở hữu enrollment (404 khi không phải chủ sở hữu).
    * Kiểm tra trạng thái: chỉ cho phép `ACTIVE` (ném 409 `STATE_CONFLICT` nếu `PAUSED`, `ABANDONED`, `COMPLETED`).
    * Kiểm tra ngày: `resumeDate >= todayInEnrollmentTz` (ném 400 `VALIDATION_FAILED` nếu trong quá khứ).
    * Tính toán bằng thuật toán dùng chung: giữ nguyên các bài `completedAt != null`; sắp xếp các bài chưa hoàn thành theo thứ tự sư phạm và tính ngày mới qua `SchedulePlanner.plan(...)`.
    * `proposedEndDate = max(maxCompletedDate, maxNewPlannedDate)`.
  - `commitReschedule(UUID enrollmentId, String idempotencyKeyHeader, ScheduleCommitRequest request, CurrentUser currentUser)`:
    * Khóa hàng user: `userRepository.findByIdForUpdate(currentUserId)`.
    * Kiểm tra Idempotency trước tiên: Nếu đã có bản ghi idempotency thành công với cùng user + operation + payloadHash, trả về ngay response đã lưu mà không kiểm tra version hay thời gian.
    * Kiểm tra trạng thái `ACTIVE` và `resumeDate >= todayInEnrollmentTz`.
    * Kiểm tra version: `enrollment.getVersion().equals(request.expectedVersion())` (ném 409 `VERSION_CONFLICT` nếu lệch).
    * Kiểm tra No-Op: Nếu không có bài nào đổi ngày và cấu hình `studyDays`, `dailyTargetMinutes` giữ nguyên $\rightarrow$ trả về 200 OK với version hiện hành, lưu idempotency thành công.
    * Tách entity khỏi persistence context (`entityManager.detach(enrollment)`) để tránh Hibernate auto-flush sớm.
    * Cập nhật `plannedDate` cho từng bài học bị đổi ngày.
    * Thực thi CAS: `updateScheduleConfigAndVersion(id, expectedVersion, expectedVersion + 1, ...)`. Nếu `rows == 0` $\rightarrow$ ném 409 `VERSION_CONFLICT`.
    * Lưu bản ghi idempotency thành công kèm snapshot `EnrollmentResponse`.
- **`EnrollmentController`:**
  - Expose `POST /api/v1/enrollments/{enrollmentId}/schedule/preview`.
  - Expose `POST /api/v1/enrollments/{enrollmentId}/schedule/commit`.

### 2.4 Frontend Implementation
- **API Client (`frontend/src/features/enrollment/api/enrollment.api.ts`):**
  - Export types: `SchedulePreviewRequest`, `ScheduleCommitRequest`, `SchedulePreviewResponse`, `RescheduledLessonItemResponse`.
  - Export methods: `previewReschedule` và `commitReschedule`.
- **`RescheduleDialog.tsx` (`frontend/src/features/schedule/components/RescheduleDialog.tsx`):**
  - Tách form con `RescheduleForm` khởi tạo state từ props, tuân thủ nghiêm ngặt quy tắc của React Compiler và OxLint (không `setState` trong `useEffect`).
  - Hỗ trợ keyboard accessibility: focus trap, Tab/Shift-Tab, phím Escape.
  - Quản lý trạng thái: `previewData`, `previewParamsHash`, `error`, `isSubmitting`, `isCommitSuccess`.
  - Tự động reset preview khi bất kỳ input nào thay đổi.
  - Xử lý 409 `VERSION_CONFLICT`: giữ form mở, hiển thị banner yêu cầu xem trước lại.
- **`SchedulePage.tsx` (`frontend/src/features/schedule/pages/SchedulePage.tsx`):**
  - Bổ sung nút "Đổi lịch" (`data-testid="reschedule-button"`) trên banner khi enrollment `ACTIVE`.
  - Tích hợp `<RescheduleDialog />` với callbacks invalidate queries.

---

## 3. Bằng chứng Kiểm thử Thực tế

### 3.1 Backend Code Quality & Tests
1. **Spotless Code Formatter:**
   ```powershell
   .\mvnw.cmd spotless:check -ntp
   # Kết quả: BUILD SUCCESS (137 files clean)
   ```
2. **Backend Unit Tests (27 tests passed):**
   ```powershell
   .\mvnw.cmd test '-Dtest=SchedulePlannerTest,RescheduleServiceTest,EnrollmentControllerTest' -ntp
   # Kết quả:
   # - SchedulePlannerTest: 10/10 passed
   # - RescheduleServiceTest: 8/8 passed:
   #   * previewReschedule_shiftsOnlyUncompletedLessons_andComputesMaxProposedEndDate
   #   * previewReschedule_whenAllLessonsCompleted_proposedEndDateIsMaxCompletedDate
   #   * previewReschedule_rejectsPastResumeDate
   #   * previewReschedule_rejectsNonActiveEnrollment
   #   * commitReschedule_replaysIdempotentRequest_evenIfVersionAdvanced
   #   * commitReschedule_rejectsStaleExpectedVersionWithVersionConflict
   #   * commitReschedule_whenNoOp_doesNotBumpVersion
   #   * commitReschedule_appliesNewDatesAndBumpsVersionAtomically
   # - EnrollmentControllerTest: 9/9 passed (3 tests preview/commit mới)
   ```
3. **Backend PostgreSQL Testcontainers Integration Test:**
   ```powershell
   .\mvnw.cmd test '-Dtest=EnrollmentAndTodayIT#testReschedulePreviewAndCommitWorkflow' '-Dapi.version=1.44' -ntp
   # Kết quả: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
   # Kiểm tra luồng thật trên PostgreSQL: preview -> commit -> CAS version bump -> hoàn thành bài 1 -> reschedule lần 2 bảo tồn bài 1.
   ```

### 3.2 Frontend Code Quality & Tests
1. **TypeScript Typecheck:**
   ```powershell
   npm run typecheck
   # Kết quả: tsc -b thành công với 0 errors
   ```
2. **Linter (OxLint):**
   ```powershell
   npm run lint
   # Kết quả: 0 errors (1 warning thông tin đã biết về react-hook-form trong EnrollmentDialog)
   ```
3. **Frontend Component & Unit Tests:**
   ```powershell
   npm test -- src/features/schedule
   # Kết quả: SchedulePage.test.tsx: 13/13 tests passed (3 tests mới cho RescheduleDialog)
   ```
4. **Production Build:**
   ```powershell
   npm run build
   # Kết quả: Vite built production bundle thành công trong 1.21s
   ```

### 3.3 Playwright Desktop E2E Tests (Dev Environment Reuse)
Toàn bộ kịch bản E2E được thực thi trên môi trường dev (PostgreSQL + Spring Boot port 8080 + Vite port 5173) với fixture tài khoản an toàn (`e2e_learner_resched_a`), không chạm dữ liệu người dùng thật:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter schedule-desktop.e2e.ts
# Kết quả: 3/3 tests passed (22.5s)
```
**Các ca kiểm thử đã xác minh:**
1. **Desktop Schedule Foundation (Test 1):** Bố cục 7 ngày, điều hướng tuần, liên kết không gian học, phản ánh hoàn thành/undo, responsive 1440×900 và 1280×800.
2. **Cross-Account Isolation & Unauthorized Protection (Test 2):**
   - User B không xem được lịch của User A (404).
   - User B không được phép gọi preview đổi lịch của User A (`POST /api/v1/enrollments/{enrollmentIdA}/schedule/preview` trả về 404).
   - User B không được phép gọi commit đổi lịch của User A (`POST /api/v1/enrollments/{enrollmentIdA}/schedule/commit` trả về 404).
3. **G4-02 Reschedule Workflow & Integrity (Test 3):**
   - Đăng nhập `e2e_learner_resched_a` $\rightarrow$ hoàn thành bài học 1 trong không gian học.
   - Mở màn hình Lịch `/schedule` $\rightarrow$ xác nhận bài 1 có badge "Đã hoàn thành".
   - Mở modal "Đổi lịch" $\rightarrow$ bấm "Xem trước lịch mới" $\rightarrow$ bảng preview chỉ hiển thị các bài chưa hoàn thành (bài 1 bị loại trừ hoàn toàn).
   - Bấm "Hủy" $\rightarrow$ xác nhận database và giao diện giữ nguyên không bị mutate.
   - Mở lại modal $\rightarrow$ thay đổi ngày học $\rightarrow$ bấm xem trước $\rightarrow$ sửa thời lượng học $\rightarrow$ xác minh bảng preview lập tức bị xóa và nút xác nhận bị disable (anti-stale commit).
   - Bấm xem trước lại $\rightarrow$ bấm "Xác nhận đổi lịch" $\rightarrow$ modal đóng thành công.
   - Xác minh trên giao diện lịch: bài 1 giữ nguyên ngày cũ và trạng thái "Đã hoàn thành"; các bài chưa hoàn thành dời sang các ngày học mới.
   - Tải lại trang (F5) $\rightarrow$ dữ liệu lịch mới và ngày kết thúc dự kiến lưu bền vững.
   - Kiểm tra an toàn concurrency: Gọi API commit với `expectedVersion` cũ $\rightarrow$ nhận đúng mã lỗi 409 `VERSION_CONFLICT`.

---

## 4. Ràng buộc & Phạm vi Tuân thủ

- **Phạm vi giao diện:** Web desktop (1440×900 và 1280×800); mobile hoãn theo quy định chung.
- **Bảo toàn dữ liệu:** Giữ nguyên vẹn dữ liệu cá nhân (`admin`, `dinhgiang1`); mọi fixture E2E cô lập trong `e2e_*`.
- **Files không chạm:** Không sửa hoặc commit `docs/DAY2_REVIEW.md` và `backend/src/main/resources/META-INF/spring/`.
- **Tính năng ngoài phạm vi:** Không triển khai trước Tạm dừng / Tiếp tục lộ trình (G4-03), kéo thả calendar, ngày nghỉ lễ.
- **Quy trình bàn giao:** Không tự ý `git commit` hoặc `git push` (dành riêng cho Codex nghiệm thu).
