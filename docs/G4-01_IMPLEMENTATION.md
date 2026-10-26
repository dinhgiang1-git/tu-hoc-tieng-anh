# Báo cáo Triển khai G4-01: Nền tảng Lịch học & Màn hình Lịch Desktop

**Mã công việc:** G4-01
**Trạng thái:** Codex nghiệm thu luồng chính G4-01 theo quy trình nhanh ngày 26/10/2026; xem kết quả độc lập ngay bên dưới. PostgreSQL JUnit IT còn vướng loopback môi trường, không ghi là đã đạt ở lượt Codex.
**Baseline xuất phát:** M1 desktop accepted (`67c44c7`)
**Ngày hoàn thành triển khai:** 26/10/2026
**Tài liệu liên quan:** [docs/tasks/G4-01_HANDOFF.md](tasks/G4-01_HANDOFF.md), [docs/tasks/backlog.md](tasks/backlog.md), [docs/api/openapi.yaml](api/openapi.yaml)

## Kết quả Codex nghiệm thu nhanh

- **Đạt chức năng G4-01**, có thể chuyển G4-02. Không có thay đổi regenerate lịch cũ; planner mới chỉ áp dụng khi tạo enrollment mới.
- **31 unit backend liên quan đạt**: planner 10, query service 15, controller 6. Maven compile và Spotless đạt. Log `backend/target/review-g401-targeted.log` có cả lỗi IT môi trường, không phải toàn command thành công.
- **10/10 SchedulePage component tests đạt**, lint không error, production build/TypeScript đạt. Log `frontend/review-g401-targeted.log`. Không chạy lại full FE suite.
- **3/3 E2E thật trên DB dev đạt**: hai case lịch (xem tuần, complete/undo, ngày kết thúc, owner A/B) và smoke M1-1 đăng ký/học/note/reload. Log `frontend/review-g401-e2e.log`. Không chạy lại toàn bộ 16 E2E.
- Reviewer tự sửa: current week/today dùng timezone enrollment thay timezone máy; lỗi tải danh sách enrollment có error/retry thay vì báo chưa đăng ký. Thêm hai regression tương ứng. Sửa test chuyển tuần chờ lần đọc đầu hoàn tất trước khi kiểm tra request kế tiếp.
- Đã xem ảnh `frontend/e2e-screenshots/schedule-desktop-1440.png`; E2E cũng kiểm tra 1280×800.
- **Giới hạn:** JUnit `EnrollmentAndTodayIT` chưa chạy thành công trong lượt này: lần đầu Docker API, sau cấu hình 1.44 thì Java TestRestTemplate không khởi tạo được loopback (`Invalid argument: connect`); thử riêng case G4-01 với IPv4 vẫn vướng cùng lỗi môi trường. Logs `backend/target/review-g401-it.log`, `backend/target/review-g401-it-ipv4.log`. Browser E2E kết nối BE/PostgreSQL thật vẫn đạt. Hoãn xử lý hạ tầng IT này và full regression đến cuối G4 hoặc khi thay đổi có rủi ro mới, không kéo dài phát triển vì test setup.
- Các số full-suite bên dưới là báo cáo của công nhân, không phải số Codex chạy lại. Mobile tiếp tục hoãn; không làm G4-02 trong lượt nghiệm thu này.


---

## 1. Tóm tắt Mục tiêu & Phạm vi Đã Hoàn Thành

