# Danh mục Công việc Chi tiết (Project Backlog & Task Specifications)

**Ngày cập nhật:** 24/10/2026  
**Trạng thái:** Sẵn sàng giao việc sau G0  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../../KE_HOACH_DU_AN.md), [BE_CODE_CONVENTION.md](../../BE_CODE_CONVENTION.md), [FE_CODE_CONVENTION.md](../../FE_CODE_CONVENTION.md), [product-rules.md](../product-rules.md), [openapi.yaml](../api/openapi.yaml)

---

## 1. Sơ đồ Tổng quan Phụ thuộc giữa các Giai đoạn

```text
[ G0: Khóa đặc tả & Mẫu nội dung ] (ĐÃ HOÀN THÀNH)
       │
       ▼
[ G1: Nền tảng Kỹ thuật & Tài khoản ]
       │
       ▼
[ G2: Gói Nội dung & Danh mục Lộ trình ]
       │
       ▼
[ G3: Vòng học Cốt lõi có Lưu Tiến độ ] ───(Mốc M1: Dùng thử hàng ngày)
       │
       ├─────────────────────────────────────────┐
       ▼                                         ▼
[ G4: Lịch cá nhân Linh hoạt & Đổi lịch ]   [ G5: Quiz & Ôn tập Tối thiểu ]
       │                                         │
       └────────────────────┬────────────────────┘
                            ▼
              [ G6: Hoàn thiện Lộ trình A & Báo cáo ]
                            │
                            ▼
              [ G7: Vận hành & Phát hành Cá nhân ] ───(Mốc M2: Hoàn chỉnh)
                            │
                            ▼ (Tùy chọn bổ sung)
              [ PA: Thử nghiệm Phát âm Anh-Anh Local ]
```

---

## 2. Chi tiết Backlog Giai đoạn G1 — Nền tảng Kỹ thuật và Tài khoản

### Task G1-01: Khảo sát WSL/DB, Khởi tạo Backend Spring Boot Skeleton & Flyway
- **Trạng thái:** [x] **HOÀN THÀNH** (Đã giải quyết F1, F2, F3 theo review).
- **Mục tiêu:** Dựng khung ứng dụng Spring Boot Java 21, kết nối an toàn đến PostgreSQL trong Docker/WSL mà không làm thay đổi container hiện có, cấu hình Flyway migration ban đầu.
- **Đầu vào:** `BE_CODE_CONVENTION.md`, `ADR-001`, cấu hình Docker WSL hiện hữu.
- **Phạm vi:**
  * Khởi tạo dự án Spring Boot tại thư mục `backend/` với Maven Wrapper (`mvnw`, `mvnw.cmd`).
  * Khảo sát thông số container PostgreSQL hiện có (chỉ đọc); tạo database `english_learning_db` và user riêng nếu cần.
  * Cấu hình `application.yml` (profile `dev`, `test`).
  * Cài đặt Flyway và tạo migration đầu tiên `V001__create_identity_and_shared_tables.sql` (bảng `app_user`, `learner_preferences`).
  * Cấu hình bean `ApplicationClock` (Clock injection) và cấu hình mã hóa mật khẩu `BCryptPasswordEncoder`.
  * Cấu hình định dạng lỗi Problem Details RFC 7807 (`GlobalExceptionHandler`, `ProblemFactory`).
  * Endpoint kiểm tra sức khỏe `GET /actuator/health`.
- **Ngoài phạm vi:** Giao diện người dùng, nghiệp vụ lộ trình và học tập.
- **Tiêu chí nghiệm thu (DoD):**
  1. Chạy `.\mvnw.cmd clean test` thành công trên Windows.
  2. Ứng dụng khởi động kết nối đúng PostgreSQL trong WSL/Docker; migration V001 thực thi an toàn, chạy lại không lỗi (idempotent).
  3. Lỗi 404/400 bất kỳ trả về chuẩn `application/problem+json` với các trường: `status`, `code`, `detail`, `traceId`, `fieldErrors`.
  4. Không có mật khẩu hoặc secret nào bị commit vào Git.

