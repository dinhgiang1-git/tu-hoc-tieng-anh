# G5-02 — Đánh dấu bài ôn tập (Bookmark), theo dõi câu sai và giải quyết câu sai (Resolution tracking)

## 1. Mục tiêu và phạm vi

Xây dựng module Ôn tập (Review & Mistake Resolution) giúp người học chủ động củng cố kiến thức và khắc phục điểm yếu:
1. **Đánh dấu Ôn tập (Bookmark System):**
   - Cho phép người học đánh dấu (bookmark) bất kỳ bài học hoặc nhiệm vụ nào từ Không gian học tập (`LessonWorkspace`) để xem lại sau.
   - Cho phép hủy đánh dấu (unbookmark) trực tiếp từ bài học hoặc từ trang Ôn tập.
2. **Tự động Theo dõi Câu hỏi Sai (Quiz Mistake Tracking):**
   - Tự động phát hiện các câu hỏi trắc nghiệm mà người học đã trả lời sai trong lượt làm bài quiz gần nhất của enrollment.
   - Liệt kê danh sách câu hỏi sai chưa được giải quyết (`unresolved`) trong trang Ôn tập.
3. **Giải quyết Câu sai (Resolution Tracking & Re-check):**
   - Cho phép người học thử làm lại từng câu hỏi sai ngay trên trang Ôn tập (`POST .../review/questions/{questionId}/checks`).
   - Nếu trả lời đúng: Ghi nhận bản ghi `review_resolution`, chuyển trạng thái câu hỏi sang "Đã giải quyết" (Resolved) và ẩn khỏi danh sách cần ôn.
   - Nếu trả lời sai: Cung cấp giải thích chi tiết, câu hỏi vẫn giữ nguyên trong danh sách cần ôn.
   - Khi làm lại toàn bộ quiz gốc: nếu tiếp tục làm sai câu hỏi đó, trạng thái resolution được mở lại để nhắc nhở người học.
4. **Màn hình Ôn tập Desktop (`/review`):**
   - Bổ sung mục "Ôn tập" trên thanh điều hướng bên trái (`AppSidebar`).
   - Trang `/review` gồm hai khối chính: "Câu hỏi trắc nghiệm cần ôn lại" và "Bài học đã đánh dấu".
   - Cho phép thao tác làm lại câu hỏi trực tiếp với instant feedback và điều hướng nhanh về bài học nguồn.

**Baseline:** `G5-01 accepted` (`71ae1e0`). Tái dùng `QuizAttempt`, `QuizAttemptAnswer`, `QuizQuestion`, `QuizOption`, `LessonWorkspacePage`, `ActivityRenderer`.  
**Phạm vi:** Web desktop (1440×900 và 1280×800). Mobile tiếp tục hoãn.  
**Ngoài phạm vi:** Thuật toán lặp lại ngắt quãng phức tạp (SM-2/Anki SRS), flashcards tự tạo.

---

## 2. Quy tắc Nghiệp vụ (Business Rules)

1. **Quyền sở hữu & Cách ly:**
   - Người học chỉ xem và thao tác trên bookmark và câu sai thuộc các enrollment của chính mình.
   - Bookmark và câu sai được gắn chặt với `enrollmentId` và `revisionId` cụ thể.
2. **Quy tắc Câu sai (Mistakes Calculation):**
   - Lấy các attempt mới nhất cho từng quiz activity trong enrollment.
   - Với mỗi attempt mới nhất, lấy các câu hỏi có câu trả lời sai (`is_correct = false`).
   - Một câu hỏi sai được coi là **Đã giải quyết** (`resolved`) nếu tồn tại bản ghi trong `review_resolution` với `resolved_at >= attempt.submitted_at` HOẶC trong lượt attempt mới nhất của quiz đó câu hỏi đã được làm đúng.
3. **Bảo toàn Dữ liệu Quiz Gốc:**
   - Việc trả lời đúng câu hỏi trong màn hình Ôn tập chỉ ghi nhận `review_resolution`, **tuyệt đối không can thiệp hay sửa đổi điểm số của `quiz_attempt` gốc**, không tự động thay đổi tiến độ hoàn thành activity.
4. **Bảo mật Nội dung (Anti-Cheat):**
   - Khi hiển thị câu hỏi sai để làm lại trên màn hình Ôn tập, API **tuyệt đối không trả về `is_correct` hay `explanation`** trước khi người học bấm nút kiểm tra đáp án.

---

## 3. Thiết kế Cấu trúc Dữ liệu & API