Task G4-01 xây dựng nền tảng tính lịch dùng chung, API đọc lịch theo khoảng ngày, hiển thị ngày kết thúc dự kiến theo lịch hiện hành và trang Lịch học desktop cho người học:
1. **Bộ tính lịch thuần túy (Schedule Planner Domain):** Tách thuật toán tính lịch thành hàm thuần không phụ thuộc I/O/DB/Clock; tái sử dụng trong `CreateEnrollmentService` mà không làm thay đổi hành vi nghiệp vụ hay dữ liệu lịch đã lưu.
2. **GET Schedule Endpoint:** Triển khai `GET /api/v1/enrollments/{enrollmentId}/schedule?from=YYYY-MM-DD&to=YYYY-MM-DD` trả về mảng `LessonScheduleResponse` có tính `isOverdue` chính xác theo timezone người học, kèm query projection nhẹ, giới hạn khoảng $\le 366$ ngày và bảo vệ phân quyền theo chủ sở hữu (404 khi truy cập chéo).
3. **Ngày kết thúc theo lịch hiện hành (`estimatedEndDate`):** Bổ sung trường `estimatedEndDate` nullable vào `GET /api/v1/enrollments/{enrollmentId}` (và OpenAPI contract / TypeScript generated types), tính bằng `MAX(planned_date)` của toàn bộ schedule của enrollment.
4. **Màn hình Lịch Desktop (`/schedule`):** Xây dựng trang Lịch học desktop với bộ chọn enrollment (chỉ đổi view, không đổi preferences), điều hướng tuần (Tuần trước, Tuần này, Tuần sau), hiển thị khoảng ngày rõ ràng, bố cục 7 ngày (Thứ Hai đến Chủ Nhật), phân biệt bài học theo lịch/quá hạn/đã hoàn thành, hiển thị ngày nghỉ theo lịch (`rest-day-indicator`), liên kết trực tiếp vào không gian học (`LessonWorkspacePage`) và cập nhật tức thì trạng thái hoàn thành khi quay lại.
5. **Kiểm thử toàn diện:** 162 backend unit tests + 11 PostgreSQL integration tests (`EnrollmentAndTodayIT`) + 106 frontend unit tests + 16/16 Playwright E2E tests trên dev DB an toàn (không chạm tài khoản cá nhân).

---

## 2. Các Thay đổi Chi tiết Theo Từng Thành phần

### 2.1 Backend Domain & Scheduling
- **`vn.englishlearning.scheduling.domain.SchedulableLesson`:** Record mô tả bài học bắt buộc đầu vào có thứ tự sư phạm ổn định.
- **`vn.englishlearning.scheduling.domain.PlannedLesson`:** Record kết quả lịch gồm `lessonId` và `plannedDate`.
- **`vn.englishlearning.scheduling.domain.SchedulePlan`:** Record kết quả tổng gồm `List<PlannedLesson>` và `LocalDate estimatedEndDate` (nullable).
- **`vn.englishlearning.scheduling.domain.SchedulePlanner`:** Hàm thuần túy `plan(...)`:
  - Chuẩn hóa, loại bỏ trùng lặp và kiểm tra tập `studyDays` hợp lệ (reject null/rỗng).
  - Nếu `startDate` không thuộc `studyDays`, tự động tìm ngày học hợp lệ kế tiếp.
  - Mỗi bài học bắt buộc chiếm đúng 1 ngày học; bỏ qua bài học tùy chọn (`is_required == false`).
  - Xử lý mượt mà chuyển tháng, chuyển năm và năm nhuận.
  - Danh sách bài học rỗng trả về kế hoạch rỗng với `estimatedEndDate = null`.
- **Unit Test:** [SchedulePlannerTest.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/scheduling/domain/SchedulePlannerTest.java) (10 test cases bao quát mọi biên logic).

### 2.2 Backend Application & API
- **`CreateEnrollmentService`:** Refactor chuyển sang gọi `SchedulePlanner.plan(...)`; bảo toàn toàn bộ owner lock, idempotency replay, transaction sinh enrollment/preferences/lịch và các kiểm tra giới hạn.
- **`LessonScheduleRepository`:**
  - `findMaxPlannedDateByEnrollmentId(@Param("enrollmentId") UUID enrollmentId)`: Lấy ngày học lớn nhất của enrollment.
  - `findScheduleItemsByEnrollmentIdAndRange(...)`: Query nhẹ qua projection `LessonScheduleItemDto` (chỉ join `lesson.title`, không kéo `content_markdown`, `instructions` hay `user_response`), sắp xếp theo `planned_date ASC, l.orderIndex ASC, s.id ASC`.
