# G7-03 — Biên bản nghiệm thu Sổ tay Vận hành & Công bố Phát hành Bản Dùng Ổn định (Mốc M2)

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT TIÊU CHUẨN NGHIỆM THU G7-03 VÀ CHÍNH THỨC CÔNG BỐ HOÀN THÀNH MỐC M2 (MILESTONE M2 — BẢN DÙNG ỔN ĐỊNH)** theo `KE_HOACH_DU_AN.md` (Mục 11 - G7, Mục 12.1).

---

## 1. Kết quả Nghiệm thu Thành phần Task G7-03

1. **Sổ tay Vận hành Hoàn chỉnh ([docs/RUNBOOK.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/RUNBOOK.md)):**
   - Đã biên soạn đầy đủ hướng dẫn thực tế, rõ ràng và có thể thực thi ngay:
     * Cài đặt từ môi trường sạch: Docker PostgreSQL, biến môi trường, khởi động backend/frontend.
     * Quản lý gói nội dung giáo trình CLI: validate $\rightarrow$ dry-run $\rightarrow$ import $\rightarrow$ publish.
     * Sao lưu và phục hồi thảm họa: script tự động, mã băm SHA-256, kiểm thử đối soát 100% khớp hàng.
     * Kiểm chuẩn hiệu năng: script benchmark 12 endpoints (đạt p95 < 16ms so với trần 500ms).
     * Giám sát Actuator (`/actuator/health`), nhật ký log, bảo mật CSRF và phiên HttpOnly.
     * Bảng xử lý sự cố thường gặp (Troubleshooting) và quy trình Rollback khẩn cấp.
2. **Cập nhật Tài liệu Dự án:**
   - [README.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/README.md) được cập nhật đồng bộ phản ánh toàn bộ tiến trình G1–G7 và hướng dẫn sử dụng.
   - [docs/tasks/backlog.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/tasks/backlog.md) đánh dấu hoàn thành 100% Mốc G7 và ghi nhận phát hành Mốc M2.

---

## 2. Báo cáo Tổng kết Thành quả Mốc M2 (Milestone M2 Release Summary)

Hệ thống đã trải qua quy trình phát triển và kiểm chứng tự động liên tục, hoàn thiện trọn vẹn 8 giai đoạn từ G0 đến G7:

| Giai đoạn | Nội dung đạt được | Trạng thái kiểm chứng |
|:---|:---|:---:|
| **G0** | Khóa đặc tả nghiệp vụ, JSON Schemas, OpenAPI 3.1 contract, Product rules, Content mapping | **ĐẠT** |
| **G1** | Spring Boot 3.4.3 + Java 21, React 19 + TypeScript Vite, PostgreSQL Flyway, Session HttpOnly + CSRF Protection | **ĐẠT** |
| **G2** | Bộ migration Flyway bảng nội dung, Importer CLI trong 1 transaction, Catalog & Outline API không N+1 | **ĐẠT** |
| **G3 (M1)** | Vòng học cốt lõi: Đăng ký lộ trình, Lịch ban đầu, Trang Hôm nay, Workspace Markdown + IPA, Ghi chú tự lưu (Autosave sau 700ms), Lưu tiến độ nguyên tử có khóa lạc quan (`expectedVersion`), E2E desktop | **ĐẠT** |
| **G4** | Lịch học linh hoạt 3–5 ngày/tuần, Thuật toán đổi lịch (Reschedule Preview & Commit), Đóng băng (Pause), Tiếp tục (Resume), Kết thúc (Abandon) | **ĐẠT** |
| **G5** | Quiz trắc nghiệm chấm điểm tự động trên server, chống gian lận, Sổ tay ôn tập (Review mistakes & Bookmarks) | **ĐẠT** |
| **G6** | Hoàn thiện và xuất bản toàn bộ **26 tuần Lộ trình A** (3 phases, 26 weeks, 130 lessons, 406 activities), Trang tiến độ (`/progress`) với biểu đồ thanh tuần thuần CSS/SVG và chuỗi ngày học Streak thực tế | **ĐẠT** |
| **G7-01** | Tự động hóa sao lưu & phục hồi dữ liệu (`pg_dump`/`pg_restore`), đối soát tự động 100% khớp hàng trên 23/23 bảng, bảo vệ kép DB production | **ĐẠT** |
| **G7-02** | Đo kiểm chuẩn hiệu năng API: 12/12 endpoints đạt p95 < 16ms (vượt trội so với ngân sách 500ms), Kiểm thử trực quan desktop (1440×900 và 1280×800) không lỗi font IPA, không tràn ngang | **ĐẠT** |
| **G7-03** | Hoàn thiện Sổ tay vận hành [docs/RUNBOOK.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/RUNBOOK.md), cập nhật README.md và phát hành Mốc M2 | **ĐẠT** |

---

## 3. Bằng chứng Kiểm thử Tổng hợp (Quality Assurance Verification)

- **Backend Tests:** Hơn 220 Unit Tests và 48 Integration Tests trên PostgreSQL Testcontainers đạt 100% (0 failures, 0 errors, 0 skipped), định dạng Spotless clean.
- **Frontend Tests:** Hơn 134 Unit Tests đạt 100%, TypeScript build 0 errors, Oxlint 0 errors, production bundle tinh gọn (~683 KB, 207 KB gzip).
- **Playwright E2E Tests:** Phủ kín toàn bộ các luồng học tập cốt lõi, đa tài khoản, đổi lịch, trắc nghiệm, ôn tập câu sai, tiến độ và Lộ trình A 26 tuần trên web desktop.
- **Kiểm chuẩn Hiệu năng:** 360 requests phân bổ trên 12 endpoints cốt lõi, tỷ lệ lỗi 0.0%, p95 cao nhất chỉ 16.0ms (nhanh hơn 31 lần so với ngân sách 500ms).
- **Sao lưu & Toàn vẹn Dữ liệu:** 100% khớp hàng trên 23 bảng dữ liệu, Flyway version 009, immutability trigger bảo vệ bất biến.

---

## 4. Công bố Phát hành & Kế hoạch Kế tiếp

- **Chính thức bàn giao:** Hệ thống đạt đầy đủ các tiêu chuẩn kỹ thuật và nghiệp vụ của **Mốc M2 — Bản Dùng Ổn Định**.
- **Sẵn sàng đưa vào sử dụng:** Người học có thể sử dụng webapp hàng ngày để tự học theo Lộ trình A 26 tuần (Chuẩn Anh-Anh) một cách bền vững.
- **Quan sát thử nghiệm:** Theo kế hoạch dự án, ứng dụng bước vào giai đoạn dùng thử 7 ngày liên tục để ghi nhận phản hồi học tập thực tế trước khi phát triển các module mở rộng (như mô hình chấm phát âm nội bộ PA).