---

### Task G1-02: Xác thực Session, CSRF Protection và Module Identity
- **Trạng thái:** [x] **HOÀN THÀNH**
- **Mục tiêu:** Hoàn thiện cơ chế đăng nhập, đăng xuất, duy trì phiên bằng HttpOnly cookie, bảo vệ CSRF và quản lý người dùng hiện hành.
- **Đầu vào:** `openapi.yaml`, `ADR-003`, Task G1-01.
- **Phạm vi:**
  * Cấu hình Spring Security với session-based authentication và CSRF token repository cho SPA.
  * Endpoint `GET /api/v1/auth/csrf`: trả về `{ headerName: "X-CSRF-TOKEN", token: "..." }` với `Cache-Control: no-store`.
  * Endpoint `POST /api/v1/auth/login`: xác thực username/password, tạo session, trả về `UserResponse`.
  * Endpoint `POST /api/v1/auth/logout`: hủy session, trả về 204.
  * Endpoint `GET /api/v1/me`: trả về thông tin người dùng từ principal phiên hiện tại.
  * Endpoint `GET /api/v1/me/preferences` & `PATCH /api/v1/me/preferences`: lấy và cập nhật thời lượng mục tiêu cùng enrollment tập trung (chuẩn hóa 10-180 phút theo OpenAPI).
  * Viết script/CLI bootstrap `BootstrapUserRunner` tạo tài khoản người dùng ban đầu với mật khẩu đọc từ biến môi trường `BOOTSTRAP_ADMIN_PASSWORD`.
  * `SecurityProblemWriter` đồng bộ định dạng lỗi 401/403 với `GlobalExceptionHandler`.
- **Ngoài phạm vi:** Đăng ký công khai, quên mật khẩu qua email.
- **Tiêu chí nghiệm thu (DoD):**
  1. Đăng nhập đúng trả về 200 kèm Set-Cookie HttpOnly; đăng nhập sai trả về 401 với code `INVALID_CREDENTIALS`.
  2. Request `POST /auth/login` thiếu header `X-CSRF-TOKEN` bị từ chối với lỗi 403 `CSRF_INVALID`.
  3. Truy cập `/api/v1/me` khi chưa đăng nhập trả về 401 Problem Details; sau khi đăng nhập trả về đúng thông tin user.
  4. Integration test với Testcontainers PostgreSQL mô phỏng 2 tài khoản riêng biệt: User A không bao giờ truy cập được dữ liệu của User B.

---

### Task G1-03: Khởi tạo Frontend React TSX + Tailwind Vite Shell, Layout Sidebar & Auth Flow
- **Trạng thái:** [x] **HOÀN THÀNH**
- **Mục tiêu:** Xây dựng khung ứng dụng frontend React SPA, cài đặt bộ UI primitives, tích hợp API client duy nhất, layout AppShell/AppSidebar và màn hình Đăng nhập.
- **Đầu vào:** `FE_CODE_CONVENTION.md`, `openapi.yaml`, `wireframes.md`, Task G1-02.
- **Phạm vi:**
  * Khởi tạo dự án `frontend/` bằng Vite + React + TypeScript TSX.
  * Cài đặt Tailwind CSS với semantic design tokens tại `src/styles/tokens.css` và `globals.css`.
  * Cài đặt các UI primitives từ `shadcn/ui` (`Button`, `Input`, `Label`, `Dialog`, `Progress`, `Skeleton`).
  * Triển khai thư viện dùng chung `src/lib/api/api-client.ts` (fetch, CSRF adapter, parse Problem Details, abort signal, decoder).
  * Sinh TypeScript types tự động từ `openapi.yaml` vào `src/generated/api-types.ts`.
  * Cài đặt React Router và TanStack Query client.
  * Triển khai `AppShell` với `AppSidebar` (hỗ trợ responsive mobile drawer và desktop collapse).
  * Triển khai trang Đăng nhập (`/login`) kết nối với BE G1-02, xử lý loading spinner, lỗi 401, lưu trạng thái đăng nhập.
  * Chức năng Đăng xuất: gọi API, xóa query cache cá nhân, xóa token CSRF, chuyển hướng về `/login`.
