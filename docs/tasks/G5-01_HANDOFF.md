# G5-01 — Model câu hỏi trắc nghiệm, server chấm điểm tự động, chính sách ON_SUBMIT vs PASS_SCORE

## 1. Mục tiêu và phạm vi

Xây dựng module Đánh giá Trắc nghiệm (Quiz Assessment) phục vụ kiểm tra và củng cố kiến thức trong không gian học tập cá nhân:
1. **Mô hình Dữ liệu Trắc nghiệm (Quiz Model):**
   - Mỗi nhiệm vụ `QUIZ` bao gồm một tập các câu hỏi trắc nghiệm đơn chọn (Single choice: mỗi câu hỏi có nhiều lựa chọn nhưng chỉ duy nhất 1 đáp án đúng).
   - Mỗi câu hỏi có lời giải thích chi tiết (`explanation`) sau khi làm bài.
   - Hỗ trợ nạp câu hỏi trắc nghiệm từ gói nội dung giáo trình (Curriculum Package) thông qua importer.
2. **Bảo mật Nội dung (Anti-Cheat / Security):**
   - API cung cấp đề thi trắc nghiệm (`GET /activities/{activityId}/quiz`) **tuyệt đối không trả về trường `is_correct` hay `explanation`**.
   - Việc chấm điểm được thực thi 100% tại Server (Server Auto-Grading).
3. **Chấm điểm Tự động & Lịch sử Làm bài (Grading & Attempt Tracking):**
   - Lưu trữ từng lượt làm bài (`QuizAttempt`) và chi tiết lựa chọn của người học (`QuizAttemptAnswer`).
   - Tính toán chính xác số câu đúng, tổng số câu và tỷ lệ phần trăm (`scorePercentage`).
   - Trả về kết quả chi tiết sau khi nộp: kết quả từng câu, chỉ định đáp án đúng và lời giải thích.
4. **Hai Chính sách Hoàn thành (Completion Policies):**
   - **`ON_SUBMIT` (Luyện tập):** Đánh dấu nhiệm vụ `COMPLETED` ngay khi người học nộp bài hợp lệ bất kể điểm số đạt bao nhiêu.
   - **`PASS_SCORE` (Kiểm tra đạt chuẩn):** Chỉ đánh dấu nhiệm vụ `COMPLETED` khi tỷ lệ điểm $\ge 80\%$ (ngưỡng chuẩn hóa). Nếu dưới 80%, lượt nộp vẫn được ghi nhận nhưng trạng thái nhiệm vụ chuyển sang `IN_PROGRESS` để người học ôn tập và làm lại.
5. **Bảo toàn Thành tích (Achievement Preservation):**
   - Người học được phép làm lại Quiz bất kỳ lúc nào để luyện tập thêm.
   - Nếu nhiệm vụ đã đạt trạng thái `COMPLETED` ở lần thi trước, việc làm lại có điểm số thấp hơn **không bao giờ hủy bỏ trạng thái `COMPLETED`** đã đạt được.
6. **Bảo vệ Idempotency & Concurrency:**
   - Hỗ trợ header `Idempotency-Key` khi nộp bài để ngăn chặn double-submit do trễ mạng.
   - Kiểm tra versioning của enrollment (`expectedVersion`) để tránh xung đột khi có cập nhật song song.
7. **Giao diện Không gian Học tập Desktop:**
   - Component `QuizActivityView` trong Không gian học tập: hiển thị danh sách câu hỏi dạng thẻ, radio chọn đáp án, thanh tiến độ làm bài, nút nộp bài (`quiz-submit-button`).
   - Bảng kết quả sau khi nộp: điểm số, tỷ lệ %, thông báo Đạt / Chưa đạt, hiển thị đáp án đúng và giải thích chi tiết, nút "Làm lại quiz" (`quiz-retry-button`).
   - Đồng bộ ngay lập tức với sidebar bài học và tổng tiến độ lộ trình.

**Baseline:** `G4 accepted`. Tái dùng `LearningProgressService`, `ActivityProgress`, `LessonSchedule`, `WorkspacePage`, `ActivityRenderer`.  
**Phạm vi:** Web desktop (1440×900 và 1280×800). Mobile tiếp tục hoãn.  
**Ngoài phạm vi:** Đánh dấu câu sai (Bookmark - G5-02), thuật toán lặp lại ngắt quãng (SRS/Flashcard), câu hỏi trắc nghiệm nhiều đáp án (Multi-choice) hoặc điền từ.