- **`EnrollmentResponses`:**
  - `EnrollmentDetailResponse`: Bổ sung `LocalDate estimatedEndDate`.
  - `LessonScheduleResponse`: Bổ sung `boolean isOverdue` bên cạnh `lessonId`, `lessonTitle`, `plannedDate`, `originalDate`, `isCompleted`.
- **`EnrollmentQueryService`:**
  - Tính `estimatedEndDate` cho `getEnrollmentDetail`.
  - Triển khai `getSchedule(enrollmentId, from, to)`:
    * Kiểm tra quyền sở hữu của `CurrentUser`; ném 404 nếu không tìm thấy hoặc không thuộc user.
    * Mặc định khoảng ngày: Nếu không truyền cả hai $\rightarrow$ Tuần hiện tại từ Thứ Hai đến Chủ Nhật theo múi giờ của enrollment (`enrollment.getTimeZone()`). Nếu chỉ truyền `from` $\rightarrow$ `to = from + 6`. Nếu chỉ truyền `to` $\rightarrow$ `from = to - 6`.
    * Kiểm tra biên: Nếu `from.isAfter(to)` hoặc khoảng cách $> 366$ ngày inclusive $\rightarrow$ ném 400 `DomainException(ErrorCode.VALIDATION_FAILED)`.
    * Tính `isOverdue`: Chỉ `true` khi enrollment `ACTIVE`, bài chưa hoàn thành (`isCompleted == false`) và `plannedDate.isBefore(localToday)`. Hôm nay không bị quá hạn. Các trạng thái `PAUSED`, `ABANDONED`, `COMPLETED` không bị quá hạn.
    * Đọc bất biến: Không ghi DB, không tăng version, không sinh event.
- **`EnrollmentController`:** Expose endpoint `GET /api/v1/enrollments/{enrollmentId}/schedule`.

### 2.3 OpenAPI & Contract Types
- **`docs/api/openapi.yaml`:**
  - Bổ sung response 400 `BadRequestProblem` cho `GET /enrollments/{enrollmentId}/schedule`.
  - Bổ sung `estimatedEndDate` (format: date, nullable) vào `EnrollmentDetailResponse`.
  - Bổ sung `isOverdue` (boolean, required) vào `LessonScheduleResponse`.
- **Đồng bộ Typescript Types:** Sinh lại tự động bằng `openapi-typescript` sang cả hai file:
  - `docs/api/generated-types.ts`
  - `frontend/src/generated/api-types.ts`
- **Validation:** Script `node scripts/validate-openapi-examples.js` chạy thành công.

### 2.4 Frontend Implementation
- **API & Query Keys (`frontend/src/features/enrollment/api/enrollment.api.ts`):**
  - Export `LessonScheduleResponse`.
  - Khai báo query keys chuẩn:
    ```ts
    export const scheduleKeys = {
      all: ["schedules"] as const,
      byEnrollment: (enrollmentId: string) => ["schedules", enrollmentId] as const,
      list: (enrollmentId: string, from?: string, to?: string) =>
        ["schedules", enrollmentId, { from, to }] as const,
    };
    ```
  - Triển khai `enrollmentApi.getSchedule(enrollmentId, params, signal)`.
- **Reconciliation & Invalidation (`useLearningProgress.ts` & `LessonWorkspacePage.tsx`):**
  - Bổ sung invalidation `scheduleKeys.byEnrollment(enrollmentId)` vào `useUpdateProgress`, `useSubmitReflection`, và `handleVersionUpdate`. Nhờ đó, khi hoàn thành hoặc hoàn tác bài học trong không gian học tập rồi trở về trang Lịch, UI lập tức phản ánh trạng thái mới nhất.
