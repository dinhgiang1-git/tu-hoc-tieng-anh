# Báo cáo Triển khai G4-03: Tạm dừng (Pause), Tiếp tục (Resume) và Kết thúc Lộ trình (Abandon)

**Mã công việc:** G4-03  
**Trạng thái:** [ ] Hoàn thành triển khai, sẵn sàng chờ Codex nghiệm thu  
**Baseline xuất phát:** G4-02 accepted (`50f236d`)  
**Ngày hoàn thành triển khai:** 28/10/2026  
**Tài liệu liên quan:** [docs/tasks/G4-03_HANDOFF.md](tasks/G4-03_HANDOFF.md), [docs/tasks/backlog.md](tasks/backlog.md), [docs/api/openapi.yaml](api/openapi.yaml)

---

## 1. Tóm tắt Mục tiêu & Phạm vi Đã Hoàn Thành

Task G4-03 hoàn thành toàn bộ nghiệp vụ quản lý vòng đời (Lifecycle) của lộ trình học cá nhân trên web desktop, bao gồm ba chuyển đổi cốt lõi có kiểm soát phiên bản (CAS versioning) và an toàn giao dịch:

1. **API Tạm dừng Lộ trình (`POST /api/v1/enrollments/{enrollmentId}/pause`):**
   - Chỉ cho phép khi enrollment ở trạng thái `ACTIVE`. Nếu đang `PAUSED`, `ABANDONED` hoặc `COMPLETED`, từ chối ngay bằng HTTP 409 `STATE_CONFLICT`.
   - Cập nhật nguyên tử `status = 'PAUSED'`, tăng `version = expectedVersion + 1`. Nếu version không khớp trả về 409 `VERSION_CONFLICT`.
   - Vô hiệu hóa tính toán bài quá hạn trên trang Hôm nay (`TodayResponse.state = PAUSED`, `overdueLessons = []`).
   - Khóa các API nộp bài/đánh dấu tiến độ học tập (chỉ cho phép đọc bài và ghi chú cá nhân).

2. **API Tiếp tục Học (`POST /api/v1/enrollments/{enrollmentId}/resume`):**
   - Chỉ cho phép khi enrollment ở trạng thái `PAUSED`. Nếu đang `ACTIVE`, `ABANDONED` hoặc `COMPLETED`, từ chối bằng 409 `STATE_CONFLICT`.
   - Bắt buộc kiểm tra Idempotency thông qua header `Idempotency-Key` (payload hash kết hợp `enrollmentId`, `resumeDate`, `studyDays`, `dailyTargetMinutes`, `expectedVersion`). Lookup idempotency record thành công được replay ngay lập tức trước các kiểm tra trạng thái/thời gian.
   - Kiểm tra ngày: `resumeDate >= todayInEnrollmentTz` (400 `VALIDATION_FAILED` nếu trong quá khứ).
   - **Giao dịch nguyên tử duy nhất (Atomic Transaction):**
     * Tính toán phân bổ lại lịch học mới cho các bài học chưa hoàn thành qua `SchedulePlanner.plan(...)`.
     * Giữ nguyên 100% các bài học đã hoàn thành (`completedAt != null`) và bảo toàn `originalDate` vĩnh viễn.
     * Cập nhật `plannedDate` cho các bài dời ngày trong bảng `lesson_schedule`.
     * Cập nhật `status = 'ACTIVE'`, `studyDays`, `dailyTargetMinutes`, tăng version bằng câu lệnh CAS `WHERE id = :id AND version = :expectedVersion AND status = 'PAUSED'`.
     * Lưu bản ghi idempotency thành công kèm snapshot `EnrollmentResponse`.

3. **API Xem trước Lịch khi Resume (`POST /api/v1/enrollments/{enrollmentId}/schedule/preview`):**
   - Cập nhật service `RescheduleService.previewReschedule` để cho phép cả hai trạng thái `ACTIVE` (đổi lịch thường) và `PAUSED` (xem trước lịch phân bổ khi resume).
   - Từ chối `ABANDONED` và `COMPLETED` bằng 409 `STATE_CONFLICT`.

