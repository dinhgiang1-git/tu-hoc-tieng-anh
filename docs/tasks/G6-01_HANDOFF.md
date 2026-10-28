# G6-01 — Thống kê tiến độ trực quan: Biểu đồ thanh theo tuần, lịch sử học tập theo ngày thực tế

## 1. Mục tiêu và phạm vi

Xây dựng module Thống kê & Phân tích Tiến độ Học tập (Visual Learning Progress & Analytics) giúp người học nắm bắt trực quan bức tranh toàn cảnh về tiến trình tự học của mình:
1. **Chỉ số học tập tổng quan (Key Metrics):**
   - Tiến độ tổng thể (tỉ lệ và phần trăm nhiệm vụ bắt buộc đã hoàn thành: `completedRequiredActivities` / `totalRequiredActivities`).
   - Số bài học đã hoàn thành / Tổng số bài học (`completedLessons` / `totalLessons`).
   - Thời lượng nội dung đã học: Tổng `estimatedMinutes` của các bài học đã hoàn thành (tuân thủ nghiêm ngặt quy tắc trong `KE_HOACH_DU_AN.md`: không gán nhãn sai là thời gian ngồi trước màn hình).
   - Chuỗi ngày học liên tục (Streak): số ngày học liên tiếp (`currentStreakDays`), chuỗi dài nhất (`longestStreakDays`), tổng số ngày có học tập (`totalActiveDays`).
   - Thống kê bài kiểm tra (Quiz stats): số lượt làm bài, số bài đạt, điểm trung bình.
2. **Biểu đồ thanh theo tuần (Weekly Progress Bar Chart):**
   - Biểu đồ thanh trực quan theo từng tuần trong lộ trình học (`WeeklyProgressItem`).
   - Thể hiện trực quan số nhiệm vụ bắt buộc đã xong vs tổng số nhiệm vụ của tuần, tỉ lệ phần trăm, trạng thái (Hoàn thành / Đang học / Chưa bắt đầu).
   - Hiển thị số bài học và tổng thời lượng dự kiến của tuần.
3. **Lịch sử học tập theo ngày thực tế (Daily Activity History & Heatmap):**
   - Tập hợp các hoạt động học tập thực tế từ `learning_event` và `activity_progress`, nhóm theo ngày thực tế (`local_date`) dựa trên múi giờ của người học.
   - Thể hiện số hoạt động hoàn thành và lượt thi quiz mỗi ngày.
   - Timeline chi tiết các hoạt động đã hoàn thành trong các ngày gần nhất.
4. **Màn hình Tiến độ học tập Desktop (`/progress`):**
   - Bổ sung mục "Tiến độ" vào thanh điều hướng bên trái (`AppSidebar`) với icon `BarChart3`.
   - Giao diện trực quan, thẩm mỹ cao, đồng bộ theme (light/dark), tương thích kích thước desktop 1440×900 và 1280×800.
   - Hỗ trợ chuyển đổi giữa các enrollment nếu người dùng học nhiều lộ trình.
   - Xử lý mượt mà trạng thái Empty state ("Chưa có dữ liệu học tập") và Loading Skeleton.

**Baseline:** `G5-02 accepted` (`de17f5d`). Tái dùng `LearningProgressService`, `LearningEventRepository`, `ActivityProgressRepository`, `LessonScheduleRepository`, `QuizAttemptRepository`, `CurriculumWeekRepository`, `LessonRepository`, `ActivityRepository`.  
**Phạm vi:** Web desktop (1440×900 và 1280×800). Mobile tiếp tục hoãn.  

---

## 2. Quy tắc Nghiệp vụ (Business Rules)

1. **Quyền sở hữu & Cách ly dữ liệu (Ownership & Isolation):**
   - Người học chỉ được phép xem thống kê của các enrollment thuộc tài khoản của chính mình. Nếu truy cập enrollment của người khác, trả về 404 `RESOURCE_NOT_FOUND`.
2. **Tính toán Tiến độ Bắt buộc Chuẩn xác (Effective Required Activities):**
   - Chỉ các nhiệm vụ có `isRequired = true` nằm trong các bài học có `isRequired = true` mới được tính vào mẫu số nhiệm vụ bắt buộc của tuần và toàn bộ lộ trình (`ProgressPolicy.isEffectivelyRequired(lesson.isRequired, activity.isRequired)`).
   - Các hoạt động tùy chọn (optional) không làm sai lệch tổng số bài / tổng số phút bắt buộc.