---

## 2. Quy tắc Nghiệp vụ (Business Rules)

1. **Bảo vệ Trạng thái Lộ trình:**
   - Chỉ cho phép lấy đề và nộp bài quiz khi enrollment đang `ACTIVE`.
   - Nếu enrollment đang `PAUSED`, từ chối nộp bài với HTTP 409 `STATE_CONFLICT` (thông báo lộ trình đang tạm dừng).
   - Nếu enrollment đang `ABANDONED`, từ chối với 409 `STATE_CONFLICT`.
2. **Quy tắc Chấm điểm (Grading Rules):**
   - Mỗi câu hỏi có đúng 1 đáp án được chọn.
   - Người học phải trả lời đầy đủ tất cả các câu hỏi của Quiz trước khi nộp bài (không nộp bài trống hoặc thiếu câu).
   - Điểm số:
     $$\text{score} = \sum [\text{option.isCorrect} == \text{true}]$$
     $$\text{scorePercentage} = \text{round}\left(\frac{\text{score}}{\text{totalQuestions}} \times 100\right)$$
3. **Quy tắc Hoàn thành Nhiệm vụ:**
   - Đối với `completionPolicy == 'ON_SUBMIT'`: `passed = true` với mọi lượt nộp hợp lệ $\rightarrow$ cập nhật `activity_progress.status = 'COMPLETED'`.
   - Đối với `completionPolicy == 'PASS_SCORE'`: `passed = (scorePercentage >= 80)`.
     * Nếu `passed == true`: cập nhật `activity_progress.status = 'COMPLETED'`.
     * Nếu `passed == false` và trạng thái hiện tại chưa phải `COMPLETED`: cập nhật `activity_progress.status = 'IN_PROGRESS'`.
   - Cập nhật `lesson_schedule.completedAt` và tiến độ lộ trình nếu toàn bộ nhiệm vụ bắt buộc trong buổi học đã hoàn thành.
4. **Không Cho phép Undo Thủ công:**
   - API `PUT /api/v1/enrollments/{enrollmentId}/activities/{activityId}/progress` tiếp tục từ chối 409 `STATE_CONFLICT` đối với `QUIZ` (tuân thủ ADR-002).

---

## 3. Thiết kế Cấu trúc Dữ liệu & API

### 3.1 Migration `V008__create_quiz_tables.sql`
- **`quiz_question`:** `id`, `activity_id` (FK), `order_index`, `prompt`, `explanation`, `created_at`, `updated_at`. Ràng buộc unique `(activity_id, order_index)`.
- **`quiz_option`:** `id`, `question_id` (FK), `order_index`, `content`, `is_correct`, `created_at`. Ràng buộc unique `(question_id, order_index)`.
- **`quiz_attempt`:** `id`, `enrollment_id` (FK), `activity_id` (FK), `user_id` (FK), `attempt_number`, `score`, `total_questions`, `score_percentage`, `passed`, `submitted_at`.
- **`quiz_attempt_answer`:** `id`, `attempt_id` (FK), `question_id` (FK), `selected_option_id` (FK), `is_correct`. Ràng buộc unique `(attempt_id, question_id)`.

### 3.2 Hợp đồng API (OpenAPI)
1. `GET /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz`:
   - Trả về: `QuizDetailResponse`:
     ```json
     {
       "activityId": "uuid",
       "title": "Tên bài quiz",
       "completionPolicy": "PASS_SCORE",
       "passingScorePercentage": 80,
       "questions": [
         {
           "id": "uuid",
           "orderIndex": 1,
           "prompt": "Câu hỏi...",
           "options": [
             { "id": "uuid", "orderIndex": 1, "content": "Lựa chọn A" },
             { "id": "uuid", "orderIndex": 2, "content": "Lựa chọn B" }
           ]
         }
       ],
       "latestAttempt": null
     }
     ```
