# G7-03 — Nhiệm vụ: Hoàn thiện Sổ tay Vận hành (Runbook) & Phát hành Bản Dùng Ổn định (Mốc M2)

## 1. Mục tiêu & Bối cảnh

Task **G7-03** là bước hoàn tất cuối cùng của giai đoạn G7 và đánh dấu việc đạt **Mốc M2 (Milestone M2 — Bản Dùng Ổn Định)** theo `KE_HOACH_DU_AN.md` (Mục 11 - G7, Mục 12):
- Hệ thống đã hoàn thành trọn vẹn từ G1 đến G6:
  * G1: Nền tảng Spring Boot, PostgreSQL, React Vite, Session HttpOnly & CSRF.
  * G2: Schema nội dung, CLI Importer/Publisher, Catalog & Đề cương lộ trình.
  * G3 (Mốc M1): Vòng học cốt lõi, lưu tiến độ nguyên tử, Workspace Markdown + IPA, Ghi chú tự lưu (Autosave), E2E kiểm chứng.
  * G4: Lịch học linh hoạt 3–5 ngày/tuần, Đổi lịch học (Reschedule), Đóng băng (Pause), Tiếp tục (Resume) và Kết thúc (Abandon).
  * G5: Quiz trắc nghiệm tự động chấm điểm trên server, chống gian lận, Sổ tay ôn tập (Review mistakes & Bookmarks).
  * G6: Trang tiến độ & Phân tích học tập (Weekly bar chart, Streak thực tế), Toàn bộ 26 tuần Lộ trình A (130 lessons, 406 activities) biên soạn và xuất bản bất biến.
  * G7-01: Tự động hóa sao lưu và phục hồi dữ liệu PostgreSQL (`pg_dump`/`pg_restore`), đối soát 100% trên 23 bảng.
  * G7-02: Kiểm chuẩn hiệu năng API (100% endpoints p95 < 16ms, vượt xa ngân sách 500ms) và kiểm thử trực quan desktop (1440×900 và 1280×800).
- Mục tiêu của G7-03:
  1. Xây dựng Sổ tay Vận hành chuẩn hóa ([docs/RUNBOOK.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/RUNBOOK.md)) hướng dẫn chi tiết từ cài đặt môi trường sạch, chạy DB, migration, import giáo trình, sao lưu/phục hồi, giám sát sức khỏe, đến khắc phục sự cố (Troubleshooting & Rollback).
  2. Cập nhật [README.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/README.md) phản ánh trung thực toàn bộ năng lực hệ thống tại mốc M2.
  3. Lập biên bản nghiệm thu [docs/G7-03_ACCEPTANCE.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/G7-03_ACCEPTANCE.md) và công bố phát hành Mốc M2 sẵn sàng cho chu kỳ tự học thực tế.

---

## 2. Tiêu chí Nghiệm thu (DoD)

1. **Sổ tay Vận hành Toàn diện ([docs/RUNBOOK.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/RUNBOOK.md)):**
   - Hướng dẫn cài đặt từ đầu trên máy mới (clean setup) với các bước rõ ràng, chạy được ngay.
   - Hướng dẫn cấu hình biến môi trường và tài khoản bootstrap an toàn (không lộ bí mật).
   - Hướng dẫn chi tiết quy trình CLI import và publish gói nội dung mới hoặc cập nhật.
   - Hướng dẫn sao lưu, phục hồi dữ liệu và kiểm chứng tính toàn vẹn (scripts đã kiểm chứng ở G7-01).
   - Hướng dẫn chạy benchmark hiệu năng (script đã kiểm chứng ở G7-02).
   - Quy trình giám sát sức khỏe qua Actuator, chẩn đoán lỗi thường gặp và rollback an toàn.
2. **Cập nhật Tài liệu Dự án:**
   - [README.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/README.md) phản ánh đầy đủ kiến trúc, stack, trạng thái hoàn thành G1–G7 và hướng dẫn sử dụng.
   - [docs/tasks/backlog.md](file:///d:/WorkSpace/tu-hoc-tieng-anh/docs/tasks/backlog.md) đánh dấu hoàn thành 100% G7 và mốc M2.
3. **Ràng buộc An toàn:**
   - Tuyệt đối không commit secrets, `.env`, `docs/DAY2_REVIEW.md`, hay `backend/src/main/resources/META-INF/spring/`.
   - Giữ nguyên toàn bộ dữ liệu học tập và cấu trúc migrations đã thiết lập.
