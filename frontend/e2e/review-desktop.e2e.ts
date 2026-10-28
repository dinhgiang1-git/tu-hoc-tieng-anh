import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import { login } from "./helpers/auth";

test.describe("G5-02 Desktop Review Flow: Bookmarks, Mistake Tracking & Resolution", () => {
  const enrollmentId = "e2e00000-0000-0000-0000-000000000099";
  const lessonId = "e2e00000-0000-0000-0000-000000000013";
  const quizActivityId = "e2e00000-0000-0000-0000-000000000015";

  test("Review Desktop: Bookmark creation in workspace, mistake extraction from quiz, resolution re-testing, and bookmark management", async ({
    page,
  }) => {
    const screenshotsDir = path.resolve(import.meta.dirname, "../e2e-screenshots");
    if (!fs.existsSync(screenshotsDir)) {
      fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    // 1. Set 1440x900 desktop viewport
    await page.setViewportSize({ width: 1440, height: 900 });

    // 2. Login with dedicated e2e_learner_quiz account
    await login(page, "e2e_learner_quiz");

    // 3. Navigate directly to the lesson workspace containing the quiz
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lessonId}`);
    await page.waitForLoadState("networkidle");

    // Verify Lesson Workspace loaded
    await expect(page.locator("h1:has-text('Bài 1: Tổng kết & Kiểm tra Trắc nghiệm')")).toBeVisible();

    // 4. Bookmark the lesson from the top action bar
    const lessonBookmarkBtn = page.locator(`[data-testid="bookmark-lesson-${lessonId}"]`);
    await expect(lessonBookmarkBtn).toBeVisible();
    await expect(lessonBookmarkBtn).toContainText("Đánh dấu ôn tập");
    await lessonBookmarkBtn.click();
    await expect(lessonBookmarkBtn).toContainText("Đã đánh dấu");

    // 5. Bookmark the quiz activity from the activity header
    const activityBookmarkBtn = page.locator(`[data-testid="bookmark-activity-${quizActivityId}"]`);
    await expect(activityBookmarkBtn).toBeVisible();
    await expect(activityBookmarkBtn).toContainText("Đánh dấu ôn tập");
    await activityBookmarkBtn.click();
    await expect(activityBookmarkBtn).toContainText("Đã đánh dấu");

    // 6. Complete quiz attempt with 3 wrong answers (Q2, Q3, Q4 wrong)
    const quizCard = page.locator(`[data-testid="quiz-activity-${quizActivityId}"]`);
    await expect(quizCard).toBeVisible();

    // Q1: ship (Correct)
    await quizCard.locator('label:has-text("ship") input[type="radio"]').click();
    // Q2: /b/ (Wrong)
    await quizCard.locator('label:has-text("/b/") input[type="radio"]').click();
    // Q3: Nhấn âm tiết 2 (Wrong)
    await quizCard.locator('label:has-text("Nhấn âm tiết 2") input[type="radio"]').click();
    // Q4: Luôn mang trọng âm chính (Wrong)
    await quizCard.locator('label:has-text("Luôn mang trọng âm chính") input[type="radio"]').click();

    // Submit quiz
    const submitBtn = quizCard.locator('[data-testid="quiz-submit-button"]');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Verify Result Banner: 1/4 (25%)
    const resultBanner = quizCard.locator('[data-testid="quiz-result-banner"]');
    await expect(resultBanner).toBeVisible();
    await expect(resultBanner.locator("text=1/4")).toBeVisible();

    // 7. Navigate to /review via sidebar
    const reviewNav = page.locator('aside a[href="/review"]:visible');
    await expect(reviewNav).toBeVisible();
    await reviewNav.click();

    await page.waitForURL("**/review");
    await page.waitForLoadState("networkidle");

    // Verify Review Page Title & Navigation
    await expect(page.locator("h1:has-text('Ôn tập & Khắc phục câu sai')")).toBeVisible();

    // 8. Verify Tabs and Counters
    const tabMistakes = page.locator('[data-testid="tab-mistakes"]');
    const tabBookmarks = page.locator('[data-testid="tab-bookmarks"]');
    await expect(tabMistakes).toBeVisible();
    await expect(tabBookmarks).toBeVisible();

    // Mistakes count badge should be 3
    await expect(tabMistakes.locator("span.rounded-full")).toHaveText("3");
    // Bookmarks count badge should be 2
    await expect(tabBookmarks.locator("span.rounded-full")).toHaveText("2");

    // Verify all 3 wrong questions are in the mistake list
    await expect(page.locator("text=Âm nào là phụ âm bật hơi mạnh nhất?")).toBeVisible();
    await expect(page.locator("text=Quy tắc trọng âm danh từ 2 âm tiết đa số rơi vào đâu?")).toBeVisible();
    await expect(page.locator("text=Âm Schwa /ə/ có đặc điểm gì?")).toBeVisible();

    // Take Screenshot 1: Review Mistakes Initial State (1440x900)
    await page.screenshot({
      path: path.join(screenshotsDir, "review-desktop-mistakes.png"),
      fullPage: false,
    });

    // 9. Switch to 'Tất cả' filter so all questions (unresolved and resolved) remain visible
    const allFilterBtn = page.locator('button:has-text("Tất cả")');
    await allFilterBtn.click();

    // 10. Resolve Q2: "Âm nào là phụ âm bật hơi mạnh nhất?"
    // Find the mistake card for Q2
    const q2Card = page.locator('[data-testid^="mistake-card-"]').filter({
      hasText: "Âm nào là phụ âm bật hơi mạnh nhất?",
    });
    await expect(q2Card).toBeVisible();
    await expect(q2Card.locator('[data-testid^="mistake-status-"]')).toContainText("Cần ôn lại");

    // Select correct answer /p/
    await q2Card.locator('label:has-text("/p/") input[type="radio"]').click();

    // Click "Kiểm tra đáp án"
    const checkBtn = q2Card.locator('[data-testid^="check-button-"]');
    await checkBtn.click();

    // Verify Instant Feedback & Resolution
    await expect(q2Card.locator("text=Chính xác! Câu hỏi đã được giải quyết.")).toBeVisible();
    await expect(q2Card.locator('[data-testid^="mistake-status-"]')).toContainText("Đã giải quyết");
    await expect(q2Card.locator('[data-testid^="mistake-explanation-"]')).toBeVisible();

    // Take Screenshot 2: Review Resolved State
    await page.screenshot({
      path: path.join(screenshotsDir, "review-desktop-resolved.png"),
      fullPage: false,
    });

    // 11. Test filter toggle: switch back to 'Chưa giải quyết'
    const unresolvedFilterBtn = page.locator('button:has-text("Chưa giải quyết")');
    await unresolvedFilterBtn.click();
    // Q2 should now be filtered out
    await expect(q2Card).not.toBeVisible();
    await expect(page.locator('[data-testid^="mistake-card-"]')).toHaveCount(2);

    // Switch back to 'Tất cả'
    await allFilterBtn.click();
    await expect(q2Card).toBeVisible();

    // 11. Switch to Bookmarks Tab
    await tabBookmarks.click();

    const bookmarksSection = page.locator('[data-testid="bookmarks-section"]');
    await expect(bookmarksSection).toBeVisible();

    // Verify 2 bookmarked cards rendered
    const bookmarkCards = bookmarksSection.locator('[data-testid^="bookmarked-item-"]');
    await expect(bookmarkCards).toHaveCount(2);

    // Verify lesson title displayed
    await expect(bookmarksSection.locator("text=Bài 1: Tổng kết & Kiểm tra Trắc nghiệm").first()).toBeVisible();

    // Take Screenshot 3: Bookmarks List
    await page.screenshot({
      path: path.join(screenshotsDir, "review-desktop-bookmarks.png"),
      fullPage: false,
    });

    // 12. Delete one bookmark from /review
    const firstDeleteBtn = bookmarksSection.locator('[data-testid^="delete-bookmark-"]').first();
    await firstDeleteBtn.click();

    // Count should decrease to 1
    await expect(bookmarksSection.locator('[data-testid^="bookmarked-item-"]')).toHaveCount(1);
    await expect(tabBookmarks.locator("span.rounded-full")).toHaveText("1");

    // 13. Test 1280x800 desktop viewport
    await page.setViewportSize({ width: 1280, height: 800 });
    await expect(page.locator("h1:has-text('Ôn tập & Khắc phục câu sai')")).toBeVisible();
    await expect(bookmarksSection.locator('[data-testid^="bookmarked-item-"]')).toHaveCount(1);

    // Click "Mở bài học" on the remaining bookmark to verify navigation
    const openLessonLink = bookmarksSection.locator('a:has-text("Mở bài học")').first();
    await openLessonLink.click();

    await page.waitForURL(`**/enrollments/${enrollmentId}/lessons/${lessonId}`);
    await expect(page.locator("h1:has-text('Bài 1: Tổng kết & Kiểm tra Trắc nghiệm')")).toBeVisible();
  });
});
