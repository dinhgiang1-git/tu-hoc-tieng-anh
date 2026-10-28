import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import { login } from "./helpers/auth";

test.describe("G5-01 Desktop Quiz Flow: Single Choice, Auto-Grading & Completion Policies", () => {
  const enrollmentId = "e2e00000-0000-0000-0000-000000000099";
  const lessonId = "e2e00000-0000-0000-0000-000000000013";
  const quizActivityId = "e2e00000-0000-0000-0000-000000000015";

  test("Quiz Desktop: Taking quiz, 80% threshold, instant feedback, retry, and achievement preservation", async ({
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

    // 4. Locate Quiz Activity card
    const quizCard = page.locator(`[data-testid="quiz-activity-${quizActivityId}"]`);
    await expect(quizCard).toBeVisible();

    // Verify Quiz policy
    await expect(quizCard.locator("text=Đạt từ 80% trở lên")).toBeVisible();

    // Verify all 4 questions are displayed
    await expect(page.locator("text=Từ nào sau đây chứa nguyên âm ngắn /ɪ/?")).toBeVisible();
    await expect(page.locator("text=Âm nào là phụ âm bật hơi mạnh nhất?")).toBeVisible();
    await expect(page.locator("text=Quy tắc trọng âm danh từ 2 âm tiết đa số rơi vào đâu?")).toBeVisible();
    await expect(page.locator("text=Âm Schwa /ə/ có đặc điểm gì?")).toBeVisible();

    // Submit button should initially be disabled
    const submitBtn = quizCard.locator('[data-testid="quiz-submit-button"]');
    await expect(submitBtn).toBeDisabled();

    // 5. Take Screenshot 1: Quiz taking initial view
    await page.screenshot({
      path: path.join(screenshotsDir, "quiz-desktop-taking.png"),
      fullPage: false,
    });

    // 6. Test Failed Attempt (< 80%): Answer only Q1 correctly (25%)
    // Q1: ship (Correct)
    await quizCard.locator('label:has-text("ship") input[type="radio"]').click();
    // Q2: /b/ (Wrong)
    await quizCard.locator('label:has-text("/b/") input[type="radio"]').click();
    // Q3: Nhấn âm tiết 2 (Wrong)
    await quizCard.locator('label:has-text("Nhấn âm tiết 2") input[type="radio"]').click();
    // Q4: Luôn mang trọng âm chính (Wrong)
    await quizCard.locator('label:has-text("Luôn mang trọng âm chính") input[type="radio"]').click();

    // Submit button is now enabled
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Verify Result Banner for Failed Attempt
    const resultBanner = quizCard.locator('[data-testid="quiz-result-banner"]');
    await expect(resultBanner).toBeVisible();
    await expect(resultBanner.locator("text=Chưa đạt yêu cầu.")).toBeVisible();
    await expect(resultBanner.locator("text=1/4")).toBeVisible();
    await expect(resultBanner.locator("text=25%")).toBeVisible();

    // Activity status should NOT be completed
    const activityStatusBadge = page.locator(`[data-testid="activity-status-${quizActivityId}"]`);
    await expect(activityStatusBadge).toContainText("Đang làm");

    // Verify explanations and feedback are displayed
    await expect(page.locator("text=Giải thích:")).toHaveCount(4);

    // 7. Click Retry ("Làm lại")
    const retryBtn = quizCard.locator('[data-testid="quiz-retry-button"]');
    await expect(retryBtn).toBeVisible();
    await retryBtn.click();

    // Result banner is hidden and options can be selected again
    await expect(resultBanner).not.toBeVisible();
    await expect(submitBtn).toBeDisabled();

    // 8. Test Passed Attempt (100% >= 80%): Answer all 4 correctly
    // Q1: ship (Correct)
    await quizCard.locator('label:has-text("ship") input[type="radio"]').click();
    // Q2: /p/ (Correct)
    await quizCard.locator('label:has-text("/p/") input[type="radio"]').click();
    // Q3: Nhấn âm tiết 1 (Correct)
    await quizCard.locator('label:has-text("Nhấn âm tiết 1") input[type="radio"]').click();
    // Q4: Không bao giờ mang trọng âm (Correct)
    await quizCard.locator('label:has-text("Không bao giờ mang trọng âm") input[type="radio"]').click();

    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Verify Result Banner for Passed Attempt
    await expect(resultBanner).toBeVisible();
    await expect(resultBanner.locator("text=Chúc mừng! Bạn đã đạt bài kiểm tra.")).toBeVisible();
    await expect(resultBanner.locator("text=4/4")).toBeVisible();
    await expect(resultBanner.locator("text=100%")).toBeVisible();

    // Verify Activity Status updated to "Đã hoàn thành"
    await expect(activityStatusBadge).toContainText("Đã hoàn thành");

    // Take Screenshot 2: Quiz passed view
    await page.screenshot({
      path: path.join(screenshotsDir, "quiz-desktop-passed.png"),
      fullPage: false,
    });

    // 9. Re-attempt Lower Score Preservation:
    // Retry and intentionally submit a failing score (0%)
    await retryBtn.click();
    // Q1: sheep (Wrong)
    await quizCard.locator('label:has-text("sheep") input[type="radio"]').click();
    // Q2: /b/ (Wrong)
    await quizCard.locator('label:has-text("/b/") input[type="radio"]').click();
    // Q3: Nhấn âm tiết 2 (Wrong)
    await quizCard.locator('label:has-text("Nhấn âm tiết 2") input[type="radio"]').click();
    // Q4: Luôn mang trọng âm chính (Wrong)
    await quizCard.locator('label:has-text("Luôn mang trọng âm chính") input[type="radio"]').click();

    await submitBtn.click();
    await expect(resultBanner).toBeVisible();
    await expect(resultBanner.locator("text=Chưa đạt yêu cầu.")).toBeVisible();

    // CRITICAL REQUIREMENT: Previous achievement preserved!
    // Status must REMAIN "Đã hoàn thành" despite the lower re-attempt!
    await expect(resultBanner.locator("text=Nhiệm vụ đã hoàn thành")).toBeVisible();
    await expect(activityStatusBadge).toContainText("Đã hoàn thành");

    // Take Screenshot 3: Re-attempt achievement preserved
    await page.screenshot({
      path: path.join(screenshotsDir, "quiz-desktop-reattempt-preserved.png"),
      fullPage: false,
    });

    // 10. Check 1280x800 desktop viewport
    await page.setViewportSize({ width: 1280, height: 800 });
    await expect(page.locator("h1:has-text('Bài 1: Tổng kết & Kiểm tra Trắc nghiệm')")).toBeVisible();
    await expect(quizCard).toBeVisible();
  });
});
