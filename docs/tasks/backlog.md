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
     - Trạng thái Docker runtime: Docker Desktop đã chạy; Codex xác minh độc lập `mvnw.cmd -o -B -ntp verify`: 38 unit tests + 15 integration tests đạt trên PostgreSQL Testcontainers biệt lập. Lượt kiểm tra này không thay đổi DB dev.
  4. Luồng HTTP thật và browser E2E 375px/1440px chưa được chạy lại trong lượt chốt này. Chốt phạm vi FE bằng regression và check ở trên; giữ mục xác minh runtime riêng.

- **Kết luận FE:** Đã chốt sau sửa trực tiếp bộ lọc cache; chuyển G2-01 theo `docs/tasks/G2-01_HANDOFF.md`, không mở lại vòng auth/style.

---

## 3. Chi tiết Backlog Giai đoạn G2 — Gói Nội dung & Danh mục Lộ trình

### Task G2-01: Bộ Migration Schema Nội dung, Importer CLI & Quy trình Publish
- **Trạng thái:** [x] Hoàn thành và kiểm chứng runtime ngày 25/10/2026: 38 unit tests + 15 integration tests đạt, không skip; xem [G2-01_RECHECK](../G2-01_RECHECK.md).
- **Mục tiêu:** Tạo các bảng quản lý nội dung (`curriculum`, `curriculum_revision`, `curriculum_phase`, `curriculum_week`, `lesson`, `activity`, `resource`, `activity_resource`), xây dựng công cụ CLI backend để validate, dry-run, import và publish gói nội dung từ thư mục `content/`.
- **Đầu vào:** `manifest.schema.json`, `week.schema.json`, `resources.schema.json`, `manifest.json`, `week-01.json`, `ADR-005`, `docs/G2-01_REVIEW.md`, `docs/G2-01_RECHECK.md`.
- **Thực hiện & Khắc phục Review / Recheck:**
  * Flyway migrations:
    - [V003__create_curriculum_tables.sql](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/resources/db/migration/V003__create_curriculum_tables.sql) tạo đầy đủ bảng quan hệ với constraints, FK, unique indexes.
    - [V004__enforce_published_revision_immutability.sql](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/resources/db/migration/V004__enforce_published_revision_immutability.sql) giải quyết triệt để **R3 (P1)**:
      * Kiểm tra cả `OLD` và `NEW` khi `UPDATE` trên toàn bộ thực thể con (`phase`, `week`, `lesson`, `activity`, `resource`, `activity_resource`), chặn hoàn toàn việc chuyển parent để đánh cắp thực thể con từ revision `PUBLISHED` sang `DRAFT` hoặc gắn ngược thực thể nháp vào revision `PUBLISHED`.
      * Bảo vệ bất biến `published_at`, `created_at` và toàn bộ metadata của revision đã published bằng so sánh null-safe `IS DISTINCT FROM`.
      * Chặn hạ cấp trạng thái về `DRAFT`, chặn thay đổi trạng thái khi đã `ARCHIVED`, chặn xóa revision/curriculum khi đã publish.
  * Domain entities & repositories:
    - [Curriculum](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Curriculum.java), [CurriculumRevision](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumRevision.java), [CurriculumPhase](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumPhase.java), [CurriculumWeek](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CurriculumWeek.java), [Lesson](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Lesson.java), [Activity](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Activity.java), [Resource](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/Resource.java).
    - Enums: [RevisionStatus](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/RevisionStatus.java), [ActivityType](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/ActivityType.java), [CompletionPolicy](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/CompletionPolicy.java), [MediaType](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/MediaType.java), [VerificationStatus](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/domain/VerificationStatus.java).
    - Policy: [ProgressPolicy](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/learning/domain/ProgressPolicy.java).
    - Repositories: `CurriculumRepository`, `CurriculumRevisionRepository`, `CurriculumPhaseRepository`, `CurriculumWeekRepository`, `LessonRepository`, `ActivityRepository`, `ResourceRepository`.
  * Validator & Checksum:
    - [ContentPackageValidator](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/ContentPackageValidator.java) dùng JSON Schema Draft 2020-12 thực thụ (`json-schema-validator:1.5.6`), kiểm tra schema cho manifest, week và resources; kiểm tra trùng lặp key; kiểm tra tham chiếu resource; kiểm tra file Markdown tồn tại; chặn đường dẫn tuyệt đối, traversal (`..`, `.`).
    - Giải quyết **R4 (P2)**: kiểm tra `path.toRealPath().startsWith(realPackageDir)` ngay từ gốc cho cả `manifest.json` và `resources.json` trước khi đọc nội dung, chặn symlink trỏ ra ngoài package ở mọi entry point. Test symlink dùng `Assumptions.abort(...)` nếu môi trường OS thiếu quyền tạo symlink, không mask lỗi thành pass giả định.
    - [PackageChecksumCalculator](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/PackageChecksumCalculator.java) áp dụng cùng cơ chế kiểm tra symlink an toàn, băm SHA-256 toàn diện gồm manifest, resources, từng file tuần và toàn bộ nội dung Markdown (chuẩn hóa LF).
  * Application Services:
    - [ImportCurriculumPackageService](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/ImportCurriculumPackageService.java): chạy dry-run hoặc import toàn bộ trong 1 transaction `@Transactional` ở trạng thái `DRAFT`; idempotent đối với cùng checksum; báo lỗi `ContentConflictException` khi trùng version nhưng khác nội dung; chặn sửa đổi revision `PUBLISHED`/`ARCHIVED`.
    - Giải quyết **R2 (P1)**: chuẩn hóa thứ tự activity đồng nhất giữa validator và importer theo vị trí tự nhiên `aIdx + 1` khi `orderIndex` vắng mặt; lưu đúng `[1, 2, 3, 4...]`, không gán 0 và không vi phạm `uq_activity_order`.
    - [PublishCurriculumRevisionService](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/application/PublishCurriculumRevisionService.java): kiểm tra tính đầy đủ của bài bắt buộc (markdown không rỗng, có activity), chuyển trạng thái sang `PUBLISHED`, báo cảnh báo tài nguyên `UNVERIFIED_DRAFT`.
  * CLI Tool & DB Target:
    - [CurriculumCli](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/cli/CurriculumCli.java) hỗ trợ 4 lệnh: `validate`, `dry-run`, `import`, `publish`. Không log/in mật khẩu vào terminal.
    - Giải quyết **R1 (P1)**: tách profile `cli` (`application-cli.yml`); triển khai [CliEnvironmentPostProcessor](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/contentops/cli/CliEnvironmentPostProcessor.java) đăng ký qua `META-INF/spring.factories` & `.imports`, nâng các thuộc tính DB tường minh (`spring.datasource.url`, `username`, `password`) lên PropertySource ưu tiên cao nhất (`addFirst`), bảo đảm target DB tường minh luôn thắng cấu hình `application-dev.yml` mà không cần mở kết nối DB trong quá trình kiểm tra.