2. `POST /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz/submit`:
   - Headers: `X-CSRF-TOKEN`, `Idempotency-Key` (tùy chọn).
   - Body: `QuizSubmitRequest`:
     ```json
     {
       "expectedVersion": 1,
       "answers": [
         { "questionId": "uuid", "selectedOptionId": "uuid" }
       ]
     }
     ```
   - Trả về: `QuizResultResponse`:
     ```json
     {
       "attemptId": "uuid",
       "attemptNumber": 1,
       "score": 4,
       "totalQuestions": 4,
       "scorePercentage": 100,
       "passed": true,
       "activityCompleted": true,
       "enrollmentVersion": 2,
       "questionResults": [
         {
           "questionId": "uuid",
           "selectedOptionId": "uuid",
           "correctOptionId": "uuid",
           "isCorrect": true,
           "explanation": "Giải thích vì sao đáp án đúng..."
         }
       ]
     }
     ```

---

## 4. Kế hoạch Triển khai Chi tiết

1. **Database & Backend Entities:**
   - Viết migration `V008__create_quiz_tables.sql`.
   - Tạo JPA Entities: `QuizQuestion`, `QuizOption`, `QuizAttempt`, `QuizAttemptAnswer`.
   - Tạo JPA Repositories: `QuizQuestionRepository`, `QuizOptionRepository`, `QuizAttemptRepository`, `QuizAttemptAnswerRepository`.
2. **Curriculum Importer:**
   - Cập nhật schema `content/schemas/week.schema.json` để cho phép `quizQuestions` trong activity type `QUIZ`.
   - Cập nhật `CurriculumImporterService` để lưu `quiz_question` và `quiz_option`.
   - Thêm câu hỏi quiz thực tế vào `content/pilot-curriculum/v1/weeks/week-01.json`.
3. **Backend Service & Controller:**
   - Tạo `QuizService`:
     * `getQuizDetail(UUID enrollmentId, UUID activityId)`: Trả về câu hỏi và các lựa chọn (giấu `is_correct` và `explanation`).
     * `submitQuiz(UUID enrollmentId, UUID activityId, String idempotencyKey, QuizSubmitRequest request)`: Chấm điểm, lưu attempt, cập nhật `ActivityProgress` qua logic dùng chung của `LearningProgressService`, tăng version.
   - Tạo `QuizController`: Expose 2 endpoints `GET` và `POST`.
4. **Frontend UI & API Integration:**
   - Cập nhật OpenAPI spec và gen types.
   - Thêm phương thức API `getQuiz` và `submitQuiz` vào API client.
   - Tạo component `QuizActivityView.tsx` trong `features/learning/components/`.
   - Tích hợp `QuizActivityView` vào `ActivityRenderer.tsx`.
5. **Kiểm thử & Nghiệm thu:**
   - Backend unit tests (`QuizServiceTest`, `QuizControllerTest`).
   - Frontend Vitest tests cho `QuizActivityView`.
   - E2E Playwright test kiểm chứng luồng làm quiz trên trình duyệt desktop 1440px và 1280px.
   - Báo cáo triển khai `docs/G5-01_IMPLEMENTATION.md` và biên bản nghiệm thu `docs/G5-01_ACCEPTANCE.md`.

---

## 5. Tiêu chí Nghiệm thu (DoD)

1. **Bảo mật Đề thi:** `GET /activities/{id}/quiz` tuyệt đối không chứa `is_correct` hay `explanation`.
2. **Độ chính xác Chấm điểm:** Server chấm điểm đúng 100% theo các lựa chọn nộp lên; tính đúng `score`, `totalQuestions`, `scorePercentage`.
3. **Chính sách Hoàn thành:**
   - `ON_SUBMIT`: Điểm bất kỳ đều chuyển activity sang `COMPLETED`.
   - `PASS_SCORE`: $\ge 80\%$ chuyển sang `COMPLETED`; $< 80\%$ chuyển sang `IN_PROGRESS`.
4. **Bảo toàn Thành tích:** Lần nộp sau có điểm thấp hơn không hủy bỏ trạng thái `COMPLETED` của lần trước.
5. **Bảo vệ Concurrency & Idempotency:** Thao tác submit có kiểm tra `expectedVersion` và replay `Idempotency-Key` an toàn.
6. **Frontend Chất lượng:** `npm run build` (TypeScript) 0 errors, `npm run lint` 0 errors, giao diện responsive mượt mà trên desktop 1440×900 và 1280×800.
7. **E2E Thực tế:** Kiểm thử tự động trên desktop Chromium hoàn thành toàn bộ hành trình làm bài quiz từ giao diện.