- **Ngoài phạm vi:** Các màn hình bài học và lộ trình (thuộc G2, G3).
- **Tiêu chí nghiệm thu (DoD):**
  1. `npm run typecheck`, `npm run lint` đạt; `npm run build` hoàn thành (ghi nhận cảnh báo chunk size minified 528.89 kB, không chặn shell).
  2. Bằng chứng kiểm thử tự động Frontend (`vitest`): 24/24 tests đạt trong 3 test suites; bản tái hiện review dùng key preferences thật cũng đạt 3/3.
     - F1: Drawer mobile giữ mở khi re-render; đóng đúng khi đổi route hoặc phím Escape; focus trap tuần hoàn; khi đóng có `inert`, `aria-hidden="true"`, `visibility: hidden` không nhận phím Tab.
     - F2: Đăng xuất chờ server xác nhận 204 hoặc 401; tự động refresh và retry 1 lần khi gặp 403 `CSRF_INVALID`; giữ nguyên user/session và hiện banner lỗi có nút thử lại khi gặp 500 hoặc lỗi mạng; chặn double-click khi đang đăng xuất.
     - F3 (R1 & R2 theo `docs/G1-03_RECHECK.md`):
       * R1: Phân biệt rõ `/me` 401 (anonymous) với 500/mạng (báo lỗi kèm nút Thử lại); phiên hết hạn nhận diện 401 `AUTHENTICATION_REQUIRED` từ query lẫn mutation, hủy in-flight queries, chỉ giữ query `/me` chính xác rồi đặt null, xóa mọi cache khác gồm `authKeys.preferences()` và dọn CSRF. Logout dùng cùng quy tắc. Kiểm chứng A hết phiên → B đăng nhập không thấy preferences của A khi request B đang pending hoặc lỗi.
       * R2: Request hoặc login nhận 403 `CSRF_INVALID` tự động clear token cũ trong bộ nhớ, không auto-replay mutation nghiệp vụ; người dùng thử lại được cấp token mới và request thành công.
  3. Bằng chứng kiểm thử Backend:
     - Formatter (`mvnw.cmd -B spotless:check`): Đạt 29/29 files Java.
     - Unit tests (`mvnw.cmd -B test`): 3/3 tests passed.
     - Trạng thái Docker runtime: Docker Desktop hiện ở trạng thái `Stopped` (`dockerDesktopLinuxEngine` named pipe không khả dụng), do đó không chạy Testcontainers integration tests ở lượt này; cơ sở dữ liệu dev hoàn toàn không bị ảnh hưởng.
  4. Luồng HTTP thật và browser E2E 375px/1440px chưa được chạy lại trong lượt chốt này. Chốt phạm vi FE bằng regression và check ở trên; giữ mục xác minh runtime riêng.

- **Kết luận FE:** Đã chốt sau sửa trực tiếp bộ lọc cache; chuyển G2-01 theo `docs/tasks/G2-01_HANDOFF.md`, không mở lại vòng auth/style.

---

## 3. Chi tiết Backlog Giai đoạn G2 — Gói Nội dung & Danh mục Lộ trình

