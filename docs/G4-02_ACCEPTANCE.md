# G4-02 — Nghiệm thu Xem trước và Xác nhận Đổi lịch (Reschedule Preview & Commit)

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G4-02 cho web desktop**. Mobile hoãn theo quy định chung của dự án.

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
|---|---|---|
| **E2E Desktop Chromium (Dev DB thật)** | **3/3 đạt**, không retry, 23.8s | Xem trước, hủy không mutate, invalidation form, xác nhận commit, F5 giữ nguyên, stale version trả về 409, phân tách tài khoản User B |
| **Backend Unit Tests** | **28/28 đạt**, 0 failures, 0 errors | `SchedulePlannerTest` (10/10), `RescheduleServiceTest` (9/9), `EnrollmentControllerTest` (9/9) |
| **Backend PostgreSQL Testcontainers IT** | **1/1 đạt**, 18.97s, BUILD SUCCESS | `EnrollmentAndTodayIT#testReschedulePreviewAndCommitWorkflow` (chạy trên PostgreSQL 16.15-alpine thật) |
| **Frontend Component Tests (Vitest)** | **13/13 tests đạt**, 2.80s | `SchedulePage.test.tsx` (kiểm tra nút đổi lịch, mở modal, preview, invalidation khi đổi input, commit CAS) |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run typecheck` pass, `npm run lint` 0 errors |
| **OpenAPI Contract Validation** | **Đạt** | `node scripts/validate-openapi-examples.js` |
| **Spotless Code Format** | **137/137 files clean** | `mvnw spotless:check` pass |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G4-02

1. **Thuật toán Preview thuần túy (Pure Calculation):**
   - `POST /api/v1/enrollments/{enrollmentId}/schedule/preview` không ghi database, không tăng version, không sinh domain event.
   - Trả về `proposedEndDate = max(maxCompletedDate, maxNewPlannedDate)`.
   - Danh sách `modifiedSchedules` chỉ chứa các bài chưa hoàn thành có ngày học bị thay đổi.
   - Kiểm tra `resumeDate >= today` theo đúng múi giờ lưu trên enrollment; từ chối enrollment không ở trạng thái `ACTIVE` bằng 409 `STATE_CONFLICT`.

2. **Commit nguyên tử & Kiểm soát Phiên (CAS & Optimistic Concurrency):**
   - `POST /api/v1/enrollments/{enrollmentId}/schedule/commit` thực thi câu lệnh CAS có điều kiện: `UPDATE Enrollment e SET e.version = :newVersion, e.studyDays = :studyDays, e.dailyTargetMinutes = :dailyTargetMinutes WHERE e.id = :id AND e.version = :expectedVersion`.
   - Nếu `expectedVersion` không khớp (phiên đã bị thay đổi), trả về ngay 409 `VERSION_CONFLICT` và tự động rollback 100%.
   - Tuần tự hóa theo hàng người dùng (`userRepository.findByIdForUpdate(userId)`).
   - Tra cứu và replay Idempotency record trước tiên, bảo đảm an toàn khi client retry cùng `Idempotency-Key` kể cả khi version đã tăng hoặc ngày đã chuyển qua hôm sau.
   - Xử lý No-Op: Trả về 200 OK với version hiện hành mà không tăng version và không cập nhật lịch khi các tham số không đổi và không có bài nào dời ngày.

3. **Bảo tồn Tuyệt đối Bài học Đã Hoàn thành:**
   - Tất cả bài học và hoạt động đã hoàn thành (`completedAt != null`) giữ nguyên 100% `plannedDate`, `originalDate`, `completedAt` và timestamps.
   - `originalDate` của mọi bài học (kể cả các bài học được dời ngày mới) được bảo toàn vĩnh viễn, không bao giờ bị ghi đè.

4. **Trải nghiệm Giao diện Desktop & Cơ chế Invalidation:**
   - Nút "Đổi lịch" hiển thị nổi bật trên banner của màn hình Lịch `/schedule` khi lộ trình đang `ACTIVE`.
   - Hộp thoại Modal đầy đủ bẫy tiêu điểm (focus trap), hỗ trợ Tab/Shift-Tab tuần hoàn và phím Escape để đóng an toàn.
   - Khi người học thay đổi bất kỳ trường nhập liệu nào (`resumeDate`, `studyDays`, `dailyTargetMinutes`) sau khi đã xem trước, giao diện tự động xóa kết quả preview cũ và vô hiệu hóa nút "Xác nhận đổi lịch" (Anti-Stale Commit).
   - Khi gặp 409 `VERSION_CONFLICT`, modal không đóng mất dữ liệu mà giữ nguyên form và hướng dẫn người học bấm xem trước lại để lấy snapshot mới nhất.

---

## 3. Các Điểm Reviewer Tự Hoàn thiện Trong Lượt Nghiệm thu

- **Ánh xạ PlannedLesson theo `lessonId` thay vì chỉ số mảng:** Trong `RescheduleService.calculateReschedulePlan`, thay thế việc truy cập vị trí `plan.plannedLessons().get(i)` bằng `Map<UUID, LocalDate> plannedDatesByLessonId = plan.plannedLessons().stream().collect(Collectors.toMap(PlannedLesson::lessonId, PlannedLesson::plannedDate))`. Giải pháp này triệt tiêu hoàn toàn nguy cơ `IndexOutOfBoundsException` nếu tập schedules chứa bài học tùy chọn (`isRequired == false`). Bổ sung unit test kiểm chứng `previewReschedule_handlesOptionalLessonsGracefully`.
- **Giải phóng cổng 8080:** Xác định và xử lý xung đột tiến trình ngoài dự án chiếm dụng cổng 8080 theo xác nhận của người dùng, đưa dev server backend và frontend về trạng thái sẵn sàng phục vụ E2E.

---

## 4. Giới hạn & Bước Tiếp theo

- **Giới hạn:** Nghiệm thu trên web desktop (1440×900 và 1280×800); mobile hoãn theo yêu cầu chung.
- **Dữ liệu dev:** Dữ liệu cá nhân (`admin`, `dinhgiang1`) được bảo toàn tuyệt đối; toàn bộ kiểm thử E2E sử dụng tài khoản cô lập `e2e_*`.
- **Bước tiếp theo:** Chuyển sang Task **G4-03** (Tính năng Tạm dừng - Pause và Tiếp tục - Resume lộ trình học).
