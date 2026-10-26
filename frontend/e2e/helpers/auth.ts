import { Page, expect } from "@playwright/test";

export function normalizeTestUsername(username: string): string {
  if (username === "admin" || username.startsWith("e2e_")) {
    return username;
  }
  return `e2e_${username}`;
}

export async function login(page: Page, username: string, password = "Pass123456!") {
  const effectiveUsername = normalizeTestUsername(username);
  await page.goto("/login");
  await page.waitForLoadState("domcontentloaded");

  // Fill in credentials
  await page.locator("#username").fill(effectiveUsername);
  await page.locator("#password").fill(password);

  // Submit and wait for redirect away from login
  await page.locator('button:has-text("Đăng nhập")').click();
  await expect(page).not.toHaveURL(/\/login/);
  await page.waitForLoadState("networkidle");
}

export async function logout(page: Page) {
  const logoutBtn = page.locator('button:has-text("Đăng xuất"):visible').first();
  if (await logoutBtn.isVisible()) {
    await logoutBtn.click();
    await expect(page).toHaveURL(/\/login/);
  } else {
    // Obtain CSRF token for safe programmatic logout
    const csrfToken = await getCsrfToken(page);
    const logoutRes = await page.request.post("/api/v1/auth/logout", {
      headers: { "X-CSRF-TOKEN": csrfToken },
    });
    expect(logoutRes.ok()).toBeTruthy();
    await page.goto("/login");
    await expect(page).toHaveURL(/\/login/);
  }
}

export async function getCsrfToken(page: Page): Promise<string> {
  const response = await page.request.get("/api/v1/auth/csrf");
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  return data.token;
}

export interface EnrolledContext {
  enrollmentId: string;
  lesson1Id: string;
}

export async function ensureEnrolled(
  page: Page,
  username: string,
  password = "Pass123456!"
): Promise<EnrolledContext> {
  const effectiveUsername = normalizeTestUsername(username);
  await login(page, effectiveUsername, password);

  // Helper to resolve lesson 1 ID from outline
  const resolveFirstLessonId = async (): Promise<string> => {
    const curRes = await page.request.get("/api/v1/curricula/pilot-curriculum");
    expect(curRes.ok()).toBeTruthy();
    const curData = await curRes.json();
    const revisionId = curData.currentRevisionId || curData.revision?.id;
    const outlineRes = await page.request.get(`/api/v1/revisions/${revisionId}/outline`);
    expect(outlineRes.ok()).toBeTruthy();
    const outline = await outlineRes.json();
    return outline.phases[0].weeks[0].lessons[0].id;
  };

  const getEnrollment = async (): Promise<string | null> => {
    for (let i = 0; i < 5; i++) {
      const enrollmentsRes = await page.request.get("/api/v1/me/enrollments");
      if (enrollmentsRes.ok()) {
        const list = await enrollmentsRes.json();
        if (Array.isArray(list) && list.length > 0) {
          return list[0].id;
        }
      }
      await page.waitForTimeout(300);
    }
    return null;
  };

  // Check if user already has an active enrollment
  const existingEnrollmentId = await getEnrollment();
  if (existingEnrollmentId) {
    const lesson1Id = await resolveFirstLessonId();
    return {
      enrollmentId: existingEnrollmentId,
      lesson1Id,
    };
  }

  // Otherwise, enroll in pilot curriculum
  await page.goto("/curricula/pilot-curriculum");
  await page.waitForLoadState("domcontentloaded");

  const enrollBtn = page.locator('button:has-text("Đăng ký học")');
  await expect(enrollBtn).toBeVisible();
  await enrollBtn.click();

  const dialog = page.locator('div[role="dialog"]');
  await expect(dialog).toBeVisible();
  await expect(dialog.locator("#enrollment-dialog-title")).toContainText("Đăng ký lộ trình học");

  const confirmBtn = dialog.locator('button:has-text("Xác nhận đăng ký")');
  await Promise.all([
    page.waitForResponse((r) => r.url().includes("/api/v1/enrollments") && (r.status() === 201 || r.status() === 200)),
    confirmBtn.click(),
  ]);

  await expect(page).toHaveURL(/\/today/);
  await expect(dialog).not.toBeVisible();

  const enrollmentIdAfter = await getEnrollment();
  expect(enrollmentIdAfter).not.toBeNull();
  const lesson1Id = await resolveFirstLessonId();

  return {
    enrollmentId: enrollmentIdAfter!,
    lesson1Id,
  };
}