### Task G2-01: Bộ Migration Schema Nội dung, Importer CLI & Quy trình Publish
- **Trạng thái:** Hoàn tất triển khai code, schema và unit tests (Chờ runtime Testcontainers khi Docker sẵn sàng)
- **Mục tiêu:** Tạo các bảng quản lý nội dung (`curriculum`, `curriculum_revision`, `curriculum_phase`, `curriculum_week`, `lesson`, `activity`, `resource`, `activity_resource`), xây dựng công cụ CLI backend để validate, dry-run, import và publish gói nội dung từ thư mục `content/`.
- **Đầu vào:** `manifest.schema.json`, `week.schema.json`, `resources.schema.json`, `manifest.json`, `week-01.json`, `ADR-005`.
- **Thực hiện:**
  * Flyway migration: [V003__create_curriculum_tables.sql](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/resources/db/migration/V003__create_curriculum_tables.sql) tạo đầy đủ bảng quan hệ với constraints, FK, unique indexes và trigger bảo vệ revision `PUBLISHED` bất biến (`trg_curriculum_revision_immutable`).
  * Domain entities & repositories:
    - [Curriculum](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Curriculum.java), [CurriculumRevision](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumRevision.java), [CurriculumPhase](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumPhase.java), [CurriculumWeek](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumWeek.java), [Lesson](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Lesson.java), [Activity](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Activity.java), [Resource](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Resource.java).
    - Enums: [RevisionStatus](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/RevisionStatus.java), [ActivityType](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/ActivityType.java), [CompletionPolicy](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CompletionPolicy.java), [MediaType](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/MediaType.java), [VerificationStatus](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/VerificationStatus.java).
    - Policy: [ProgressPolicy](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/learning/domain/ProgressPolicy.java).
    - Repositories: `CurriculumRepository`, `CurriculumRevisionRepository`, `CurriculumPhaseRepository`, `CurriculumWeekRepository`, `LessonRepository`, `ActivityRepository`, `ResourceRepository`.
  * Validator: [ContentPackageValidator](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/ContentPackageValidator.java) dùng JSON Schema Draft 2020-12 thực thụ (`json-schema-validator:1.5.6`), kiểm tra schema cho manifest, week và resources; kiểm tra trùng lặp key; kiểm tra tham chiếu resource; kiểm tra file Markdown tồn tại; chặn đường dẫn tuyệt đối, traversal (`..`, `.`) và symlink trỏ ra ngoài package; giữ nguyên trạng thái `UNVERIFIED_DRAFT`; chỉ đọc tuần được manifest tham chiếu (không gom `drafts/`).
  * Checksum calculator: [PackageChecksumCalculator](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/PackageChecksumCalculator.java) băm SHA-256 toàn diện gồm manifest, resources, từng file tuần và toàn bộ nội dung Markdown (chuẩn hóa LF).
  * Application Services:
    - [ImportCurriculumPackageService](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/ImportCurriculumPackageService.java): chạy dry-run hoặc import toàn bộ trong 1 transaction `@Transactional` ở trạng thái `DRAFT`; idempotent đối với cùng checksum; báo lỗi `ContentConflictException` khi trùng version nhưng khác nội dung; chặn sửa đổi revision `PUBLISHED`/`ARCHIVED`.
    - [PublishCurriculumRevisionService](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/PublishCurriculumRevisionService.java): kiểm tra tính đầy đủ của bài bắt buộc (markdown không rỗng, có activity), chuyển trạng thái sang `PUBLISHED`, báo cảnh báo tài nguyên `UNVERIFIED_DRAFT`.
  * CLI Tool: [CurriculumCli](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/cli/CurriculumCli.java) hỗ trợ 4 lệnh: `validate`, `dry-run`, `import`, `publish`.
    - `validate` và `dry-run` chạy độc lập với filesystem, không chạm DB, không khởi động web server.
    - `import` và `publish` chạy với `WebApplicationType.NONE`, bỏ qua `BootstrapUserRunner`, yêu cầu cấu hình DB tường minh (`--db-url`, `CONTENT_DB_URL` hoặc `--confirm-db`), chặn ghi âm thầm vào dev DB.
