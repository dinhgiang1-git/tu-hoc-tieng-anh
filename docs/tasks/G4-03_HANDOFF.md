# G4-03 — Tạm dừng (Pause), Tiếp tục (Resume) và Kết thúc Lộ trình (Abandon)

## 1. Mục tiêu và phạm vi

Cho phép người học chủ động quản lý vòng đời (Lifecycle) của lộ trình học cá nhân trên web desktop:
1. **Tạm dừng (Pause):** Đóng băng tiến độ khi có việc bận; ngừng cảnh báo nợ bài quá hạn trên trang Hôm nay và Màn hình Lịch; khóa các thao tác nộp bài/đánh dấu hoàn thành (chỉ cho phép đọc bài và ghi chú cá nhân).
2. **Tiếp tục học (Resume):** Chuyển từ `PAUSED` về `ACTIVE` và phân bổ lại lịch học mới cho các bài chưa hoàn thành trong **cùng 1 transaction nguyên tử**, đảm bảo ngày tiếp tục không nằm trong quá khứ và không tạo nợ bài ảo.
3. **Chủ động kết thúc (Abandon):** Chuyển enrollment đang dở (`ACTIVE` hoặc `PAUSED`) sang `ABANDONED` để giải phóng ràng buộc duy nhất (Partial Unique Constraint), cho phép người học đăng ký lại từ đầu lộ trình đó nếu muốn.

**Baseline:** `G4-02 accepted`. Tái dùng các module: `SchedulePlanner`, `RescheduleService`, `EnrollmentRepository`, `IdempotencyService`, `SchedulePage`, `TodayPage`.  
**Phạm vi:** Web desktop (1440×900 và 1280×800). Mobile tiếp tục hoãn.  
**Ngoài phạm vi:** Đổi timezone của enrollment, kéo thả calendar, ngày nghỉ lễ quốc gia, tính năng Quiz/Ôn tập (G5).

---

## 2. Quy tắc Nghiệp vụ (Business Rules)

1. **Trạng thái hợp lệ cho từng chuyển đổi (Lifecycle State Machine):**
   - **`pause`:** Chỉ cho phép khi enrollment đang `ACTIVE`. Nếu đang `PAUSED`, `ABANDONED`, hoặc `COMPLETED` $\rightarrow$ ném 409 `STATE_CONFLICT`.
   - **`resume`:** Chỉ cho phép khi enrollment đang `PAUSED`. Nếu đang `ACTIVE`, `ABANDONED`, hoặc `COMPLETED` $\rightarrow$ ném 409 `STATE_CONFLICT`.
   - **`abandon`:** Chỉ cho phép khi enrollment đang `ACTIVE` hoặc `PAUSED`. Nếu đã `ABANDONED` hoặc `COMPLETED` $\rightarrow$ ném 409 `STATE_CONFLICT`.
2. **Nguyên tử trong Resume:**
   - Người học không thể Resume mà không xác nhận lịch học mới.
   - Thao tác chuyển trạng thái sang `ACTIVE`, cập nhật cấu hình lịch (`studyDays`, `dailyTargetMinutes`), cập nhật `plannedDate` cho các bài học chưa hoàn thành, tăng version (`expectedVersion + 1`) và lưu bản ghi Idempotency được thực thi trong **duy nhất 1 transaction**.
   - Bảo toàn 100% bài đã hoàn thành (`completedAt != null`) và `originalDate` vĩnh viễn.
3. **Chặn thao tác học tập khi PAUSED / ABANDONED:**
   - Các API ghi nhận tiến độ (`PUT /activities/{id}/progress`, `POST /activities/{id}/response/submit`) đã có guard từ chối 409 `STATE_CONFLICT` khi enrollment không `ACTIVE`.
   - Ghi chú cá nhân (`Note`) vẫn được phép xem/sửa (độc lập với lifecycle).
4. **Trang Hôm nay (`Today`):**
   - Khi enrollment `PAUSED`: `TodayResponse.state = PAUSED`, `primaryAction = null`, `overdueLessons = []`. Không phát sinh cảnh báo nợ bài. Giao diện hiển thị nút "Tiếp tục học" (Resume).
   - Khi enrollment `ABANDONED`: Không tạo hành động học; nếu người dùng đang focus lộ trình đã abandon, Today chuyển về trạng thái phù hợp hoặc gợi ý chọn lộ trình khác / đăng ký lại.

---

## 3. Hợp đồng API (OpenAPI & Backend)

### 3.1 Tạm dừng: `POST /api/v1/enrollments/{enrollmentId}/pause`
- **Headers:** CSRF token.
- **Body:** `LifecycleTransitionRequest`: `{ expectedVersion: Long }`.
- **Xử lý:**
  * Xác thực owner từ SecurityContext; 404 nếu không khớp.
  * Kiểm tra status `ACTIVE`; 409 `STATE_CONFLICT` nếu không phải.
  * CAS update: `UPDATE Enrollment e SET e.status = 'PAUSED', e.version = :newVersion, e.updatedAt = :now WHERE e.id = :id AND e.version = :expectedVersion`.
  * Nếu 0 dòng bị tác động $\rightarrow$ 409 `VERSION_CONFLICT`.
  * Trả về `EnrollmentResponse` (200 OK) với `status = PAUSED`.

