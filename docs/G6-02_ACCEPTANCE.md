# G6-02 — Nghiệm thu Rà soát, Biên soạn và Xuất bản toàn bộ 26 tuần của Lộ trình A v1

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G6-02 cho web desktop**. Hoàn thành toàn diện mốc G6 (Hoàn thiện Lộ trình A & Báo cáo Học tập) theo `KE_HOACH_DU_AN.md` và `docs/content-mapping.md`.

---

## 1. Bằng chứng Kiểm thử Độc lập

| Hạng mục kiểm tra | Kết quả | Chi tiết / Log |
| :--- | :--- | :--- |
| **Playwright Desktop E2E (Dev DB thật)** | **1/1 đạt**, 4.2s (`roadmap-a-desktop.e2e.ts`) | `frontend/e2e/roadmap-a-desktop.e2e.ts`: Đăng nhập học viên kiểm thử, truy cập `/curricula`, xác nhận thẻ Lộ trình A hiển thị đầy đủ thông tin chuẩn `en-GB` và `v1.0.0`, chuyển sang trang chi tiết `/curricula/roadmap-a`, kiểm tra sự hiện diện của toàn bộ 3 Giai đoạn và 26 tuần học, kích hoạt hộp thoại đăng ký học, điều hướng sang `/progress` và kiểm tra biểu đồ thanh hiển thị đầy đủ từ Tuần 1 đến Tuần 26. |
| **Regression Playwright E2E** | **Đạt 100%**: `progress-desktop.e2e.ts` (3.8s), `review-desktop.e2e.ts` (4.8s), `quiz-desktop.e2e.ts` (5.1s) | Chạy an toàn qua bộ điều phối `scripts/run-e2e.ps1`, bảo toàn 100% dữ liệu của các mốc trước. |
| **Backend Content Validator** | **0 lỗi (SUCCESSFUL)** | `java -jar target/...jar content validate ../content/roadmap-a/v1`: 100% hợp lệ JSON Schema Draft 2020-12, an toàn đường dẫn traversal, kiểm tra toàn bộ 130 tệp Markdown không rỗng, 10 tài nguyên tham chiếu chuẩn xác. |
| **Backend Content Dry-run** | **Checksum xác thực** | `Checksum SHA-256: 36f114aa724074288955c9681311221080eba2efc05faf4658b309223420a836`. Báo cáo: 3 phases, 26 weeks, 130 lessons (130 required), 406 activities (390 effectively required), 10 resources. |
| **Backend Content Importer & Publisher** | **Thành công nguyên tử (Atomic)** | - Import: Revision `9f36eec2-5967-47a9-916e-29901b90712d` ở trạng thái `DRAFT`.<br>- Publish: Xuất bản chính thức trạng thái `PUBLISHED (IMMUTABLE)` lúc `2026-10-28T10:07:26Z`. |
| **Backend Unit Tests** | **220/220 đạt**, 0 failures, 0 errors | `mvnw.cmd test "-Dtest=!*IT"` chạy trong 11.6s, không lỗi. |
| **Frontend Code Quality** | **TypeScript 0 errors, OxLint 0 errors** | `npm run build` (`tsc -b && vite build`) thành công trong 1.26s, `npm run lint` 0 errors. |
| **Spotless Code Format** | **162/162 files clean** | `mvnw.cmd spotless:check` đạt chuẩn format toàn bộ mã nguồn Java. |
| **Desktop Screenshots** | **3 ảnh chụp sắc nét** | `frontend/e2e-screenshots/`: `roadmap-a-catalog.png`, `roadmap-a-detail-phases.png`, `roadmap-a-progress-26weeks.png`. |

---

## 2. Các Tiêu chí Nghiệm thu Cốt lõi của G6-02

