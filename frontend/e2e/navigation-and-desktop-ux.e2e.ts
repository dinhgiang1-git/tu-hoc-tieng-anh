import { test, expect } from "@playwright/test";
import { ensureEnrolled } from "./helpers/auth";
import * as fs from "fs";
import * as path from "path";

test.describe("Navigation Guards & Desktop UX (Sections 5.3 & 5.4)", () => {
  test("5.3 Autosave Serialization: A in-flight có kiểm soát -> gõ B -> chỉ gửi tuần tự, reload thấy B", async ({
    page,
  }) => {
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_autosave");
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const noteTextarea = page.locator("#learner-lesson-note");
    await expect(noteTextarea).toBeVisible();

    let releaseA!: () => void;
    let observeA!: () => void;
    const holdA = new Promise<void>(resolve => { releaseA = resolve; });
    const startedA = new Promise<void>(resolve => { observeA = resolve; });
    const writes: { content: string; expectedVersion?: number; serverVersion?: number }[] = [];
    let active = 0;
    let maxActive = 0;
    await page.route("**/api/v1/notes**", async route => {
      if (!["POST", "PATCH"].includes(route.request().method())) return route.continue();
      const body = route.request().postDataJSON();
      const write = { ...body, serverVersion: undefined as number | undefined };
      writes.push(write);
      active++;
      maxActive = Math.max(maxActive, active);
      const response = await route.fetch();
      expect(response.ok()).toBeTruthy();
      write.serverVersion = (await response.json()).version;
      if (writes.length === 1) { observeA(); await holdA; }
      active--;
      await route.fulfill({ response });
    });
    await noteTextarea.fill("Nội dung A đang gửi...");
    await page.locator('button:has-text("Lưu ngay")').click();
    await startedA;
    await noteTextarea.fill("Nội dung B cuối cùng được xác nhận");
    await expect(page.getByTestId("note-editor")).not.toContainText("Đã lưu");
    releaseA();
    await expect(page.getByTestId("note-editor")).toContainText("Đã lưu");
    expect(writes).toHaveLength(2);
    expect(maxActive).toBe(1);
    expect(writes[1].expectedVersion).toBe(writes[0].serverVersion);
    await page.unroute("**/api/v1/notes**");

    // 3. Reload page and verify B is persisted from server
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Nội dung B cuối cùng được xác nhận"
    );
  });

  test("5.3 Focus Mode: Bật/thoát Focus Mode bằng phím Escape -> Ghi chú và câu trả lời còn nguyên", async ({
    page,
  }) => {
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_focus");
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    // Write note and response
    const noteTextarea = page.locator("#learner-lesson-note");
    await noteTextarea.fill("Ghi chú trước khi bật Focus Mode.");

    const activityCards = page.locator('[data-testid^="activity-card-"]');
    const responseTextarea = activityCards.nth(2).locator("textarea");
    if (await responseTextarea.isVisible()) {
      await responseTextarea.fill("Câu trả lời draft trước khi bật Focus Mode.");
    }

    // Toggle Focus Mode
    const focusToggleBtn = page
      .locator('button:has-text("Chế độ tập trung"), button[title*="tập trung"]')
      .first();
    await focusToggleBtn.click();

    // In Focus Mode: Header/sidebar is hidden, exit focus button is present
    await expect(
      page.locator('button:has-text("Thoát tập trung"), button[title*="Thoát"]')
    ).toBeVisible();

    // Note and draft must be preserved inside Focus Mode
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Ghi chú trước khi bật Focus Mode."
    );
    if (await responseTextarea.isVisible()) {
      await expect(responseTextarea).toHaveValue("Câu trả lời draft trước khi bật Focus Mode.");
    }

    // Press Escape to exit Focus Mode
    await page.keyboard.press("Escape");

    // Verify exited Focus mode and content is still intact
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Ghi chú trước khi bật Focus Mode."
    );
    if (await responseTextarea.isVisible()) {
      await expect(responseTextarea).toHaveValue("Câu trả lời draft trước khi bật Focus Mode.");
    }
  });

  test("5.3 Unified Navigation Blocker: 'Ở lại trang', 'Rời khỏi không lưu', 'Lưu rồi rời'", async ({
    page,
  }) => {
    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_blocker");
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");

    const noteTextarea = page.locator("#learner-lesson-note");
    await noteTextarea.fill("Thay đổi chưa được lưu để test guard điều hướng.");

    // Action 1: Click 'Hôm nay' link -> Blocker modal triggers
    await page.locator('a[href="/today"]').first().click();
    const blockerModal = page.locator('[data-testid="workspace-navigation-blocker-modal"]');
    await expect(blockerModal).toBeVisible();
    await expect(blockerModal.locator("#blocker-dialog-title")).toContainText(
      "Bạn có thay đổi chưa lưu!"
    );

    await expect(blockerModal.getByRole("button", { name: "Ở lại trang" })).toBeFocused();
    for (let i = 0; i < 4; i++) {
      await page.keyboard.press("Tab");
      expect(await blockerModal.evaluate(el => el.contains(document.activeElement))).toBe(true);
    }

    // Test 'Ở lại trang'
    await blockerModal.locator('button:has-text("Ở lại trang")').click();
    await expect(blockerModal).not.toBeVisible();
    expect(page.url()).toContain("/lessons/");

    // Make a fresh unsaved change for Action 2 so hasUnsavedChanges is active
    await noteTextarea.fill("Thay đổi mới cần Lưu rồi rời.");

    // Action 2: Trigger blocker again -> 'Lưu rồi rời'
    await page.locator('a[href="/today"]').first().click();
    await expect(blockerModal).toBeVisible();

    const saveAndLeaveBtn = blockerModal.locator('button:has-text("Lưu rồi rời")');
    await expect(saveAndLeaveBtn).toBeVisible();
    await saveAndLeaveBtn.click();

    // Should wait for save to complete and then proceed to '/today'
    await expect(page).toHaveURL(/\/today/);
    await page.waitForLoadState("networkidle");

    // Return to workspace and verify note was actually saved by 'Lưu rồi rời'
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");
    await expect(page.locator("#learner-lesson-note")).toHaveValue(
      "Thay đổi mới cần Lưu rồi rời."
    );
  });

  test("5.4 Desktop UX: 1440x900 & Smoke 1280x800 -> No horizontal overflow, IPA & GFM render, secure links, screenshots", async ({
    page,
  }) => {
    const consoleErrors: string[] = [];
    const pageErrors: string[] = [];
    page.on("pageerror", error => pageErrors.push(error.message));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    const screenshotsDir = path.resolve(import.meta.dirname, "../e2e-screenshots");
    if (!fs.existsSync(screenshotsDir)) {
      fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    const { enrollmentId, lesson1Id } = await ensureEnrolled(page, "learner_ux");

    // 1. Check 1440x900 Desktop Viewport
    await page.setViewportSize({ width: 1440, height: 900 });

    // Screenshot Today page
    await page.goto("/");
    await page.waitForLoadState("networkidle");
    await page.screenshot({ path: path.join(screenshotsDir, "today-desktop-1440.png") });

    // Screenshot Catalog page
    await page.goto("/curricula");
    await page.waitForLoadState("networkidle");
    await page.screenshot({ path: path.join(screenshotsDir, "catalog-desktop-1440.png") });

    // Screenshot Workspace page
    await page.goto(`/enrollments/${enrollmentId}/lessons/${lesson1Id}`);
    await page.waitForLoadState("networkidle");
    await page.screenshot({ path: path.join(screenshotsDir, "workspace-desktop-1440.png") });

    // Verify left sidebar is visible on 1440px
    const desktopSidebar = page.locator("aside.md\\:flex");
    await expect(desktopSidebar).toBeVisible();

    // Verify NO horizontal overflow on entire page
    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(hasHorizontalOverflow).toBeFalsy();

    // Verify Markdown IPA phonetic symbols rendered in workspace
    const markdownContent = page.locator('[data-testid="markdown-content"]');
    await expect(markdownContent).toBeVisible();
    const markdownText = await markdownContent.innerText();
    expect(markdownText).toMatch(/\/iː\/|\/ɪ\/|\/ə\//);

    // Verify External Resource links have target="_blank" and rel="noopener noreferrer"
    const resourceLinks = page.locator('a[target="_blank"]');
    const count = await resourceLinks.count();
    expect(count).toBeGreaterThan(0);
    for (let i = 0; i < count; i++) {
      const rel = await resourceLinks.nth(i).getAttribute("rel");
      expect(rel).toContain("noopener");
      expect(rel).toContain("noreferrer");
    }

    // Screenshot Focus Mode
    const focusBtn = page
      .locator('button:has-text("Chế độ tập trung"), button[title*="tập trung"]')
      .first();
    await focusBtn.click();
    await expect(page.locator('button:has-text("Thoát tập trung")')).toBeVisible();
    await page.screenshot({ path: path.join(screenshotsDir, "focus-mode-desktop-1440.png") });
    await page.keyboard.press("Escape");

    // 2. Smoke check for 1280x800 viewport
    await page.setViewportSize({ width: 1280, height: 800 });
    await expect(page.locator("h1").first()).toBeVisible();

    const hasHorizontalOverflow1280 = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(hasHorizontalOverflow1280).toBeFalsy();

    // Controls remain accessible on 1280x800
    await expect(page.locator("#learner-lesson-note")).toBeVisible();
    await expect(page.locator('[data-testid^="activity-card-"]').first()).toBeVisible();

    // Filter out expected intentional 401/404/409 test probes from console errors
    const unexpectedErrors = consoleErrors.filter(
      (e) =>
        !e.includes("409") &&
        !e.includes("404") &&
        !e.includes("401") &&
        !e.includes("favicon")
    );
    expect(unexpectedErrors).toEqual([]);
    expect(pageErrors).toEqual([]);
  });
});
