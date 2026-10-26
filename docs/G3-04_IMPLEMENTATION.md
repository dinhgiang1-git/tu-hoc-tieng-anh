# Báo cáo Triển khai G3-04: Kiểm thử E2E Vòng học Cốt lõi & Mốc M1

**Ngày thực hiện:** 26/10/2026
**Trạng thái:** Codex đã nghiệm thu M1 web desktop; xem [G3-04_ACCEPTANCE.md](G3-04_ACCEPTANCE.md) cho kết quả cuối và phần reviewer tự sửa. Các số liệu bên dưới là báo cáo triển khai ban đầu.
**Môi trường thực thi:** Tái dùng trực tiếp dịch vụ Development (FE: `http://localhost:5173`, BE: `http://localhost:8080`, PostgreSQL: `localhost:5432` / `english_learning_db`) theo chỉ đạo ngày 26/10/2026.
**Phạm vi:** Web Desktop Chromium (1440×900 và smoke 1280×800); Mobile 375px hoãn lại (deferred) theo quyết định người dùng, không chặn nghiệm thu M1.

---

## 1. Dịch vụ Development, Fixtures và An toàn Dữ liệu

### 1.1 Cấu hình Dịch vụ
- **Backend API:** `http://localhost:8080` (Spring Boot Java 21, profile dev, Actuator health: `UP`).
- **Frontend SPA:** `http://localhost:5173` (Vite React SPA, responsive, proxy API).
- **PostgreSQL Database:** Container `postgresql-postgres-1` (PostgreSQL 16, database `english_learning_db` trên port 5432).

### 1.2 Quản lý Fixture & Bảo vệ Dữ liệu Cá nhân
- **Nguyên tắc an toàn dữ liệu:** Toàn bộ dữ liệu test được định danh bằng tiền tố `e2e_`. Tuyệt đối không xóa, không sửa, không truncate dữ liệu của tài khoản người dùng cá nhân (`dinhgiang1`, `admin`).
- **Tài khoản Test E2E:** 17 tài khoản độc lập tạo qua [scripts/seed-e2e.sql](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/seed-e2e.sql):
  - `e2e_learner_m1_1` .. `e2e_learner_m1_6`: Phục vụ 6 kịch bản cốt lõi M1-1 đến M1-6.
  - `e2e_learner_iso_a`, `e2e_learner_iso_b`: Phục vụ kiểm thử phân tách và quyền truy cập đa tài khoản.
  - `e2e_learner_ctx_a`, `e2e_learner_ctx_b`: Phục vụ kiểm thử chuyển đổi ngữ cảnh (Context Switching) và xóa cache phiên.
  - `e2e_learner_tabs_act`, `e2e_learner_tabs_note`: Phục vụ kiểm thử đồng bộ 2 tab đồng thời (Activity Sync & Note CAS).
  - `e2e_learner_autosave`, `e2e_learner_focus`, `e2e_learner_blocker`: Phục vụ kiểm thử tuần tự hóa autosave, chế độ tập trung và navigation blocker.
  - `e2e_learner_ux`, `e2e_learner_race`: Phục vụ kiểm thử giao diện Desktop 1440×900 và chống race condition.
  - *Mật khẩu chung cho tất cả tài khoản e2e:* `Pass123456!` (mã hóa chuẩn BCrypt `$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW`).
- **Lộ trình học thử nghiệm (Pilot Curriculum):**
  - Giáo trình `pilot-curriculum` (v1.0.0, PUBLISHED) với 19 hoạt động bắt buộc trong các buổi học bắt buộc (Buổi 1–5: 4+4+4+4+3 = 19; Buổi 6 tự chọn: 1). Mẫu số tiến độ lộ trình chuẩn hóa tuyệt đối: 19.
- **Fixture cách ly Cross-revision (M1-6):**
  - Giáo trình phụ `e2e_other_curriculum` (ID: `c0000000-0000-0000-0000-000000000002`, Revision: `a0000000-0000-0000-0000-000000000002`, Activity: `e2e00000-0000-0000-0000-000000000004`) được tạo sẵn trong DB để kiểm thử bảo vệ ranh giới revision mà không tác động dữ liệu pilot thật.

---

## 2. Các tệp thay đổi và bổ sung

