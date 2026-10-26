import { test, expect } from "@playwright/test";
import { login, logout, ensureEnrolled, getCsrfToken } from "./helpers/auth";

test.describe("Multi-account Isolation & Multi-tab Synchronization (Sections 5.1 & 5.2)", () => {
  test("5.1 Isolation: Contexts A/B độc lập; User B không truy cập/xóa được dữ liệu User A; CSRF bảo vệ", async ({
    browser,
  }) => {
    // 1. Context A - Learner A
    const contextA = await browser.newContext();
    const pageA = await contextA.newPage();
    const { enrollmentId: enrollmentIdA, lesson1Id: lessonIdA } = await ensureEnrolled(
      pageA,
      "learner_iso_a"
    );

    // Learner A creates a note
    const csrfA = await getCsrfToken(pageA);
    const createNoteResA = await pageA.request.post("/api/v1/notes", {
      headers: { "X-CSRF-TOKEN": csrfA },
      data: {
        enrollmentId: enrollmentIdA,
        lessonId: lessonIdA,
        content: "Ghi chú bí mật của Học viên A",
      },
    });
    expect(createNoteResA.status()).toBe(201);
    const noteDataA = await createNoteResA.json();
    const noteIdA = noteDataA.id;

    // 2. Context B - Learner B
    const contextB = await browser.newContext();
    const pageB = await contextB.newPage();
    await ensureEnrolled(pageB, "learner_iso_b");

    // Attempt 1: Learner B attempts to query Learner A's workspace
    const illegalWsRes = await pageB.request.get(
      `/api/v1/enrollments/${enrollmentIdA}/lessons/${lessonIdA}`
    );
    expect(illegalWsRes.status()).toBe(404);

    // Attempt 2: Learner B attempts to delete Learner A's note
    const csrfB = await getCsrfToken(pageB);
    const illegalDeleteRes = await pageB.request.delete(
      `/api/v1/notes/${noteIdA}?expectedVersion=0`,
      {
        headers: { "X-CSRF-TOKEN": csrfB },
      }
    );
    expect(illegalDeleteRes.status()).toBe(404);

    // 3. CSRF Protection Check: Mutation without CSRF token must be 403 CSRF_INVALID
    const noCsrfRes = await pageA.request.post("/api/v1/notes", {
      data: {
        enrollmentId: enrollmentIdA,
        lessonId: lessonIdA,
        content: "Attempt without CSRF",
      },
    });
    expect(noCsrfRes.status()).toBe(403);
    const problem = await noCsrfRes.json();
    expect(problem.code).toBe("CSRF_INVALID");

    await contextA.close();
    await contextB.close();
  });

  test("5.1 Context Switching: Logout A -> Login B không rò rỉ cache/draft -> Login lại A dữ liệu còn nguyên", async ({
    page,
  }) => {
    // 1. Learner A logs in and writes note
    const { enrollmentId: idA, lesson1Id: lA } = await ensureEnrolled(page, "learner_ctx_a");
    await page.goto(`/enrollments/${idA}/lessons/${lA}`);
    await page.waitForLoadState("networkidle");

    const noteTextarea = page.locator("#learner-lesson-note");
    await expect(noteTextarea).toBeVisible();
    await noteTextarea.fill("Dữ liệu độc quyền của A trong phiên.");
    await page.locator('button:has-text("Lưu ngay")').click();
    await expect(page.locator('[data-testid="note-editor"]')).toContainText("Đã lưu");

    // 2. Logout Learner A using UI logout button
    await logout(page);

    // 3. Login Learner B in same browser context
    const { enrollmentId: idB, lesson1Id: lB } = await ensureEnrolled(page, "learner_ctx_b");
    await page.goto(`/enrollments/${idB}/lessons/${lB}`);
    await page.waitForLoadState("networkidle");

    // Check Learner B's note editor: MUST NOT contain Learner A's text!
    const noteTextareaB = page.locator("#learner-lesson-note");
    await expect(noteTextareaB).not.toHaveValue("Dữ liệu độc quyền của A trong phiên.");

    // 4. Logout Learner B and re-login Learner A
    await logout(page);
    await login(page, "learner_ctx_a");
    await page.goto(`/enrollments/${idA}/lessons/${lA}`);
    await page.waitForLoadState("networkidle");

    // Learner A's note must remain completely intact
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Dữ liệu độc quyền của A trong phiên."
    );
  });

  test("5.2 Two Tabs Activity Sync: Tab 1 complete -> Tab 2 xung đột 409, giữ draft, đồng bộ phục hồi", async ({
    context,
  }) => {
    // Tab 1 setup
    const tab1 = await context.newPage();
    const { enrollmentId, lesson1Id } = await ensureEnrolled(tab1, "learner_tabs_act");
    const workspaceUrl = `/enrollments/${enrollmentId}/lessons/${lesson1Id}`;

    await tab1.goto(workspaceUrl);
    await tab1.waitForLoadState("networkidle");

    // Tab 2 opens the exact same workspace URL
    const tab2 = await context.newPage();
    await tab2.goto(workspaceUrl);
    await tab2.waitForLoadState("networkidle");

    // In Tab 2: type draft in Activity 4 (REFLECTION) but do NOT submit yet
    const activityCardsTab2 = tab2.locator('[data-testid^="activity-card-"]');
    const responseTextareaTab2 = activityCardsTab2.nth(3).locator("textarea");
    await responseTextareaTab2.fill("Câu trả lời đang gõ ở Tab 2.");

    // In Tab 1: complete Activity 1 (increases enrollment version on server)
    const activityCardsTab1 = tab1.locator('[data-testid^="activity-card-"]');
    const completeBtn1 = activityCardsTab1.nth(0).locator('button:has-text("Hoàn thành")');
    await completeBtn1.click();
    await expect(activityCardsTab1.nth(0)).toContainText("Đã hoàn thành");

    // In Tab 2: now try to submit Activity 4 (has outdated expectedVersion)
    const submitBtnTab2 = activityCardsTab2.nth(3).locator('button:has-text("Nộp & Hoàn thành")');
    await submitBtnTab2.click();

    // Tab 2 must receive 409 Conflict notification
    await expect(
      activityCardsTab2.nth(3).locator("text=Phiên bản tiến độ đã cũ")
    ).toBeVisible();

    // CRITICAL: Draft text in Tab 2 MUST be preserved
    await expect(responseTextareaTab2).toHaveValue("Câu trả lời đang gõ ở Tab 2.");

    // Tab 2 shows 'Đồng bộ tiến độ' button
    const syncBtn = activityCardsTab2.nth(3).locator('button:has-text("Đồng bộ tiến độ")');
    await expect(syncBtn).toBeVisible();
    await syncBtn.click();

    // After sync, draft is STILL preserved, and user can re-submit
    await expect(responseTextareaTab2).toHaveValue("Câu trả lời đang gõ ở Tab 2.");
    await submitBtnTab2.click();
    await expect(activityCardsTab2.nth(3)).toContainText("Đã hoàn thành");

    await tab1.close();
    await tab2.close();
  });

  test("5.2 Two Tabs Note CAS: Tab 1 save -> Tab 2 xung đột 409, latch autosave, ghi đè an toàn", async ({
    context,
  }) => {
    const tab1 = await context.newPage();
    const { enrollmentId, lesson1Id } = await ensureEnrolled(tab1, "learner_tabs_note");
    const workspaceUrl = `/enrollments/${enrollmentId}/lessons/${lesson1Id}`;

    await tab1.goto(workspaceUrl);
    await tab1.waitForLoadState("networkidle");

    // In Tab 1: Create initial baseline note so that a persistent versioned note exists
    const note1 = tab1.locator("#learner-lesson-note");
    await note1.fill("Ghi chú ban đầu phiên bản 0");
    await tab1.locator('button:has-text("Lưu ngay")').click();
    await expect(tab1.locator('[data-testid="note-editor"]')).toContainText("Đã lưu");

    // In Tab 2: Open workspace to load the existing baseline note (version 0)
    const tab2 = await context.newPage();
    await tab2.goto(workspaceUrl);
    await tab2.waitForLoadState("networkidle");
    const note2 = tab2.locator("#learner-lesson-note");
    await expect(note2).toHaveValue("Ghi chú ban đầu phiên bản 0");

    // In Tab 1: Update note -> advances version from 0 to 1
    await note1.fill("Nội dung ghi chú từ Tab 1");
    await tab1.locator('button:has-text("Lưu ngay")').click();
    await expect(tab1.locator('[data-testid="note-editor"]')).toContainText("Đã lưu");

    // In Tab 2: Edit note and try to save with outdated expectedVersion 0
    await note2.fill("Nội dung ghi chú từ Tab 2 bị xung đột");
    await tab2.locator('button:has-text("Lưu ngay")').click();

    // Tab 2 receives 409 conflict
    const noteEditor2 = tab2.locator('[data-testid="note-editor"]');
    await expect(noteEditor2).toContainText("Xung đột phiên bản");
    await expect(note2).toHaveValue("Nội dung ghi chú từ Tab 2 bị xung đột");

    // Tab 2 chooses "Ghi đè bằng bản này"
    const overwriteBtn = noteEditor2.locator('button:has-text("Ghi đè bằng bản này")');
    await expect(overwriteBtn).toBeVisible();
    await overwriteBtn.click();

    // Overwrite succeeds and reaches 'Đã lưu'
    await expect(noteEditor2).toContainText("Đã lưu");
    await expect(note2).toHaveValue("Nội dung ghi chú từ Tab 2 bị xung đột");

    await tab1.close();
    await tab2.close();
  });
});
