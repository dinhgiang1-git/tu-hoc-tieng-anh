import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import { login } from "./helpers/auth";

test.describe("G6-01 Desktop Learning Progress & Visual Analytics Flow", () => {
  const enrollmentId = "e2e00000-0000-0000-0000-000000000099";
  const lessonId = "e2e00000-0000-0000-0000-000000000013";
  const quizActivityId = "e2e00000-0000-0000-0000-000000000015";

  test("Progress Desktop: Complete lesson activity, navigate via sidebar to /progress, verify KPI metrics, weekly chart & daily timeline", async ({
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

    // 3. Complete quiz attempt in workspace so we have real learning events and quiz stats
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lessonId}`);
    await page.waitForLoadState("networkidle");

    const quizCard = page.locator(`[data-testid="quiz-activity-${quizActivityId}"]`);
    await expect(quizCard).toBeVisible();

    // Select answers
    await quizCard.locator('label:has-text("ship") input[type="radio"]').click();
    await quizCard.locator('label:has-text("/b/") input[type="radio"]').click();
    await quizCard.locator('label:has-text("Nhấn âm tiết 2") input[type="radio"]').click();
    await quizCard.locator('label:has-text("Luôn mang trọng âm chính") input[type="radio"]').click();

    // Submit quiz
    const submitBtn = quizCard.locator('[data-testid="quiz-submit-button"]');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();
    await expect(quizCard.locator('[data-testid="quiz-result-banner"]')).toBeVisible({ timeout: 10000 });


    // 4. Navigate to Progress page via visible desktop sidebar
    const progressNavLink = page.locator('aside a[href="/progress"]:visible');
    await expect(progressNavLink).toBeVisible();
    await progressNavLink.click();

    // 5. Verify URL and Page Header
    await expect(page).toHaveURL(/\/progress/);
    await expect(page.locator("h1:has-text('Tiến độ học tập')")).toBeVisible();

    // 6. Verify KPI Metric Cards
    const progressCard = page.locator('[data-testid="metric-progress"]');
    await expect(progressCard).toBeVisible();
    await expect(progressCard).toContainText("nhiệm vụ bắt buộc");

    const streakCard = page.locator('[data-testid="metric-streak"]');
    await expect(streakCard).toBeVisible();
    await expect(streakCard).toContainText("ngày liên tiếp");
    await expect(streakCard).toContainText("Kỷ lục:");

    const durationCard = page.locator('[data-testid="metric-duration"]');
    await expect(durationCard).toBeVisible();
    await expect(durationCard).toContainText("phút");

    const lessonsCard = page.locator('[data-testid="metric-lessons"]');
    await expect(lessonsCard).toBeVisible();

    // Capture Screenshot 1: Overview & KPI Cards
    await page.screenshot({
      path: path.join(screenshotsDir, "progress-desktop-overview.png"),
      fullPage: false,
    });

    // 7. Verify Weekly Progress Bar Chart
    const weeklyChart = page.locator('[data-testid="weekly-progress-chart"]');
    await expect(weeklyChart).toBeVisible();
    await expect(weeklyChart.locator("h3:has-text('Tiến độ Theo Tuần Lộ trình')")).toBeVisible();

    // Capture Screenshot 2: Weekly Progress Chart
    await weeklyChart.screenshot({
      path: path.join(screenshotsDir, "progress-desktop-weekly-chart.png"),
    });

    // 8. Verify Daily Activity Timeline
    const dailyTimeline = page.locator('[data-testid="daily-activity-timeline"]');
    await expect(dailyTimeline).toBeVisible();
    await expect(dailyTimeline.locator("h3:has-text('Lịch Sử Học Tập Theo Ngày Thực Tế')")).toBeVisible();

    // Capture Screenshot 3: Daily Activity Timeline
    await dailyTimeline.screenshot({
      path: path.join(screenshotsDir, "progress-desktop-daily-timeline.png"),
    });

    // 9. Test Refresh Button
    const refreshBtn = page.locator('[data-testid="refresh-analytics-button"]');
    await expect(refreshBtn).toBeVisible();
    await refreshBtn.click();
    await expect(refreshBtn).toContainText("Làm mới");
  });
});