### 3.2 Tiếp tục học: `POST /api/v1/enrollments/{enrollmentId}/resume`
- **Headers:** CSRF token, `Idempotency-Key` (bắt buộc).
- **Body:** `ScheduleCommitRequest`: `{ resumeDate, studyDays, dailyTargetMinutes, expectedVersion }`.
- **Xử lý:**
  * Khóa hàng user: `userRepository.findByIdForUpdate(userId)`.
  * Tra cứu Idempotency replay trước tiên; nếu đã có bản ghi thành công với cùng key và hash $\rightarrow$ replay ngay kết quả cũ (200 OK).
  * Xác thực owner (404); kiểm tra status `PAUSED` (409 `STATE_CONFLICT` nếu không phải).
  * Kiểm tra version: `enrollment.getVersion().equals(request.expectedVersion())` (409 `VERSION_CONFLICT` nếu stale).
  * Kiểm tra `resumeDate >= today` theo múi giờ enrollment.
  * Tính toán phân bổ ngày mới cho các bài học chưa hoàn thành qua `SchedulePlanner.plan(...)`.
  * Cập nhật `plannedDate` cho các bài dời ngày.
  * Tách entity (`entityManager.detach`) và CAS update: `UPDATE Enrollment e SET e.status = 'ACTIVE', e.studyDays = :studyDays, e.dailyTargetMinutes = :dailyTargetMinutes, e.version = :newVersion, e.updatedAt = :now WHERE e.id = :id AND e.version = :expectedVersion AND e.status = 'PAUSED'`.
  * Lưu Idempotency record thành công.
  * Trả về `EnrollmentResponse` (200 OK) với `status = ACTIVE`.

### 3.3 Chủ động kết thúc: `POST /api/v1/enrollments/{enrollmentId}/abandon`
- **Headers:** CSRF token.
- **Body:** `LifecycleTransitionRequest`: `{ expectedVersion: Long }`.
- **Xử lý:**
  * Xác thực owner (404); kiểm tra status thuộc `[ACTIVE, PAUSED]` (409 `STATE_CONFLICT` nếu đã `ABANDONED` hoặc `COMPLETED`).
  * CAS update chuyển `status = 'ABANDONED'`, tăng version.
  * Trả về `EnrollmentResponse` (200 OK) với `status = ABANDONED`.

---

## 4. Giao diện Desktop & Trải nghiệm Người dùng

1. **Màn hình Lịch (`SchedulePage`):**
   - Khi `ACTIVE`:
     * Nút "Đổi lịch" (giữ nguyên từ G4-02).
     * Nút "Tạm dừng" mở dialog xác nhận: giải thích rõ tiến độ sẽ đóng băng, không bị tính quá hạn và có thể tiếp tục bất cứ lúc nào.
     * Dropdown menu hoặc nút phụ "Kết thúc lộ trình" (Abandon) kèm cảnh báo không thể hoàn tác, cho phép đăng ký lại từ đầu.
   - Khi `PAUSED`:
     * Banner hiển thị badge "Tạm dừng" (màu hổ phách / amber), ghi chú "(Lộ trình đang tạm dừng)".
     * Ẩn nút "Đổi lịch"; thay bằng nút chính **"Tiếp tục học"** (`data-testid="schedule-resume-button"`).
     * Bấm "Tiếp tục học" mở Dialog Resume (tái sử dụng form và logic xem trước của `RescheduleDialog`, tiêu đề "Tiếp tục lộ trình học", nút hành động "Xem trước lịch mới" và "Xác nhận tiếp tục").
     * Cho phép bấm "Kết thúc lộ trình" nếu không muốn học tiếp.
2. **Trang Hôm nay (`TodayPage`):**
   - Khối `today.state === "PAUSED"`: Thay thế text placeholder G4 cũ bằng thông điệp thân thiện và nút hành động CTA **"Tiếp tục học"** dẫn đến mở Resume Dialog hoặc chuyển tới `/schedule`.
3. **Đồng bộ TanStack Query:**
   - Invalidate đồng bộ sau khi Pause/Resume/Abandon: `scheduleKeys.all`, `enrollmentKeys.all`, `todayKeys.all`.

---

## 5. Tiêu chí Nghiệm thu (DoD)

1. **Backend Unit & IT Tests:**
   - Unit tests bao phủ đầy đủ: `pauseEnrollment` (thành công, stale version 409, sai trạng thái 409), `resumeEnrollment` (thành công 1 transaction, replay idempotency, stale version, ngày quá khứ 400, sai trạng thái 409), `abandonEnrollment` (thành công từ ACTIVE/PAUSED, stale version, sai trạng thái).
   - Testcontainers PostgreSQL IT kiểm tra luồng thật: Pause $\rightarrow$ xác nhận Today trả `PAUSED` $\rightarrow$ Resume với lịch mới $\rightarrow$ xác nhận trở lại `ACTIVE` và các bài chưa học có ngày mới.
2. **Frontend Quality:**
   - `npm run typecheck` đạt 0 errors.
   - `npm run lint` đạt 0 errors.
   - Vitest component tests cho Pause Dialog, Resume Dialog trên SchedulePage.
   - `npm run build` thành công.
3. **E2E Desktop:**
   - Thêm kịch bản E2E kiểm chứng luồng người học hoàn chỉnh:
     * Đang học `ACTIVE` $\rightarrow$ bấm "Tạm dừng" $\rightarrow$ trạng thái chuyển `PAUSED`, Today không cảnh báo quá hạn.
     * Bấm "Tiếp tục học" $\rightarrow$ xem trước lịch mới $\rightarrow$ xác nhận $\rightarrow$ chuyển lại `ACTIVE`, lịch cập nhật ngày mới.
     * Thử mutation với stale version nhận 409.
4. **Báo cáo:**
   - Tạo `docs/G4-03_IMPLEMENTATION.md` và cập nhật `docs/tasks/backlog.md`.