- **Lệnh CLI thực tế đã kiểm tra:**
  * Validate:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content validate ../content/pilot-curriculum/v1
    ```
    Kết quả: SUCCESSFUL (0 errors, 5 warnings cho tài nguyên UNVERIFIED_DRAFT).
  * Dry-run:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content dry-run ../content/pilot-curriculum/v1
    ```
    Báo cáo: 1 phase, 1 tuần, 6 lessons (5 bắt buộc, 1 tùy chọn), 21 activities (19 bắt buộc hiệu lực), 5 resources (5 UNVERIFIED_DRAFT). Checksum: `98b297866781af20cb6800835618b233a8b73328de6a9e6edc4009526e65c961`.
  * Import chặn DB không xác nhận:
    ```bash
    java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content import ../content/pilot-curriculum/v1
    ```
    Kết quả: Bị chặn an toàn (Exit code 1, `SECURITY CHECK FAILED: Explicit database target configuration is required`).
- **Kiểm thử đã chạy:**
  * Spotless: `.\mvnw.cmd spotless:check` đạt 69/69 files clean (0 violations).
  * Unit tests: `.\mvnw.cmd test` đạt **32/32 tests passed**:
    - `ContentPackageValidatorTest` (8 tests: bao gồm R4 regressions `rootResourcesSymlinkMustBeRejected` và `rootManifestSymlinkMustBeRejected` dùng `Assumptions.abort`).
    - `ImportAndPublishServiceTest` (7 tests: bao gồm R2 regression `pilotActivitiesHaveSequentialOrder` kiểm tra 21 activities và 4 activity đầu đạt `[1, 2, 3, 4]`).
    - `DryRunAndCliTest` (7 tests: bao gồm R1 regressions `reviewExplicitCliDatabaseOverridesApplicationConfig` và `explicitCliDatabaseOverridesDevProfileConfig`).
    - `PackageChecksumCalculatorTest` (3 tests).
    - `ProgressPolicyTest` (4 tests).
    - `ProblemFactoryTest` (2 tests), `ApplicationClockConfigTest` (1 test).
  * Integration test: [CurriculumImportPublishIT](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/curriculum/CurriculumImportPublishIT.java) đã sẵn sàng kiểm tra trọn vẹn luồng pilot import/publish, kiểm thử trigger immutability mở rộng (R3: chặn update markdown lesson, chặn đánh cắp phase/lesson sang draft revision, chặn gán week draft vào phase published, chặn sửa `published_at` hoặc metadata, xóa activity, xóa link resource, hạ cấp draft, xóa revision; xác nhận draft hợp lệ vẫn sửa được) và kiểm thử rollback 100% khi lỗi phát sinh giữa giao dịch sau khi đã ghi một phần DB (trigger nổ khi insert activity sau khi revision/resources/phase/week đã ghi).
  * Trạng thái Docker runtime: Docker Desktop đã chạy; Codex xác minh độc lập `mvnw.cmd -o -B -ntp verify`: 38 unit tests + 15 integration tests đạt trên PostgreSQL Testcontainers biệt lập. Lượt kiểm tra này không thay đổi DB dev.


---

### Task G2-02: API Catalog, Outline & Màn hình Danh mục / Chi tiết Lộ trình
- **Trạng thái:** [x] Codex nghiệm thu ngày 25/10/2026: 48 backend unit tests + 21 PostgreSQL integration tests + 33 frontend tests đạt. Xem [G2-02_REVIEW](../G2-02_REVIEW.md).
- **Mục tiêu:** Cung cấp API và giao diện để người học xem danh sách lộ trình sẵn có, chi tiết các giai đoạn, tuần học và danh sách buổi học mà không tải toàn bộ nội dung bài viết nặng.
- **Đầu vào:** Task G2-01, `openapi.yaml`, `wireframes.md`.
- **Phạm vi & Triển khai thực tế:**
  * Backend:
    - [CurriculumCatalogController](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/api/CurriculumCatalogController.java): REST endpoints `/api/v1/curricula`, `/api/v1/curricula/{slug}`, `/api/v1/revisions/{revisionId}/outline` public (`permitAll()`).
    - [CatalogResponses](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/api/CatalogResponses.java): DTO records khớp chuẩn OpenAPI (`CurriculumSummaryResponse`, `CurriculumDetailResponse`, `RevisionOutlineResponse`, `PhaseOutlineDto`, `WeekOutlineDto`, `LessonOutlineDto`). `LessonOutlineDto` chiếu nhẹ (`id, key, title, orderIndex, estimatedMinutes, required`), tuyệt đối không chứa `content_markdown`, activity payloads hoặc đáp án trắc nghiệm.
    - [CurriculumCatalogQueryService](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/application/CurriculumCatalogQueryService.java): Điều phối truy vấn danh mục, parse JSON tác giả và provenance, dựng cấu trúc phân cấp phase $\rightarrow$ week $\rightarrow$ lesson với thứ tự ổn định (`LinkedHashMap`). Xử lý không tìm thấy bằng `DomainException(RESOURCE_NOT_FOUND)`.
    - [CurriculumCatalogRepository](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/infrastructure/CurriculumCatalogRepository.java): Truy vấn tối ưu PostgreSQL: `listPublished()` dùng `DISTINCT ON (c.id)` lấy revision `PUBLISHED` mới nhất; `outline` dùng đúng 3 câu truy vấn phẳng (phases, weeks, lessons theo `revision_id`), gom nhóm in-memory, triệt tiêu hoàn toàn vấn đề N+1 query. Chặn mọi revision `DRAFT` (truy vấn lọc nghiêm ngặt `r.status = 'PUBLISHED'`).
  * Frontend:
    - [curricula.api.ts](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/curricula/api/curricula.api.ts): API client và TanStack Query keys (`list`, `detail`, `outline`).
    - [CurriculaPage](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/curricula/pages/CurriculaPage.tsx): Trang danh mục lộ trình tại `/curricula`, có Skeleton loading (`role="status"`), ErrorState có nút thử lại, Empty state khi chưa có lộ trình xuất bản, danh sách thẻ lộ trình với huy hiệu `Anh-Anh · en-GB`, phiên bản `v1.0.0` và liên kết chi tiết.
    - [CurriculumDetailPage](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/curricula/pages/CurriculumDetailPage.tsx): Trang chi tiết lộ trình tại `/curricula/:slug`, có Skeleton loading, phân biệt lỗi 404 ("Lộ trình không tồn tại hoặc chưa được xuất bản.") và lỗi mạng có nút thử lại, hiển thị trung thực xuất xứ giáo trình (`UNVERIFIED_DRAFT` hiển thị "Nguồn tham khảo chưa được xác minh", không mô tả là đã xác minh), accordion các giai đoạn và tuần học (`<details open>`), thời lượng từng buổi và huy hiệu `[Bắt buộc]` / `[Tùy chọn]`.
    - [router.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/app/router.tsx): Khai báo route `/curricula`, `/curricula/:slug` và redirect tương thích từ `/curriculum` sang `/curricula`.
    - [AppSidebar.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/app/layouts/AppSidebar.tsx): Mục điều hướng "Lộ trình học" trỏ tới `/curricula`.