| Tệp | Loại | Mô tả thay đổi |
|---|---|---|
| [scripts/run-e2e.ps1](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/run-e2e.ps1) | Viết lại | Runner E2E tái dùng dev server, kiểm tra health backend :8080 và frontend :5173, seed an toàn, hỗ trợ `-TestFilter`. |
| [scripts/seed-e2e.sql](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/seed-e2e.sql) | Tạo mới | Script SQL seed/reset trong cùng DB dev, theo allowlist 17 usernames, tái dùng fixture `e2e_other_curriculum` chuẩn JSON attribution. |
| [frontend/playwright.config.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/playwright.config.ts) | Tạo mới | Cấu hình Playwright Desktop Chromium 1440×900, trỏ `baseURL: http://localhost:5173`. |
| [frontend/e2e/helpers/auth.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/e2e/helpers/auth.ts) | Tạo mới | Tiện ích chuẩn hóa username test `e2e_`, helper login/logout (chống lỗi strict mode nút bấm), helper `ensureEnrolled` đồng bộ an toàn. |
| [frontend/e2e/m1-core-loop.e2e.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/e2e/m1-core-loop.e2e.ts) | Tạo mới | Bộ 6 test E2E kiểm chứng trực tiếp 6 tiêu chí M1 trên giao diện thật. |
| [frontend/e2e/multi-account-and-sync.e2e.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/e2e/multi-account-and-sync.e2e.ts) | Tạo mới | Bộ 4 test E2E kiểm chứng phân tách 2 tài khoản, dọn cache khi chuyển phiên, 2 tab xung đột tiến độ và Note CAS overwrite. |
| [frontend/e2e/navigation-and-desktop-ux.e2e.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/e2e/navigation-and-desktop-ux.e2e.ts) | Tạo mới | Bộ 4 test E2E kiểm chứng autosave serialization, Focus mode (Esc), Unified Navigation Blocker và Desktop UX 1440×900 + 1280×800. |
| [docs/HUONG_DAN_CHAY.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/HUONG_DAN_CHAY.md) | Cập nhật | Bổ sung hướng dẫn chạy E2E Playwright và cập nhật phạm vi chức năng G3-03. |
| [docs/tasks/backlog.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/tasks/backlog.md) | Cập nhật | Cập nhật trạng thái G3-04: Chờ Codex nghiệm thu. |

---

## 3. Lệnh thực thi & Kết quả kiểm thử

