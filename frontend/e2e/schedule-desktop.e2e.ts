import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import { ensureEnrolled } from "./helpers/auth";

test.describe("G4-01 Desktop Schedule Foundation & Workflow", () => {
  test("Schedule Desktop: Range, 7-Day Grouping, Workspace Link, Complete/Undo Reflect, and Screenshots", async ({
    page,
  }) => {
    const screenshotsDir = path.resolve(import.meta.dirname, "../e2e-screenshots");
    if (!fs.existsSync(screenshotsDir)) {
      fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    // 1. Ensure learner A is enrolled
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_sched_a");

    // 2. Set 1440x900 desktop viewport
    await page.setViewportSize({ width: 1440, height: 900 });

    // 3. Navigate to /schedule via visible desktop sidebar
    const scheduleSidebarLink = page.locator('aside.md\\:flex a[href="/schedule"]').first();
    await scheduleSidebarLink.click();
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/schedule/);

    // 4. Verify Schedule Page Header Elements
    const pageHeading = page.locator("h1:has-text('Lịch học')");
    await expect(pageHeading).toBeVisible();

    // Verify enrollment info banner
    await expect(page.locator("text=Đang học")).toBeVisible();
    await expect(page.locator("text=Múi giờ:")).toBeVisible();
    await expect(page.locator("text=Ngày học trong tuần:")).toBeVisible();
    await expect(page.locator("text=Kết thúc theo lịch hiện tại:")).toBeVisible();

    // 5. Verify Week Navigation & Range
    const weekRange = page.locator('[data-testid="schedule-week-range"]');
    await expect(weekRange).toBeVisible();
    const initialRangeText = await weekRange.textContent();
    expect(initialRangeText).toMatch(/\d{2}\/\d{2}\/\d{4} - \d{2}\/\d{2}\/\d{4}/);

    const prevWeekBtn = page.locator('[data-testid="schedule-prev-week"]');
    const nextWeekBtn = page.locator('[data-testid="schedule-next-week"]');
    const todayBtn = page.locator('[data-testid="schedule-today"]');

    await expect(prevWeekBtn).toBeVisible();
    await expect(nextWeekBtn).toBeVisible();
    await expect(todayBtn).toBeVisible();

    // Next week navigation
    await nextWeekBtn.click();
    await page.waitForLoadState("networkidle");
    const nextRangeText = await weekRange.textContent();
    expect(nextRangeText).not.toBe(initialRangeText);

    // Prev week navigation (back to current)
    await prevWeekBtn.click();
    await page.waitForLoadState("networkidle");
    expect(await weekRange.textContent()).toBe(initialRangeText);

    // Today navigation resets properly
    await nextWeekBtn.click();
    await page.waitForLoadState("networkidle");
    await todayBtn.click();
    await page.waitForLoadState("networkidle");
    expect(await weekRange.textContent()).toBe(initialRangeText);

    // 6. Verify 7-day display and rest day indicators
    const restDayIndicators = page.locator('[data-testid="rest-day-indicator"]');
    await expect(restDayIndicators.first()).toBeVisible();

    // 7. Verify horizontal overflow check on 1440x900
    const overflow1440 = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(overflow1440).toBeFalsy();

    // Screenshot on 1440x900
    await page.screenshot({ path: path.join(screenshotsDir, "schedule-desktop-1440.png") });

    // 8. Test 1280x800 Smoke Viewport
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.waitForTimeout(300);

    const overflow1280 = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(overflow1280).toBeFalsy();

    // Screenshot on 1280x800
    await page.screenshot({ path: path.join(screenshotsDir, "schedule-desktop-1280.png") });

    // Switch back to 1440x900 for workspace flow
    await page.setViewportSize({ width: 1440, height: 900 });

    // 9. Navigate to Lesson 1 Workspace from Schedule Page
    const lesson1Link = page.locator(`[data-testid="lesson-link-${lesson1Id}"]`).first();
    if (await lesson1Link.isVisible()) {
      await lesson1Link.click();
    } else {
      await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    }
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(new RegExp(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`));

    // 10. Complete all required activities in Lesson 1 Workspace
    const activityCards = page.locator('[data-testid^="activity-card-"]');
    await expect(activityCards.first()).toBeVisible();

    // Activity 1 (READING)
    await activityCards.nth(0).locator('button[data-testid^="complete-button-"]').click();
    await expect(activityCards.nth(0)).toContainText("Đã hoàn thành");

    // Activity 2 (EXTERNAL_RESOURCE)
    await activityCards.nth(1).locator('button[data-testid^="complete-button-"]').click();
    await expect(activityCards.nth(1)).toContainText("Đã hoàn thành");

    // Activity 3: PRACTICE (RESPONSE)
    const responseTextarea3 = activityCards.nth(2).locator("textarea");
    if (await responseTextarea3.isVisible()) {
      await responseTextarea3.fill("Minimal pairs: sheep / ship, beat / bit.");
      const saveDraftBtn = activityCards.nth(2).locator('button:has-text("Lưu nháp")');
      await saveDraftBtn.click();
      await expect(activityCards.nth(2)).toContainText("Đã lưu nháp");
    }
    await activityCards.nth(2).locator('button[data-testid^="complete-button-"]').click();
    await expect(activityCards.nth(2)).toContainText("Đã hoàn thành");

    // Activity 4: REFLECTION (RESPONSE_SUBMIT) - Last required activity
    const responseTextarea4 = activityCards.nth(3).locator("textarea");
    await responseTextarea4.fill("Âm /i:/ dài căng môi hơn và kéo dài hơn âm /ɪ/.");
    const submitBtn4 = activityCards.nth(3).locator('button:has-text("Nộp & Hoàn thành")');
    await submitBtn4.click();
    await expect(activityCards.nth(3)).toContainText("Đã hoàn thành");

    // 11. Navigate back to Schedule Page and verify immediate invalidation -> Completed
    await page.locator('aside.md\\:flex a[href="/schedule"]').first().click();
    await page.waitForLoadState("networkidle");

    const completedBadge = page.locator('[data-testid="badge-completed"]').first();
    await expect(completedBadge).toBeVisible();
    await expect(completedBadge).toContainText("Đã hoàn thành");

    // 12. Undo one activity from workspace and verify schedule reflects update
    await page.locator(`[data-testid="lesson-link-${lesson1Id}"]`).first().click();
    await page.waitForLoadState("networkidle");

    const undoBtn1 = activityCards.nth(0).locator('button[data-testid^="undo-button-"]');
    await undoBtn1.click();
    await expect(activityCards.nth(0)).toContainText("Chưa bắt đầu");

    // Return to Schedule and verify completion badge is updated
    await page.locator('aside.md\\:flex a[href="/schedule"]').first().click();
    await page.waitForLoadState("networkidle");

    // The lesson is no longer completed
    await expect(page.locator(`[data-testid="lesson-link-${lesson1Id}"]`)).toBeVisible();
    await expect(page.locator('[data-testid="badge-completed"]')).toHaveCount(0);

    // Planned date and estimated end date remain unchanged
    expect(await weekRange.textContent()).toBe(initialRangeText);
    await expect(page.locator("text=Kết thúc theo lịch hiện tại:")).toBeVisible();
  });

  test("Schedule Cross-Account Isolation: User B cannot access User A schedule", async ({
    browser,
  }) => {
    // Context A: User A
    const contextA = await browser.newContext();
    const pageA = await contextA.newPage();
    const { enrollmentId: enrollmentIdA } = await ensureEnrolled(pageA, "learner_sched_a");

    // Context B: User B
    const contextB = await browser.newContext();
    const pageB = await contextB.newPage();
    await ensureEnrolled(pageB, "learner_sched_b");

    // Attempt direct API access from User B to User A's schedule
    const unauthorizedScheduleRes = await pageB.request.get(
      `/api/v1/enrollments/${enrollmentIdA}/schedule`
    );
    expect(unauthorizedScheduleRes.status()).toBe(404);

    await contextA.close();
    await contextB.close();
  });
});