- **Lệnh CLI thực tế đã kiểm tra:**
  * Validate:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content validate content/pilot-curriculum/v1
    ```
    Kết quả: SUCCESSFUL (0 errors, 5 warnings cho tài nguyên UNVERIFIED_DRAFT).
  * Dry-run:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content dry-run content/pilot-curriculum/v1
    ```
    Báo cáo: 1 phase, 1 tuần, 6 lessons (5 bắt buộc, 1 tùy chọn), 21 activities (19 bắt buộc hiệu lực), 5 resources (5 UNVERIFIED_DRAFT). Checksum: `98b297866781af20cb6800835618b233a8b73328de6a9e6edc4009526e65c961`.
  * Import chặn DB không xác nhận:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content import content/pilot-curriculum/v1
    ```
    Kết quả: Bị chặn (Exit code 1, `SECURITY CHECK FAILED: Explicit database target configuration is required`).
- **Kiểm thử đã chạy:**
  * Spotless: `.\mvnw.cmd spotless:check` đạt 68/68 files clean.
  * Unit tests: `.\mvnw.cmd test` đạt 27/27 tests passed:
    - `ContentPackageValidatorTest` (6 tests: package thật, file thiếu, traversal, absolute path, resource không tồn tại, trùng key).
    - `ImportAndPublishServiceTest` (6 tests: import mới, idempotency cùng package, conflict khác checksum, conflict published revision, publish thành công, publish trùng lặp).
    - `PackageChecksumCalculatorTest` (3 tests: tính tất định, đổi markdown đổi checksum, đổi resources đổi checksum).
    - `DryRunAndCliTest` (5 tests: số liệu dry-run thật, CLI validate, CLI dry-run, CLI import chặn DB, CLI publish chặn DB).
    - `ProgressPolicyTest` (4 tests: bảng chân trị required hiệu lực, tính %, validation input).
    - `ProblemFactoryTest` (2 tests), `ApplicationClockConfigTest` (1 test).
  * Integration test: [CurriculumImportPublishIT](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/curriculum/CurriculumImportPublishIT.java) đã sẵn sàng. Do Docker Desktop đang `Stopped`, test suite này chưa chạy qua `mvn verify`; cơ sở dữ liệu dev hoàn toàn không bị ảnh hưởng.

---

### Task G2-02: API Catalog, Outline & Màn hình Danh mục / Chi tiết Lộ trình
- **Mục tiêu:** Cung cấp API và giao diện để người học xem danh sách lộ trình sẵn có, chi tiết các giai đoạn, tuần học và danh sách buổi học mà không tải toàn bộ nội dung bài viết nặng.
- **Đầu vào:** Task G2-01, `openapi.yaml`, `wireframes.md`.
- **Phạm vi:**
  * Backend:
    * `GET /api/v1/curricula`: danh sách lộ trình có revision Published.
    * `GET /api/v1/curricula/{slug}`: chi tiết lộ trình kèm thông tin tác giả, chuẩn ngữ âm (`en-GB`), mục tiêu.
    * `GET /api/v1/revisions/{id}/outline`: mục lục nhẹ phân cấp (Phase $\rightarrow$ Week $\rightarrow$ Lesson).
  * Frontend:
    * Trang Danh mục Lộ trình (`/curricula`).
    * Trang Chi tiết Lộ trình (`/curricula/:slug`): Accordion các giai đoạn và tuần học, danh sách buổi học với thời lượng ước tính và huy hiệu [Bắt buộc] / [Tùy chọn].
    * Hiển thị rõ nguồn gốc giáo trình và chuẩn phát âm Anh-Anh.
- **Tiêu chí nghiệm thu (DoD):**
  1. Hiển thị đúng Lộ trình Thử nghiệm (Pilot) với 1 tuần nội dung hoàn chỉnh (và hiển thị tuần nháp phác thảo ở trạng thái dự kiến nếu có).
  2. Bấm mở từng tuần hiển thị danh sách các buổi học kèm thời lượng ước tính; không lộ trước đáp án hoặc nội dung chi tiết.
  3. Lộ trình chưa publish không xuất hiện trên catalog.
  4. UI có trạng thái Skeleton loading và Error state có nút retry.

---

## 4. Chi tiết Backlog Giai đoạn G3 — Vòng học Cốt lõi có Lưu Tiến độ (Mốc M1)

### Task G3-01: Đăng ký Lộ trình (Enrollment), Lịch ban đầu & Trang "Hôm nay"
- **Mục tiêu:** Người học bấm đăng ký lộ trình, hệ thống sinh lịch học ban đầu và hiển thị hành động chính trên trang "Hôm nay" theo đúng quy tắc ưu tiên và 8 trạng thái.
- **Đầu vào:** `product-rules.md` (Mục 5), `openapi.yaml`, Task G2-02.
- **Phạm vi:**
  * Backend:
    * Flyway migration `V004__create_enrollment_and_learning_tables.sql` (`enrollment`, `lesson_schedule`, `activity_progress`, `learning_event`).
    * Service `CreateEnrollmentService`: nhận ngày bắt đầu, timezone, studyDays, phân bổ 1 buổi bắt buộc cho 1 ngày học, hỗ trợ Idempotency-Key.
    * `GET /api/v1/me/today`: truy vấn bài theo 8 trạng thái (`NO_ENROLLMENT`, `PAUSED`, `IN_PROGRESS`, `DUE_TODAY`, `REST_DAY`, `OVERDUE`, `UPCOMING`, `CURRICULUM_COMPLETED`).
    * `GET /api/v1/enrollments/{enrollmentId}`: trả về chi tiết enrollment kèm `enrollmentVersion` để khôi phục phiên sau khi tải lại.
  * Frontend:
    * Dialog/Form đăng ký lộ trình: chọn ngày bắt đầu, chọn các thứ học (checkboxes), mục tiêu phút/ngày.
    * Trang "Hôm nay" (`/today`): Card hành động chính nổi bật, thông báo ngày nghỉ nếu rơi vào ngày nghỉ, danh sách bài quá hạn thu gọn.
- **Tiêu chí nghiệm thu (DoD):**
  1. Người học chọn học 3 ngày/tuần (T2-T4-T6) bắt đầu từ 26/10: 5 buổi bắt buộc được xếp đúng vào 26/10, 28/10, 30/10, 02/11, 04/11. Ngày T3, T5, T7, CN không có bài bắt buộc.
  2. Một người học chỉ tạo được tối đa 1 enrollment Active/Paused cho cùng 1 lộ trình.
  3. Trang Hôm nay hiển thị đúng Buổi 1 là hành động chính với nút `"Bắt đầu học ➔"`.
  4. Khi người học chưa đăng ký lộ trình nào, `GET /api/v1/me/today` trả về trạng thái `NO_ENROLLMENT` với các trường chi tiết null an toàn, UI điều hướng người học đến trang danh mục.

---

### Task G3-02: Cập nhật Tiến độ Nhiệm vụ (Complete / Undo), Nộp Phản tư & Tóm tắt Tiến độ
- **Mục tiêu:** Người học hoàn thành từng nhiệm vụ hoặc nộp phản tư, hệ thống tính toán lại tiến độ nguyên tử, hỗ trợ hoàn tác và đồng bộ qua phiên với optimistic lock.
- **Đầu vào:** `product-rules.md` (Mục 4, 6), `BE_CODE_CONVENTION.md` (Mục 6.1), `openapi.yaml`.
- **Phạm vi:**
  * Backend:
    * `PUT /api/v1/enrollments/{id}/activities/{activityId}/progress`: nhận status (`COMPLETED` / `IN_PROGRESS`) và `expectedVersion`.
    * `POST /api/v1/enrollments/{id}/activities/{activityId}/response/submit`: nộp câu trả lời phản tư bắt buộc text kèm `expectedVersion`, lưu câu trả lời và đánh dấu `COMPLETED` trong cùng 1 transaction nguyên tử, trả về `ProgressResponse`.
    * Cập nhật `activity_progress`, ghi `learning_event`, tăng `enrollmentVersion`.
    * Tự động kiểm tra hoàn thành Buổi học và hoàn thành Lộ trình.
    * Xử lý Undo khi enrollment đang `COMPLETED` (quay lại `ACTIVE` an toàn).
    * `GET /api/v1/enrollments/{id}/summary`: trả về số lượng và phần trăm chính xác kèm `enrollmentVersion`.
  * Frontend:
    * Hook `useUpdateProgress`: gửi mutation, xử lý pending, cập nhật TanStack Query cache.
    * Hook `useSubmitReflection`: gửi mutation nộp phản tư kèm expectedVersion.
    * Huy hiệu trạng thái và thanh tiến độ tổng thể.
- **Tiêu chí nghiệm thu (DoD):**
  1. Bấm hoàn thành nhiệm vụ $\rightarrow$ tải lại trang $\rightarrow$ trạng thái vẫn được giữ nguyên `COMPLETED`.
  2. Bấm hoàn thành liên tiếp hai lần không làm tăng gấp đôi số đếm trong learning event (idempotent).
  3. Bấm Bỏ hoàn thành (Undo) $\rightarrow$ tiến độ buổi học và lộ trình giảm tương ứng.
  4. Mở 2 tab cùng sửa 1 nhiệm vụ: tab thao tác sau bị từ chối 409 `VERSION_CONFLICT`, hiển thị thông báo tải lại trang; sau khi fetch lại `GET /enrollments/{id}`, tab có thể tiếp tục với version mới nhất.

---

### Task G3-03: Không gian Học tập (Learning Workspace), Trình đọc Markdown & Ghi chú Tự lưu
- **Mục tiêu:** Màn hình học bài chi tiết tập trung, đọc Markdown an toàn, chuyển đổi giữa các nhiệm vụ, nhập câu trả lời thực hành (lưu nháp và nộp) và ghi chú tự lưu (autosave).
- **Đầu vào:** `wireframes.md` (Mục 4), `FE_CODE_CONVENTION.md` (Mục 8), `openapi.yaml`.
- **Phạm vi:**
  * Backend:
    * `GET /api/v1/enrollments/{id}/lessons/{lessonId}`: trả về nội dung Markdown, danh sách nhiệm vụ, trạng thái học của người dùng, câu trả lời đã lưu và `enrollmentVersion`.
    * `PUT /api/v1/enrollments/{id}/activities/{activityId}/response`: lưu nháp câu trả lời bài thực hành/phản tư (không làm thay đổi trạng thái hoàn thành).
    * Module Notes: API CRUD ghi chú (`/api/v1/notes`, `PATCH /notes/{id}` với `expectedVersion`).
  * Frontend:
    * Trang Không gian học (`/enrollments/:enrollmentId/lessons/:lessonId`).
    * Trình hiển thị nội dung Markdown an toàn (sanitize raw HTML, hỗ trợ ký tự IPA rõ nét).
    * Bộ hiển thị nhiệm vụ đa năng (`ActivityRenderer`): hỗ trợ `READING`, `EXTERNAL_RESOURCE` (link mở tab mới kèm `noopener`), `PRACTICE` (text input kèm nút lưu nháp), `REFLECTION` (nút "Lưu nháp" và nút "Nộp & Hoàn thành").
    * Panel ghi chú cá nhân với hook `useAutosaveNote` (debounce 700ms, xếp hàng request, nhãn Đang lưu/Đã lưu/Lỗi lưu).
    * Chế độ tập trung (Focus Mode): ẩn thanh sidebar điều hướng, chỉ giữ lại bài học.
- **Tiêu chí nghiệm thu (DoD):**
  1. Hiển thị mượt mà bài học Buổi 1 của Pilot Curriculum với đầy đủ bảng IPA và ký hiệu ngữ âm.
  2. Gõ ghi chú cá nhân $\rightarrow$ sau 700ms tự lưu lên server $\rightarrow$ tải lại trang ghi chú vẫn còn nguyên.
  3. Gặp sự cố mất mạng khi đang lưu ghi chú: hiển thị nhãn `"Chưa lưu được"` và nút `"Thử lại"`, không báo đã lưu giả.
  4. Hoàn thành nhiệm vụ cuối cùng trong buổi $\rightarrow$ buổi học tự động chuyển sang `COMPLETED` và giao diện gợi ý chuyển sang buổi tiếp theo.

---

### Task G3-04: Kiểm thử E2E Vòng học Cốt lõi & Hoàn thiện Mốc M1
- **Mục tiêu:** Kiểm chứng toàn diện hành trình người học từ Đăng nhập $\rightarrow$ Đăng ký $\rightarrow$ Học bài $\rightarrow$ Ghi chú $\rightarrow$ Hoàn thành $\rightarrow$ Hôm nay cập nhật đúng.
- **Đầu vào:** Toàn bộ code G1–G3.
- **Tiêu chí nghiệm thu (DoD):**
  1. Vượt qua toàn bộ 6 tiêu chí nghiệm thu bắt buộc của mốc M1 được quy định trong `KE_HOACH_DU_AN.md` (Mục 11 - G3).
  2. Không có lỗi rò rỉ dữ liệu giữa 2 tài khoản.
  3. Giao diện hiển thị chuẩn xác trên cả màn hình Desktop (1440px) và Mobile (375px).

---

## 5. Kế hoạch các Giai đoạn Tiếp theo (Tóm tắt G4–G7)

- **G4 — Lịch cá nhân Linh hoạt & Đổi lịch (2–4 ngày):**
  * G4-01: Engine xử lý ngày nghỉ, tính ngày kết thúc và truy vấn lịch theo khoảng ngày.
  * G4-02: Quy trình Đổi lịch 2 bước (Preview & Commit) có kiểm tra version conflict.
  * G4-03: Tính năng Tạm dừng (Pause) và Tiếp tục (Resume) lộ trình trong 1 transaction.
- **G5 — Quiz & Ôn tập Tối thiểu (3–5 ngày):**
  * G5-01: Model câu hỏi trắc nghiệm, server chấm điểm tự động, chính sách `ON_SUBMIT` vs `PASS_SCORE`.
  * G5-02: Đánh dấu bài ôn tập (Bookmark), theo dõi câu sai và giải quyết câu sai (Resolution tracking).
- **G6 — Hoàn thiện Lộ trình A & Báo cáo Học tập (2–4 ngày):**
  * G6-01: Thống kê tiến độ trực quan (Biểu đồ thanh theo tuần, lịch sử học tập theo ngày thực tế).
  * G6-02: Rà soát, biên soạn và xuất bản toàn bộ 26 tuần của Lộ trình A v1.
- **G7 — Vận hành & Phát hành Cá nhân (2–3 ngày):**
  * G7-01: Thử nghiệm sao lưu và phục hồi dữ liệu PostgreSQL (`pg_dump` / `pg_restore`).
  * G7-02: Kiểm thử hiệu năng API (p95 < 500ms) và kiểm thử trực quan trên thiết bị thật.
  * G7-03: Hoàn thiện tài liệu hướng dẫn vận hành (Runbook) và phát hành bản dùng ổn định (Mốc M2).
- **PA — Thử nghiệm Luyện phát âm Anh-Anh Local (PoC sau G3):**
  * PA-01: Pin phiên bản model `wav2vec2-xlsr-53-espeak-cv-ft`, license kiểm tra, eSpeak-NG British profile.
  * PA-02: Chuẩn bị tập thử đối chiếu (người Anh, người Việt, lỗi cố ý).
  * PA-03: Đóng gói FastAPI worker và adapter `PronunciationProvider` trong Spring Boot.
  * PA-04: Giao diện thu âm, nghe lại và gợi ý luyện tập thử nghiệm.