- **Desktop Schedule Page (`frontend/src/features/schedule/pages/SchedulePage.tsx`):**
  - **Header & Switcher:** Hiển thị tên lộ trình, timezone, danh sách ngày học định dạng tiếng Việt ("Thứ Hai, Thứ Tư, Thứ Sáu"), badge trạng thái màu sắc ("Đang học", "Tạm dừng", "Đã hoàn thành", "Đã kết thúc"), ngày kết thúc dự kiến ("Kết thúc theo lịch hiện tại: DD/MM/YYYY"). Có dropdown chuyển đổi lộ trình (chỉ đổi view, không mutate preferences).
  - **Empty State:** Khi người dùng chưa có lộ trình nào, hiển thị `EmptyState` với CTA chuyển hướng đến `/curricula`.
  - **Điều hướng tuần:** Các nút "Tuần trước" (`schedule-prev-week`), "Tuần này" (`schedule-today`), "Tuần sau" (`schedule-next-week`) và nhãn hiển thị khoảng ngày `DD/MM/YYYY - DD/MM/YYYY` (`schedule-week-range`).
  - **Bố cục 7 ngày (Thứ Hai đến Chủ Nhật):**
    * Thẻ ngày hôm nay được làm nổi bật với viền `primary` và badge "Hôm nay".
    * Thẻ bài học hiển thị tiêu đề, status badge ("Đã hoàn thành", "Quá hạn", "Theo lịch"), ghi chú lịch gốc nếu có đổi lịch (`originalDate !== plannedDate`), và nút "Vào bài học" / "Xem lại" (`lesson-link-{id}`).
    * Ngày nghỉ theo lịch hiển thị container riêng với icon Coffee và badge `data-testid="rest-day-indicator"`.
    * Ngày học không có bài hiển thị "Không có bài học".
- **Router Integration (`frontend/src/app/router.tsx`):** Thay thế placeholder G4 cũ tại `/schedule` bằng `<SchedulePage />`.

---

## 3. Quyết định Kỹ thuật Quan trọng

1. **Phân tách trách nhiệm Planner thuần túy:**
   - Planner không đọc DB, không nhận `Clock`, không phụ thuộc Security Context. Thuật toán là hàm toán học thuần túy nhận đầu vào `(lessons, startDate, studyDays)` trả về `SchedulePlan`. Nhờ vậy, planner có thể tái sử dụng dễ dàng cho tính năng xem trước đổi lịch (Preview) của G4-02 sau này mà không gây side-effect.
2. **Không tái tạo lịch cũ (Immutable Persisted Schedule):**
   - Không chạy migration dời ngày, không tính toán lại `plannedDate` cho các enrollment đang có trong hệ thống khi khởi động hay khi GET. Dữ liệu đã lưu trong bảng `lesson_schedule` là nguồn chuẩn duy nhất.
3. **Mặc định khoảng ngày và Múi giờ:**
   - Khi không truyền tham số `from` / `to`, backend tính thứ Hai và Chủ nhật của tuần hiện tại theo múi giờ lưu trên chính `Enrollment` (`timeZone`), đảm bảo tính nhất quán kể cả khi người học truy cập từ các khu vực múi giờ khác nhau.
4. **Quy tắc tính `isOverdue`:**
   - Bài học chỉ được đánh dấu quá hạn khi: enrollment đang ở trạng thái `ACTIVE`, bài chưa hoàn thành (`isCompleted == false`), và `plannedDate < localToday`. Ngày hôm nay tuyệt đối không quá hạn. Các enrollment đã tạm dừng (`PAUSED`), đã kết thúc (`ABANDONED`) hoặc đã hoàn thành (`COMPLETED`) không hiển thị cảnh báo quá hạn.
5. **Hiệu năng Query (Anti N+1 & Heavy Column Exclusion):**
   - Sử dụng Spring Data projection `LessonScheduleItemDto` và câu lệnh JPQL join lấy `lesson.title`. Không truy vấn các trường nặng như `content_markdown`, `instructions`, `user_response`. Tận dụng index có sẵn `idx_lesson_schedule_enrollment_planned_date`.

---

## 4. Lỗi Gặp phải & Cách Khắc phục

