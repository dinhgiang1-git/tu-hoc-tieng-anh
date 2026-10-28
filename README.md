# WebApp Tự Học Tiếng Anh Theo Lộ Trình (English Self-Study Platform)

App điều phối AI đã tách thành dự án độc lập: [Agent Desk Desktop](../agent-desk/README.md) tại `D:/WorkSpace/agent-desk`. Mở `Agent Desk.exe` để chat Codex trong terminal và theo dõi hai Antigravity; bản trong `tools/agent-desk` là bản cũ.

Dự án ứng dụng web tự học tiếng Anh theo lộ trình có cấu trúc sư phạm, tập trung vào trải nghiệm học tập cá nhân hóa, theo dõi tiến độ bền vững và chuẩn phát âm **Anh-Anh (`en-GB` / Received Pronunciation)**.

---

## 1. Trạng thái Dự án

- **Giai đoạn hiện tại:** **MỐC M2 — BẢN DÙNG ỔN ĐỊNH ĐÃ HOÀN THÀNH VÀ NGHIỆM THU (28/10/2026)**.
- Toàn bộ các giai đoạn từ **G1 đến G7** đã được triển khai, kiểm thử tự động và nghiệm thu hoàn chỉnh:
  - `[x]` **Mốc G1 (Nền tảng):** Khởi tạo Spring Boot 3.4.3 + Java 21, Flyway migration, React 19 + TypeScript Vite Shell, Tailwind CSS, Session HttpOnly `SESSION`, CSRF Token `X-CSRF-TOKEN`, Module Identity (`/auth/login`, `/auth/logout`, `/me/preferences`).
  - `[x]` **Mốc G2 (Nội dung & Danh mục):** Flyway content tables, JSON Schema validator Draft 2020-12, CLI importer/publisher trong 1 transaction an toàn, API catalog & outline phẳng không N+1 (`/curricula`, `/outline`), UI danh mục và chi tiết lộ trình.
  - `[x]` **Mốc G3 — MỐC M1 (Vòng học Cốt lõi):** Đăng ký lộ trình, lịch ban đầu, Dashboard "Hôm nay", Không gian học tập (Learning Workspace) Markdown + IPA, Ghi chú tự lưu (Autosave sau 700ms), Lưu tiến độ nguyên tử với optimistic locking (`expectedVersion`), E2E kiểm chứng đạt 100%.
  - `[x]` **Mốc G4 (Lịch học Linh hoạt):** Lịch học tùy biến 3–5 ngày/tuần, Thuật toán đổi lịch (Reschedule Preview & Commit) bảo toàn bài đã học, Đóng băng tiến độ (Pause), Tiếp tục học (Resume) và Kết thúc (Abandon).
  - `[x]` **Mốc G5 (Quiz & Ôn tập):** Mô hình trắc nghiệm chống gian lận (che đáp án trước submit), Chấm điểm tự động trên server, Quy tắc `ON_SUBMIT` vs `PASS_SCORE` (80%), Sổ tay ôn tập (Review mistakes & Bookmarks) tự động đồng bộ.
  - `[x]` **Mốc G6 (Hoàn thiện Lộ trình A & Báo cáo):** Biên soạn và xuất bản đầy đủ **26 tuần Lộ trình A** (3 phases, 26 weeks, 130 lessons, 406 activities) ở trạng thái `PUBLISHED (IMMUTABLE)`, Trang tiến độ (`/progress`) với biểu đồ thanh tuần thuần CSS/SVG và chuỗi ngày học Streak thực tế.
  - `[x]` **Mốc G7 (Vận hành & Phát hành Cá nhân):**
    * **G7-01:** Tự động hóa sao lưu và phục hồi dữ liệu (`pg_dump`/`pg_restore`), đối soát 100% khớp hàng trên toàn bộ 23/23 bảng dữ liệu, Flyway version 009, immutability trigger; cơ chế bảo vệ kép chống ghi đè DB production.
    * **G7-02:** Kiểm chuẩn hiệu năng API tự động (12/12 endpoints đạt p95 < 16ms, vượt trội so với ngân sách 500ms), Kiểm thử trực quan desktop (1440×900 và 1280×800) không lỗi font IPA, không tràn ngang.
    * **G7-03:** Sổ tay vận hành toàn diện [docs/RUNBOOK.md](docs/RUNBOOK.md) và biên bản nghiệm thu mốc M2 [docs/G7-03_ACCEPTANCE.md](docs/G7-03_ACCEPTANCE.md).