- **Tiêu chí nghiệm thu (DoD) & Bằng chứng kiểm thử:**
  1. *Hiển thị đúng Lộ trình Thử nghiệm (Pilot) với 1 tuần nội dung hoàn chỉnh:* Đạt. Pilot curriculum hiển thị 1 phase, 1 tuần, 6 lessons (5 bắt buộc, 1 tùy chọn; tổng 135 phút).
  2. *Bấm mở từng tuần hiển thị danh sách các buổi học kèm thời lượng ước tính; không lộ trước đáp án hoặc nội dung chi tiết:* Đạt. `LessonOutlineDto` và endpoint outline không chứa `content_markdown`, payload hoặc đáp án; FE hiển thị đầy đủ thời lượng và huy hiệu.
  3. *Lộ trình chưa publish không xuất hiện trên catalog:* Đạt. Truy vấn BE lọc `r.status = 'PUBLISHED'`; draft slug và draft revision outline trả về 404 Problem Details (`RESOURCE_NOT_FOUND`).
  4. *UI có trạng thái Skeleton loading và Error state có nút retry:* Đạt. Cả hai trang có Skeleton loading và Error state với callback `refetch()`.
- **Kết quả kiểm thử thực tế:**
  * Backend Spotless: `.\mvnw.cmd spotless:check` đạt 77/77 files clean.
  * Backend Unit Tests: `.\mvnw.cmd test "-Dtest=CurriculumCatalogQueryServiceTest,CurriculumCatalogControllerTest"` đạt **10/10 tests passed** (5 query service + 5 controller).
  * Backend All Unit Tests: `.\mvnw.cmd test "-Dtest=!*IT"` đạt **48/48 tests passed** (0 failures, 0 errors).
  * Backend Testcontainers Integration Test: [CurriculumCatalogIT](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/curriculum/CurriculumCatalogIT.java) đã sẵn sàng 6 kịch bản kiểm thử (draft không xuất hiện/404, pilot published 1 phase/1 tuần/6 lessons, outline không rò rỉ markdown/đáp án, 404 Problem Details, sắp xếp ổn định, truy vấn phẳng không N+1). Codex đã chạy cả 6 kịch bản trên PostgreSQL Testcontainers: đạt. Tổng integration backend là 21 tests, không skip; phép đếm xác nhận 4 truy vấn cho endpoint outline (1 metadata + 3 truy vấn phẳng).
  * Frontend Tests: `npm test -- --run` đạt **33/33 tests passed** (4 suites, bao gồm 9 tests trong `Curricula.test.tsx` phủ loading, empty, error/retry, pilot detail, provenance honesty, accordion, badges, redirect).
  * Frontend Typecheck: `npm run typecheck` đạt (0 errors).
  * Frontend Lint: `npm run lint` đạt (oxlint: 0 errors, 0 warnings).
  * Frontend Build: `npm run build` đạt (Vite production bundle thành công).

---

## 4. Chi tiết Backlog Giai đoạn G3 — Vòng học Cốt lõi có Lưu Tiến độ (Mốc M1)

