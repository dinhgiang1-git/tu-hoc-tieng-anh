# G5-02 — Nghiệm thu Đánh dấu Bài ôn tập (Bookmark), Theo dõi Câu sai và Giải quyết Câu sai (Resolution Tracking)

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G5-02 cho web desktop**. Mobile hoãn theo quy định chung của dự án. Hoàn thành toàn bộ mốc G5 (Quiz & Ôn tập Tối thiểu).

---

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
| :--- | :--- | :--- |
| **Playwright Desktop E2E (Dev DB thật)** | **1/1 đạt**, 5.3s (`review-desktop.e2e.ts`) | `frontend/e2e/review-desktop.e2e.ts`: Đánh dấu bài học & activity từ workspace, trắc nghiệm sai 3 câu, sang `/review`, kiểm tra badge bộ đếm (3 câu sai, 2 bookmark), giải quyết câu sai Q2 (/p/), nhận instant feedback & giải thích, cập nhật badge (còn 2 câu sai), chuyển tab bookmark xóa 1 bookmark, kiểm tra desktop 1440×900 và 1280×800. |
| **Regression Playwright E2E** | **Đạt 100%**: `quiz-desktop.e2e.ts` (1/1, 5.3s), `m1-core-loop.e2e.ts` (6/6 đạt) | Không phát sinh bất kỳ regression nào trên các luồng học cốt lõi và quiz đã nghiệm thu. |
| **Backend Unit Tests** | **28/28 đạt**, 0 failures, 0 errors | `ReviewServiceTest` (10/10), `ReviewControllerTest` (4/4), `QuizServiceTest` (11/11), `QuizControllerTest` (3/3). |
| **Frontend Component Tests (Vitest)** | **6/6 đạt**, 1.63s trên 2 test files | `ReviewPage.test.tsx` (4/4 tests: rendering tabs, check answer feedback, filter toggle, bookmark deletion, empty states), `BookmarkButton.test.tsx` (2/2 tests: toggle create & delete). |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run build` (`tsc -b && vite build`) thành công trong 934ms, `npm run lint` 0 errors. |
| **Spotless Code Format** | **159/159 files clean** | `mvnw spotless:apply` pass sạch trên toàn bộ mã nguồn Java. |
| **Desktop Screenshots** | **3 ảnh chụp sắc nét** | `frontend/e2e-screenshots/`: `review-desktop-mistakes.png`, `review-desktop-resolved.png`, `review-desktop-bookmarks.png`. |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G5-02

1. **Model Dữ liệu Đánh dấu & Giải quyết Câu sai (Database & Migrations):**
   - Migration `V009__create_review_tables.sql` bổ sung 2 bảng với quan hệ chuẩn:
     * `review_bookmark`: Lưu đánh dấu bài học (`lesson_id`) hoặc nhiệm vụ (`activity_id`) theo `enrollment_id` và `user_id`, hỗ trợ ghi chú cá nhân (`note`), có ràng buộc `UNIQUE (enrollment_id, lesson_id, COALESCE(activity_id, '00000000-0000-0000-0000-000000000000'::uuid))`.
     * `review_resolution`: Lưu câu hỏi sai đã được người học giải quyết (`enrollment_id`, `question_id`, `resolved_at`), có ràng buộc `UNIQUE (enrollment_id, question_id)`.
   - Các bảng đều có `ON DELETE CASCADE` khi xóa user/enrollment/lesson/activity/question, bảo đảm toàn vẹn dữ liệu.

2. **Cơ chế Trích xuất Câu sai từ Latest Attempt (Mistake Extraction Semantics):**
   - Endpoint `GET /api/v1/enrollments/{enrollmentId}/review`:
     * Chỉ kiểm tra kết quả lượt làm bài gần nhất (`latestAttempt`) cho mỗi quiz activity trong enrollment thông qua `findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc`.
     * Trích xuất các câu trả lời sai (`is_correct = false`) từ latest attempt.
     * Đối chiếu với `review_resolution`: Một câu hỏi được coi là đã giải quyết (`resolved = true`) nếu và chỉ nếu `resolution.resolvedAt >= latestAttempt.submittedAt`.
     * **Tính năng Re-open tự động:** Nếu người học đã giải quyết câu sai trong `/review`, nhưng sau đó quay lại `LessonWorkspace` làm lại cả bài quiz và trả lời sai lại câu hỏi đó, câu hỏi sẽ tự động chuyển lại thành `resolved = false` (chưa giải quyết) vì `latestAttempt.submittedAt > resolution.resolvedAt`.
     * Tuyệt đối không làm thay đổi điểm số gốc của `quiz_attempt`.

3. **API Kiểm tra Đáp án Ôn tập (Resolution Check API):**
   - Endpoint `POST /api/v1/enrollments/{enrollmentId}/review/questions/{questionId}/checks`:
     * Nhận `selectedOptionId`.
     * Kiểm tra đáp án trực tiếp trên server, ghi nhận vào `review_resolution` khi người học trả lời đúng.
     * Trả về `QuestionResultDto` chứa `isCorrect`, `selectedOptionId`, `correctOptionId`, và `explanation` để học viên nắm rõ lý do ngữ pháp/ngữ âm.

4. **Tích hợp Nút Đánh dấu Ôn tập (`BookmarkButton`):**
   - Nút `BookmarkButton` được tích hợp tại:
     * Header của `LessonWorkspacePage` (đánh dấu toàn bộ bài học).
     * Header của từng thẻ nhiệm vụ trong `ActivityRenderer` (đánh dấu activity cụ thể).
   - Hiển thị trạng thái "Đánh dấu ôn tập" (icon rỗng) $\leftrightarrow$ "Đã đánh dấu" (icon màu hổ phách), cập nhật reactive và đồng bộ tức thì qua TanStack Query cache.

5. **Giao diện Trang Ôn tập Desktop (`/review`):**
   - Đăng ký route `/review` và icon "Ôn tập" (`RotateCcw`) trên `AppSidebar`.
   - Gồm 2 tab chuyển đổi mượt mà kèm huy hiệu số lượng (`badges`):
     * **Tab "Câu hỏi cần ôn lại":**
       - Hiển thị danh sách câu hỏi làm sai từ các bài kiểm tra gần nhất.
       - Bộ lọc trạng thái: "Chưa giải quyết" (mặc định) và "Tất cả".
       - Học viên có thể chọn đáp án và nhấn "Kiểm tra đáp án" trực tiếp trên từng thẻ câu hỏi.
       - Hiển thị phản hồi tức thì: viền xanh lá & icon check cho đáp án đúng, viền đỏ cho đáp án sai, huy hiệu "Đã giải quyết" / "Cần ôn lại", banner giải thích chi tiết, và link quay lại bài học gốc.
     * **Tab "Bài học đã đánh dấu":**
       - Hiển thị danh sách bài học và nhiệm vụ đã đánh dấu, thẻ phân loại ("QUIZ", "READING", v.v.), ngày lưu, và link "Mở bài học".
       - Nút thùng rác cho phép hủy đánh dấu ngay tại trang ôn tập mà không cần quay lại workspace.
     * **Trạng thái Trống (Empty State) & Tải (Skeleton):**
       - Hiển thị thông điệp khích lệ thân thiện khi không có câu sai nào hoặc chưa có bài nào được đánh dấu.

---

## 3. Các Điểm Hoàn thiện Trong Quá trình Triển khai

1. **Khắc phục xung đột Cache 5 phút của TanStack Query:**
   - Cấu hình toàn cục trong `query-client.ts` mặc định `staleTime: 5 phút`. Khi học viên nộp bài trắc nghiệm trong `QuizActivityView`, query review trước đó được cache từ `BookmarkButton` chưa tự làm mới.
   - Đã thêm lệnh `queryClient.invalidateQueries({ queryKey: ["review"] })` ngay khi nộp quiz thành công, đồng thời đặt `staleTime: 0` và `refetchOnMount: "always"` trong `ReviewPage` để đảm bảo dữ liệu ôn tập luôn tươi mới 100%.

2. **Playwright Strict Mode trên Desktop Sidebar:**
   - Cả mobile drawer và desktop sidebar đều chứa thẻ `<aside>` với liên kết `/review`. Đã dùng bộ chọn `:visible` (`aside a[href="/review"]:visible`) để Playwright tương tác chuẩn xác trên desktop.

3. **Tương thích Bộ lọc và Phản hồi Tức thì:**
   - Khi trả lời đúng một câu hỏi sai với bộ lọc mặc định "Chưa giải quyết", câu hỏi được server đánh dấu `resolved = true` và tự động ẩn khỏi danh sách câu sai chưa giải quyết. Khi chuyển bộ lọc sang "Tất cả", câu hỏi hiển thị đầy đủ với trạng thái "Đã giải quyết".

---

## 4. Trạng thái Git & Kết luận

- Toàn bộ tính năng G5-02 đã hoàn thiện, kiểm chứng thực tế và sẵn sàng chuyển tiếp sang G6.
- Tuân thủ tuyệt đối các ràng buộc:
  * Không sửa/commit `docs/DAY2_REVIEW.md` và `backend/src/main/resources/META-INF/spring/`.
  * Không thay đổi hoặc làm ảnh hưởng dữ liệu tài khoản cá nhân.
  * Web desktop 1440×900 và 1280×800 đạt chuẩn visual.