- **Tài liệu vận hành chính:** Xem chi tiết tại [docs/RUNBOOK.md](docs/RUNBOOK.md).

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
├── docs/                         # Toàn bộ tài liệu phân tích kỹ thuật, nghiệm thu và vận hành
│   ├── RUNBOOK.md                # Sổ tay hướng dẫn vận hành toàn diện hệ thống Mốc M2
│   ├── G7-01_ACCEPTANCE.md       # Nghiệm thu sao lưu & phục hồi dữ liệu tự động
│   ├── G7-02_ACCEPTANCE.md       # Nghiệm thu hiệu năng API và kiểm thử trực quan desktop
│   ├── G7-03_ACCEPTANCE.md       # Nghiệm thu Runbook và phát hành bản dùng ổn định (Mốc M2)
│   ├── product-rules.md          # Quy tắc nghiệp vụ, công thức tiến độ, vòng đời trạng thái
│   ├── content-mapping.md        # Đối chiếu 26 tuần lộ trình A, phân loại nguồn & pilot 3 ngày/tuần
│   ├── wireframes.md             # Mô tả chi tiết wireframe các màn hình chính và responsive
│   ├── api/
│   │   ├── openapi.yaml          # OpenAPI 3.1 contract ban đầu đầy đủ (0 lint errors)
│   │   └── generated-types.ts    # TypeScript types sinh từ openapi.yaml
│   ├── adr/                      # Các quyết định kiến trúc quan trọng (ADR 001 - 005)
│   └── tasks/
│       └── backlog.md            # Danh mục công việc chi tiết từ G1 đến G7 và PA
│
├── content/                      # Gói nội dung giáo trình được quản lý bằng Git
│   ├── schemas/                  # Bộ JSON Schema chuyên biệt xác thực nội dung
│   ├── pilot-curriculum/v1/      # Lộ trình thử nghiệm (1 tuần, 6 bài, 21 activities)
│   └── roadmap-a/v1/             # Lộ trình A hoàn chỉnh 26 tuần (3 phases, 130 lessons, 406 activities)
│
├── frontend/                     # Ứng dụng Frontend React SPA (Vite + TSX + Tailwind)
│
└── scripts/                      # Bộ công cụ tự động hóa vận hành, kiểm thử & sao lưu
    ├── backup-db.ps1             # Sao lưu dữ liệu pg_dump nén an toàn nhị phân + SHA-256
    ├── restore-db.ps1            # Phục hồi dữ liệu pg_restore tự động xác thực SHA-256
    ├── verify-backup-restore.ps1 # Kiểm thử tự động đối soát toàn vẹn 23/23 bảng dữ liệu
    ├── benchmark-api.mjs         # Đo kiểm chuẩn hiệu năng API (12 endpoints, p95 < 500ms)
    ├── validate-content.js       # Kiểm tra Schema, Markdown paths, Resource keys
    └── validate-openapi-examples.js # Kiểm tra cú pháp OpenAPI 3.1 & tính đầy đủ của schema/payload
```

---

## 7. Hướng dẫn Dành cho AI Triển khai Kế tiếp

Khi được giao việc ở các giai đoạn tiếp theo (bắt đầu từ G1):
1. **Đọc kỹ:** `KE_HOACH_DU_AN.md`, `BE_CODE_CONVENTION.md`, `FE_CODE_CONVENTION.md`, và `docs/product-rules.md`.
2. **Tuân thủ hợp đồng:** Toàn bộ DTO, Error code, Headers, và Endpoints phải khớp chính xác với `docs/api/openapi.yaml`.
3. **Thực hiện từng task:** Nhận task từ `docs/tasks/backlog.md`, hoàn thành đầy đủ tiêu chí nghiệm thu (DoD) trước khi chuyển sang task phụ thuộc.
4. **Không thay đổi hạ tầng ngoài phạm vi:** Không sửa đổi container database PostgreSQL hoặc compose có sẵn ngoài phạm vi yêu cầu của task.
