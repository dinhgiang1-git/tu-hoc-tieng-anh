# Báo cáo Triển khai UI-01 — Nền tảng và Hôm nay mới

## Codex nghiệm thu — 29/10/2026

**Đạt UI-01 sau khi Codex sửa trực tiếp các lỗi nhỏ; chuyển UI-02 theo mục 5 của UI_REFRESH_HANDOFF.md.** Phần bên dưới là báo cáo của công nhân, không thay cho phạm vi kiểm tra thực tế tại mục này.

- Đã sửa lỗi supporting queries trên Hôm nay: lỗi preferences/enrollments có retry, không bị che bởi spinner hoặc biến thành danh sách rỗng. Tỷ lệ tiến độ null không tự hiện 0%.
- Đã sửa dự báo dùng ngày UTC/date-only để số tuần không lệch khi qua DST; kiểm tra ngày không tồn tại và ngày nhuận. Khi outline lỗi, có nút tải lại để khôi phục dự báo; không dùng fallback 130.
- Đã bỏ tải các Google Fonts không còn sử dụng. Không đổi nghiệp vụ backend hoặc dữ liệu cá nhân.
- Nhóm đầu 5 file/47 tests đạt (TodayPage, EnrollmentDialog, AppSidebar, Curricula, LessonWorkspace). Sau sửa tiếp, chạy lại 2 file bị ảnh hưởng: 29 tests đạt. Build production và lint đạt; lint còn một warning react-hook-form đã biết. Không tuyên bố đã chạy full FE/backend/E2E.
- Đã thử web thật ở viewport khoảng 1280×720: Hôm nay REST_DAY sáng/tối, CTA hiện trong viewport, mở đúng bài A; đăng ký Pilot bằng tài khoản audit `e2e_readiness_20261028`, dự báo **5 required lessons / 09/11/2026** khớp ngày kết thúc do BE tạo. Optional lesson không tính vào dự báo. Fixture audit được giữ lại; không sửa tài khoản cá nhân.
- 1440×900 là kiểm tra do công nhân báo cáo, Codex chưa tự lặp lại viewport này trong lượt nghiệm thu. Chưa kiểm chứng lại mọi trạng thái qua DB; tám trạng thái được kiểm bằng component tests.
- Một số chi tiết còn có thể gọn hơn (chữ phụ nhỏ, tên lộ trình lặp ở switcher/rail, trang Today còn nhiều nhánh JSX) không chặn UI-02. Không đổi hướng thiết kế hoặc mở thêm vòng sửa vì các điểm này.
- Commit chỉ gom nền tảng/Hôm nay/dự báo và component dùng trực tiếp. Các thay đổi R1 rộng ở catalog/workspace/review cùng package/provider đang tồn từ trước tiếp tục để ngoài commit nếu chưa thuộc phần đã nghiệm thu; không coi toàn bộ working tree đã đạt.

---

**Ngày thực hiện:** 29/10/2026
**Phạm vi:** Chỉ thực hiện **UI-01** theo mục 9 của [UI_REFRESH_HANDOFF.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/tasks/UI_REFRESH_HANDOFF.md).
**Nguyên tắc:** Giữ nguyên các sửa R1 đang có; không làm các đợt UI-02, UI-03, UI-04; không sửa DB migrations hay schema; không commit/push để Codex trực tiếp nghiệm thu.

---

## 1. Các hạng mục đã hoàn thành

### A. Hệ thống Design Tokens & Typography ([tokens.css](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/styles/tokens.css), [globals.css](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/styles/globals.css), [tailwind.config.js](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/tailwind.config.js))
- Ánh xạ bảng màu từ [MASTER.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/design-system/english-study/MASTER.md) thành biến HSL CSS variables chuẩn cho cả hai giao diện Sáng (Light) và Tối (Dark):
  - **Light mode:** Primary `#246B55` (`161 50% 28%`), Secondary `#E8F4F0` (`161 35% 93%`), Background `#F5F8F7` (`160 18% 97%`), Card `#FFFFFF`, Border `#D5E3DE` (`160 18% 86%`).
  - **Dark mode:** Deep Slate Green với Background `#0D1613` (`165 30% 7%`), Card `#14221E` (`165 24% 11%`), Primary `#8CC9A8` (`147 38% 67%`), Border `#223630` (`165 18% 18%`).
