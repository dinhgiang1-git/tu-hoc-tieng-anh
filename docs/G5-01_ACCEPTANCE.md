# G5-01 — Nghiệm thu Model Câu hỏi Trắc nghiệm, Server Chấm điểm Tự động & Chính sách ON_SUBMIT vs PASS_SCORE

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G5-01 cho web desktop**. Mobile hoãn theo quy định chung của dự án.

---

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
| :--- | :--- | :--- |
| **Playwright Desktop E2E (Dev DB thật)** | **1/1 đạt**, 5.5s (toàn bộ 19 suite regression đang chạy) | `frontend/e2e/quiz-desktop.e2e.ts`: Kiểm tra luồng làm bài, trượt (<80%), làm lại, đạt (100%), thi lại điểm thấp bảo toàn "Đã hoàn thành", kiểm tra viewport 1440x900 và 1280x800. |
| **Backend Unit Tests** | **28/28 đạt**, 0 failures, 0 errors | `QuizServiceTest` (11/11), `QuizControllerTest` (3/3), `DryRunAndCliTest` (7/7), `ImportAndPublishServiceTest` (7/7) |
| **Frontend Component Tests (Vitest)** | **56/56 đạt** trên 11 test files | Bao gồm `QuizActivityView.test.tsx` (4/4 tests: rendering không lộ đáp án, validation nút submit, submit đạt kèm feedback & giải thích, xử lý version conflict 409) |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run build` (`tsc -b && vite build`) thành công, `npm run lint` 0 errors |
| **Spotless Code Format** | **149/149 files clean** | `mvnw spotless:apply` pass |
| **Desktop Screenshots** | **3 ảnh chụp sắc nét** | `frontend/e2e-screenshots/`: `quiz-desktop-taking.png`, `quiz-desktop-passed.png`, `quiz-desktop-reattempt-preserved.png` |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G5-01

1. **Model Dữ liệu Câu hỏi Trắc nghiệm (Data Model & Migrations):**
   - Migration `V008__create_quiz_tables.sql` bổ sung 4 bảng:
     * `quiz_question`: Lưu câu hỏi thuộc `activity_id`, `question_type` (mặc định `'SINGLE_CHOICE'`), `prompt`, `explanation`, `order_index`.
     * `quiz_option`: Lưu 4 phương án cho mỗi câu hỏi, `option_text`, `is_correct`, `order_index`.
     * `quiz_attempt`: Lưu lịch sử mỗi lần làm bài của học viên (`enrollment_id`, `activity_id`, `attempt_number`, `score_percentage`, `correct_answers_count`, `total_questions_count`, `passed`, `completed_activity`).
     * `quiz_attempt_answer`: Lưu chi tiết câu trả lời của từng attempt (`question_id`, `selected_option_id`, `is_correct`).
   - Ràng buộc toàn vẹn: Foreign key cascading phù hợp, index tối ưu hóa theo `activity_id`, `enrollment_id`, và `attempt_number`.

2. **Chống Gian lận — Ẩn Đáp án & Giải thích (Anti-Cheat & Security):**
   - Endpoint `GET /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz`:
     * Trả về danh sách câu hỏi và các phương án lựa chọn được sắp xếp theo `order_index`.
     * **TUYỆT ĐỐI KHÔNG TRẢ VỀ** trường `isCorrect` và `explanation` trong JSON response trước khi nộp bài.
     * Bảo vệ kiểm tra quyền sở hữu enrollment: trả về 404/403 nếu không thuộc người dùng hiện hành.

3. **Server Chấm điểm Tự động & Chính sách Hoàn thành (Auto-Grading & Policies):**
   - Endpoint `POST /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz/submit`:
     * Nhận payload `QuizSubmissionRequest` chứa danh sách `{ questionId, selectedOptionId }`, `expectedVersion`, và header `Idempotency-Key`.
     * Server duyệt đối chiếu từng phương án đã chọn trong cơ sở dữ liệu, đếm chính xác số câu đúng (`correctAnswersCount`) trên tổng số câu hỏi (`totalQuestionsCount`), tính `scorePercentage = Math.round((correct * 100.0) / total)`.
     * **Chính sách `ON_SUBMIT` vs `PASS_SCORE`:**
       - Đối với `ON_SUBMIT`: Bất kỳ lượt nộp bài hợp lệ nào cũng được xem là hoàn thành bài tập (`isPassed = true`).
       - Đối với `PASS_SCORE`: Chỉ đạt yêu cầu khi `scorePercentage >= 80` (ngưỡng 80% theo quy định).
     * **Bảo toàn Thành tích khi Thi lại Điểm Thấp hơn (Achievement Preservation):**
       - Nếu activity đã đạt trạng thái `COMPLETED` ở lần thi trước, việc thi lại đạt điểm thấp hơn (< 80%) không được hạ trạng thái về `IN_PROGRESS`. Activity vẫn bảo toàn 100% trạng thái `COMPLETED`.
       - Lịch sử attempt mới vẫn được ghi lại đầy đủ vào `quiz_attempt` và `quiz_attempt_answer` để phục vụ thống kê ôn tập.
     * **Tích hợp Tiến độ & Phiên bản:**
       - Đồng bộ với `LearningProgressService.recordQuizAssessmentResult`: tăng `version = expectedVersion + 1`, kiểm tra CAS version conflict (409 `VERSION_CONFLICT`).
       - Replay an toàn qua `IdempotencyService`.

4. **Trải nghiệm Giao diện Desktop (UI/UX Desktop Experience):**
   - **Giao diện làm bài trắc nghiệm (`QuizActivityView.tsx`):**
     * Hiển thị danh sách câu hỏi trực quan với các radio button được bọc trong thẻ label có hiệu ứng hover mượt mà.
     * Bộ đếm tiến độ làm bài (vd: `Tiến độ trả lời: 3/4 câu - Còn 1 câu chưa chọn`).
     * Nút Nộp bài bị vô hiệu hóa (`disabled`) cho đến khi chọn đầy đủ tất cả câu hỏi, ngăn ngừa việc nộp sót câu.
   - **Kết quả chấm điểm tức thì (Instant Feedback Banner):**
     * Banner kết quả hiển thị tỷ lệ % điểm số, số câu đúng và trạng thái: "Chúc mừng! Bạn đã đạt bài kiểm tra." (màu xanh lá) hoặc "Chưa đạt yêu cầu." (màu hổ phách kèm ngưỡng 80%).
     * Hiển thị thông báo "Nhiệm vụ đã hoàn thành từ lượt nộp trước và tiếp tục được bảo toàn." khi thi lại điểm thấp hơn.
     * Chi tiết từng câu hỏi:
       - Đáp án đúng được viền xanh lá kèm biểu tượng checkmark.
       - Đáp án người dùng chọn sai được viền đỏ nổi bật kèm biểu tượng dấu gạch chéo.
       - Hiển thị khối giải thích (`explanation`) chi tiết để củng cố kiến thức ngữ âm / ngữ pháp.
   - **Chức năng Thi lại ("Làm lại"):**
     * Nút "Làm lại" cho phép xóa lựa chọn hiện tại, ẩn banner kết quả và mở lại form để học viên thử sức lần nữa.
   - **Khả năng tương thích:**
     * Đã kiểm chứng chuẩn xác trên 2 kích thước màn hình desktop tiêu chuẩn: 1440×900 và 1280×800, không bị tràn ngang (`scrollWidth <= window.innerWidth`).

---

## 3. Các Điểm Hoàn thiện Trong Lượt Nghiệm thu

- **Tích hợp Tự động hóa Nội dung:** Cập nhật `DryRunAndCliTest` và `ImportAndPublishServiceTest` để đồng bộ dữ liệu giáo trình mẫu khi mở rộng thêm bài kiểm tra trắc nghiệm.
- **Tối ưu Hóa API Types:** Tạo type an toàn thông qua `openapi-typescript` cho các DTO trắc nghiệm mới, đồng bộ cả ở `docs/api/generated-types.ts` và `frontend/src/generated/api-types.ts`.
- **Bảo mật & Kiểm thử Độc lập:** Fixture E2E được tạo riêng cho `e2e_quiz_curriculum` và tài khoản `e2e_learner_quiz`, hoàn toàn cách ly và không tác động đến dữ liệu cá nhân hay các bài học của tài khoản thật.

---

## 4. Kết luận & Mốc Tiếp theo

- **Nghiệm thu G5-01:** ĐẠT 100% các tiêu chí chức năng, kỹ thuật và kiểm thử theo chuẩn giao việc.
- **Bước tiếp theo:** Sẵn sàng chuyển tiếp sang **G5-02** (Lịch sử làm bài, danh sách câu sai và vòng lặp ôn tập) hoặc các nhiệm vụ tiếp theo trong lộ trình.