### 3.1 Migration `V009__create_review_tables.sql`
- **`review_bookmark`:**
  * `id`: UUID PRIMARY KEY
  * `user_id`: UUID NOT NULL REFERENCES `app_user(id)` ON DELETE CASCADE
  * `enrollment_id`: UUID NOT NULL REFERENCES `enrollment(id)` ON DELETE CASCADE
  * `lesson_id`: UUID NOT NULL REFERENCES `lesson(id)` ON DELETE CASCADE
  * `activity_id`: UUID NULL REFERENCES `activity(id)` ON DELETE CASCADE
  * `note`: VARCHAR(500) NULL
  * `created_at`: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
  * Unique constraint: `uq_review_bookmark (enrollment_id, lesson_id, COALESCE(activity_id, '00000000-0000-0000-0000-000000000000'))`
- **`review_resolution`:**
  * `id`: UUID PRIMARY KEY
  * `enrollment_id`: UUID NOT NULL REFERENCES `enrollment(id)` ON DELETE CASCADE
  * `question_id`: UUID NOT NULL REFERENCES `quiz_question(id)` ON DELETE CASCADE
  * `resolved_at`: TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
  * Unique constraint: `uq_review_resolution (enrollment_id, question_id)`

### 3.2 Hợp đồng API
1. `GET /api/v1/enrollments/{enrollmentId}/review`:
   - Trả về: `ReviewSummaryResponse`:
     ```json
     {
       "enrollmentId": "uuid",
       "bookmarkedItems": [
         {
           "id": "uuid",
           "lessonId": "uuid",
           "lessonTitle": "Bài 1: Tổng kết & Kiểm tra Trắc nghiệm",
           "activityId": "uuid",
           "activityTitle": "Đọc tóm tắt lý thuyết",
           "activityType": "READING",
           "note": null,
           "createdAt": "2026-10-28T08:00:00Z"
         }
       ],
       "mistakes": [
         {
           "questionId": "uuid",
           "prompt": "Từ nào sau đây chứa nguyên âm ngắn /ɪ/?",
           "lessonId": "uuid",
           "lessonTitle": "Bài 1: Tổng kết & Kiểm tra Trắc nghiệm",
           "activityId": "uuid",
           "activityTitle": "Bài trắc nghiệm cuối bài",
           "options": [
             { "id": "uuid", "orderIndex": 1, "content": "sheep" },
             { "id": "uuid", "orderIndex": 2, "content": "ship" }
           ],
           "resolved": false,
           "lastAttemptedAt": "2026-10-28T08:00:00Z"
         }
       ]
     }
     ```
2. `POST /api/v1/review-bookmarks`:
   - Body: `{ "enrollmentId": "uuid", "lessonId": "uuid", "activityId": "uuid" (optional), "note": "string" (optional) }`
   - Trả về: `BookmarkResponse` (201 Created).
3. `DELETE /api/v1/review-bookmarks/{id}`:
   - Trả về: 204 No Content.
4. `POST /api/v1/enrollments/{enrollmentId}/review/questions/{questionId}/checks`:
   - Body: `{ "selectedOptionId": "uuid" }`
   - Trả về: `QuestionCheckResponse`:
     ```json
     {
       "questionId": "uuid",
       "selectedOptionId": "uuid",
       "correctOptionId": "uuid",
       "isCorrect": true,
       "explanation": "Giải thích chi tiết...",
       "resolved": true
     }
     ```

---

## 4. Kế hoạch Triển khai Chi tiết

1. **Database & Backend Entities:**
   - Viết migration `V009__create_review_tables.sql`.
   - Tạo JPA Entities: `ReviewBookmark`, `ReviewResolution`.
   - Tạo Repositories: `ReviewBookmarkRepository`, `ReviewResolutionRepository`.
2. **Review Application Service & Controller:**
   - Tạo `ReviewService`:
     * `getReviewSummary(UUID enrollmentId)`
     * `createBookmark(UUID userId, CreateBookmarkRequest request)`
     * `deleteBookmark(UUID userId, UUID bookmarkId)`
     * `checkMistakeQuestion(UUID enrollmentId, UUID questionId, CheckQuestionRequest request)`
   - Tạo `ReviewController` expose các endpoints theo đặc tả.
3. **Frontend API & Components:**
   - Cập nhật OpenAPI spec và gen types.
   - Thêm `review.api.ts`.
   - Tạo trang `ReviewPage.tsx` tại route `/review`.
   - Tạo component `BookmarkButton.tsx` và gắn vào `ActivityRenderer` / `LessonWorkspacePage`.
   - Cập nhật `AppSidebar.tsx` hiển thị menu "Ôn tập".
4. **Kiểm thử & Nghiệm thu:**
   - Unit tests backend: `ReviewServiceTest`, `ReviewControllerTest`.
   - Vitest tests frontend: `ReviewPage.test.tsx`, `BookmarkButton.test.tsx`.
   - Playwright Desktop E2E: `review-desktop.e2e.ts`.
   - Tài liệu nghiệm thu `docs/G5-02_ACCEPTANCE.md` và cập nhật `backlog.md`.