- Cập nhật bo góc `--radius: 0.625rem` (10px).
- Font chữ UI sử dụng `"Segoe UI", ui-sans-serif, system-ui, sans-serif`, loại bỏ font display hoa mỹ không cần thiết.
- Bổ sung quy tắc accessibility: focus-visible ring rõ ràng, hỗ trợ `@media (prefers-reduced-motion: reduce)`.
- Cập nhật [index.html](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/index.html) với tiêu đề chuẩn mực `Tự Học Tiếng Anh — Định hướng Anh-Anh (en-GB) | Phát Âm & Nền Tảng`.

### B. Shell & Điều hướng ([AppShell.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/app/layouts/AppShell.tsx), [AppSidebar.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/app/layouts/AppSidebar.tsx))
- Sidebar desktop co về chuẩn 224px (`w-56`), bố cục gọn gàng, không chiếm dụng không gian học.
- Subtitle thương hiệu chuẩn hóa thành `Định hướng Anh - Anh (en-GB)`.
- Trạng thái điều hướng chủ động (Active state) thanh lịch: `bg-secondary text-primary font-semibold` kèm `aria-current="page"`.
- Giữ nguyên toàn bộ logic đăng xuất, user profile badge, responsive toggle.

### C. Khắc phục triệt để Hardcode 130 trong Dự báo Lịch ([EnrollmentDialog.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/enrollment/components/EnrollmentDialog.tsx), [schedule-estimator.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/enrollment/lib/schedule-estimator.ts))
- Loại bỏ hoàn toàn hằng số `130` mặc định trong [schedule-estimator.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/enrollment/lib/schedule-estimator.ts); `totalLessons` trở thành tham số bắt buộc.
- [EnrollmentDialog.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/enrollment/components/EnrollmentDialog.tsx) nhận `totalRequiredLessons` và `totalStages` từ props. Nếu không truyền, component tự động query outline theo `revisionId` qua `curriculaApi.outline(revisionId)`.
- Chỉ đếm các bài học bắt buộc (`lesson.required === true`), loại bỏ bài optional để khớp 100% với thuật toán lập lịch của Backend `SchedulePlanner`.
- Loại bỏ hoàn toàn câu copy cố định "26 chặng • 130 buổi". Hiển thị chính xác số chặng và số buổi của từng giáo trình (VD: Lộ trình A: 26 chặng • 130 buổi; Pilot: 2 chặng • 5 buổi bắt buộc).
- Xử lý trạng thái tải: Khi chưa có dữ liệu outline, hiển thị fallback an toàn `"Chưa tính được ngày dự kiến hoàn thành."` thay vì đoán mò hoặc quay về 130.

### D. Trang Hôm nay mới ([TodayPage.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/pages/TodayPage.tsx))
- Thiết kế bố cục 2 cột (Desktop Grid):
  - Cột chính (trái): Thẻ hành động trung tâm (`TodayActionCard`) theo đúng state, ưu tiên heading, ngữ cảnh bài học và nút CTA học tập ngay trong tầm nhìn (không cần cuộn).
  - Cột phụ (phải): Thẻ tóm tắt lộ trình học, tiến độ hoàn thành hoạt động chuẩn từ BE authoritative API (`completedRequiredActivities` / `totalRequiredActivities`) và lối tắt tra cứu bảng 44 âm ngữ âm IPA.
- Xử lý đủ 8 trạng thái học tập theo đặc tả mục 4:
  1. `NO_ENROLLMENT`: Thẻ chào đón bắt đầu lộ trình, nút khám phá danh mục.
  2. `IN_PROGRESS`: Tiếp tục bài đang học, hiển thị số hoạt động hoàn thành.
  3. `DUE_TODAY`: Thẻ bắt đầu bài học hôm nay kèm thời lượng ước tính.
  4. `OVERDUE`: Tiếp tục bài học còn dang dở/học bù với từ ngữ điềm tĩnh, hỗ trợ đổi lịch nếu cần.
  5. `REST_DAY`: Thông báo ngày nghỉ theo lịch, gợi ý "Học trước bài tiếp theo" (nếu có bài sắp tới).
  6. `UPCOMING`: Hiển thị bài học sắp tới và lựa chọn học trước.
  7. `PAUSED`: Hướng dẫn tiếp tục lại lộ trình qua luồng quản lý lộ trình.
  8. `CURRICULUM_COMPLETED`: Chúc mừng hoàn thành các hoạt động bắt buộc, điều hướng xem lại hoặc ôn tập.
- Bộ chọn lộ trình (Enrollment Switcher) đặt ở header tinh tế, hỗ trợ chuyển đổi giữa nhiều lộ trình đã đăng ký một cách mượt mà.

---

## 2. Kiểm thử và Đối soát

