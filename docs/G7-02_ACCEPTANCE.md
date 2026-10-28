# G7-02 — Biên bản nghiệm thu Kiểm thử Hiệu năng API & Kiểm thử Trực quan Desktop

Ngày 28/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT tiêu chuẩn nghiệm thu G7-02**, hoàn thành đánh giá hiệu năng API (100% endpoints đạt p95 < 500ms) và kiểm thử trực quan giao diện web desktop (1440×900 và 1280×800) trên bộ dữ liệu thực tế đầy đủ của Lộ trình A (26 tuần, 130 lessons, 406 activities).

---

## 1. Báo cáo Hiệu năng API (API Performance Benchmark)

Kịch bản kiểm thử hiệu năng tự động [scripts/benchmark-api.mjs](file:///d:/WorkSpace/tu-hoc-tieng-anh/scripts/benchmark-api.mjs) đo lường độ trễ thực tế qua 30 chu kỳ đo (+ 3 lần chạy làm nóng) có phiên đăng nhập xác thực của học viên trên dữ liệu Lộ trình A 26 tuần.

- **Ngân sách quy định (Budget Threshold):** `p95 < 500ms`
- **Kết quả tổng thể:** **12/12 Endpoints ĐẠT (100% PASSED)**
- **Tỷ lệ lỗi (Error Rate):** **0.0% (0 errors / 360 requests)**
- **Độ trễ p95 cao nhất:** **16.0ms** (nhanh hơn ngân sách 500ms hơn **31 lần**)
- **Báo cáo chi tiết đã lưu:** [reports/api-benchmark-report.json](file:///d:/WorkSpace/tu-hoc-tieng-anh/reports/api-benchmark-report.json)

### Bảng tổng hợp số đo độ trễ (Latency Breakdown Table):

| STT | Endpoint | Mô tả chức năng | Mẫu (Samples) | Min (ms) | Avg (ms) | p50 (ms) | p90 (ms) | p95 (ms) | Max (ms) | Kết luận (Ngân sách <500ms) |
|:---:|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | `GET /curricula` | Danh mục lộ trình công khai | 30 | 2.0 | 3.0 | 2.9 | 3.9 | **4.0** | 4.0 | **ĐẠT (Vượt 125x)** |
| 2 | `GET /curricula/roadmap-a` | Chi tiết Lộ trình A | 30 | 4.7 | 6.6 | 6.7 | 7.4 | **7.9** | 8.3 | **ĐẠT (Vượt 63x)** |
| 3 | `GET /revisions/.../outline` | Đề cương 26 tuần (130 bài học) | 30 | 6.4 | 7.1 | 7.0 | 7.6 | **7.8** | 8.7 | **ĐẠT (Vượt 64x)** |
| 4 | `GET /me/today` | Bảng điều khiển Hôm nay | 30 | 7.9 | 10.2 | 9.2 | 11.4 | **12.5** | 32.5 | **ĐẠT (Vượt 40x)** |
| 5 | `GET /me/enrollments` | Danh sách lộ trình của tôi | 30 | 6.5 | 8.6 | 7.6 | 9.2 | **15.7** | 20.3 | **ĐẠT (Vượt 31x)** |
| 6 | `GET /enrollments/.../detail` | Chi tiết Enrollment | 30 | 2.4 | 3.2 | 3.1 | 3.9 | **4.1** | 4.2 | **ĐẠT (Vượt 121x)** |
| 7 | `GET /enrollments/.../lessons/.../summary` | Tóm tắt buổi học (DTO nhẹ) | 30 | 3.4 | 5.0 | 4.9 | 5.8 | **6.5** | 6.8 | **ĐẠT (Vượt 76x)** |
| 8 | `GET /enrollments/.../lessons/...` | Không gian học tập (Workspace) | 30 | 7.0 | 8.3 | 8.2 | 9.4 | **10.2** | 10.5 | **ĐẠT (Vượt 49x)** |
| 9 | `GET /enrollments/.../summary` | Tóm tắt tiến độ lộ trình | 30 | 5.2 | 6.4 | 6.5 | 7.5 | **7.8** | 7.9 | **ĐẠT (Vượt 64x)** |
| 10 | `GET /enrollments/.../analytics` | Thống kê tuần & Chuỗi Streak | 30 | 11.2 | 12.7 | 12.3 | 14.1 | **16.0** | 16.5 | **ĐẠT (Vượt 31x)** |
| 11 | `GET /enrollments/.../review` | Danh sách câu sai & Đánh dấu | 30 | 4.6 | 6.0 | 5.7 | 7.2 | **8.1** | 8.7 | **ĐẠT (Vượt 61x)** |
| 12 | `GET /enrollments/.../schedule` | Lịch học cá nhân | 30 | 3.2 | 4.7 | 4.4 | 5.5 | **7.0** | 8.8 | **ĐẠT (Vượt 71x)** |

---

## 2. Kết quả Kiểm thử Trực quan & Độ tương thích Desktop (Visual & UX Audit)

Đã kiểm tra trực quan trên trình duyệt desktop ở hai kích thước viewport tiêu chuẩn: **1440×900** (màn hình chuẩn) và **1280×800** (laptop gọn):

1. **Trang Hôm nay (`/today`):**
   - Hiển thị rõ nét tiêu đề lộ trình đang học "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng (Chuẩn Anh-Anh) (ACTIVE)".
   - Tiến độ lộ trình: hiển thị đúng mẫu số bài bắt buộc `0 / 390 hoạt động bắt buộc`.
   - Card hành động chính: "Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/" kèm CTA nổi bật "Bắt đầu bài học ->".
2. **Hiển thị Ký tự Phiên âm IPA:**
   - Ký tự phiên âm quốc tế như `/iː/` và `/ɪ/` hiển thị sắc nét, chuẩn UTF-8/Unicode, không bị lỗi font hay ô vuông trên toàn bộ các trang (Hôm nay, Danh mục, Đề cương, Workspace bài học).
3. **Không gian Học tập (`/enrollments/.../lessons/...`):**
   - Header bài học có đầy đủ các nút chức năng: "Đánh dấu ôn tập", "Ghi chú", "Chế độ tập trung", "Bài tiếp ->".
   - Trình đọc Markdown hiển thị chuẩn xác cấu trúc phân cấp, danh sách mục tiêu và kiến thức trọng tâm.
   - Panel Ghi chú (Note Editor) hỗ trợ tự lưu (autosave sau 700ms) có chỉ báo trạng thái và bộ đếm từ/ký tự.
4. **Trang Tiến độ (`/progress`):**
   - Bốn thẻ chỉ số: Tiến độ lộ trình (0%), Chuỗi ngày học (0 ngày), Thời lượng nội dung (2970 phút), Bài học hoàn thành (0 / 130 bài).
   - Biểu đồ thanh tỷ lệ hoàn thành theo tuần hiển thị mượt mà, phân loại rõ ràng trạng thái.
5. **Trang Lịch học (`/schedule`):**
   - Lịch tuần trực quan từ Thứ Hai đến Chủ Nhật với thẻ buổi học chi tiết.
   - Thẻ ngày hiện tại (Hôm nay - Thứ Tư) được tô viền nổi bật.
   - Modal "Đổi lịch học" mở trơn tru, cho phép chọn ngày học trong tuần và tính toán preview.
6. **Không vỡ giao diện & Không tràn ngang:**
   - Tại viewport 1280×800, các trang co giãn linh hoạt, không phát sinh thanh cuộn ngang (horizontal scrollbar), các nút bấm và văn bản giữ nguyên độ tương phản và khoảng cách thẩm mỹ.

---

## 3. Ảnh chụp Minh chứng Giao diện Desktop (Screenshots)

| Màn hình | Viewport | Tệp ảnh chụp | Đánh giá trực quan |
|:---|:---:|:---|:---:|
| **Bảng điều khiển Hôm nay** | 1440×900 | `today_dashboard_1793190907030.png` | Rõ ràng, CTA nổi bật, IPA hiển thị chuẩn |
| **Chi tiết Lộ trình A 26 tuần** | 1440×900 | `roadmap_a_page_1793190928057.png` | 3 Phases và 26 Weeks mở rộng trơn tru |
| **Không gian Học tập & Ghi chú** | 1440×900 | `learning_workspace_1793190949392.png` | Markdown phân cấp đẹp, Note autosave chuẩn |
| **Tiến độ & Biểu đồ Tuần** | 1440×900 | `progress_page_1793190980053.png` | 4 thẻ metrics và biểu đồ trực quan |
| **Trang Ôn tập & Câu sai** | 1440×900 | `review_page_1793191006872.png` | Tabs chuyển đổi câu sai và đánh dấu mượt mà |
| **Lịch học & Thao tác Đổi lịch** | 1440×900 | `schedule_page_1793191037439.png` | Lịch tuần rõ ràng, modal đổi lịch chuẩn xác |
| **Lịch học co giãn Laptop** | 1280×800 | `schedule_1280_800_1793191054922.png` | Không tràn ngang, responsive hoàn hảo |
| **Hôm nay co giãn Laptop** | 1280×800 | `today_1280_800_1793191072658.png` | Bố cục cân đối, hiển thị sắc nét |

---

## 4. Ràng buộc & Chuyển tiếp

- Đạt toàn diện cả 2 nội dung của Task G7-02.
- Sẵn sàng chuyển tiếp sang **Task G7-03**: "Hoàn thiện tài liệu hướng dẫn vận hành (Runbook) và phát hành bản dùng ổn định (Mốc M2)".