1. **Spotless Check Failure:**
   - *Nguyên nhân:* Các file mới tạo chưa tuân thủ quy tắc định dạng của Spotless.
   - *Khắc phục:* Chạy `.\mvnw.cmd spotless:apply -ntp`.
2. **MockMvc Standalone Serialization:**
   - *Nguyên nhân:* Khởi tạo standalone MockMvc với `MappingJackson2HttpMessageConverter(new ObjectMapper())` mặc định serialize `LocalDate` thành mảng `[year, month, day]`.
   - *Khắc phục:* Sử dụng `Jackson2ObjectMapperBuilder.json().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()` trong `EnrollmentControllerTest`.
3. **Testcontainers Windows Docker Pipe:**
   - *Nguyên nhân:* Môi trường Windows Docker Desktop yêu cầu API version 1.44 cho named pipe.
   - *Khắc phục:* Luôn truyền `-Dapi.version=1.44` khi thực thi `mvnw verify` / `mvnw test`.
4. **Idempotency Key Header Format trong IT Test:**
   - *Nguyên nhân:* Header `Idempotency-Key` yêu cầu định dạng UUID hợp lệ.
   - *Khắc phục:* Dùng `UUID.randomUUID().toString()` làm giá trị header trong `EnrollmentAndTodayIT`.
5. **Oxlint Purity & Memoization Warnings:**
   - *Nguyên nhân:* `useMemo(() => new Date(), [])` và `myEnrollmentsQuery.data ?? []` gây warning về purity và dependency re-evaluation.
   - *Khắc phục:* Dùng `useState(() => new Date())` và memoize `enrollments` bằng `useMemo` với dependency `myEnrollmentsQuery.data`.
6. **Playwright Mobile Aside vs Desktop Aside Selection:**
   - *Nguyên nhân:* Trang có hai thẻ `<aside>` (một cho drawer mobile `md:hidden`, một cho desktop `hidden md:flex`), selector `aside a[href="/schedule"]` trúng phần tử mobile đang bị ẩn trên màn hình 1440px.
   - *Khắc phục:* Dùng selector rõ ràng `aside.md\\:flex a[href="/schedule"]`.
7. **E2E Timeout trên Vite HMR:**
   - *Nguyên nhân:* `page.waitForLoadState("networkidle")` đôi khi chờ đủ 500ms im lặng mạng trong môi trường Vite dev server có keep-alive websocket.
   - *Khắc phục:* Chuyển sang `page.waitForLoadState("domcontentloaded")` kết hợp `expect(locator).toBeVisible()` chờ phần tử UI cụ thể.

---

## 5. Kết quả Kiểm thử Toàn diện

### 5.1 Backend Tests
- **Format Code:**
  ```powershell
  .\mvnw.cmd spotless:check -ntp
  # Kết quả: BUILD SUCCESS (134 files clean)
  ```
- **Backend Unit Tests:**
  ```powershell
  .\mvnw.cmd test "-Dapi.version=1.44" -ntp
  # Kết quả: Tests run: 162, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
  # Bao gồm:
  # - SchedulePlannerTest: 10/10 passed
  # - EnrollmentQueryServiceTest: 15/15 passed
  # - EnrollmentControllerTest: 6/6 passed
  ```
- **Backend Integration Tests (PostgreSQL Testcontainers):**
  ```powershell
  .\mvnw.cmd test -Dtest="EnrollmentAndTodayIT" "-Dapi.version=1.44" -ntp
  # Kết quả: Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
  # Bao gồm: g4_01_scheduleEndpoint_comprehensiveIntegrationTest kiểm tra:
  # - Range defaults (Monday to Sunday theo enrollment timezone)
  # - Full range query đúng thứ tự sư phạm
  # - 404 khi truy cập không phải chủ sở hữu
  # - 400 khi from > to hoặc range > 366 ngày
  # - Bất biến version, timestamp, event (không có mutation)
  # - SqlStatementInspector xác minh không select content_markdown, instructions, user_response
  ```

