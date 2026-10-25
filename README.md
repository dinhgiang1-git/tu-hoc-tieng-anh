# WebApp Tự Học Tiếng Anh Theo Lộ Trình (English Self-Study Platform)

App điều phối AI đã tách thành dự án độc lập: [Agent Desk Desktop](../agent-desk/README.md) tại `D:/WorkSpace/agent-desk`. Mở `Agent Desk.exe` để chat Codex trong terminal và theo dõi hai Antigravity; bản trong `tools/agent-desk` là bản cũ.

Dự án ứng dụng web tự học tiếng Anh theo lộ trình có cấu trúc sư phạm, tập trung vào trải nghiệm học tập cá nhân hóa, theo dõi tiến độ bền vững và chuẩn phát âm **Anh-Anh (`en-GB` / Received Pronunciation)**.

---

## 1. Trạng thái Dự án

- **Giai đoạn hiện tại:** **G2-01 đã chốt**. Codex kiểm tra độc lập backend: 38 unit tests và 15 integration tests PostgreSQL Testcontainers đạt, không skip; xem [kết luận G2-01](docs/G2-01_RECHECK.md). FE G1-03 đã chốt trước đó. G2-02 catalog/outline đang triển khai và chờ review.
  - `[x]` **Task G1-01:** Khảo sát DB, Khởi tạo Backend Spring Boot 3.4.3 + Java 21, Flyway migration, Clock bean, Problem Details RFC 7807, Healthcheck.
  - `[x]` **Task G1-02:** Xác thực Session (HttpOnly cookie `SESSION`), CSRF Protection (`X-CSRF-TOKEN`), Module Identity (`/auth/csrf`, `/auth/login`, `/auth/logout`, `/me`, `/me/preferences`), xử lý triệt để A1/A2/A3, regression tests trên Testcontainers.
  - `[x]` **Task G1-03:** Khởi tạo Frontend React 19 + TypeScript Vite Shell, Tailwind CSS tokens, UI primitives, API client (CSRF in-memory adapter, Problem Details decoder), AppShell, AppSidebar (responsive mobile drawer + desktop collapse) & Login/Logout flow.
- **Mục tiêu tiếp theo:** **G2-02 — Catalog và Outline**.
- **Phạm vi tiếp theo:** [G2-02_HANDOFF.md](docs/tasks/G2-02_HANDOFF.md). FE G1-03 đạt 24/24 tests ở lượt nghiệm thu trước; xem [kết luận G1-03](docs/G1-03_RECHECK.md).
- **Mốc quan trọng hướng tới:** **M1** (Dùng thử vòng học cốt lõi hàng ngày với Lộ trình Thử nghiệm sau G3).

---

## 2. Hướng dẫn Chạy Kiểm thử & Khởi động Backend

### 2.1. Yêu cầu Môi trường
- **Java:** JDK 21 LTS (ví dụ: Oracle JDK 21.0.10 trên Windows hoặc OpenJDK 21 trên WSL).
- **Docker Engine:** Docker daemon trên Linux/WSL (truy cập qua socket `/var/run/docker.sock`) hoặc Docker Desktop (qua named pipe). Không yêu cầu mở cổng TCP không mã hóa.
- **Múi giờ & Mã hóa:** Khuyến nghị `-Duser.timezone=UTC -Dfile.encoding=UTF-8`.

### 2.2. Kiểm thử Tự động Backend (Độc lập 100% qua Testcontainers)
Integration tests sử dụng `Testcontainers` tự động khởi chạy một container PostgreSQL 16 tạm thời (ephemeral), hoàn toàn không can thiệp hay sửa đổi database/volume PostgreSQL phát triển có sẵn.
```bash
# Chạy trên Windows (với Docker Desktop) hoặc trong WSL (với native Docker socket):
cd backend
./mvnw clean verify
```
Lệnh trên thực hiện toàn bộ:
1. `spotless:check` (kiểm tra định dạng mã nguồn theo Google Java Format với chuẩn UNIX LF).
2. Unit tests (ProblemFactoryTest, ApplicationClockConfigTest).
3. Integration tests (`*IT.java`) chạy trên database PostgreSQL cô lập (12 test cases kiểm tra CSRF, Session Fixation A1, PATCH partial update A2, User Isolation).

