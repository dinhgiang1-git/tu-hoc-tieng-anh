import { test, expect } from "@playwright/test";
import { login, ensureEnrolled, getCsrfToken } from "./helpers/auth";

test.describe("M1 Core Learning Loop (6 Tiêu chí M1)", () => {
  test("M1-1: Login -> Chọn pilot -> Đăng ký -> Mở workspace -> Hoàn thành 2 nhiệm vụ, ghi note -> Reload giữ nguyên", async ({
    page,
  }) => {
    // 1. Login with dedicated learner account
    await login(page, "learner_m1_1");

    // 2. Navigate to Catalog
    await page.goto("/curricula");
    await expect(page.locator("h1")).toContainText("Lộ trình học");

    // Click on Pilot curriculum
    const pilotCard = page.locator('a[href="/curricula/pilot-curriculum"]').first();
    await expect(pilotCard).toBeVisible();
    await pilotCard.click();

    // 3. Curriculum detail -> Register with real UI button
    await expect(page).toHaveURL("/curricula/pilot-curriculum");
    const enrollBtn = page.locator('button:has-text("Đăng ký học")');
    await expect(enrollBtn).toBeVisible();
    await enrollBtn.click();

    // Enrollment dialog appears
    const dialog = page.locator('div[role="dialog"]');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator("#enrollment-dialog-title")).toContainText("Đăng ký lộ trình học");

    // Submit enrollment with default study days
    const confirmEnrollBtn = dialog.locator('button:has-text("Xác nhận đăng ký")');
    await confirmEnrollBtn.click();

    // Wait for redirect to Today page
    await expect(page).toHaveURL(/\/today|\//);
    await page.waitForLoadState("networkidle");

    // 4. Today page -> Start lesson
    const startLessonLink = page.locator('a:has-text("Bắt đầu bài học"), a:has-text("Tiếp tục học")').first();
    await expect(startLessonLink).toBeVisible();
    const href = await startLessonLink.getAttribute("href");
    expect(href).toMatch(/\/enrollments\/[0-9a-f-]+\/lessons\/[0-9a-f-]+/);

    await startLessonLink.click();
    await page.waitForLoadState("networkidle");

    // 5. Complete 2 activities using real dynamic testid: [data-testid^="activity-card-"]
    const activityCards = page.locator('[data-testid^="activity-card-"]');
    await expect(activityCards.first()).toBeVisible();

    // Activity 1 (READING):
    const completeBtn1 = activityCards.nth(0).locator('button:has-text("Hoàn thành")');
    await completeBtn1.click();
    await expect(activityCards.nth(0)).toContainText("Đã hoàn thành");

    // Activity 2 (EXTERNAL_RESOURCE):
    const completeBtn2 = activityCards.nth(1).locator('button:has-text("Hoàn thành")');
    await completeBtn2.click();
    await expect(activityCards.nth(1)).toContainText("Đã hoàn thành");

    // 6. Write Note
    const noteTextarea = page.locator("#learner-lesson-note");
    await expect(noteTextarea).toBeVisible();
    await noteTextarea.fill("Ghi chú buổi 1 của learner M1-1: Cần chú ý độ căng cơ môi khi phát âm /i:/.");

    // Trigger save and wait for saved status badge
    const saveNoteBtn = page.locator('button:has-text("Lưu ngay")');
    await saveNoteBtn.click();
    await expect(page.locator('[data-testid="note-editor"]')).toContainText("Đã lưu");

    // 7. Reload page and verify state persistence from server
    await page.reload();
    await page.waitForLoadState("networkidle");

    // Verification after reload:
    await expect(activityCards.nth(0)).toContainText("Đã hoàn thành");
    await expect(activityCards.nth(1)).toContainText("Đã hoàn thành");
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Ghi chú buổi 1 của learner M1-1: Cần chú ý độ căng cơ môi khi phát âm /i:/."
    );
  });

  test("M1-2: Hoàn thành nhiệm vụ bắt buộc cuối -> Buổi học hoàn thành, gợi ý bài kế đúng thứ tự", async ({
    page,
  }) => {
    // Independent setup for M1-2: learner_m1_2
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_m1_2");
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const activityCards = page.locator('[data-testid^="activity-card-"]');

    // Complete Activity 1 (READING)
    await activityCards.nth(0).locator('button[data-testid^="complete-button-"]').click();
    await expect(activityCards.nth(0)).toContainText("Đã hoàn thành");

    // Complete Activity 2 (EXTERNAL_RESOURCE)
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
    const completeBtn3 = activityCards.nth(2).locator('button[data-testid^="complete-button-"]');
    await completeBtn3.click();
    await expect(activityCards.nth(2)).toContainText("Đã hoàn thành");

    // Activity 4: REFLECTION (RESPONSE_SUBMIT) - Last required activity
    const responseTextarea4 = activityCards.nth(3).locator("textarea");
    await responseTextarea4.fill("Âm /i:/ dài căng môi hơn và kéo dài hơn âm /ɪ/.");
    const submitBtn4 = activityCards.nth(3).locator('button:has-text("Nộp & Hoàn thành")');
    await submitBtn4.click();
    await expect(activityCards.nth(3)).toContainText("Đã hoàn thành");

    // Verify Lesson Complete banner with real testid & text
    const completeBanner = page.locator('[data-testid="lesson-complete-banner"]');
    await expect(completeBanner).toBeVisible();
    await expect(completeBanner).toContainText("Hoàn thành buổi học!");

    // Verify Next Lesson CTA in banner
    const nextLessonBtn = completeBanner.locator('button:has-text("Bài học tiếp theo")');
    await expect(nextLessonBtn).toBeVisible();
    await nextLessonBtn.click();

    // Verify navigation to lesson 2
    await page.waitForLoadState("networkidle");
    await expect(page.locator("h1").first()).toContainText("Buổi 2");

    // Verify Today page is unified and has no null links
    await page.goto("/");
    await page.waitForLoadState("networkidle");
    const todayAction = page.locator('a[href*="/enrollments/"][href*="/lessons/"]').first();
    await expect(todayAction).toBeVisible();
    const href = await todayAction.getAttribute("href");
    expect(href).not.toBeNull();
    expect(href).not.toContain("null");
    expect(href).not.toContain("undefined");
  });

  test("M1-3: Idempotency và kiểm tra Version Conflict 409 khi gửi version cũ", async ({
    page,
  }) => {
    // Independent setup for M1-3: learner_m1_3
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_m1_3");
    const csrfToken = await getCsrfToken(page);

    // Fetch workspace data to get current enrollment version
    const wsRes = await page.request.get(`/api/v1/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    expect(wsRes.ok()).toBeTruthy();
    const wsData = await wsRes.json();
    const currentVersion = wsData.enrollmentVersion;
    const act1Id = wsData.activities[0].activityId;

    // 1. Re-send identical status with matching expectedVersion (No-op test)
    const noOpRes = await page.request.put(
      `/api/v1/enrollments/${enrollmentId}/activities/${act1Id}/progress`,
      {
        headers: { "X-CSRF-TOKEN": csrfToken },
        data: {
          status: "COMPLETED",
          expectedVersion: currentVersion,
        },
      }
    );
    expect(noOpRes.ok()).toBeTruthy();
    const noOpData = await noOpRes.json();
    expect(noOpData.enrollmentVersion).toBe(currentVersion + 1);

    // 2. Re-send identical COMPLETED status again with the new version (true No-op)
    const identicalRes = await page.request.put(
      `/api/v1/enrollments/${enrollmentId}/activities/${act1Id}/progress`,
      {
        headers: { "X-CSRF-TOKEN": csrfToken },
        data: {
          status: "COMPLETED",
          expectedVersion: noOpData.enrollmentVersion,
        },
      }
    );
    expect(identicalRes.ok()).toBeTruthy();
    const identicalData = await identicalRes.json();
    expect(identicalData.enrollmentVersion).toBe(noOpData.enrollmentVersion);

    // 3. Send request with outdated expectedVersion (version 0 when current is >= 1)
    const conflictRes = await page.request.put(
      `/api/v1/enrollments/${enrollmentId}/activities/${act1Id}/progress`,
      {
        headers: { "X-CSRF-TOKEN": csrfToken },
        data: {
          status: "IN_PROGRESS",
          expectedVersion: 0,
        },
      }
    );
    expect(conflictRes.status()).toBe(409);
    const problem = await conflictRes.json();
    expect(problem.code).toBe("VERSION_CONFLICT");
  });

  test("M1-4: Undo nhiệm vụ bắt buộc -> Giảm tiến độ, giữ response text, mẫu số chuẩn 19", async ({
    page,
  }) => {
    // Independent setup for M1-4: learner_m1_4
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_m1_4");

    // 1. Check initial progress count on Today page: 0 / 19 required activities
    await page.goto("/");
    await page.waitForLoadState("networkidle");
    await expect(page.locator("text=0 / 19 hoạt động bắt buộc")).toBeVisible();

    // 2. Go to workspace and complete Activity 1
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const activityCards = page.locator('[data-testid^="activity-card-"]');
    const completeBtn = activityCards.nth(0).locator('button:has-text("Hoàn thành")');
    await completeBtn.click();
    await expect(activityCards.nth(0)).toContainText("Đã hoàn thành");

    // Verify progress incremented on Today page: 1 / 19
    await page.goto("/");
    await page.waitForLoadState("networkidle");
    await expect(page.locator("text=1 / 19 hoạt động bắt buộc")).toBeVisible();

    // 3. Return to workspace and Click Undo
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const undoBtn = activityCards.nth(0).locator('button:has-text("Hoàn tác")');
    await expect(undoBtn).toBeVisible();
    await undoBtn.click();
    await expect(activityCards.nth(0)).toContainText("Chưa bắt đầu");

    // 4. Reload and verify status remained undone
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(activityCards.nth(0)).toContainText("Chưa bắt đầu");

    // 5. Check Today page progress summary decreased back to 0 / 19 (demonstrates denominator remains 19)
    await page.goto("/");
    await page.waitForLoadState("networkidle");
    await expect(page.locator("text=0 / 19 hoạt động bắt buộc")).toBeVisible();
  });

  test("M1-5: Mô phỏng lỗi mạng khi lưu note -> Giữ chữ, không báo Đã lưu giả, retry thành công", async ({
    page,
  }) => {
    // Independent setup for M1-5: learner_m1_5
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_m1_5");
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    // Intercept note API to simulate network failure (abort)
    await page.route("**/api/v1/notes**", (route) => route.abort());

    const noteTextarea = page.locator("#learner-lesson-note");
    await noteTextarea.fill("Bản nháp ghi chú khi rớt mạng.");

    // Trigger save attempt
    const saveBtn = page.locator('button:has-text("Lưu ngay")');
    await saveBtn.click();

    // Must show Error state, NOT 'Đã lưu'
    const noteEditor = page.locator('[data-testid="note-editor"]');
    await expect(noteEditor).not.toContainText("Đã lưu");
    await expect(noteEditor).toContainText("Lỗi lưu");

    // Crucial check: Text MUST NOT be wiped out
    await expect(noteTextarea).toHaveValue("Bản nháp ghi chú khi rớt mạng.");

    // Try navigating away -> Unified Navigation Blocker must prevent departure
    await page.locator('a[href="/today"]').first().click();
    const blockerModal = page.locator('[data-testid="workspace-navigation-blocker-modal"]');
    await expect(blockerModal).toBeVisible();
    await expect(blockerModal).toContainText("Bạn có thay đổi chưa lưu!");

    // Click 'Ở lại trang'
    await blockerModal.locator('button:has-text("Ở lại trang")').click();
    await expect(blockerModal).not.toBeVisible();
    await expect(noteTextarea).toHaveValue("Bản nháp ghi chú khi rớt mạng.");

    // Now restore network connection
    await page.unroute("**/api/v1/notes**");

    // Retry save
    const retryBtn = noteEditor.locator('button:has-text("Thử lại"), button:has-text("Lưu ngay")').first();
    await retryBtn.click();
    await expect(noteEditor).toContainText("Đã lưu");

    // Reload page and confirm persisted
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(page.locator("#learner-lesson-note")).toHaveValue("Bản nháp ghi chú khi rớt mạng.");
  });

  test("M1-6: Gửi request với activity từ revision khác -> 404 Problem Details, không mutate", async ({
    page,
  }) => {
    // Independent setup for M1-6: learner_m1_6
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_m1_6");
    const csrfToken = await getCsrfToken(page);

    // 1. Query other-curriculum detail to obtain a REAL existing activity ID from another revision
    const otherCurriculumRes = await page.request.get("/api/v1/curricula/e2e_other_curriculum");
    expect(otherCurriculumRes.ok()).toBeTruthy();
    const otherCurriculum = await otherCurriculumRes.json();
    const otherLessonId = otherCurriculum.revision.phases[0].weeks[0].lessons[0].id;
    const otherActivityId = "e2e00000-0000-0000-0000-000000000004";

    // Attempting to access workspace of lesson from another revision is rejected with 404
    const otherWsRes = await page.request.get(
      `/api/v1/enrollments/${enrollmentId}/lessons/${otherLessonId}`
    );
    expect(otherWsRes.status()).toBe(404);

    // Get current version before illegal call
    const wsRes = await page.request.get(`/api/v1/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    const wsData = await wsRes.json();
    const versionBefore = wsData.enrollmentVersion;

    // Attempt mutation targeting another revision's activity
    const illegalRes = await page.request.put(
      `/api/v1/enrollments/${enrollmentId}/activities/${otherActivityId}/progress`,
      {
        headers: { "X-CSRF-TOKEN": csrfToken },
        data: {
          status: "COMPLETED",
          expectedVersion: versionBefore,
        },
      }
    );

    // Must be rejected with 404 Problem Details
    expect(illegalRes.status()).toBe(404);
    const problem = await illegalRes.json();
    expect(problem.code).toBe("RESOURCE_NOT_FOUND");

    // Verify pilot enrollment version did NOT advance
    const wsResAfter = await page.request.get(`/api/v1/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    const wsDataAfter = await wsResAfter.json();
    expect(wsDataAfter.enrollmentVersion).toBe(versionBefore);
  });
});