### 3.1 Lệnh chạy nhanh toàn bộ E2E

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1
```

### 3.2 Kết quả Playwright E2E Suite (14/14 Tests Passed - 100%)

```text
Running 14 tests using 1 worker

  ok  1 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:5:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-1: Login -> Chọn pilot -> Đăng ký -> Mở workspace -> Hoàn thành 2 nhiệm vụ, ghi note -> Reload giữ nguyên (5.4s)
  ok  2 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:84:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-2: Hoàn thành nhiệm vụ bắt buộc cuối -> Buổi học hoàn thành, gợi ý bài kế đúng thứ tự (8.9s)
  ok  3 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:146:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-3: Idempotency và kiểm tra Version Conflict 409 khi gửi version cũ (5.2s)
  ok  4 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:206:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-4: Undo nhiệm vụ bắt buộc -> Giảm tiến độ, giữ response text, mẫu số chuẩn 19 (12.1s)
  ok  5 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:251:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-5: Mô phỏng lỗi mạng khi lưu note -> Giữ chữ, không báo Đã lưu giả, retry thành công (8.3s)
  ok  6 [desktop-chromium-1440] › e2e\m1-core-loop.e2e.ts:302:3 › M1 Core Learning Loop (6 Tiêu chí M1) › M1-6: Gửi request với activity từ revision khác -> 404 Problem Details, không mutate (5.0s)
  ok  7 [desktop-chromium-1440] › e2e\multi-account-and-sync.e2e.ts:5:3 › Multi-account Isolation & Multi-tab Synchronization (Sections 5.1 & 5.2) › 5.1 Isolation: Contexts A/B độc lập; User B không truy cập/xóa được dữ liệu User A; CSRF bảo vệ (9.9s)
  ok  8 [desktop-chromium-1440] › e2e\multi-account-and-sync.e2e.ts:67:3 › Multi-account Isolation & Multi-tab Synchronization (Sections 5.1 & 5.2) › 5.1 Context Switching: Logout A -> Login B không rò rỉ cache/draft -> Login lại A dữ liệu còn nguyên (14.8s)
  ok  9 [desktop-chromium-1440] › e2e\multi-account-and-sync.e2e.ts:105:3 › Multi-account Isolation & Multi-tab Synchronization (Sections 5.1 & 5.2) › 5.2 Two Tabs Activity Sync: Tab 1 complete -> Tab 2 xung đột 409, giữ draft, đồng bộ phục hồi (7.8s)
  ok 10 [desktop-chromium-1440] › e2e\multi-account-and-sync.e2e.ts:158:3 › Multi-account Isolation & Multi-tab Synchronization (Sections 5.1 & 5.2) › 5.2 Two Tabs Note CAS: Tab 1 save -> Tab 2 xung đột 409, latch autosave, ghi đè an toàn (8.0s)
  ok 11 [desktop-chromium-1440] › e2e\navigation-and-desktop-ux.e2e.ts:7:3 › Navigation Guards & Desktop UX (Sections 5.3 & 5.4) › 5.3 Autosave Serialization: A in-flight có kiểm soát -> gõ B -> chỉ gửi tuần tự, reload thấy B (8.2s)
  ok 12 [desktop-chromium-1440] › e2e\navigation-and-desktop-ux.e2e.ts:50:3 › Navigation Guards & Desktop UX (Sections 5.3 & 5.4) › 5.3 Focus Mode: Bật/thoát Focus Mode bằng phím Escape -> Ghi chú và câu trả lời còn nguyên (6.4s)
  ok 13 [desktop-chromium-1440] › e2e\navigation-and-desktop-ux.e2e.ts:98:3 › Navigation Guards & Desktop UX (Sections 5.3 & 5.4) › 5.3 Unified Navigation Blocker: 'Ở lại trang', 'Rời khỏi không lưu', 'Lưu rồi rời' (8.2s)
  ok 14 [desktop-chromium-1440] › e2e\navigation-and-desktop-ux.e2e.ts:144:3 › Navigation Guards & Desktop UX (Sections 5.3 & 5.4) › 5.4 Desktop UX: 1440x900 & Smoke 1280x800 -> No horizontal overflow, IPA & GFM render, secure links, screenshots (8.7s)

  14 passed (2.0m)