3. **Quy tắc Tính Ngày Học Thực Tế & Streak (Streak & Real Activity Days):**
   - Ngày học căn cứ vào trường `local_date` của các bản ghi `learning_event` có `eventType IN ('COMPLETED', 'SUBMITTED', 'QUIZ_SUBMITTED')` (hoặc `activity_progress.completed_at` quy đổi theo timezone của enrollment).
   - Chuỗi ngày học liên tục (`currentStreakDays`):
     - Xác định `today` theo múi giờ `timeZone` của enrollment.
     - Nếu người học có hoạt động vào ngày `today`, chuỗi tính từ `today` lùi về trước các ngày liên tiếp trước đó.
     - Nếu hôm nay chưa học nhưng ngày hôm qua (`today - 1 day`) có hoạt động, chuỗi được giữ nguyên tính từ hôm qua lùi về trước (chưa bị ngắt quãng).
     - Nếu cả hôm nay và hôm qua đều không có hoạt động học tập, `currentStreakDays = 0`.
4. **Quy tắc Thời lượng (Estimated Duration vs Actual):**
   - Không giả định thời gian người dùng mở tab là thời gian học thực tế. Thời lượng hoàn thành được tính bằng tổng `estimatedMinutes` của các bài học đã hoàn thành.

---

## 3. Thiết kế API Backend

### Endpoint: `GET /api/v1/enrollments/{enrollmentId}/analytics`

**Response `200 OK` (EnrollmentAnalyticsResponse):**
```json
{
  "enrollmentId": "uuid",
  "curriculumTitle": "Lộ trình Tiếng Anh Giao Tiếp Toàn Diện",
  "status": "ACTIVE",
  "startDate": "2026-10-01",
  "timeZone": "Asia/Ho_Chi_Minh",
  "enrollmentVersion": 4,
  "completedRequiredActivities": 12,
  "totalRequiredActivities": 45,
  "progressPercent": 26.7,
  "completedLessons": 3,
  "totalLessons": 12,
  "estimatedCompletedMinutes": 90,
  "estimatedTotalMinutes": 360,
  "streak": {
    "currentStreakDays": 3,
    "longestStreakDays": 5,
    "totalActiveDays": 7,
    "lastActiveDate": "2026-10-28"
  },
  "quizSummary": {
    "totalQuizzesTaken": 2,
    "passedQuizzes": 2,
    "averageScorePercent": 85.0
  },
  "weeklyProgress": [
    {
      "weekId": "uuid",
      "weekNumber": 1,
      "weekTitle": "Nhập môn phát âm IPA & Từ vựng căn bản",
      "totalRequiredActivities": 15,
      "completedRequiredActivities": 12,
      "progressPercent": 80.0,
      "totalLessons": 4,
      "completedLessons": 3,
      "estimatedMinutes": 120,
      "status": "IN_PROGRESS"
    }
  ],
  "dailyHistory": [
    {
      "date": "2026-10-28",
      "completedActivitiesCount": 3,
      "quizAttemptsCount": 1,
      "events": [
        {
          "id": "uuid",
          "eventType": "COMPLETED",
          "occurredAt": "2026-10-28T09:30:00Z",
          "activityTitle": "Luyện âm /i:/ và /ɪ/",
          "lessonTitle": "Nguyên âm đơn cơ bản"
        }
      ]
    }
  ]
}
```

---

## 4. Kế hoạch Kiểm thử & Tiêu chí Nghiệm thu

1. **Backend Unit Tests:**
   - Kiểm tra tính toán phân tích tuần (`weeklyProgress`), thống kê hoàn thành, streak liên tục / đứt quãng / học hôm nay / học hôm qua.
   - Kiểm tra kiểm soát quyền sở hữu (404 khi truy cập enrollment của người dùng khác).
   - Spotless check 100% clean.
2. **Frontend Unit & Component Tests:**
   - `WeeklyProgressBarChart.test.tsx`: render đúng tỉ lệ các tuần, nhãn phần trăm, trạng thái.
   - `DailyActivityTimeline.test.tsx`: render đúng danh sách ngày, số hoạt động, empty state khi chưa có dữ liệu.
   - `ProgressPage.test.tsx`: render các khối KPI, gọi đúng API, hỗ trợ refetch và chuyển đổi enrollment.
3. **E2E Playwright Desktop Test:**
   - Đăng nhập tài khoản test `e2e_learner`.
   - Điều hướng đến `/progress` từ thanh Sidebar.
   - Xác minh hiển thị đầy đủ: Thẻ KPI (tiến độ %, streak, thời lượng), Biểu đồ thanh tuần, và Lịch sử ngày thực tế.
   - Chụp ảnh màn hình lưu vào `frontend/e2e-screenshots/`.
4. **Tích hợp & Regression:**
   - Toàn bộ test suite backend và frontend tiếp tục pass 100%.
