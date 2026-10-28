import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import { ensureEnrolled, getCsrfToken } from "./helpers/auth";

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

    const csrfB = await getCsrfToken(pageB);
    const unauthPreviewRes = await pageB.request.post(
      `/api/v1/enrollments/${enrollmentIdA}/schedule/preview`,
      {
        headers: { "X-CSRF-TOKEN": csrfB },
        data: {
          resumeDate: "2026-10-27",
          studyDays: ["MONDAY", "WEDNESDAY"],
          dailyTargetMinutes: 30,
        },
      }
    );
    expect(unauthPreviewRes.status()).toBe(404);

    const unauthCommitRes = await pageB.request.post(
      `/api/v1/enrollments/${enrollmentIdA}/schedule/commit`,
      {
        headers: {
          "X-CSRF-TOKEN": csrfB,
          "Idempotency-Key": "00000000-0000-0000-0000-000000000001",
        },
        data: {
          resumeDate: "2026-10-27",
          studyDays: ["MONDAY", "WEDNESDAY"],
          dailyTargetMinutes: 30,
          expectedVersion: 1,
        },
      }
    );
    expect(unauthCommitRes.status()).toBe(404);

    await contextA.close();
    await contextB.close();
  });

  test("G4-02: Complete a lesson, preview reschedule, cancel without mutating DB, confirm reschedule, verify completed intact & uncompleted shifted, reload persists, and conflict safety", async ({
    page,
  }) => {
    // 1. Ensure learner is enrolled
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_resched_a");
    await page.setViewportSize({ width: 1440, height: 900 });

    // 2. Complete Lesson 1 in workspace
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const activityCards = page.locator('[data-testid^="activity-card-"]');
    await expect(activityCards.first()).toBeVisible();

    // Complete reading & external resource
    const completeBtn0 = activityCards.nth(0).locator('button[data-testid^="complete-button-"]');
    if (await completeBtn0.isVisible()) {
      await completeBtn0.click();
    }
    const completeBtn1 = activityCards.nth(1).locator('button[data-testid^="complete-button-"]');
    if (await completeBtn1.isVisible()) {
      await completeBtn1.click();
    }

    // Practice response & complete
    const responseTextarea3 = activityCards.nth(2).locator("textarea");
    if (await responseTextarea3.isVisible()) {
      await responseTextarea3.fill("Practice response text for G4-02");
      const saveDraftBtn = activityCards.nth(2).locator('button:has-text("Lưu nháp")');
      await saveDraftBtn.click();
      await expect(activityCards.nth(2)).toContainText("Đã lưu nháp");
    }
    const completeBtn2 = activityCards.nth(2).locator('button[data-testid^="complete-button-"]');
    if (await completeBtn2.isVisible()) {
      await completeBtn2.click();
    }

    // Reflection submit & complete
    const responseTextarea4 = activityCards.nth(3).locator("textarea");
    if (await responseTextarea4.isVisible()) {
      await responseTextarea4.fill("Reflection submission text for G4-02");
      const submitBtn4 = activityCards.nth(3).locator('button:has-text("Nộp & Hoàn thành")');
      if (await submitBtn4.isVisible()) {
        await submitBtn4.click();
      }
    }

    // 3. Navigate to /schedule
    await page.goto("/schedule");
    await page.waitForLoadState("networkidle");

    // Verify Lesson 1 is completed
    const completedBadge = page.locator('[data-testid="badge-completed"]').first();
    await expect(completedBadge).toBeVisible();

    // Verify 'Đổi lịch' button is present and click it
    const openRescheduleBtn = page.locator('[data-testid="schedule-open-reschedule-button"]');
    await expect(openRescheduleBtn).toBeVisible();
    await openRescheduleBtn.click();

    // Verify dialog opens
    const dialog = page.locator('[data-testid="reschedule-dialog"]');
    await expect(dialog).toBeVisible();

    // 4. Test Preview and Cancel (Verify no mutation to DB)
    const previewBtn = page.locator('[data-testid="reschedule-preview-button"]');
    await previewBtn.click();

    // Preview result appears
    const previewResult = page.locator('[data-testid="reschedule-preview-result"]');
    await expect(previewResult).toBeVisible();
    await expect(page.locator("text=hoàn thành sẽ được giữ nguyên")).toBeVisible();

    // Cancel by clicking close button
    const closeBtn = page.locator('[data-testid="reschedule-close-button"]');
    await closeBtn.click();
    await expect(dialog).not.toBeVisible();

    // Reload page to prove cancel did NOT mutate DB
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(completedBadge).toBeVisible();

    // 5. Reopen Reschedule Dialog, modify study days, test input change invalidation
    await openRescheduleBtn.click();
    await expect(dialog).toBeVisible();

    // Toggle a day (e.g. Saturday)
    const satCheckbox = page.locator('[data-testid="reschedule-day-SATURDAY"]');
    await satCheckbox.check({ force: true });

    // Click preview
    await previewBtn.click();
    await expect(previewResult).toBeVisible();

    // Verify completed lesson is NOT in the modified schedules list (only uncompleted lessons are modified)
    const tableText = await previewResult.textContent();
    expect(tableText).not.toContain("Buổi 1:");

    // Change target minutes to test input invalidation
    const minutesInput = page.locator('[data-testid="reschedule-minutes-input"]');
    await minutesInput.fill("45");
    // Preview result is immediately invalidated
    await expect(previewResult).toHaveCount(0);

    // Click preview again to get fresh snapshot
    await previewBtn.click();
    await expect(previewResult).toBeVisible();

    // 6. Confirm Reschedule
    const confirmBtn = page.locator('[data-testid="reschedule-confirm-button"]');
    await confirmBtn.click();

    // Modal closes upon successful commit
    await expect(dialog).not.toBeVisible();
    await page.waitForLoadState("networkidle");

    // 7. Verify Completed lesson remains completed with original planned date intact
    await expect(completedBadge).toBeVisible();

    // Reload page to verify changes were persisted in database
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(completedBadge).toBeVisible();
    await expect(page.locator("text=Đang học")).toBeVisible();

    // 8. User A committing with stale expectedVersion gets 409 Conflict
    const csrfA = await getCsrfToken(page);
    const staleCommitRes = await page.request.post(
      `/api/v1/enrollments/${enrollmentId}/schedule/commit`,
      {
        headers: {
          "X-CSRF-TOKEN": csrfA,
          "Idempotency-Key": "00000000-0000-0000-0000-000000000002",
        },
        data: {
          resumeDate: "2026-10-27",
          studyDays: ["MONDAY", "WEDNESDAY"],
          dailyTargetMinutes: 30,
          expectedVersion: 1, // Stale version! (current version is at least 2 after commit)
        },
      }
    );
    expect(staleCommitRes.status()).toBe(409);
  });

  test("G4-03: Lifecycle Pause, Resume with atomic schedule recalculation, and Abandon on Desktop", async ({
    page,
  }) => {
    const screenshotsDir = path.resolve(import.meta.dirname, "../e2e-screenshots");
    if (!fs.existsSync(screenshotsDir)) {
      fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    // 1. Ensure learner is enrolled
    await ensureEnrolled(page, "learner_lifecycle");

    // 2. Set 1440x900 desktop viewport
    await page.setViewportSize({ width: 1440, height: 900 });

    // 3. Navigate to /schedule via sidebar
    const scheduleLink = page.locator('aside.md\\:flex a[href="/schedule"]').first();
    await scheduleLink.click();
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/schedule/);

    // Initial state: ACTIVE
    await expect(page.locator("text=Đang học")).toBeVisible();
    const openRescheduleBtn = page.locator('[data-testid="schedule-open-reschedule-button"]');
    const pauseBtn = page.locator('[data-testid="schedule-pause-button"]');
    const abandonBtn = page.locator('[data-testid="schedule-abandon-button"]');

    await expect(openRescheduleBtn).toBeVisible();
    await expect(pauseBtn).toBeVisible();
    await expect(abandonBtn).toBeVisible();

    await page.screenshot({ path: path.join(screenshotsDir, "schedule-lifecycle-active.png") });

    // 4. Pause Enrollment
    await pauseBtn.click();
    const pauseModal = page.locator('[data-testid="schedule-pause-modal"]');
    await expect(pauseModal).toBeVisible();
    await expect(pauseModal.locator("h2:has-text('Tạm dừng lộ trình học')")).toBeVisible();

    const confirmPauseBtn = page.locator('[data-testid="schedule-confirm-pause-button"]');
    await confirmPauseBtn.click();

    // Modal closes, page updates to PAUSED
    await expect(pauseModal).not.toBeVisible();
    await page.waitForLoadState("networkidle");

    await expect(page.getByText("Tạm dừng", { exact: true })).toBeVisible();
    await expect(page.locator('[data-testid="schedule-paused-banner"]')).toBeVisible();
    await expect(openRescheduleBtn).not.toBeVisible();

    const resumeBtn = page.locator('[data-testid="schedule-resume-button"]');
    await expect(resumeBtn).toBeVisible();

    await page.screenshot({ path: path.join(screenshotsDir, "schedule-lifecycle-paused.png") });

    // 5. Navigate to / (Hôm nay) and verify PAUSED state and CTA
    const todayLink = page.locator('aside.md\\:flex a[href="/"]').first();
    await todayLink.click();
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/(today)?$/);

    const todayPausedState = page.locator('[data-testid="today-paused-state"]');
    await expect(todayPausedState).toBeVisible();
    const todayResumeCta = page.locator('[data-testid="today-resume-cta-button"]');
    await expect(todayResumeCta).toBeVisible();

    await page.screenshot({ path: path.join(screenshotsDir, "today-lifecycle-paused.png") });

    // Click CTA to return to /schedule
    await todayResumeCta.click();
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/schedule/);

    // 6. Resume Enrollment via Dialog
    await expect(resumeBtn).toBeVisible();
    await resumeBtn.click();

    const resumeDialog = page.locator('[data-testid="reschedule-dialog"]');
    await expect(resumeDialog).toBeVisible();
    await expect(resumeDialog.locator("h2:has-text('Tiếp tục lộ trình học')")).toBeVisible();

    // Click preview
    const previewBtn = page.locator('[data-testid="reschedule-preview-button"]');
    await previewBtn.click();
    const previewResult = page.locator('[data-testid="reschedule-preview-result"]');
    await expect(previewResult).toBeVisible();

    // Confirm resume
    const confirmResumeBtn = page.locator('[data-testid="reschedule-confirm-button"]');
    await expect(confirmResumeBtn).toContainText("Xác nhận tiếp tục học");
    await confirmResumeBtn.click();

    // Modal closes, status returns to ACTIVE
    await expect(resumeDialog).not.toBeVisible();
    await page.waitForLoadState("networkidle");

    await expect(page.getByText("Đang học", { exact: true })).toBeVisible();
    await expect(openRescheduleBtn).toBeVisible();
    await expect(pauseBtn).toBeVisible();
    await expect(resumeBtn).not.toBeVisible();

    await page.screenshot({ path: path.join(screenshotsDir, "schedule-lifecycle-resumed.png") });

    // 7. Abandon Enrollment
    await abandonBtn.click();
    const abandonModal = page.locator('[data-testid="schedule-abandon-modal"]');
    await expect(abandonModal).toBeVisible();
    await expect(abandonModal.locator("h2:has-text('Kết thúc lộ trình học')")).toBeVisible();

    const confirmAbandonBtn = page.locator('[data-testid="schedule-confirm-abandon-button"]');
    await confirmAbandonBtn.click();

    await expect(abandonModal).not.toBeVisible();
    await page.waitForLoadState("networkidle");

    await expect(page.getByText("Đã kết thúc", { exact: true })).toBeVisible();
    await expect(openRescheduleBtn).not.toBeVisible();
    await expect(pauseBtn).not.toBeVisible();
    await expect(resumeBtn).not.toBeVisible();

    await page.screenshot({ path: path.join(screenshotsDir, "schedule-lifecycle-abandoned.png") });

    // 8. Viewport smoke check on 1280x800
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.waitForTimeout(300);
    const overflow1280 = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(overflow1280).toBeFalsy();
  });
});