1. **Gói Nội dung Giáo trình Đầy đủ 26 Tuần (`content/roadmap-a/v1`):**
   - **Định danh chuẩn:** `curriculumKey: "roadmap-a"`, `version: "1.0.0"`, `targetLocale: "en-GB"`.
   - **3 Giai đoạn Sư phạm (Phases):**
     * **Giai đoạn 1 (Tuần 1):** Chuẩn hóa Ngữ âm IPA & Nền tảng Ban đầu.
     * **Giai đoạn 2 (Tuần 2 – Tuần 13):** Tích lũy Ngữ pháp & Kỹ năng Nghe hiểu cốt lõi (Hiện tại đơn, tiếp diễn, quá khứ đơn, tương lai, câu hỏi Wh-, danh từ đếm được/không đếm được, mạo từ, so sánh, động từ khuyết thiếu, hiện tại hoàn thành, giới từ In/On/At).
     * **Giai đoạn 3 (Tuần 14 – Tuần 26):** Phản xạ Giao tiếp Đời sống & Ứng dụng Thực tế (Du lịch, sân bay, ẩm thực, tiền tip, mua sắm, khách sạn, y tế, thời tiết kiểu Anh, sở thích, điện ảnh, email công việc, công nghệ, tranh luận P-E-E, kế hoạch tương lai, tổng kết).
   - **130 Bài học Hoàn chỉnh:** Mỗi tuần có 5 bài học (tổng 130 bài học), có thời lượng ước tính cụ thể (`estimatedMinutes`), và tệp nội dung Markdown riêng biệt (`lessons/*.md`).
   - **406 Nhiệm vụ phong phú:** Kết hợp chặt chẽ giữa `READING` (lý thuyết & IPA), `PRACTICE` (luyện viết câu/phản hồi), `REFLECTION` (phản tư), `EXTERNAL_RESOURCE` (BBC, British Council, Oxford, Cambridge), và `QUIZ` (bài kiểm tra trắc nghiệm định kỳ các tuần 1, 8, 13, 26).

2. **Chính sách Bất biến của Giáo trình Đã Xuất bản (Revision Immutability):**
   - Lộ trình A v1.0.0 sau khi xuất bản được chuyển sang trạng thái `PUBLISHED` và được bảo vệ bất biến bởi các trigger database `V004__enforce_published_revision_immutability.sql`.
   - Không cho phép sửa đổi nội dung bài học hoặc thay đổi cấu trúc revision sau khi đã xuất bản.

3. **Tương thích API & Hiển thị Giao diện Người học:**
   - Endpoint `GET /api/v1/curricula` trả về "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng (Chuẩn Anh-Anh)" ở vị trí ưu tiên.
   - Endpoint `GET /api/v1/curricula/{id}/outline` trả về toàn bộ cây mục lục 3 giai đoạn và 26 tuần cho người học đã xác thực.
   - Trang Chi tiết Lộ trình (`/curricula/roadmap-a`) hỗ trợ mở từng tuần, xem danh sách bài học, thời lượng dự kiến và trạng thái bắt buộc.
   - Trang Tiến độ (`/progress`) tự động nhận diện và kết xuất đầy đủ 26 tuần trong biểu đồ thanh tiến độ tuần.

4. **Bảo tồn Dữ liệu & Tính Cô lập:**
   - Không ảnh hưởng đến các giáo trình thử nghiệm trước đó (`pilot-curriculum`).
   - Tuân thủ tuyệt đối quy định an toàn dữ liệu: không chạm đến tài khoản cá nhân, mọi thao tác test đều cô lập với tiền tố `e2e_`.

---

## 3. Các Điểm Codex Đã Xử Lý & Hoàn Thiện

1. **Biên soạn Tự động & Chuẩn hóa Cấu trúc Dữ liệu:**
   - Xây dựng công cụ `scripts/generate-roadmap-a.js` chuyển đổi toàn bộ khung đối chiếu sư phạm 26 tuần trong `docs/content-mapping.md` thành gói nội dung chuẩn JSON Schema Draft 2020-12, tự động tạo 130 tệp Markdown chi tiết kèm phiên âm IPA và ví dụ ngữ cảnh.

2. **Cơ chế CLI Bảo vệ Cơ sở Dữ liệu:**
   - Tận dụng cờ `--confirm-db` của `CurriculumCli` để xác thực rõ ràng mục tiêu ghi vào cơ sở dữ liệu phát triển mà không làm xáo trộn các tài nguyên ngoài luồng.

3. **Xử lý Xung đột Đăng ký trong Playwright E2E:**
   - Khắc phục trường hợp người dùng test đã có sẵn enrollment từ phiên trước dẫn đến lỗi `STATE_CONFLICT` trong hộp thoại đăng ký; bổ sung cơ chế kiểm tra và đóng hộp thoại an toàn, giúp test chạy ổn định và idempotent 100%.

---

## 4. Trạng thái Git & Kết luận

- Toàn bộ tính năng G6-02 đã hoàn thiện xuất sắc và đạt tiêu chí nghiệm thu.
- Toàn bộ Mốc G6 (G6-01 & G6-02) đã hoàn thành trọn vẹn.
- Sẵn sàng chuyển tiếp sang **Giai đoạn G7 — Vận hành & Phát hành Cá nhân**.