### Task G3-01: Đăng ký Lộ trình (Enrollment), Lịch ban đầu & Trang "Hôm nay"
- **Trạng thái:** [x] Codex đã nghiệm thu ngày 25/10/2026 sau tối đa hai lượt sửa: 81 unit + 31 PostgreSQL integration + 54 FE tests đạt. R1–R5 đã đóng; xem [G3-01_RECHECK.md](../G3-01_RECHECK.md).
- **Mục tiêu:** Người học bấm đăng ký lộ trình, hệ thống sinh lịch học ban đầu và hiển thị hành động chính trên trang "Hôm nay" theo đúng quy tắc ưu tiên và 8 trạng thái.
- **Đầu vào:** `product-rules.md` (Mục 5), `openapi.yaml`, `docs/tasks/G3-01_HANDOFF.md`, `docs/G3-01_REVIEW.md`, `docs/G3-01_RECHECK.md`, Task G2-02.
- **Khắc phục Review R1–R5 & Bổ sung Regression Suite:**
  * **R1 (P1 - Completion Policy & PAUSED Priority):**
    - `TodayQueryService`: Sửa điều kiện hoàn thành buổi học — buổi học chỉ đóng khi toàn bộ activity bắt buộc hiệu lực (`ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), a.isRequired())`) hoàn thành; activity tùy chọn (`isRequired == false`) không giữ nợ bài và không cản trở bài học tiếp theo chuyển sang `DUE_TODAY`. Buổi không có activity bắt buộc không bị kẹt `IN_PROGRESS`.
    - `TodayQueryService`: Đặt guard kiểm tra trạng thái `PAUSED` lên trước khối suy diễn `CURRICULUM_COMPLETED`. Khi enrollment bị tạm dừng, trả ngay trạng thái `PAUSED` với `primaryAction = null` và `overdueLessons = emptyList()`.
    - Tích hợp repro test `reviewOptionalActivityDoesNotKeepLessonOpen` và `reviewPausedMustTakePriorityOverCompletedCounts` vào [TodayQueryServiceTest.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/learning/application/TodayQueryServiceTest.java).
  * **R2 (P1 - Enrollment Focus Switcher & Empty State):**
    - `TodayPage.tsx`: Hiển thị combobox chọn lộ trình khi `enrollments.length >= 1` (thay vì `> 1`); bổ sung option placeholder `-- Chọn lộ trình học --` khi user có lộ trình nhưng `focusedEnrollmentId == null`.
    - Trạng thái `NO_ENROLLMENT`: Khi user đã có enrollment trong danh sách (`enrollments.length > 0`), hiển thị danh sách lộ trình đã đăng ký kèm CTA «Tiếp tục học lộ trình này» để kích hoạt focus ngay lập tức qua `authApi.updatePreferences`.
    - Hiển thị banner thông báo lỗi khi mutation chuyển đổi lộ trình gặp sự cố.
    - Tích hợp test repro và focus selection vào [G301Review.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/G301Review.test.tsx).
  * **R3 (P2 - Lesson Summary Lightweight Endpoint & Page):**
    - Cập nhật [openapi.yaml](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/api/openapi.yaml) và sinh type tự động: bổ sung endpoint `GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/summary` và schema `LessonSummaryResponse`.
    - Triển khai endpoint tại `EnrollmentController` và `EnrollmentQueryService.getLessonSummary`: kiểm tra quyền sở hữu enrollment (404 khi không phải owner), xác minh lesson thuộc đúng `revisionId` của enrollment (404 khi không khớp), truy vấn ngày học theo lịch và thời lượng ước tính; hoạt động chính xác cả khi revision ở trạng thái `ARCHIVED`.
    - Cập nhật [LessonSummaryPage.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/pages/LessonSummaryPage.tsx): gọi `enrollmentApi.getLessonSummary`, hiển thị tên bài, lộ trình, ngày theo lịch, thời lượng ước tính, huy hiệu bắt buộc/tự chọn; loại bỏ hoàn toàn raw UUID và mã giai đoạn nội bộ; hiển thị thông báo thân thiện và trạng thái 404 rõ ràng.
    - Bổ sung unit tests cho summary trong [EnrollmentQueryServiceTest.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/enrollment/application/EnrollmentQueryServiceTest.java) và [LessonSummaryPage.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/LessonSummaryPage.test.tsx).
  * **R4 (P2 - N+1 Elimination, Projection DTOs & Batch Querying):**
    - Triển khai các lightweight DTO projection không chọn cột nặng: [LessonSummaryDto](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/infrastructure/LessonSummaryDto.java) (loại bỏ `content_markdown`), [ActivitySummaryDto](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/infrastructure/ActivitySummaryDto.java) (loại bỏ `instructions`, `skill_tags`), [RevisionInfoDto](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/curriculum/infrastructure/RevisionInfoDto.java) (loại bỏ `description`, `authors`, `provenance`), [ActivityProgressDto](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/main/java/vn/englishlearning/learning/infrastructure/ActivityProgressDto.java) (loại bỏ `user_response`).
    - Bổ sung JPQL projection constructor queries trong `LessonRepository`, `ActivityRepository`, `CurriculumRevisionRepository`, `ActivityProgressRepository`.
    - `TodayQueryService`: Chuyển hoàn toàn sang truy vấn projection DTOs; không tải entity đầy đủ và không rơi vào fallback entity query khi tập dữ liệu progress rỗng.
    - `EnrollmentQueryService`: `getMyEnrollments()` thực hiện 4 batch query cố định qua SQL độc lập với số lượng enrollment (`findRevisionInfosByIds`, `findLessonSummariesByRevisionIds`, `findActivitySummariesByRevisionIds`, `findProgressDtosByEnrollmentIds`); loại bỏ hoàn toàn việc query entity riêng lẻ từng enrollment hoặc revision.
    - Kiểm thử tích hợp đo SQL thật: Triển khai [SqlStatementInspector](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/shared/test/SqlStatementInspector.java) (Hibernate `StatementInspector`) và bổ sung test `testQueryCountAndNoHeavyColumnsSelectedForTodayAndListEnrollments` trong [EnrollmentAndTodayIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/enrollment/EnrollmentAndTodayIT.java):
      * Đo SQL của `GET /api/v1/me/today`: so sánh số lượng query giữa pilot (5 lessons, 10 activities) và giáo trình lớn gấp đôi (10 lessons, 20 activities) $\rightarrow$ số lượng câu lệnh SQL hoàn toàn bằng nhau (hằng số $O(1)$, không tăng theo lessons), 100% không chọn bất kỳ cột nặng nào (`content_markdown`, `instructions`, `skill_tags`, `user_response`).
      * Đo số lượng câu query của `GET /api/v1/me/enrollments` khi có 1 enrollment (pilot 5 lessons) so với khi có 2 enrollment trên 2 giáo trình/revision độc lập (tổng 15 lessons, 30 activities) $\rightarrow$ số lượng query giữ nguyên hằng số $O(1)$ (không phát sinh N+1 per enrollment hoặc per lesson) và 100% không chọn cột nặng.
      * Đo SQL của `GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/summary`: xác nhận dùng projection `findLessonSummaryById`, 100% không chọn cột nặng.
      * Kiểm thử đơn vị: [TodayQueryServiceTest.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/learning/application/TodayQueryServiceTest.java) và [EnrollmentQueryServiceTest.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/enrollment/application/EnrollmentQueryServiceTest.java) xác minh qua Mockito `never()` đối với các method entity (`findByLessonIdOrderByOrderIndexAsc`, `findByLessonWeekPhaseRevisionId`, `findRequiredLessonsByRevisionIdOrdered`, `findByEnrollmentId`, `findById`).
  * **R5 (P2 - Concurrency, Rollback & Edge-case Integration Evidence):**
    - Bổ sung 6 kịch bản kiểm thử tích hợp thực tế trên PostgreSQL Testcontainers trong [EnrollmentAndTodayIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/enrollment/EnrollmentAndTodayIT.java):
      1. Hai request đồng thời cùng `Idempotency-Key` và cùng payload: Cả 2 request đều nhận 201 CREATED với cùng `enrollmentId`, DB chỉ tạo đúng 1 enrollment và 5 schedules.
      2. Hai request đồng thời khác key cho cùng lộ trình: Một request nhận 201 CREATED, request còn lại nhận 409 `STATE_CONFLICT`, không có lỗi 500.
      3. Transaction rollback khi gặp lỗi giữa chừng: Inject lỗi ghi lịch (`saveAll`), xác minh toàn bộ enrollment, schedules, preference focus và idempotency record đều được rollback nguyên tử (DB sạch 0 bản ghi).
      4. Chặn đăng ký revision `DRAFT` hoặc `ARCHIVED`: Trả về 409 `STATE_CONFLICT`.
      5. Đọc enrollment hiện có khi revision đã chuyển `ARCHIVED`: Các API `getDetail`, `getToday`, và `getLessonSummary` vẫn đọc dữ liệu thành công.
      6. Tính toán lịch học qua ranh giới đổi giờ London DST (25/10/2026) và chuyển giao cuối tháng (31/10 $\rightarrow$ 01/11): Sinh đúng 5 ngày học `LocalDate` chuẩn xác (23/10, 26/10, 28/10, 30/10, 02/11).