### 2.3. Cấu hình Biến môi trường Backend (.env)
Sao chép cấu hình vào `backend/.env`, sửa kết nối DB, rồi tạo thông tin đăng nhập ban đầu (PowerShell, từ thư mục gốc):
```powershell
Copy-Item backend/.env.example backend/.env # Chỉ chạy nếu chưa có backend/.env
.\scripts\initialize-admin.ps1
```
Mở `backend/.env` để lấy username/password đã tạo. Khởi động backend từ thư mục `backend`; cần khởi động lại nếu vừa bổ sung cấu hình. Xem [hướng dẫn chạy](docs/HUONG_DAN_CHAY.md).
Các biến chính:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: Thông tin kết nối PostgreSQL dev.
- `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`: Khởi tạo tài khoản admin đầu tiên lúc bootstrap nếu database trống.

---

## 3. Hướng dẫn Chạy & Kiểm tra Frontend

### 3.1. Cài đặt & Chạy Dev Server
```bash
cd frontend
npm install
npm run dev
```
Truy cập `http://localhost:5173`. Dev server đã được cấu hình reverse proxy tự động chuyển tiếp `/api/*` và `/actuator/*` về `http://localhost:8080` (Spring Boot), đảm bảo cùng origin và hoạt động trơn tru với HttpOnly session cookie và CSRF token.

### 3.2. Kiểm tra Chất lượng Mã nguồn Frontend
```bash
cd frontend
npm run lint         # Kiểm tra cú pháp & quy tắc React hooks qua Oxlint (0 errors)
npm run typecheck    # Kiểm tra kiểu nghiêm ngặt TypeScript qua tsc (0 errors)
npm run build        # Biên dịch production bundle với Vite (0 errors)
```

---

## 4. Trạng thái Git Repository
- Kho chứa Git đã được khởi tạo tại workspace root: `git init`.
- Remote origin đã được liên kết: `https://github.com/dinhgiang1-git/tu-hoc-tieng-anh.git`.
- File `.gitignore` và `.gitattributes` đã được cấu hình chặt chẽ, loại bỏ hoàn toàn `.env`, credentials, build targets, node_modules, logs và system caches.
- Mã nguồn sẵn sàng commit và push theo lệnh điều phối của người dùng.

---

## 5. Công nghệ Cốt lõi (Tech Stack)

- **Backend:** Java 21, Spring Boot, Spring Security (Session Cookie HttpOnly + CSRF), Spring Data JPA, Flyway migration.
- **Database:** PostgreSQL (chạy trong Docker qua môi trường WSL).
- **Frontend:** React + TypeScript TSX, Vite, Tailwind CSS, shadcn/ui primitives, TanStack Query, React Hook Form + Zod, Lucide icons.
- **Speech Engine (PoC thử nghiệm sau G3):** Worker Python FastAPI nội bộ chạy mô hình mã nguồn mở Wav2Vec2 XLSR-53 kết hợp eSpeak-NG British profile (`en-gb`).

---

## 6. Cấu trúc Tài liệu và Dự án

