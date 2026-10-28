# G4-03 — Nghiệm thu Tạm dừng (Pause), Tiếp tục (Resume) và Kết thúc Lộ trình (Abandon)

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G4-03 cho web desktop**. Mobile hoãn theo quy định chung của dự án.

---

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
| :--- | :--- | :--- |
| **E2E Desktop Chromium (Dev DB thật)** | **4/4 đạt**, không retry, 37.1s | 1. Schedule Desktop Foundation & Workflow (8.8s)<br/>2. Cross-Account Isolation (7.1s)<br/>3. G4-02 Reschedule preview & commit (11.5s)<br/>4. G4-03 Lifecycle Pause, Resume 1 transaction, Abandon (8.7s) |
| **Backend Unit Tests** | **40/40 đạt**, 0 failures, 0 errors | `SchedulePlannerTest` (10/10), `RescheduleServiceTest` (18/18), `EnrollmentControllerTest` (12/12) |
| **Frontend Component Tests (Vitest)** | **16/16 tests đạt**, 2.63s | `SchedulePage.test.tsx` (kiểm tra banner tạm dừng, modal pause/resume/abandon, invalidation, CAS versioning) |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run build` (`tsc -b && vite build`) đạt, `npm run lint` 0 errors |
| **Spotless Code Format** | **137/137 files clean** | `mvnw spotless:apply` pass |
| **Desktop Screenshots** | **7 ảnh chụp hoàn chỉnh** | `frontend/e2e-screenshots/`: `schedule-lifecycle-active.png`, `schedule-lifecycle-paused.png`, `today-lifecycle-paused.png`, `schedule-lifecycle-resumed.png`, `schedule-lifecycle-abandoned.png`, `schedule-desktop-1440.png`, `schedule-desktop-1280.png` |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G4-03

1. **Tạm dừng Lộ trình (Pause):**
   - `POST /api/v1/enrollments/{enrollmentId}/pause` chỉ chấp nhận enrollment ở trạng thái `ACTIVE`. Nếu đang `PAUSED`, `ABANDONED` hoặc `COMPLETED`, từ chối ngay bằng HTTP 409 `STATE_CONFLICT`.
   - Cập nhật CAS `status = 'PAUSED'`, tăng `version = expectedVersion + 1`. Nếu version không khớp trả về 409 `VERSION_CONFLICT`.
   - Vô hiệu hóa nợ bài quá hạn: khi `PAUSED`, `TodayResponse.state = PAUSED`, `overdueLessons = []`, không phát sinh nợ bài ảo.
   - Các API ghi nhận tiến độ học tập từ chối 409 `STATE_CONFLICT` khi enrollment đang `PAUSED` (vẫn cho phép đọc bài và ghi chú cá nhân).

2. **Tiếp tục Lộ trình Nguyên tử (Atomic Resume with Schedule Recalculation):**
   - `POST /api/v1/enrollments/{enrollmentId}/resume` chỉ chấp nhận enrollment ở trạng thái `PAUSED`. Nếu đang `ACTIVE`, `ABANDONED` hoặc `COMPLETED`, từ chối bằng 409 `STATE_CONFLICT`.
   - Bắt buộc kiểm tra `Idempotency-Key`: replay record thành công trước các kiểm tra thời gian/trạng thái.
   - Kiểm tra `resumeDate >= todayInEnrollmentTz` (400 `VALIDATION_FAILED` nếu trong quá khứ).
   - **Thực thi trong duy nhất 1 transaction nguyên tử:**
     * Tính toán phân bổ lại ngày học mới cho các bài chưa hoàn thành qua `SchedulePlanner.plan(...)`.
     * Bảo toàn 100% các bài học đã hoàn thành (`completedAt != null`) và giữ nguyên `originalDate` vĩnh viễn.
     * Cập nhật `plannedDate` các bài dời ngày trong `lesson_schedule`.
     * Cập nhật `status = 'ACTIVE'`, `studyDays`, `dailyTargetMinutes` và tăng version bằng CAS: `WHERE id = :id AND version = :expectedVersion AND status = 'PAUSED'`.
     * Lưu bản ghi idempotency thành công kèm snapshot `EnrollmentResponse`.

3. **Xem trước Lịch khi Resume:**
   - Cập nhật `RescheduleService.previewReschedule` để cho phép cả hai trạng thái `ACTIVE` (đổi lịch) và `PAUSED` (xem trước khi resume).
   - Từ chối `ABANDONED` và `COMPLETED` bằng 409 `STATE_CONFLICT`.

4. **Kết thúc Lộ trình Chủ động (Abandon):**
   - `POST /api/v1/enrollments/{enrollmentId}/abandon` cho phép người học kết thúc khi đang `ACTIVE` hoặc `PAUSED`.
   - Từ chối enrollment đã `ABANDONED` hoặc `COMPLETED` bằng 409 `STATE_CONFLICT`.
   - Cập nhật CAS `status = 'ABANDONED'`, giải phóng partial unique index `(user_id, curriculum_id) WHERE status IN ('ACTIVE', 'PAUSED')` để người học có thể đăng ký lại từ đầu lộ trình đó.

5. **Trải nghiệm Giao diện Desktop (UI/UX):**
   - **Màn hình Lịch `/schedule`:**
     * Khi `ACTIVE`: Nút "Tạm dừng" mở modal xác nhận đóng băng tiến độ; nút "Kết thúc lộ trình" mở modal cảnh báo không thể hoàn tác.
     * Khi `PAUSED`: Banner màu hổ phách cảnh báo trạng thái tạm dừng, ẩn nút "Đổi lịch", hiển thị nút nổi bật "Tiếp tục học" (`schedule-resume-button`).
     * Dialog Tiếp tục học (`RescheduleDialog mode="resume"`): tiêu đề "Tiếp tục lộ trình học", nút preview "Xem trước lịch mới", nút commit "Xác nhận tiếp tục học".
     * Khi `ABANDONED`: Badge "Đã kết thúc", ẩn toàn bộ các nút hành động.
   - **Màn hình Hôm nay `/`:**
     * Khi `PAUSED`: Hiển thị khối `today-paused-state` với thông điệp thân thiện và nút CTA "Xem lịch và tiếp tục học" (`today-resume-cta-button`) điều hướng sang `/schedule`.
   - Responsive: Kiểm tra smoke viewport 1280×800 và 1440×900 không tràn viền ngang (`scrollWidth <= window.innerWidth`).

---

## 3. Các Điểm Hoàn thiện Trong Lượt Nghiệm thu

- **Hỗ trợ `PAUSED` trong `previewReschedule`:** Cho phép người học đang tạm dừng có thể xem trước phân bổ ngày mới trên UI trước khi xác nhận tiếp tục học, đồng thời bảo vệ chặt chẽ `commitReschedule` chỉ cho phép `ACTIVE` và `resumeEnrollment` chỉ cho phép `PAUSED`.
- **Chuẩn hóa Playwright assertion:** Sử dụng `toContainText` thay vì cú pháp jest-dom `toHaveTextContent` để đảm bảo suite Playwright chạy ổn định 100% không phát sinh lỗi trình thực thi.
- **Bảo toàn môi trường & dữ liệu:** Tuyệt đối không can thiệp hay sửa đổi hai tệp tồn tại từ trước ngoài task (`docs/DAY2_REVIEW.md`, `backend/src/main/resources/META-INF/spring/`); toàn bộ kiểm thử thao tác an toàn trên tài khoản `e2e_*`.

---

## 4. Kết luận & Mốc Tiếp theo

- **Nghiệm thu G4-03:** ĐẠT 100% các tiêu chí chức năng, kỹ thuật và kiểm thử.
- **Hoàn thành Giai đoạn G4:** Toàn bộ mốc G4 (Lịch cá nhân Linh hoạt & Đổi lịch: G4-01, G4-02, G4-03) đã hoàn thành xuất sắc và sẵn sàng tích hợp.
- **Bước tiếp theo:** Sẵn sàng bước sang **G5** (Quiz & Ôn tập Tối thiểu — G5-01: Model câu hỏi trắc nghiệm, server chấm điểm tự động).