- **Tiêu chí nghiệm thu & Bằng chứng kiểm thử thực tế:**
  1. Backend Spotless: `.\mvnw.cmd spotless:check` đạt **109/109 files clean**.
  2. Backend Unit Tests: `.\mvnw.cmd test "-Dtest=!*IT"` đạt **81/81 tests passed** (0 failures, 0 errors, 0 skips, bao gồm 13 tests `TodayQueryServiceTest` và 7 tests `EnrollmentQueryServiceTest`).
  3. Backend Integration Tests: `.\mvnw.cmd verify "-Dapi.version=1.44"` đạt trọn vẹn **81 unit tests + 31 integration tests** (tổng cộng 112 tests passed, 0 failures, 0 errors, 0 skipped) chạy trên PostgreSQL 16.15 Testcontainers biệt lập (gồm 10 kịch bản toàn diện trong `EnrollmentAndTodayIT`, `CurriculumCatalogIT`, `CurriculumImportPublishIT`, `AuthAndIdentityIT`, `EnglishLearningApplicationIT`).
  4. Frontend Unit Tests: `npm test -- --run` đạt **54/54 tests passed** trên 8 test suites (gồm `G301Review.test.tsx`, `TodayPage.test.tsx`, `LessonSummaryPage.test.tsx`, `EnrollmentDialog.test.tsx`, `Curricula.test.tsx`, `AuthContext.test.tsx`, `AppSidebar.test.tsx`, `auth.api.test.ts`).
  5. Frontend Typecheck: `npm run typecheck` đạt (**0 errors**).
  6. Frontend Lint: `npm run lint` đạt (**0 errors**, 1 warning thông tin của React Compiler về `form.watch()`).
  7. Frontend Build: `npm run build` đạt (Vite production bundle thành công trong 731ms).
  8. OpenAPI Contract: `node scripts/validate-openapi-examples.js` đạt. Không chạm DB dev (`localhost:5432`), không sửa `V001`–`V004`, không commit/push.

---

### Task G3-02: Cập nhật Tiến độ Nhiệm vụ (Complete / Undo), Nộp Phản tư & Tóm tắt Tiến độ
- **Trạng thái:** [x] Codex nghiệm thu ngày 26/10/2026: 113 unit + 42 PostgreSQL integration + 68 frontend tests đạt. R1/R2 đã đóng sau lượt sửa 1 và Codex tự sửa phần đồng bộ còn sót; xem [G3-02_RECHECK.md](../G3-02_RECHECK.md).
- **Mục tiêu:** Người học hoàn thành từng nhiệm vụ hoặc nộp phản tư, hệ thống tính toán lại tiến độ nguyên tử, hỗ trợ hoàn tác và đồng bộ qua phiên với optimistic lock (`expectedVersion`).
- **Đầu vào:** `product-rules.md` (Mục 4, 6), `BE_CODE_CONVENTION.md` (Mục 6.1), `openapi.yaml`, `docs/tasks/G3-02_HANDOFF.md`, `docs/G3-02_REVIEW.md`.
- **Khắc phục Review R1 & R2:**
  * **R1 (Backend - Version Increment & Concurrent Serialization):**
    - `EnrollmentRepository`: Bổ sung `@Modifying(flushAutomatically = true) @Query("UPDATE Enrollment e SET e.version = :newVersion, e.status = :status, e.completedAt = :completedAt, e.updatedAt = :now WHERE e.id = :id AND e.version = :expectedVersion") int updateVersionAndState(...)`. Câu lệnh JPQL có điều kiện nguyên tử cập nhật version tăng đúng 1 đơn vị bất kể clock có tiến hay đóng băng (`now == updatedAt`), đồng thời trả về số dòng bị tác động.
    - `LearningProgressService`: Trong `updateProgress` và `submitResponse`, tính toán trạng thái mục tiêu (`targetEnrollmentStatus`, `targetCompletedAt`) trong biến cục bộ mà không gán lên entity đang được quản lý (tránh Hibernate auto-flush sớm làm nhảy version trước khi so khớp); tách entity khỏi persistence context (`entityManager.detach(enrollment)`) rồi gọi `updateVersionAndState`. Nếu `updatedRows == 0`, ném ngay `DomainException(ErrorCode.VERSION_CONFLICT)`. Trả về `newVersion` chuẩn xác trong `ProgressResponse`.
    - Concurrency & Serialization: Trong `updateProgress`, khi undo activity bắt buộc trên enrollment `COMPLETED`, hệ thống serialize trên hàng user (`userRepository.findByIdForUpdate(currentUserId)`), ngăn chặn race condition giữa việc tạo enrollment mới và undo reopening, trả về 409 `STATE_CONFLICT` thay vì 500.
    - Test Suite Regression R1: Bổ sung 5 regression tests trên PostgreSQL Testcontainers trong [LearningProgressIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/learning/LearningProgressIT.java) (tổng cộng 11 tests):
      1. `testMeaningfulChangeAdvancesVersionWhenClockDoesNotAdvance`: Đồng hồ đóng băng vẫn tăng version từ 1 lên 2 khi hoàn thành activity.
      2. `testNoOpDoesNotAdvanceVersionWhenClockDoesNotAdvance`: No-op với matching version không tăng version và không sinh event dư thừa.
      3. `testSubmitResponseAdvancesVersionWhenClockDoesNotAdvance`: Nộp response mới tăng version khi đồng hồ đóng băng; nộp lại text y hệt là no-op không tăng version.
      4. `testConcurrentMutationsOnDifferentActivitiesWithSameExpectedVersion`: Hai luồng đồng thời gọi PUT trên hai activity khác nhau với cùng expectedVersion $\rightarrow$ duy nhất 1 luồng commit thành công (200), luồng còn lại nhận 409 `VERSION_CONFLICT`.
      5. `testConcurrentRaceBetweenCreateEnrollmentAndUndoCompletedEnrollment`: Race condition giữa tạo enrollment mới và undo reopening được serialize trên user lock, trả về 409 `STATE_CONFLICT` mà không sinh 500.
  * **R2 (Frontend - Safe Synchronization, Draft Retention, Error Recovery & Lifecycle Guards):**
    - `useLearningProgress.ts`: Trong `useUpdateProgress` và `useSubmitReflection`, cập nhật đồng bộ Query Cache (`activities` và `summary`) bằng dữ liệu trả về từ `ProgressResponse` ngay trong `onSuccess`, sau đó `await queryClient.invalidateQueries(...)` qua `activities`, `summary`, `today`, `detail`, và `my-enrollments`.
    - `ActivityProgressPanel.tsx`:
      * Khai báo cờ đồng bộ `isSyncing = updateProgressMutation.isPending || submitReflectionMutation.isPending || activitiesQuery.isFetching`. Khóa toàn bộ nút bấm, checkbox, textarea khi `isSyncing || isEnrollmentReadOnly || isConflict` để chặn stale version mutation.
      * Xử lý lỗi 409: Phân biệt rõ `VERSION_CONFLICT` và `STATE_CONFLICT`. Khi gặp `VERSION_CONFLICT`, giữ nguyên toàn bộ nội dung phản tư trong `draftEdits`, khóa nút gửi và hiển thị nút «Tải lại trạng thái mới nhất» (`activitiesQuery.refetch()`). Không bao giờ tự động resubmit khi chưa có sự xác nhận của người dùng.
      * Banner phục hồi: Khi background refetch gặp lỗi (`activitiesQuery.isError && activitiesQuery.data`), hiển thị banner cảnh báo kèm nút «Thử lại».
    - `LessonSummaryPage.tsx`:
      * Hiển thị Skeleton loading cho đến khi cả `lessonSummaryQuery` và `enrollmentDetailQuery` hoàn tất; hiển thị ErrorState và nút thử lại khi `enrollmentDetailQuery.isError`.
      * Khóa toàn bộ tương tác ghi (read-only) khi enrollment ở trạng thái `PAUSED` hoặc `ABANDONED`.
      * Duy trì quyền Undo hợp lệ khi enrollment ở trạng thái `COMPLETED` (`isReadOnly = false`).
    - Test Suite Regression R2: Bổ sung [G302Review.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/G302Review.test.tsx) với 7 unit tests mô phỏng chính xác các kịch bản của reviewer.