4. **API Kết thúc Lộ trình (`POST /api/v1/enrollments/{enrollmentId}/abandon`):**
   - Cho phép người học chủ động từ bỏ enrollment khi đang `ACTIVE` hoặc `PAUSED`.
   - Từ chối enrollment đã `ABANDONED` hoặc `COMPLETED` bằng 409 `STATE_CONFLICT`.
   - Cập nhật nguyên tử `status = 'ABANDONED'` bằng CAS, giải phóng partial unique index `(user_id, curriculum_id) WHERE status IN ('ACTIVE', 'PAUSED')` để người học có thể ghi danh lại từ đầu curriculum đó nếu muốn.

5. **Giao diện Màn hình Lịch Desktop (`SchedulePage`, `RescheduleDialog`, `PauseConfirmationDialog`, `AbandonConfirmationDialog`):**
   - Khi `ACTIVE`: Hiển thị nút "Tạm dừng" (`schedule-pause-button`) và nút "Kết thúc lộ trình" (`schedule-abandon-button`).
   - Modal Tạm dừng: Cảnh báo rõ việc đóng băng tiến độ, không tính nợ bài, yêu cầu xác nhận.
   - Khi `PAUSED`:
     * Banner màu hổ phách/amber (`schedule-paused-banner`) với ghi chú trạng thái rõ ràng.
     * Ẩn nút "Đổi lịch"; hiển thị nổi bật nút chính **"Tiếp tục học"** (`schedule-resume-button`).
     * Mở `RescheduleDialog` ở chế độ `mode="resume"`: tiêu đề "Tiếp tục lộ trình học", nút preview "Xem trước lịch mới", nút commit "Xác nhận tiếp tục học".
     * Cho phép bấm "Kết thúc lộ trình" nếu không muốn tiếp tục học.
   - Modal Kết thúc: Cảnh báo không thể hoàn tác, cho phép đăng ký lại từ đầu, nút xác nhận "Xác nhận kết thúc".

6. **Tích hợp Trang Hôm nay (`TodayPage`):**
   - Khối `today.state === "PAUSED"` (`today-paused-state`) thay thế text placeholder cũ bằng thông điệp thân thiện: thông báo lộ trình đang tạm dừng, không bị tính quá hạn và cung cấp nút CTA chính **"Xem lịch và tiếp tục học"** (`today-resume-cta-button`) điều hướng trực tiếp sang `/schedule`.

---

## 2. Chi tiết Triển khai Theo Từng Tầng

### 2.1 Hợp đồng API (OpenAPI & Backend Endpoints)
- **`EnrollmentRequests.java`:**
  - Bổ sung record `LifecycleTransitionRequest(@NotNull Long expectedVersion)`.
- **`EnrollmentRepository.java`:**
  - `updateStatusAndVersion`: Cập nhật `status = :newStatus`, `version = :newVersion`, `updatedAt = :now` với điều kiện `id = :id AND version = :expectedVersion`.
  - `resumeEnrollment`: Cập nhật `status = 'ACTIVE'`, `studyDays`, `dailyTargetMinutes`, `version = :newVersion`, `updatedAt = :now` với điều kiện `id = :id AND version = :expectedVersion AND status = 'PAUSED'`.
  - `abandonEnrollment`: Cập nhật `status = 'ABANDONED'`, `version = :newVersion`, `updatedAt = :now` với điều kiện `id = :id AND version = :expectedVersion AND status IN ('ACTIVE', 'PAUSED')`.
- **`RescheduleService.java`:**
  - `previewReschedule`: Cho phép cả `ACTIVE` và `PAUSED`, từ chối `ABANDONED`/`COMPLETED` bằng `STATE_CONFLICT`.
  - `commitReschedule`: Bảo vệ chặt chẽ trạng thái `ACTIVE`.
  - `pauseEnrollment(UUID enrollmentId, LifecycleTransitionRequest request)`: Kiểm tra owner, trạng thái `ACTIVE`, thực thi CAS chuyển `PAUSED`, tăng version.
  - `resumeEnrollment(String rawIdempotencyKey, UUID enrollmentId, ScheduleCommitRequest request)`: Kiểm tra owner, trạng thái `PAUSED`, kiểm tra version, tính toán phân bổ ngày mới qua `SchedulePlanner`, cập nhật schedules dời ngày, thực thi CAS `resumeEnrollment`, lưu idempotency.
  - `abandonEnrollment(UUID enrollmentId, LifecycleTransitionRequest request)`: Kiểm tra owner, trạng thái `ACTIVE` hoặc `PAUSED`, thực thi CAS chuyển `ABANDONED`, tăng version.