### 5.2 Frontend Tests
- **Frontend Typecheck:**
  ```powershell
  npm run typecheck
  # Kết quả: 0 errors
  ```
- **Frontend Linter:**
  ```powershell
  npm run lint
  # Kết quả: 0 errors (1 warning thông tin đã biết về react-hook-form form.watch)
  ```
- **Frontend Unit & Component Tests:**
  ```powershell
  npm test
  # Kết quả: 17 passed (17 test files), 106 passed (106 tests)
  # Bao gồm: SchedulePage.test.tsx: 8/8 tests passed
  ```
- **Frontend Production Bundle:**
  ```powershell
  npm run build
  # Kết quả: Vite built in ~1.0s, không có lỗi biên dịch
  ```

### 5.3 Playwright E2E Tests
- **E2E Runner:**
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1
  # Kết quả: 16 passed (16 tests, 2.2m)
  # - m1-core-loop.e2e.ts: 6/6 passed
  # - multi-account-and-sync.e2e.ts: 4/4 passed
  # - navigation-and-desktop-ux.e2e.ts: 4/4 passed
  # - schedule-desktop.e2e.ts: 2/2 passed
  ```
- **Các kịch bản G4-01 E2E được xác minh:**
  1. Đăng nhập $\rightarrow$ Đăng ký lộ trình $\rightarrow$ Điều hướng tới `/schedule` qua sidebar.
  2. Hiển thị thông tin lộ trình, timezone, ngày học trong tuần, trạng thái "Đang học", và ngày kết thúc dự kiến.
  3. Điều hướng tuần (Tuần trước, Tuần này, Tuần sau) và nhãn khoảng ngày `DD/MM/YYYY - DD/MM/YYYY`.
  4. Hiển thị đủ 7 ngày và huy hiệu ngày nghỉ theo lịch (`rest-day-indicator`).
  5. Kiểm tra không bị tràn ngang (horizontal overflow) trên cả hai viewport 1440×900 và 1280×800.
  6. Nhấn link bài học vào không gian học tập $\rightarrow$ hoàn thành toàn bộ bài học $\rightarrow$ quay lại lịch thấy badge "Đã hoàn thành".
  7. Quay lại workspace hủy hoàn thành (undo) $\rightarrow$ quay lại lịch cập nhật bài học chưa hoàn thành, ngày học và ngày kết thúc dự kiến giữ nguyên vẹn.
  8. Kiểm tra phân tách tài khoản: Tài khoản B gọi `GET /enrollments/{enrollmentIdA}/schedule` nhận đúng mã lỗi 404; mở trang Lịch chỉ thấy lịch riêng của tài khoản B.

### 5.4 Screenshots Đã Tạo
Các ảnh chụp màn hình desktop thực tế đã được lưu vào thư mục `frontend/e2e-screenshots/`:
- `frontend/e2e-screenshots/schedule-desktop-1440.png` (Giao diện chuẩn 1440×900)
- `frontend/e2e-screenshots/schedule-desktop-1280.png` (Giao diện kiểm tra nhanh 1280×800)

---

## 6. Ràng buộc & Phạm vi Tuân thủ

- Không sửa hoặc commit 2 file ngoài phạm vi: `docs/DAY2_REVIEW.md` và thư mục `backend/src/main/resources/META-INF/spring/`.
- Không xóa, sửa dữ liệu cá nhân (`admin`, `dinhgiang1`) trong database dev; toàn bộ fixture E2E được cô lập trong tiền tố `e2e_*`.
- Không triển khai trước các tính năng thuộc các task sau:
  - Preview / Commit đổi lịch (thuộc G4-02).
  - Tạm dừng / Tiếp tục lộ trình (thuộc G4-03).
  - Kéo thả calendar, ngày nghỉ lễ quốc gia, dashboard thống kê mới.
- Không tự ý commit hoặc push Git (Codex sẽ thực hiện nghiệm thu và quản lý Git).
