# G6-01 — Nghiệm thu Thống kê Tiến độ Trực quan (Biểu đồ thanh theo tuần, Lịch sử học tập theo ngày thực tế)

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G6-01 cho web desktop**. Mobile hoãn theo quy định chung của dự án. Hoàn thành toàn diện tính năng thống kê và trực quan hóa tiến độ học tập cá nhân.

---

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
| :--- | :--- | :--- |
| **Playwright Desktop E2E (Dev DB thật)** | **1/1 đạt**, 4.6s (`progress-desktop.e2e.ts`) | `frontend/e2e/progress-desktop.e2e.ts`: Đăng nhập học viên thực tế, hoàn thành quiz trắc nghiệm Week 1, điều hướng sang trang `/progress` qua sidebar "Tiến độ", kiểm tra 5 thẻ KPI tổng quan (tiến độ %, chuỗi ngày học streak kèm huy hiệu lửa, thời lượng nội dung ước tính, số bài học đã hoàn thành, tỉ lệ hoàn thành quiz), kiểm tra biểu đồ thanh theo tuần (Tuần 1 hiển thị 100% màu xanh lục và badge Hoàn thành, các tuần sau 0%), kiểm tra dòng thời gian lịch sử học theo ngày thực tế định dạng tiếng Việt, nhấn nút "Làm mới dữ liệu" và kiểm tra hiển thị đồng bộ. |
| **Regression Playwright E2E** | **Đạt 100%**: `review-desktop.e2e.ts` (1/1, 5.7s), `quiz-desktop.e2e.ts` (1/1, 5.8s) | Không phát sinh bất kỳ regression nào trên các luồng học cốt lõi, quiz và ôn tập đã nghiệm thu trước đó. |
| **Backend Unit Tests** | **220/220 đạt**, 0 failures, 0 errors | `mvnw.cmd test "-Dtest=!*IT"`: Toàn bộ suite backend unit tests đạt 100%, bao gồm `LearningAnalyticsServiceTest` (5/5 tests), `LearningProgressControllerTest` (11/11 tests). |
| **Frontend Component Tests (Vitest)** | **10/10 đạt**, 1.48s trên 4 test files | `ProgressMetricCards.test.tsx` (2/2), `WeeklyProgressBarChart.test.tsx` (2/2), `DailyActivityTimeline.test.tsx` (2/2), `ProgressPage.test.tsx` (4/4). Full test suite frontend đạt **134/134 tests** trên 24 files. |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run build` (`tsc -b && vite build`) thành công trong 1.09s, `npm run lint` 0 errors. |
| **Spotless Code Format** | **162/162 files clean** | `mvnw.cmd spotless:check` pass sạch trên toàn bộ mã nguồn Java backend. |
| **Desktop Screenshots** | **3 ảnh chụp sắc nét** | `frontend/e2e-screenshots/`: `progress-desktop-overview.png`, `progress-desktop-weekly-chart.png`, `progress-desktop-daily-timeline.png`. |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G6-01

1. **API Endpoint Thống kê Tiến độ (`GET /api/v1/enrollments/{enrollmentId}/analytics`):**
   - Đăng ký đầy đủ trong `docs/api/openapi.yaml`, validate schema hợp lệ với `node scripts/validate-openapi-examples.js`.
   - Sinh kiểu TypeScript tự động vào `frontend/src/generated/api-types.ts`.
   - Kiểm soát quyền sở hữu nghiêm ngặt: Trả về `404 Not Found` (không làm lộ tài nguyên) nếu `enrollment.getUserId()` không khớp với `currentUser.id()`.

2. **Chính sách Tính toán Nhiệm vụ Bắt buộc Hiệu lực (Effective Required Policy):**
   - Mẫu số tổng số nhiệm vụ (`totalActivities`) và số nhiệm vụ đã hoàn thành (`completedActivities`) chỉ tính các hoạt động bắt buộc nằm trong các bài học bắt buộc (`lesson.isRequired = true` và `activity.isRequired = true`).
   - Các hoạt động tùy chọn (`isRequired = false`) tuyệt đối không làm sai lệch phần trăm tiến độ hoàn thành của học viên.

3. **Chính sách Thời lượng Học tập Ước tính (Estimated Duration Policy):**
   - Thời lượng được tính dựa trên tổng thời lượng ước lượng của bài học (`estimatedTotalMinutes`) và các bài học đã hoàn thành (`estimatedCompletedMinutes`).
   - Giao diện dán nhãn rõ ràng "Thời lượng nội dung ước tính", tuyệt đối không giả lập hay mạo danh thời gian ngồi học thực tế (seat time).

4. **Thuật toán Chuỗi Ngày Học Thực tế (Real-date Streak Calculation):**
   - Lấy danh sách các ngày học từ `learning_event.local_date` (hoặc chuyển đổi `occurred_at` sang múi giờ ghi danh `enrollment.timezone`, mặc định `Asia/Ho_Chi_Minh`).
   - Duy trì streak nếu ngày hôm qua có hoạt động học tập kể cả khi hôm nay học viên chưa học; chỉ reset streak về 0 khi có khoảng trống $\ge 2$ ngày không học.
   - Tính toán và lưu giữ đồng thời `currentStreak` và `longestStreak`.

5. **Biểu đồ Thanh Tiến độ Theo Tuần (Weekly Progress Bar Chart):**
   - Xây dựng thuần CSS / Flexbox / Tailwind và SVG icons, không phụ thuộc vào các thư viện biểu đồ nặng nề (Chart.js, Recharts), tối ưu kích thước bundle.
   - Thể hiện trực quan thanh tiến độ từng tuần với màu sắc phân cấp rõ ràng (xanh lục emerald khi đạt 100%, xanh dương indigo khi đang học dở, slate khi chưa bắt đầu).
   - Thể hiện rõ số bài học đã hoàn thành / tổng số bài học của tuần, số nhiệm vụ bắt buộc đã hoàn thành / tổng số nhiệm vụ bắt buộc của tuần.
   - Huy hiệu trạng thái tuần: "Hoàn thành", "Đang học", hoặc "Chưa bắt đầu".

6. **Dòng Thời gian Lịch sử Học tập Theo Ngày Thực tế (Daily Activity Timeline):**
   - Nhóm các sự kiện học tập theo từng ngày thực tế theo thứ tự thời gian đảo ngược (mới nhất lên đầu).
   - Tiêu đề ngày được định dạng tiếng Việt chuẩn: "Hôm nay", "Hôm qua", hoặc "Thứ X, dd/MM/yyyy".
   - Phân biệt rõ loại hoạt động bằng huy hiệu: "Trắc nghiệm" (kèm điểm số và trạng thái Đạt/Chưa đạt), "Hoàn thành bài tập", "Đọc nội dung".

7. **Trang Tiến độ Học tập Desktop (`/progress`):**
   - Đăng ký route `/progress` và thêm mục "Tiến độ" (`TrendingUp`) vào `AppSidebar` trên desktop.
   - Bộ chọn ghi danh (Enrollment Switcher) khi học viên đăng ký nhiều lộ trình.
   - Trạng thái tải trực quan với Skeleton loaders, trạng thái lỗi thân thiện kèm nút "Thử lại", và nút "Làm mới dữ liệu" để đồng bộ ngay lập tức.

---

## 3. Các Điểm Codex Đã Xử Lý & Hoàn Thiện

1. **Truy vấn Tiêu đề Lộ trình qua Revision:**
   - Thực thể `Curriculum` chỉ lưu trữ `curriculumKey` và `targetLocale`. Tiêu đề hiển thị của khóa học được lưu trữ tại `CurriculumRevision.title`. `LearningAnalyticsService` đã truy vấn qua `curriculumRevisionRepository.findById(enrollment.getRevisionId())` để hiển thị đúng tên lộ trình cho học viên.

2. **Chuẩn hóa Kiểu Dữ liệu Điểm Quiz:**
   - Trong `QuizAttempt`, hàm getter điểm số là `getScorePercentage()` trả về số nguyên phần trăm (0 - 100). Đã chuẩn hóa việc tính toán `averageScore` và `passedAttempts` trong thống kê bài kiểm tra.

3. **Cơ chế Truy vấn Tối ưu Dữ liệu Tuần và Hoạt động:**
   - Bổ sung `findWeeksByRevisionIdOrdered` trong `CurriculumWeekRepository` và `findActivityProjectionsByRevisionId` trong `ActivityRepository` để gom dữ liệu theo từng revision trong 1-2 truy vấn ngắn gọn, tránh N+1 query.

4. **Tuân thủ Chuẩn Giao diện & Test Isolation:**
   - Thay thế việc dùng `asChild` trên UI Button bằng thẻ `<Link className={buttonVariants(...)}>` tương thích React Router.
   - Tối ưu hóa scope test trong Playwright và Vitest, đảm bảo dữ liệu test độc lập và ổn định 100%.

---

## 4. Trạng thái Git & Kết luận

- Toàn bộ tính năng G6-01 đã hoàn thiện, kiểm chứng thực tế và sẵn sàng chuyển tiếp sang G6-02.
- Tuân thủ tuyệt đối các ràng buộc:
  * Không sửa/commit `docs/DAY2_REVIEW.md` và `backend/src/main/resources/META-INF/spring/`.
  * Không thay đổi hoặc làm ảnh hưởng dữ liệu tài khoản cá nhân.
  * Web desktop 1440×900 và 1280×800 đạt chuẩn visual.
