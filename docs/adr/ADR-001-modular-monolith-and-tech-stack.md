# ADR-001: Lựa chọn Kiến trúc Modular Monolith và Tech Stack

**Ngày:** 24/10/2026  
**Trạng thái:** Accepted (Đã duyệt)  
**Người quyết định:** AI Specialist & Project Coordinator  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../../KE_HOACH_DU_AN.md), [BE_CODE_CONVENTION.md](../../BE_CODE_CONVENTION.md), [FE_CODE_CONVENTION.md](../../FE_CODE_CONVENTION.md)

---

## 1. Bối cảnh (Context)
Dự án xây dựng ứng dụng webapp tự học tiếng Anh theo lộ trình, ban đầu phục vụ nhu cầu tự học cá nhân, có khả năng mở rộng cho nhiều người dùng sau này. 
Yêu cầu hệ thống cần:
- Lưu trữ dữ liệu tiến độ bền vững, đáng tin cậy.
- Xử lý các quy tắc nghiệp vụ phức tạp về lịch học cá nhân, kiểm soát đồng thời, và tính toán tiến độ.
- Giao diện người dùng hiện đại, phản hồi nhanh, hỗ trợ cả máy tính và điện thoại.
- Hạ tầng hiện có sẵn gồm Docker và PostgreSQL trong môi trường WSL.

## 2. Quyết định (Decision)

### 2.1. Kiến trúc Backend: Modular Monolith với Java Spring Boot
- **Framework:** Java 21, Spring Boot (phiên bản ổn định mới nhất được pin tại G1), Spring Web MVC, Spring Security, Spring Data JPA.
- **Cấu trúc:** Modular Monolith (một ứng dụng duy nhất, chia package theo ranh giới nghiệp vụ: `identity`, `curriculum`, `contentops`, `enrollment`, `scheduling`, `learning`, `assessment`, `notes`, `review`, `reporting`, `pronunciation`).
- **Database:** PostgreSQL hiện có trong Docker/WSL. Quản lý schema bằng Flyway migration.
- **Lý do không chọn Microservices:** Tránh phân tán giao dịch (distributed transactions), giảm chi phí vận hành mạng và hạ tầng, phù hợp với quy mô đội ngũ và mục tiêu ra mắt nhanh bản dùng được.

### 2.2. Kiến trúc Frontend: React SPA với TypeScript + Vite + Tailwind CSS
- **Framework:** React function components với TypeScript ở chế độ nghiêm ngặt (`strict: true`).
- **Build tool:** Vite (tốc độ khởi động và HMR cực nhanh).
- **Styling & UI:** Tailwind CSS kết hợp bộ UI primitives thống nhất từ `shadcn/ui`. Sử dụng semantic design tokens, hạn chế dùng màu hex rải rác.
- **Quản lý dữ liệu Server State:** TanStack Query (React Query). Tuyệt đối không thêm Redux hoặc Zustand chỉ để lưu trữ lại dữ liệu đã có trong query cache.
- **Quản lý Form:** React Hook Form kết hợp Zod schema validation.

### 2.3. Ranh giới giao tiếp (API Boundary)
- Tích hợp qua REST API với prefix `/api/v1` cùng origin qua Vite proxy (ở dev) hoặc reverse proxy (ở prod).
- Dữ liệu trao đổi dạng JSON camelCase; lỗi định dạng RFC 7807 Problem Details (`application/problem+json`).
- Contract API duy nhất được định nghĩa tại `docs/api/openapi.yaml`. Typescript types cho FE được sinh tự động từ file này.

## 3. Hệ quả (Consequences)
- **Tích cực:**
  * Toàn bộ mã nguồn nằm trong một monorepo, dễ dàng điều phối, kiểm thử tích hợp (integration tests với Testcontainers PostgreSQL).
  * Tiết kiệm tài nguyên máy, chạy mượt mà trên môi trường cá nhân.
  * Phân tách rõ ràng giữa frontend state và server state, tránh lỗi đồng bộ dữ liệu.
- **Thách thức:**
  * Cần kỷ luật cao giữa các package backend để không tạo phụ thuộc vòng (circular dependencies) hoặc truy cập trực tiếp repository của nhau.
  * Phải cấu hình mạng WSL/Windows đồng bộ để Spring Boot kết nối PostgreSQL trong Docker ổn định.