- **Tiêu chí nghiệm thu (DoD) & Bằng chứng kiểm thử thực tế:**
  1. *Backend Spotless:* `.\mvnw.cmd spotless:check` đạt **117/117 files clean**.
  2. *Backend Unit Tests:* `.\mvnw.cmd test "-Dtest=!*IT"` đạt **113/113 tests passed** (0 failures, 0 errors, bao gồm 24 tests trong `LearningProgressServiceTest`).
  3. *Backend Integration Tests (PostgreSQL 16 Testcontainers):*
     - `.\mvnw.cmd verify "-Dtest=!*" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=*IT" "-Dapi.version=1.44"` đạt **42/42 tests passed** (0 failures, 0 errors) trên container PostgreSQL 16.15-alpine biệt lập:
       * [LearningProgressIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/learning/LearningProgressIT.java): **11/11 tests passed** (bao gồm 6 tests ban đầu + 5 regression tests cho R1).
       * [EnrollmentAndTodayIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/enrollment/EnrollmentAndTodayIT.java): **10/10 tests passed**.
       * [CurriculumCatalogIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/curriculum/CurriculumCatalogIT.java): **5/5 tests passed**.
       * [CurriculumImportPublishIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/curriculum/CurriculumImportPublishIT.java): **5/5 tests passed**.
       * [AuthAndIdentityIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/identity/AuthAndIdentityIT.java): **10/10 tests passed**.
       * [EnglishLearningApplicationIT.java](file:///d:/WorkSpace/tu-hoc-tieng-anh/backend/src/test/java/vn/englishlearning/EnglishLearningApplicationIT.java): **1/1 test passed**.
  4. *Frontend Tests:* `npm test -- --run` đạt **66/66 tests passed** trên 10 test files (bao gồm 7 tests trong `G302Review.test.tsx`, 5 tests trong `ActivityProgressPanel.test.tsx`, 2 tests trong `LessonSummaryPage.test.tsx`).
  5. *Frontend Typecheck:* `npm run typecheck` đạt (**0 errors**).
  6. *Frontend Lint:* `npm run lint` đạt (**0 errors**, 1 warning thông tin đã biết về `form.watch` trong `EnrollmentDialog`).
  7. *Frontend Build:* `npm run build` đạt (Vite production bundle thành công trong 1.19s).
  8. *OpenAPI Contract Validation:* `node scripts/validate-openapi-examples.js` thành công.
  9. *Ràng buộc môi trường:* Không sửa `V001`–`V005` (dùng `V006`), không chạm dev DB `localhost:5432`, không sửa `.env`, không commit/push Git.

---

### Task G3-03: Không gian Học tập (Learning Workspace), Trình đọc Markdown & Ghi chú Tự lưu
- **Trạng thái:** [x] Codex nghiệm thu ngày 26/10/2026 sau 2 lượt sửa và tự xử lý phần còn lại: 143 unit + 48 PostgreSQL integration + 98 frontend tests đạt; xem [G3-03_ACCEPTANCE.md](../G3-03_ACCEPTANCE.md). Tiếp theo G3-04; chưa thay cho E2E/visual QA. Baseline: `6c784b4`.
- **Mục tiêu:** Màn hình học bài chi tiết tập trung, đọc Markdown an toàn, chuyển đổi giữa các nhiệm vụ, nhập câu trả lời thực hành (lưu nháp và nộp) và ghi chú tự lưu (autosave).
- **Khắc phục Toàn diện Review R1–R3 (Lượt 2/2 Final):**
  * **R1 (Generation Guard, Khóa Autosave khi 409 & Loại bỏ Fallback Đoán Version):**
    - `useAutosaveNote.ts`:
      * **Repro A (Scope & Generation Guard):** Bổ sung `generationRef = useRef(0)`. Mỗi khi thay đổi scope (`enrollmentId`, `lessonId`, `initialNote?.id`) hoặc unmount, `generationRef.current += 1`. Mọi request in-flight từ scope cũ trước đó khi hoàn tất (hoặc reject) đều kiểm tra `if (generationRef.current !== requestGen) return;`, bị hủy continuation hoàn toàn và không được phép ghi đè state, refs, resolvers, queue, callbacks của scope mới.
      * **Repro B (Khóa Autosave khi 409):** Khi gặp 409 `VERSION_CONFLICT`, hủy ngay timer debounce, xóa hàng đợi, chốt cờ `isConflictLatchedRef.current = true` và khóa đường autosave. Khi người học gõ thêm, hook cập nhật text cục bộ nhưng **không lập timer mới**, không tự động gửi PATCH. Autosave chỉ mở lại khi người học chủ động chọn "Tải lại phiên bản mới" (`reloadNote`) hoặc "Ghi đè bằng bản này" (`overwriteConflict`).
    - `NoteEditor.tsx`:
      * **Loại bỏ Fallback Đoán Version:** Xóa bỏ hoàn toàn logic `currentNote.version + 1` khi đọc note mới bị lỗi hoặc không thấy note. Nếu đọc bản mới thất bại, giữ nguyên draft và hiển thị thông báo lỗi yêu cầu thử lại sau, tuyệt đối không gửi request PATCH.
      * **Bảo vệ Trạng thái Tải:** Tiếp nhận `isLoading`, `isLoadError` và `onRetryLoad` từ `notesQuery`. Hiển thị Skeleton loading khi đang tải và banner lỗi khi tải thất bại, ngăn chặn việc người học gõ vào editor khi chưa khởi tạo xong gây POST tạo trùng note. Gọi `notesApi.getNotes` với `size: 100` để bảo đảm lấy đủ note cấp bài học trong 1 trang duy nhất.
  * **R2 (Bảo toàn Text Response khi Lưu/Lỗi, Phục hồi Xung đột & Unified Navigation Blocker):**
    - `ActivityRenderer.tsx`:
      * **Repro C (Bảo toàn Text Draft):** Tách rời hoàn toàn `responseText` khỏi các chuyển đổi của `draftStatus`. Xóa bỏ điều kiện `draftStatus !== "dirty"` gây reset textarea về snapshot cũ khi nhấn "Lưu nháp" (chuyển sang `saving`). `responseText` được giữ nguyên vẹn xuyên suốt các trạng thái `saving`, `error`, và `saved`.
      * **Phân biệt 409 Conflict & Phục hồi:** Phân biệt rõ `VERSION_CONFLICT` vs `STATE_CONFLICT`. Khi gặp `VERSION_CONFLICT`, giữ nguyên draft và hiển thị nút "Đồng bộ tiến độ" (`onRefreshWorkspace`) để nạp lại version mới từ server mà không mất nội dung đã nhập.
      * **Generation Guard Activity:** Bổ sung `activityGenRef` bảo vệ callbacks mutation, ngăn chặn stale response ghi đè vào activity/workspace mới khi chuyển bài.
    - `LessonWorkspacePage.tsx`:
      * **Reconciliation & Mutating Synchronization:** Trong `handleVersionUpdate`, giữ `isMutating = true` qua toàn bộ chu trình `await Promise.all(...)` invalidation (bao gồm `learningWorkspaceKeys.lesson`), chỉ gỡ temporary overrides sau khi dữ liệu server mới đã về đến cache.
      * **Unified Navigation Blocker:** Gom tất cả thay đổi chưa lưu (note dirty/saving/conflict, activity drafts, mutations) vào một guard duy nhất tại root workspace, gọi `useBlocker` đúng 1 lần (loại bỏ hoàn toàn lỗi "A router only supports one blocker at a time"). Hỗ trợ đầy đủ hành động "Lưu rồi rời" (Save and Proceed), "Ở lại trang", và "Rời khỏi không lưu".
  * **R3 (Ma trận Chính sách & Chặn Thao tác Thủ công trên RECORDING/DICTATION):**
    - `ActivityRenderer.tsx`: Nhận diện `isUnsupportedType = activity.type === "RECORDING" || activity.type === "DICTATION"`. Ép buộc `isManualCheck = false` và `hasResponse = false`, không hiển thị nút "Hoàn thành" thủ công (tránh bị backend từ chối với 400/422). Hiển thị badge thông báo loại hoạt động chưa hỗ trợ thao tác trên web.
  * **Bộ Test Suite Reviewer Repros Hoàn chỉnh:**
    - [frontend/src/features/learning/__tests__/G303Round1Review.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/G303Round1Review.test.tsx): **3/3 tests passed** (Repro A, Repro B, Repro C).
    - [frontend/src/features/learning/__tests__/G303Review.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/G303Review.test.tsx): **5/5 tests passed** (5 repros từ Round 1).
    - [frontend/src/features/learning/__tests__/G303RecheckVerification.test.tsx](file:///d:/WorkSpace/tu-hoc-tieng-anh/frontend/src/features/learning/__tests__/G303RecheckVerification.test.tsx): **5/5 tests passed** (Chính sách unsupported types, 409 recovery, overwrite safety không đoán version, Data Router Unified Navigation Blocker thực tế).
- **Tiêu chí nghiệm thu (DoD) & Bằng chứng kiểm thử thực tế:**
  1. *Backend Spotless:* `.\mvnw.cmd spotless:check` đạt **128/128 files clean**.
  2. *Backend Unit Tests:* `.\mvnw.cmd test "-Dtest=!*IT"` đạt **142/142 tests passed** (0 failures, 0 errors, bao gồm 12 tests `NoteServiceTest`, 7 tests `NoteControllerTest`, 32 tests `LearningProgressServiceTest`, 10 tests `LearningProgressControllerTest`).
  3. *Backend Integration Tests (PostgreSQL 16 Testcontainers):*
     - `.\mvnw.cmd verify "-Dapi.version=1.44"` đạt trọn vẹn **142 unit tests + 48 integration tests** (tổng cộng 190 tests passed, 0 failures, 0 errors, 0 skipped) chạy trên container PostgreSQL 16.15-alpine biệt lập:
       * `LearningWorkspaceAndNotesIT.java`: **6/6 tests passed** (gồm workspace detail, draft lifecycle & locking, notes CRUD, concurrent draft vs submit, concurrent note create race, fixed clock no-op version stability).
       * `LearningProgressIT.java`: **11/11 tests passed**.
       * `EnrollmentAndTodayIT.java`: **10/10 tests passed**.
       * `CurriculumCatalogIT.java`: **5/5 tests passed**.
       * `CurriculumImportPublishIT.java`: **5/5 tests passed**.
       * `AuthAndIdentityIT.java`: **10/10 tests passed**.
       * `EnglishLearningApplicationIT.java`: **1/1 test passed**.
  4. *Frontend Unit Tests:* `npm test` đạt **93/93 tests passed** trên 16 test suites.
  5. *Frontend Typecheck:* `npm run typecheck` đạt (**0 errors**).
  6. *Frontend Lint:* `npm run lint` đạt (**0 errors**, 2 warnings thông tin React Compiler).
  7. *Frontend Build:* `npm run build` đạt (Vite production bundle thành công trong 818ms, bundle 683 KB, 207 KB gzip).
  8. *Ràng buộc an toàn:* Không sửa `V001`–`V006` (dùng `V007`), không chạm dev DB `localhost:5432`, không sửa `.env`, không commit/push Git, không mở sang G3-04. Dừng chờ Codex nghiệm thu.

---

### Task G3-04: Kiểm thử E2E Vòng học Cốt lõi & Hoàn thiện Mốc M1
- **Trạng thái:** [x] Codex nghiệm thu ngày 26/10/2026: M1 web desktop đạt; E2E 14/14 + lượt lặp 9/9, desktop cuối 1/1; regression 143 unit + 48 integration + 98 FE tests đạt. Xem [G3-04_ACCEPTANCE.md](../G3-04_ACCEPTANCE.md). Mobile hoãn, chưa bắt đầu G4.
- **Ưu tiên đã chốt ngày 26/10/2026:** Chỉ web desktop đợt này; mobile hoãn, không chặn nghiệm thu M1 desktop.
- **Môi trường đã đổi theo người dùng:** E2E dùng trực tiếp app/DB development, dữ liệu test `e2e_` riêng trong cùng DB; bỏ dựng container/profile/cổng biệt lập. Không xóa dữ liệu đang học. Theo mục 3 mới trong handoff và R1 đã điều chỉnh trong review.
- **Mục tiêu:** Kiểm chứng toàn diện hành trình người học từ Đăng nhập $\rightarrow$ Đăng ký $\rightarrow$ Học bài $\rightarrow$ Ghi chú $\rightarrow$ Hoàn thành $\rightarrow$ Hôm nay cập nhật đúng.
- **Đầu vào:** Toàn bộ code G1–G3.
- **Tiêu chí nghiệm thu (DoD):**
  1. Vượt qua toàn bộ 6 tiêu chí nghiệm thu bắt buộc của mốc M1 được quy định trong `KE_HOACH_DU_AN.md` (Mục 11 - G3).
  2. Không có lỗi rò rỉ dữ liệu giữa 2 tài khoản.
  3. Giao diện và thao tác đúng trên desktop 1440×900, thêm smoke 1280×800. Mobile 375px hoãn theo yêu cầu người dùng, chưa nghiệm thu trong G3-04.

---

## 5. Kế hoạch các Giai đoạn Tiếp theo (Tóm tắt G4–G7)

- **G4 — Lịch cá nhân Linh hoạt & Đổi lịch (2–4 ngày):**
  * G4-01: [x] Codex nghiệm thu luồng chính ngày 26/10/2026 theo quy trình nhanh: 31 BE unit liên quan + 10 FE schedule tests + 3 E2E thật đạt, build/lint qua; JUnit PostgreSQL IT vướng loopback môi trường, hoãn kiểm tra lại cuối G4. Chi tiết [G4-01_IMPLEMENTATION.md](../G4-01_IMPLEMENTATION.md). Tiếp theo G4-02.
  * G4-02: [x] Codex nghiệm thu ngày 28/10/2026 theo quy trình nhanh: 28 BE unit + 1 PostgreSQL Testcontainers IT + 13 FE schedule tests + 3 E2E thật đạt; preview tính toán thuần, commit nguyên tử CAS expectedVersion, idempotency, bảo toàn 100% bài đã hoàn thành & originalDate, modal UI desktop có invalidation và 409 conflict handling. Chi tiết [G4-02_ACCEPTANCE.md](../G4-02_ACCEPTANCE.md). Tiếp theo G4-03.
  * G4-03: [x] Nghiệm thu hoàn thành ngày 28/10/2026: 40 BE unit + 16 FE unit + 4/4 E2E Playwright desktop đạt 100%, build 0 errors, lint 0 errors; Pause đóng băng tiến độ & ngừng nợ bài, Resume nguyên tử 1 transaction tính toán lịch mới, Abandon giải phóng partial unique index. Chi tiết [G4-03_ACCEPTANCE.md](../G4-03_ACCEPTANCE.md). Toàn bộ mốc G4 hoàn thành.
- **G5 — Quiz & Ôn tập Tối thiểu (3–5 ngày):**
  * G5-01: [x] Nghiệm thu hoàn thành ngày 28/10/2026: 28 BE unit + 56 FE unit + E2E Playwright desktop đạt 100%, 3 ảnh chụp màn hình; model trắc nghiệm V008, ẩn đáp án & giải thích chống gian lận, server auto-grading, chính sách ON_SUBMIT vs PASS_SCORE (80%), bảo toàn thành tích khi thi lại điểm thấp hơn. Chi tiết [G5-01_ACCEPTANCE.md](../G5-01_ACCEPTANCE.md). Tiếp theo G5-02.
  * G5-02: [x] Nghiệm thu hoàn thành ngày 28/10/2026: 28 BE unit + 6 FE review unit + E2E Playwright desktop đạt 100%, 3 ảnh chụp màn hình; model review V009 (review_bookmark, review_resolution), BookmarkButton gắn tại workspace bài học & activity, tự động trích xuất câu sai từ latest quiz attempt, API check mistake với instant feedback và explanation, resolution tracking tự động đánh dấu đã giải quyết (và tự mở lại nếu thi lại quiz bị sai lại), trang /review desktop với tabs và bộ lọc. Chi tiết [G5-02_ACCEPTANCE.md](../G5-02_ACCEPTANCE.md). Toàn bộ mốc G5 hoàn thành.
- **G6 — Hoàn thiện Lộ trình A & Báo cáo Học tập (2–4 ngày):**
  * G6-01: [x] Nghiệm thu hoàn thành ngày 28/10/2026: 220 BE unit (LearningAnalyticsServiceTest 5/5, LearningProgressControllerTest 11/11) + 134 FE unit (10 analytics tests) + E2E Playwright desktop đạt 100%, 3 ảnh chụp màn hình; Endpoint GET /analytics, kiểm soát quyền 404, lọc mẫu số chỉ tính activity bắt buộc hiệu lực trong lesson bắt buộc, thời lượng ước tính nội dung, thuật toán streak theo ngày thực tế (gap >= 2 mới reset, giữ streak ngày hôm qua), biểu đồ thanh theo tuần thuần CSS/SVG, dòng thời gian ngày học tiếng Việt, trang /progress desktop với sync refresh. Chi tiết [G6-01_ACCEPTANCE.md](../G6-01_ACCEPTANCE.md). Tiếp theo G6-02.
  * G6-02: [x] Nghiệm thu hoàn thành ngày 28/10/2026: Gói nội dung Roadmap A v1 hoàn chỉnh 26 tuần (3 phases, 26 weeks, 130 lessons, 406 activities, 10 resources) tại `content/roadmap-a/v1/`; Content Validator 0 lỗi, Dry-run checksum SHA-256 xác thực; Import & Publish nguyên tử trạng thái `PUBLISHED (IMMUTABLE)` (Revision `9f36eec2-5967-47a9-916e-29901b90712d`); API `/curricula` và `/outline` đầy đủ 26 tuần; Playwright E2E desktop đạt 100%, 3 ảnh chụp màn hình; Toàn bộ Mốc G6 hoàn thành. Chi tiết [G6-02_ACCEPTANCE.md](../G6-02_ACCEPTANCE.md). Tiếp theo G7-01.
- **G7 — Vận hành & Phát hành Cá nhân (2–3 ngày):**
  * G7-01: Thử nghiệm sao lưu và phục hồi dữ liệu PostgreSQL (`pg_dump` / `pg_restore`).
  * G7-02: Kiểm thử hiệu năng API (p95 < 500ms) và kiểm thử trực quan trên thiết bị thật.
  * G7-03: Hoàn thiện tài liệu hướng dẫn vận hành (Runbook) và phát hành bản dùng ổn định (Mốc M2).
- **PA — Thử nghiệm Luyện phát âm Anh-Anh Local (PoC sau G3):**
  * PA-01: Pin phiên bản model `wav2vec2-xlsr-53-espeak-cv-ft`, license kiểm tra, eSpeak-NG British profile.
  * PA-02: Chuẩn bị tập thử đối chiếu (người Anh, người Việt, lỗi cố ý).
  * PA-03: Đóng gói FastAPI worker và adapter `PronunciationProvider` trong Spring Boot.
  * PA-04: Giao diện thu âm, nghe lại và gợi ý luyện tập thử nghiệm.