```

### 3.3 Kết quả Regression Suites
- **Frontend Typecheck (`npm run typecheck`):** 0 errors.
- **Frontend Linter (`npm run lint` - oxlint):** 0 errors, 1 warning thông tin đã biết về `form.watch()`.
- **Frontend Vitest Unit/Integration Tests (`npm test`):** **16/16 test files passed, 98/98 tests passed**.
- **Backend Unit & Integration Tests (`mvnw.cmd test -B -ntp`):** **143/143 unit tests passed**, BUILD SUCCESS trong 13.5s.

---

## 4. Bảng Đối chiếu Kiểm chứng 6 Tiêu chí M1 (M1-1 đến M1-6)

| Tiêu chí | Spec File & Kịch bản Test | Các Khẳng định (Assertions) Trọng yếu | Kết quả |
|---|---|---|:---:|
| **M1-1** | `m1-core-loop.e2e.ts`<br>Login UI $\rightarrow$ Chọn pilot $\rightarrow$ Đăng ký $\rightarrow$ Mở workspace $\rightarrow$ Hoàn thành 2 nhiệm vụ, ghi note $\rightarrow$ Reload giữ nguyên | - Đăng ký thành công và điều hướng sang `/today`<br>- Mở workspace bài học 1<br>- Hoàn thành Activity 1 & 2 thành công<br>- Nhập ghi chú, nhấn «Lưu ngay» hiển thị «Đã lưu»<br>- **Reload trang:** Trạng thái «Đã hoàn thành» của cả 2 activity và nội dung ghi chú được giữ nguyên vẹn từ server | **PASSED** (5.4s) |
| **M1-2** | `m1-core-loop.e2e.ts`<br>Hoàn thành nhiệm vụ bắt buộc cuối $\rightarrow$ Buổi học hoàn thành, gợi ý bài kế đúng thứ tự | - Hoàn thành Act 1, Act 2, lưu nháp Act 3, hoàn thành Act 3<br>- Nhập phản tư và nhấn «Nộp & Hoàn thành» Act 4 (nhiệm vụ bắt buộc cuối)<br>- Banner `lesson-complete-banner` hiển thị «Hoàn thành buổi học!»<br>- Nhấn «Bài học tiếp theo» điều hướng đúng đến «Buổi 2»<br>- Về trang chủ Hôm nay: nút hành động gợi ý tiếp tục học bài tiếp theo hiển thị rõ ràng, `href` hợp lệ (không chứa `null`/`undefined`) | **PASSED** (8.9s) |
| **M1-3** | `m1-core-loop.e2e.ts`<br>Idempotency và kiểm tra Version Conflict 409 khi gửi version cũ | - PUT progress cùng trạng thái với expectedVersion mới nhất trả về version giữ nguyên (idempotent no-op)<br>- Không tăng version dư thừa, không tạo event trùng<br>- Gửi request với expectedVersion cũ (`0` khi server đang $\ge 1$) bị backend từ chối với HTTP 409 Problem Details (`code: "VERSION_CONFLICT"`) | **PASSED** (5.2s) |
| **M1-4** | `m1-core-loop.e2e.ts`<br>Undo nhiệm vụ bắt buộc $\rightarrow$ Giảm tiến độ, giữ response text, mẫu số chuẩn 19 | - Kiểm tra tiến độ Hôm nay trước khi học: `0 / 19 hoạt động bắt buộc`<br>- Hoàn thành Act 1 $\rightarrow$ tiến độ Hôm nay tăng thành `1 / 19`<br>- Quay lại workspace nhấn «Hoàn tác» $\rightarrow$ trạng thái trở về «Chưa bắt đầu»<br>- Reload trang xác nhận trạng thái undone được lưu trên server<br>- Kiểm tra lại Hôm nay: tiến độ giảm về `0 / 19`, **mẫu số 19 được bảo toàn tuyệt đối** | **PASSED** (12.1s) |
| **M1-5** | `m1-core-loop.e2e.ts`<br>Mô phỏng lỗi mạng khi lưu note $\rightarrow$ Giữ chữ, không báo Đã lưu giả, retry thành công | - Mock abort route `**/api/v1/notes**`<br>- Gõ nội dung ghi chú và nhấn «Lưu ngay»<br>- Note editor hiển thị trạng thái «Lỗi lưu», **tuyệt đối không hiển thị «Đã lưu»**<br>- Textarea ghi chú giữ nguyên vẹn 100% nội dung<br>- Thử bấm link rời trang $\rightarrow$ Unified Navigation Blocker chặn lại với modal «Bạn có thay đổi chưa lưu!»<br>- Nhấn «Ở lại trang», gỡ mock mạng, nhấn «Thử lại» $\rightarrow$ lưu thành công («Đã lưu»)<br>- Reload trang xác nhận ghi chú được lưu an toàn trên server | **PASSED** (8.3s) |
| **M1-6** | `m1-core-loop.e2e.ts`<br>Gửi request với activity từ revision khác $\rightarrow$ 404 Problem Details, không mutate | - Truy vấn API lộ trình `e2e_other_curriculum` để lấy ID bài học và activity thực tế từ revision khác (`e2e00000-0000-0000-0000-000000000004`)<br>- Thử truy cập workspace bài học của revision khác bằng enrollment của pilot $\rightarrow$ trả về HTTP 404<br>- Gửi PUT cập nhật tiến độ nhắm vào activity của revision khác kèm CSRF token hợp lệ $\rightarrow$ bị từ chối với HTTP 404 Problem Details (`code: "RESOURCE_NOT_FOUND"`)<br>- Xác nhận `enrollmentVersion` của pilot không bị tăng hay biến đổi | **PASSED** (5.0s) |

---

## 5. Ảnh chụp Màn hình Desktop (Desktop Visual Verification)

Theo quy định DoD cho giao diện Desktop (1440×900 và smoke 1280×800), bộ test đã tự động ghi lại các ảnh chụp màn hình tại thư mục `frontend/e2e-screenshots/`:

1. `frontend/e2e-screenshots/today-desktop-1440.png`: Trang Hôm nay trên độ phân giải 1440×900, hiển thị tiến độ lộ trình, thẻ bài học đang học, thanh điều hướng sidebar và không có thanh cuộn ngang (`scrollWidth <= innerWidth`).
2. `frontend/e2e-screenshots/catalog-desktop-1440.png`: Trang Danh mục lộ trình với thẻ Pilot Curriculum và thông tin minh bạch bản quyền, tác giả.
3. `frontend/e2e-screenshots/workspace-desktop-1440.png`: Không gian học tập buổi học, hiển thị nội dung Markdown kèm ký hiệu phiên âm quốc tế IPA (`/iː/`, `/ɪ/`), danh sách hoạt động, tài liệu đính kèm chuẩn `rel="noopener noreferrer"`, và trình soạn thảo ghi chú.
4. `frontend/e2e-screenshots/focus-mode-desktop-1440.png`: Chế độ tập trung (Focus Mode), ẩn toàn bộ thanh điều hướng để người học tập trung vào bài học, hỗ trợ thoát bằng phím `Escape`.

---

## 6. Các vấn đề Đã xử lý trong Lượt sửa

Trong quá trình thực hiện kiểm thử khớp UI thực tế, đã phát hiện và khắc phục các vấn đề:
1. **Khớp Selector nút bấm theo tiêu chuẩn Playwright:**
   - Thay thế các selector mơ hồ trên `button:has-text("Hoàn thành")` (vốn dễ trùng với `Nộp & Hoàn thành` trên Activity 4) bằng selector chính xác `button[data-testid^="complete-button-"]`.
   - Tránh lỗi strict mode trên `h1` (do cả page title và markdown body đều có thẻ heading 1) bằng cách dùng `page.locator("h1").first()`.
   - Tránh lỗi strict mode trên nút «Đăng xuất» (do cả Mobile Drawer ẩn và Desktop Sidebar đều render nút này) bằng cách dùng selector lọc hiển thị: `page.locator('button:has-text("Đăng xuất"):visible').first()`.
2. **Đồng bộ thời gian khi Đăng ký lộ trình (`ensureEnrolled`):**
   - Đảm bảo `ensureEnrolled` chờ phản hồi `POST /api/v1/enrollments` và kiểm tra modal đóng trước khi query `getEnrollment()`, đồng thời bổ sung retry polling ngắn (5 lần x 300ms) để không bị race condition khi backend vừa commit enrollment.
3. **Cơ chế kiểm thử Note CAS Overwrite:**
   - Khi kiểm tra tính năng «Ghi đè bằng bản này», test đã chủ động tạo bản ghi note ban đầu (version 0) trước khi mở Tab 2. Nhờ đó, cả hai tab đều nạp đúng `currentNote` và kích hoạt đúng nút «Ghi đè bằng bản này» khi có xung đột version 409.
4. **Phối hợp Timer Autosave và Navigation Blocker:**
   - Do NoteEditor có debounce autosave 700ms, khi kiểm tra hành vi «Lưu rồi rời» của Navigation Blocker, test đã thực hiện thao tác nhập mới ngay trước khi click điều hướng để đảm bảo cờ `hasUnsavedChanges` đang kích hoạt đúng lúc blocker modal mở ra.
5. **Fixture Giáo trình Cross-revision chuẩn JSON:**
   - Sửa định dạng `authors` thành JSON array string (`'["Antigravity E2E"]'`) và `provenance` thành JSON object string có đầy đủ thuộc tính `ProvenanceResponse` theo yêu cầu của `CurriculumCatalogQueryService.java`.

---

## 7. Giới hạn & Hạng mục Tạm hoãn (Deferred Scope)

- **Mobile Viewport 375px:** Tạm hoãn theo chỉ đạo của người dùng ngày 26/10/2026; không đánh giá đạt/hỏng cho mobile trong đợt này.
- **Tính năng G4/G5:** Các tính năng đổi lịch linh hoạt, tạm dừng/tiếp tục lộ trình, quiz grading, bookmark câu hỏi chưa thuộc phạm vi G3-04 và không được mock hay thêm vào để test.
- **Git Commit:** Không thực hiện commit/push theo quy định bàn giao, giữ nguyên workspace để Codex tiến hành review và nghiệm thu.

---

## 8. Kết luận nghiệm thu nội bộ

Toàn bộ 6 tiêu chí của **Mốc M1** cùng các kịch bản phụ trợ (đa tài khoản, đa tab, phân tách revision, autosave serialization, navigation guards, và UX desktop) đã được kiểm chứng tự động thành công 100% bằng trình duyệt thật kết nối với backend/DB thật. Dự án đã sẵn sàng để Codex thực hiện nghiệm thu chính thức.