- **`EnrollmentController.java`:**
  - Expose `POST /api/v1/enrollments/{enrollmentId}/pause`.
  - Expose `POST /api/v1/enrollments/{enrollmentId}/resume`.
  - Expose `POST /api/v1/enrollments/{enrollmentId}/abandon`.

### 2.2 Frontend API & Components
- **`frontend/src/features/enrollment/api/enrollment.api.ts`:**
  - Export kiểu `LifecycleTransitionRequest`.
  - Bổ sung phương thức API `enrollmentApi.pause`, `enrollmentApi.resume`, `enrollmentApi.abandon`.
- **`frontend/src/features/schedule/components/PauseConfirmationDialog.tsx`:**
  - Modal xác nhận tạm dừng với focus trap, phím Escape, thông điệp rõ ràng, nút "Xác nhận tạm dừng" (`schedule-confirm-pause-button`).
- **`frontend/src/features/schedule/components/AbandonConfirmationDialog.tsx`:**
  - Modal xác nhận kết thúc lộ trình với cảnh báo không thể hoàn tác, focus trap, nút "Xác nhận kết thúc" (`schedule-confirm-abandon-button`).
- **`frontend/src/features/schedule/components/RescheduleDialog.tsx`:**
  - Hỗ trợ thuộc tính `mode?: "reschedule" | "resume"`.
  - Khi `mode === "resume"`: tiêu đề chuyển thành "Tiếp tục lộ trình học", nút commit chuyển thành "Xác nhận tiếp tục học", gửi mutation tới `enrollmentApi.resume`.
- **`frontend/src/features/schedule/pages/SchedulePage.tsx`:**
  - Tích hợp các nút hành động theo trạng thái lộ trình (`ACTIVE`, `PAUSED`, `ABANDONED`).
  - Tích hợp banner thông báo trạng thái tạm dừng màu hổ phách.
  - Invalidate đồng bộ các query TanStack: `scheduleKeys.all`, `enrollmentKeys.all`, `todayKeys.all`.
- **`frontend/src/features/learning/pages/TodayPage.tsx`:**
  - Tích hợp khối trạng thái `PAUSED` với giải thích thân thiện và nút CTA `today-resume-cta-button` chuyển hướng sang `/schedule`.

---

## 3. An toàn Dữ liệu & Xử lý Đồng thời

1. **CAS Versioning trên Mọi Chuyển đổi Vòng đời:**
   - Mọi thao tác `pause`, `resume`, `abandon` đều yêu cầu `expectedVersion`.
   - Nếu hai tab cùng thao tác hoặc người dùng vừa đổi lịch trên tab khác: tab chậm hơn sẽ nhận 0 dòng bị tác động từ câu lệnh CAS và trả về HTTP 409 `VERSION_CONFLICT`.
2. **Idempotency Bảo vệ Resume:**
   - Chặn tuyệt đối nguy cơ mạng chập chờn gửi lặp request khiến lịch bị dịch chuyển nhiều lần liên tiếp.
   - Replay kết quả trước khi kiểm tra thời gian/trạng thái.
3. **Bảo tồn Tuyệt đối Bài học Đã Hoàn thành:**
   - Khi Resume, tất cả bài đã hoàn thành (`isCompleted == true`) được giữ nguyên ngày học gốc, `completedAt` và timestamp.
   - `originalDate` của mọi bài học không bao giờ bị ghi đè.

---

## 4. Kết quả Kiểm thử & Nghiệm thu

