import { test, expect } from "@playwright/test";
import { login } from "./helpers/auth";

test.describe("Roadmap A v1 - 26 Weeks Desktop E2E", () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test("Khám phá Lộ trình A 26 tuần, xem chi tiết 3 giai đoạn, đăng ký học và xem biểu đồ tiến độ", async ({
    page,
  }) => {
    // 1. Đăng nhập học viên kiểm thử có sẵn
    await login(page, "e2e_learner_ux", "Pass123456!");

    // 2. Vào trang danh mục Lộ trình học (/curricula)
    await page.goto("/curricula");
    await page.waitForLoadState("networkidle");

    // Kiểm tra card Lộ trình A xuất hiện
    const roadmapCard = page.locator("article").filter({
      hasText: "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng",
    });
    await expect(roadmapCard).toBeVisible();
    await expect(roadmapCard).toContainText("v1.0.0");
    await expect(roadmapCard).toContainText("Anh-Anh · en-GB");

    // Chụp ảnh danh mục lộ trình
    await page.screenshot({
      path: "e2e-screenshots/roadmap-a-catalog.png",
      fullPage: true,
    });

    // 3. Bấm "Xem lộ trình" để vào trang chi tiết (/curricula/roadmap-a)
    await roadmapCard.locator("a", { hasText: "Xem lộ trình" }).click();
    await page.waitForURL(/\/curricula\/roadmap-a/);
    await page.waitForLoadState("networkidle");

    // Kiểm tra thông tin lộ trình và 3 Giai đoạn
    await expect(
      page.locator("h1", { hasText: "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng" })
    ).toBeVisible();
    await expect(
      page.locator("summary", {
        hasText: "Giai đoạn 1: Chuẩn hóa Ngữ âm IPA Anh-Anh Cốt lõi",
      })
    ).toBeVisible();
    await expect(
      page.locator("summary", {
        hasText: "Giai đoạn 2: Tích lũy Ngữ pháp & Kỹ năng Nghe hiểu",
      })
    ).toBeVisible();
    await expect(
      page.locator("summary", {
        hasText: "Giai đoạn 3: Phản xạ Giao tiếp & Ứng dụng Thực tế",
      })
    ).toBeVisible();

    // Kiểm tra sự xuất hiện của Tuần 1 và Tuần 26
    await expect(
      page.locator("summary").filter({ hasText: /^Tuần 1:/ })
    ).toBeVisible();
    await expect(
      page.locator("summary").filter({ hasText: /^Tuần 26:/ })
    ).toBeVisible();

    // Chụp ảnh chi tiết lộ trình và các tuần
    await page.screenshot({
      path: "e2e-screenshots/roadmap-a-detail-phases.png",
      fullPage: false,
    });

    // 4. Bấm "Đăng ký học" nếu nút hiển thị
    const enrollBtn = page.locator('button:has-text("Đăng ký học"):visible').first();
    if (await enrollBtn.isVisible()) {
      await enrollBtn.click();

      // Dialog đăng ký xuất hiện
      const dialog = page.locator('div[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog).toContainText("Đăng ký lộ trình học");

      // Bấm nút "Xác nhận đăng ký"
      const confirmEnrollBtn = dialog.locator('button:has-text("Xác nhận đăng ký")');
      await confirmEnrollBtn.click();
      await page.waitForTimeout(500);

      // Nếu đã có enrollment trước đó hoặc gặp lỗi, đóng dialog lại
      if (await dialog.isVisible()) {
        const closeBtn = dialog.locator('button[aria-label="Đóng hộp thoại"], button:has-text("Hủy")').first();
        await closeBtn.click();
      }

      await expect(dialog).not.toBeVisible();
    }

    // 5. Điều hướng sang trang Tiến độ (/progress) qua sidebar
    const progressNav = page.locator('aside a[href="/progress"]:visible');
    await expect(progressNav).toBeVisible();
    await progressNav.click();
    await page.waitForURL(/\/progress/);
    await page.waitForLoadState("networkidle");

    // Chọn đúng enrollment Lộ trình A nếu selector hiển thị
    const enrollmentSelect = page.locator("select#enrollment-select");
    if (await enrollmentSelect.isVisible()) {
      const options = await enrollmentSelect.locator("option").all();
      for (const opt of options) {
        const text = await opt.textContent();
        if (text && text.includes("Lộ trình A")) {
          const val = await opt.getAttribute("value");
          if (val) {
            await enrollmentSelect.selectOption(val);
            await page.waitForLoadState("networkidle");
            break;
          }
        }
      }
    }

    // Kiểm tra tiêu đề trang và biểu đồ tiến độ theo tuần
    await expect(page.locator("h1", { hasText: "Tiến độ học tập" })).toBeVisible();
    await expect(page.locator('[data-testid="weekly-progress-chart"]')).toBeVisible();

    // Kiểm tra biểu đồ có render Tuần 1 và Tuần 26
    await expect(page.locator('[data-testid="week-item-1"]')).toBeVisible();
    await expect(page.locator('[data-testid="week-item-26"]')).toBeVisible();

    // Chụp ảnh toàn cảnh biểu đồ 26 tuần trên desktop 1440
    await page.screenshot({
      path: "e2e-screenshots/roadmap-a-progress-26weeks.png",
      fullPage: true,
    });
  });
});