```text
tu-hoc-tieng-anh/
├── KE_HOACH_DU_AN.md            # Bản kế hoạch tổng thể toàn diện của dự án
├── BE_CODE_CONVENTION.md         # Quy chuẩn lập trình Backend (Spring Boot + PostgreSQL)
├── FE_CODE_CONVENTION.md         # Quy chuẩn lập trình Frontend (React TSX + Tailwind Vite)
├── README.md                     # Tài liệu giới thiệu dự án và hướng dẫn điều phối
│
├── docs/                         # Toàn bộ tài liệu phân tích kỹ thuật và đặc tả G0
│   ├── G0_REVIEW.md              # Biên bản rà soát G0 độc lập và các điểm R1–R8 đã xử lý
│   ├── G1-01_REVIEW.md           # Biên bản rà soát G1-01
│   ├── G1-02_REVIEW.md           # Biên bản rà soát G1-02
│   ├── product-rules.md          # Quy tắc nghiệp vụ, công thức tiến độ, vòng đời trạng thái
│   ├── content-mapping.md        # Đối chiếu 26 tuần lộ trình A, phân loại nguồn & pilot 3 ngày/tuần
│   ├── wireframes.md             # Mô tả chi tiết wireframe các màn hình chính và responsive
│   ├── api/
│   │   ├── openapi.yaml          # OpenAPI 3.1 contract ban đầu đầy đủ (0 lint errors)
│   │   └── generated-types.ts    # TypeScript types sinh từ openapi.yaml
│   ├── adr/                      # Các quyết định kiến trúc quan trọng (ADR 001 - 005)
│   │   ├── ADR-001-modular-monolith-and-tech-stack.md
│   │   ├── ADR-002-learning-model-and-completion-policy.md
│   │   ├── ADR-003-session-csrf-and-optimistic-concurrency.md
│   │   ├── ADR-004-british-english-offline-pronunciation-architecture.md
│   │   └── ADR-005-content-package-versioning-and-verification.md
│   └── tasks/
│       └── backlog.md            # Danh mục công việc chi tiết từ G1 đến G7 và PA
│
├── content/                      # Gói nội dung giáo trình được quản lý bằng Git
│   ├── schemas/                  # Bộ JSON Schema chuyên biệt xác thực nội dung
│   │   ├── manifest.schema.json  # Schema xác thực manifest.json
│   │   ├── week.schema.json      # Schema xác thực các file weeks/*.json
│   │   ├── resources.schema.json # Schema xác thực resources.json
│   │   └── curriculum.schema.json# Schema tổng hợp dạng bundled
│   └── pilot-curriculum/         # Lộ trình thử nghiệm (Pilot Curriculum) chuẩn Anh-Anh
│       ├── v1/                   # Gói v1 xuất bản chính thức (100% hoàn chỉnh, 1 tuần)
│       │   ├── manifest.json     # Khai báo metadata, bản quyền và phase
│       │   ├── resources.json    # Danh mục tài liệu tham khảo ngoài (phân định nguồn gốc)
│       │   ├── weeks/
│       │   │   └── week-01.json  # Tuần 1: 6 bài, 21 activities (19 bắt buộc hiệu lực)
│       │   └── lessons/          # 6 file Markdown chi tiết cho Tuần 1
│       │       ├── w01-l01-vowels.md
│       │       ├── w01-l02-consonants.md
│       │       ├── w01-l03-word-stress.md
│       │       ├── w01-l04-schwa.md
│       │       ├── w01-l05-week1-review.md
│       │       └── w01-l06-optional-rhoticity.md
│       └── drafts/               # Tách riêng các tuần nháp chưa đưa vào manifest
│           └── week-02-sketch.json
│
├── frontend/                     # Ứng dụng Frontend React SPA (Vite + TSX + Tailwind)
│
└── scripts/                      # Script kiểm thử tự động G0-FIX
    ├── validate-content.js       # Kiểm tra Schema, Markdown paths, Resource keys, Key uniqueness
    └── validate-openapi-examples.js # Kiểm tra cú pháp OpenAPI 3.1 & tính đầy đủ của schema/payload
```

---

## 7. Hướng dẫn Dành cho AI Triển khai Kế tiếp

Khi được giao việc ở các giai đoạn tiếp theo (bắt đầu từ G1):
1. **Đọc kỹ:** `KE_HOACH_DU_AN.md`, `BE_CODE_CONVENTION.md`, `FE_CODE_CONVENTION.md`, và `docs/product-rules.md`.
2. **Tuân thủ hợp đồng:** Toàn bộ DTO, Error code, Headers, và Endpoints phải khớp chính xác với `docs/api/openapi.yaml`.
3. **Thực hiện từng task:** Nhận task từ `docs/tasks/backlog.md`, hoàn thành đầy đủ tiêu chí nghiệm thu (DoD) trước khi chuyển sang task phụ thuộc.
4. **Không thay đổi hạ tầng ngoài phạm vi:** Không sửa đổi container database PostgreSQL hoặc compose có sẵn ngoài phạm vi yêu cầu của task.