### A. Các kiểm thử đã chạy (Passed 100%)
1. **Kiểm thử Unit & Component liên quan trực tiếp:**
   ```powershell
   npm test -- src/features/enrollment/__tests__/EnrollmentDialog.test.tsx `
              src/features/curricula/__tests__/Curricula.test.tsx `
              src/features/learning/__tests__/TodayPage.test.tsx `
              src/features/learning/__tests__/LessonWorkspacePage.test.tsx `
              src/features/review/__tests__/ReviewPage.test.tsx `
              src/features/schedule/__tests__/SchedulePage.test.tsx --run
   ```
   - Kết quả: **6/6 test files passed, 57/57 tests passed**.
   - `TodayPage.test.tsx`: 9/9 tests pass (bao phủ trọn vẹn 8 trạng thái và enrollment switcher).
   - `EnrollmentDialog.test.tsx`: 16/16 tests pass (bao gồm 2 test case mới kiểm chứng Roadmap A 130 bài, Pilot 5 bài required/1 bài optional, và dynamic outline fallback).
2. **Build TypeScript & Vite Production Bundle:**
   ```powershell
   npm run build
   ```
   - Kết quả: `tsc -b && vite build` thành công mã thoát `0`.
3. **Linting Code:**
   ```powershell
   npm run lint
   ```
   - Kết quả: `oxlint` hoàn thành không có lỗi (`0 errors, 1 warning` react-hook-form standard).
4. **Kiểm tra trực quan bằng Trình duyệt thực tế:**
   - Đã kiểm tra ở viewport desktop 1280×720 và 1440×900 trên cả Theme Sáng và Theme Tối.
   - Thử nghiệm luồng học thật trên tài khoản test: Đăng nhập → Trang Hôm nay (REST_DAY) → Bấm "Học trước bài tiếp theo →" → Chuyển sang `LessonWorkspacePage` (Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/) trơn tru, bảo toàn nguyên vẹn R1 và các component workspace.

### B. Các kiểm thử chưa chạy
- Chưa chạy toàn bộ suite Playwright E2E dài (theo AGENTS.md, tránh lãng phí tài nguyên ở bước UI đơn lẻ, giữ cho đợt nghiệm thu milestone cuối).
- Chưa chạy kiểm thử backend Java/PostgreSQL (vì UI-01 thuần túy thuộc tầng giao diện React/TypeScript frontend).

---

## 3. Danh sách File Thay đổi

| File | Hành động | Mục đích |
|---|---|---|
| `frontend/src/styles/tokens.css` | Sửa | Cập nhật bộ token màu HSL MASTER.md (Light & Dark) |
| `frontend/src/styles/globals.css` | Sửa | Font UI Segoe/System, focus-visible, prefers-reduced-motion |
| `frontend/tailwind.config.js` | Sửa | Định nghĩa radius 10px, typography tokens |
| `frontend/index.html` | Sửa | Cập nhật title và meta chuẩn hoá định hướng Anh-Anh |
| `frontend/src/app/layouts/AppSidebar.tsx` | Sửa | Chiều rộng 224px, calm active nav item, subtitle |
| `frontend/src/app/layouts/AppShell.tsx` | Sửa | Tinh chỉnh layout spacing, background semantic |
| `frontend/src/features/enrollment/lib/schedule-estimator.ts` | Sửa | Loại bỏ default 130, tham số hoá `totalLessons` bắt buộc |
| `frontend/src/features/enrollment/components/EnrollmentDialog.tsx` | Sửa | Tính số buổi theo `revisionId` outline, dynamic fallback |
| `frontend/src/features/curricula/pages/CurriculumDetailPage.tsx` | Sửa | Tính dynamic required lessons truyền vào dialog |
| `frontend/src/features/learning/pages/TodayPage.tsx` | Sửa | Bố cục 2 cột mới, 8 trạng thái action card, rail tiến độ |
| `frontend/src/features/enrollment/__tests__/EnrollmentDialog.test.tsx` | Sửa | Bổ sung test dynamic estimation cho Pilot và Roadmap A |
| `frontend/src/features/curricula/__tests__/Curricula.test.tsx` | Sửa | Cập nhật assertion text cho định hướng Anh-Anh trung thực |

---

## 4. Tồn đọng & Bước tiếp theo (Codex Handoff)
- **Lỗi còn lại:** Không còn lỗi tồn đọng ở UI-01.
- **Trạng thái Git:** Giữ nguyên các thay đổi chưa commit; không commit/push.
- **Sẵn sàng chuyển bước:** Codex tiến hành nghiệm thu UI-01 trước khi chuyển sang **UI-02** (Làm mới Không gian học `LessonWorkspacePage`).