### 4.1 Backend Unit Tests
- Lệnh chạy: `mvnw.cmd test -Dtest=SchedulePlannerTest,RescheduleServiceTest,EnrollmentControllerTest`
- Kết quả: **40/40 tests PASS (0 failures, 0 errors)**:
  * `SchedulePlannerTest`: 10/10 tests pass (phân bổ ngày, thứ trong tuần, dồn ngày, skip completed).
  * `RescheduleServiceTest`: 18/18 tests pass (preview `ACTIVE` & `PAUSED`, state conflict `ABANDONED`, commit reschedule, pause, resume atomic transaction, idempotency replay, abandon).
  * `EnrollmentControllerTest`: 12/12 tests pass (mock MVC endpoints, CSRF, input validation).

### 4.2 Frontend Unit Tests
- Lệnh chạy: `npm test -- src/features/schedule`
- Kết quả: **16/16 tests PASS (1 test file)**:
  * Hiển thị banner, tuần học, bài học hoàn thành, tính toán tiến độ.
  * Mở và xử lý modal Tạm dừng, Tiếp tục học và Kết thúc lộ trình.

### 4.3 Frontend Typecheck, Lint & Build
- `npm run build` (`tsc -b && vite build`): **0 errors, build production thành công**.
- `npm run lint` (`oxlint`): **0 errors (1 warning tương thích React Hook Form đã biết)**.

### 4.4 End-to-End Playwright Tests (Desktop Chromium)
- Lệnh chạy: `powershell.exe -ExecutionPolicy Bypass -File scripts/run-e2e.ps1 -TestFilter schedule-desktop.e2e.ts`
- Môi trường: Tái sử dụng dev services (Backend Spring Boot :8080, Frontend Vite :5173, PostgreSQL Docker :5432) với fixture an toàn `e2e_*`.
- Kết quả: **4/4 tests PASS (100%)**:
  1. `Schedule Desktop: Range, 7-Day Grouping, Workspace Link, Complete/Undo Reflect, and Screenshots` (PASS - 8.8s)
  2. `Schedule Cross-Account Isolation: User B cannot access User A schedule` (PASS - 7.1s)
  3. `G4-02: Complete a lesson, preview reschedule, cancel without mutating DB, confirm reschedule, verify completed intact & uncompleted shifted, reload persists, and conflict safety` (PASS - 11.5s)
  4. `G4-03: Lifecycle Pause, Resume with atomic schedule recalculation, and Abandon on Desktop` (PASS - 8.7s)

### 4.5 Screenshots Nghiệm thu Giao diện Desktop
Đã sinh đầy đủ ảnh chụp màn hình trong `frontend/e2e-screenshots/`:
- `schedule-lifecycle-active.png`: Trạng thái ban đầu đang học với đầy đủ nút Đổi lịch, Tạm dừng, Kết thúc.
- `schedule-lifecycle-paused.png`: Trạng thái tạm dừng với banner màu hổ phách, ẩn Đổi lịch, hiển thị nút "Tiếp tục học".
- `today-lifecycle-paused.png`: Màn hình Hôm nay hiển thị thông báo tạm dừng và nút CTA "Xem lịch và tiếp tục học".
- `schedule-lifecycle-resumed.png`: Trạng thái sau khi tính toán preview và commit resume thành công, trở về `ACTIVE`.
- `schedule-lifecycle-abandoned.png`: Trạng thái sau khi xác nhận kết thúc lộ trình (`ABANDONED`).
- `schedule-desktop-1440.png` và `schedule-desktop-1280.png`: Kiểm tra responsive desktop 1440px và 1280px không tràn viền ngang.

---

## 5. Tình trạng Tệp & Tuân thủ Quy tắc

- Không sửa hoặc commit hai tệp cấm: `docs/DAY2_REVIEW.md`, `backend/src/main/resources/META-INF/spring/`.
- Không tự ý chạy `git commit` hay `git push` (bảo lưu cho Codex).
- Dữ liệu test hoàn toàn cô lập trong tiền tố `e2e_*`, bảo toàn 100% tài khoản cá nhân.
- Cập nhật trạng thái trong `docs/tasks/backlog.md`: G4-03 chuyển sang `[ ] Chờ Codex nghiệm thu`.
