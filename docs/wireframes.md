# Mô tả Wireframe và Bố cục Giao diện (Wireframes & UI Layout Specification)

**Ngày lập:** 24/10/2026  
**Trạng thái:** Hoàn thành cho G0  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../KE_HOACH_DU_AN.md), [FE_CODE_CONVENTION.md](../FE_CODE_CONVENTION.md), [product-rules.md](./product-rules.md)

---

## 1. Hệ thống Bố cục Chung (Application Shell & Navigation)

Ứng dụng tuân thủ triết lý: **Tối giản, ưu tiên tối đa sự tập trung của người học (Distraction-free learning space)**.

### 1.1. Bố cục Desktop Shell (Màn hình $\ge$ 1024px)
- **Thanh điều hướng bên trái (AppSidebar):** Chiều rộng cố định `240px`, có nút thu gọn (Collapse) về dạng icon bar `64px`.
- **Khu vực nội dung chính (Main Content Area):** Tự động co giãn, có giới hạn bề rộng đọc tối ưu (`max-w-4xl` hoặc `max-w-5xl` tùy màn hình).

```text
┌─────────────────┬──────────────────────────────────────────────────────────────────┐
│ [Logo] Tự học   │ [Breadcrumb: Lộ trình > Tuần 1 > Buổi 1]         [User: Giang D.]│
│ Lộ trình:       ├──────────────────────────────────────────────────────────────────┤
│ [Pilot (RP)  ▾] │                                                                  │
│                 │                                                                  │
│ 🏠 Hôm nay      │                                                                  │
│ 📚 Lộ trình     │                     KHU VỰC NỘI DUNG CHÍNH                       │
│ 📝 Ghi chú      │                                                                  │
│ 📊 Tiến độ      │                                                                  │
│                 │                                                                  │
│ ─────────────── │                                                                  │
│ ⚙️ Cài đặt      │                                                                  │
│ 🚪 Đăng xuất    │                                                                  │
└─────────────────┴──────────────────────────────────────────────────────────────────┘
```

### 1.2. Bố cục Mobile / Tablet (< 1024px)
- Header trên cùng cố định (`h-14`): Gồm nút Hamburger mở Drawer bên trái, logo ứng dụng, và icon tài khoản.
- Sidebar chuyển thành **Slide-over Drawer** phủ lên màn hình với lớp phủ mờ (backdrop overlay), tự động đóng khi người dùng chọn một mục điều hướng hoặc bấm phím Escape.

---

## 2. Màn hình Đăng nhập (Login Screen)

- **URL:** `/login`
- **Mục tiêu:** Xác thực người dùng qua session cookie bảo mật, hỗ trợ tự động lấy token CSRF.

```text
┌────────────────────────────────────────────────────────┐
│                                                        │
│                    [Logo Tự Học]                       │
│              KHÔNG GIAN TỰ HỌC TIẾNG ANH              │
│       Học có lộ trình • Rèn luyện bền bỉ mỗi ngày      │
│                                                        │
│       ┌────────────────────────────────────────┐       │
│       │ Tên đăng nhập                          │       │
│       │ [ giangdinh                          ] │       │
│       │                                        │       │
│       │ Mật khẩu                               │       │
│       │ [ •••••••••••••••••                  ] │       │
│       │                                        │       │
│       │ [ ⚠️ Sai tên đăng nhập hoặc mật khẩu ] │       │
│       │                                        │       │
│       │ [          Đăng nhập (Enter)         ] │       │
│       └────────────────────────────────────────┘       │
│                                                        │
│         Hệ thống tự học cá nhân • v1.0.0               │
└────────────────────────────────────────────────────────┘
```
- **Các trạng thái UI bắt buộc:**
  * `Idle`: Form sẵn sàng nhập.
  * `Submitting`: Nút submit hiển thị spinner xoay (`LoaderCircle`), vô hiệu hóa nút (`disabled`), `aria-busy="true"`.
  * `Error (401)`: Hiển thị thông báo an toàn `"Tên đăng nhập hoặc mật khẩu không chính xác."`.
  * `Network Error`: Thông báo `"Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng."`.

---

## 3. Màn hình "Hôm nay" (Today Screen - Trang Cốt lõi)

- **URL:** `/` hoặc `/today`
- **Mục tiêu:** Trả lời ngay lập tức câu hỏi: **"Hôm nay tôi cần học bài gì, bắt đầu ở đâu và tiến độ tổng thể ra sao?"**

### 3.1. Trường hợp 1: Có bài học đang dở hoặc đến hạn hôm nay

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ Xin chào, Giang! 👋                                                           │
│ Hôm nay: Thứ Tư, 28/10/2026 • Lộ trình đang học: Pilot Curriculum (Anh-Anh)    │
│                                                                                │
│ ┌────────────────────────────────────────────────────────────────────────────┐ │
│ │ 🎯 HÀNH ĐỘNG HÔM NAY                                                       │ │
│ │                                                                            │ │
│ │ [ ĐANG HỌC DỞ • Còn 2/4 nhiệm vụ ]                            ⏱️ 20 phút  │ │
│ │ Buổi 2: Cặp phụ âm /p/ bật hơi và /b/ hữu thanh                           │ │
│ │ Mục tiêu: Nắm vững kỹ thuật bật hơi âm /p/ và thử nghiệm tờ giấy bay       │ │
│ │                                                                            │ │
│ │ [Tiến độ buổi: ████████████░░░░░░░░░░░░░░ 50%]                            │ │
│ │                                                                            │ │
│ │                       [  Tiếp tục học Buổi 2 ➔  ]                          │ │
│ └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                │
│ ┌────────────────────────────────────────────────────────────────────────────┐ │
│ │ ⚠️ BÀI QUÁ HẠN CẦN BÙ (1 buổi)                                            │ │
│ │ • Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/ (Đến hạn: 26/10)   [ Học bù ➔ ]     │ │
│ └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                │
│ ┌────────────────────────────────────────────────────────────────────────────┐ │
│ │ 📊 TIẾN ĐỘ LỘ TRÌNH TỔNG THỂ                                              │ │
│ │ Đã hoàn thành: 2 / 10 nhiệm vụ bắt buộc                            20.0%    │ │
│ │ [████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]          │ │
│ └────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2. Trường hợp 2: Hôm nay là ngày nghỉ theo lịch

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ 🌴 HÔM NAY LÀ NGÀY NGHỈ CỦA BẠN                                               │
│ Bạn đã chọn lịch học 3 buổi/tuần (Thứ Hai, Thứ Tư, Thứ Sáu).                   │
│ Hãy nghỉ ngơi để não bộ củng cố kiến thức đã học!                             │
│                                                                                │
│ Bài học tiếp theo:                                                             │
│ 📅 Buổi 3: Quy tắc Trọng âm từ 2 âm tiết (Dự kiến: Thứ Sáu, 30/10)            │
│                                                                                │
│                   [  Học trước bài tiếp theo ➔  ]                            │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 3.3. Trường hợp 3: Chưa đăng ký lộ trình (NO_ENROLLMENT) hoặc Tạm dừng (PAUSED)

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ 📖 CHƯA CÓ LỘ TRÌNH ĐANG HỌC                                                  │
│ Bạn chưa đăng ký hoặc chưa chọn lộ trình nào làm tiêu điểm học tập.             │
│ Khám phá các lộ trình được thiết kế sẵn để bắt đầu hành trình tự học!        │
│                                                                                │
│               [  Khám phá Danh mục Lộ trình ➔  ]                              │
└────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ ⏸️ LỘ TRÌNH ĐANG TẠM DỪNG (PAUSED)                                            │
│ Lộ trình "Pilot Curriculum" đang được tạm dừng. Tiến độ của bạn được bảo toàn.│
│                                                                                │
│                   [  Tiếp tục học & Thiết lập lịch mới ➔  ]                   │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 3.4. Trường hợp 4: Đã hoàn thành toàn bộ lộ trình (CURRICULUM_COMPLETED)

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ 🏆 CHÚC MỪNG BẠN ĐÃ HOÀN THÀNH LỘ TRÌNH!                                      │
│ Bạn đã hoàn thành 100% nhiệm vụ bắt buộc của lộ trình "Pilot Curriculum".      │
│ Hãy tiếp tục ôn luyện hoặc chọn một lộ trình mới để nâng cao trình độ!        │
│                                                                                │
│             [ Xem lại Ghi chú ]        [ Khám phá Lộ trình khác ➔ ]           │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Không gian Học tập (Learning Workspace - Màn hình Học bài)

- **URL:** `/enrollments/:enrollmentId/lessons/:lessonId`
- **Mục tiêu:** Môi trường học tập không phân tâm. Tập trung hoàn toàn vào 1 nhiệm vụ tại một thời điểm, có panel ghi chú và thanh điều hướng dưới chân trang.
- **Xử lý đặc biệt cho nhiệm vụ REFLECTION (xử lý R7):**
  * Nhiệm vụ Reflection yêu cầu nhập câu trả lời.
  * Có nút `[ Lưu nháp ]` (gọi `PUT /response`, không đánh dấu hoàn thành).
  * Nút chính chuyển thành `[ Nộp phản tư & Hoàn thành ➔ ]` (gọi `POST /response/submit` với `expectedVersion`, lưu text và hoàn thành atomic).

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ ❮ Quay lại Lộ trình  |  Tuần 1 > Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/         │
│ Thời lượng: ~25 phút • Trạng thái: [ Đang học ]           [ ⛶ Chế độ tập trung ]│
├────────────────────────────────────────────────────────────────────────────────┤
│ Danh sách nhiệm vụ:                                                            │
│ (✓) 1. Lý thuyết khẩu hình      (✓) 2. Xem video BBC                          │
│ (●) 3. Luyện 10 cặp từ tối thiểu   ( ) 4. Phản tư cuối buổi                   │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│ 📌 Nhiệm vụ 3/4: Thực hành phát âm 10 cặp từ tối thiểu (Minimal Pairs)          │
│ Phân loại: [ Luyện tập - PRACTICE ] • Yêu cầu: [ Bắt buộc ]                   │
│                                                                                │
│ Hướng dẫn:                                                                     │
│ Đọc to từng cặp từ sau đây. Cố gắng kéo căng môi cho âm /iː/ và thả lỏng cho  │
│ âm /ɪ/. Nhập vào ô bên dưới cặp từ bạn thấy khó phát âm nhất:                  │
│                                                                                │
│ ┌────────────────────────────────────────────────────────┐                     │
│ │ • sheep /ʃiːp/ (con cừu)    -  ship /ʃɪp/ (con tàu)    │                     │
│ │ • feet /fiːt/ (bàn chân)    -  fit /fɪt/ (vừa vặn)     │                     │
│ │ • seat /siːt/ (chỗ ngồi)    -  sit /sɪt/ (ngồi xuống)  │                     │
│ └────────────────────────────────────────────────────────┘                     │
│                                                                                │
│ Câu trả lời / Ghi chú luyện tập của bạn:                                       │
│ ┌────────────────────────────────────────────────────────────────────────────┐ │
│ │ Cặp "seat" và "sit" mình hay bị đọc giống nhau. Cần chú ý thả lỏng cơ miệng│ │
│ │ khi đọc "sit".                                                             │ │
│ └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                │
│ 📝 GHI CHÚ CÁ NHÂN (Tự động lưu)                         [ Trạng thái: Đã lưu ]│
│ ┌────────────────────────────────────────────────────────────────────────────┐ │
│ │ Nhớ: /iː/ phải căng khóe miệng như đang cười nhẹ!                          │ │
│ └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                │
├────────────────────────────────────────────────────────────────────────────────┤
│ [ ❮ Nhiệm vụ trước ]        [ 🔖 Đánh dấu ôn tập ]       [ Hoàn thành & Tiếp ➔ ]│
└────────────────────────────────────────────────────────────────────────────────┘
```

- **Quy tắc thanh thao tác dưới chân trang (Sticky Footer):**
  * Nút `"Hoàn thành & Tiếp ➔"` (Primary Action): Gửi mutation cập nhật tiến độ, sau đó tự động chuyển sang nhiệm vụ tiếp theo.
  * Nếu nhiệm vụ đã hoàn thành: Nút đổi thành `"Bỏ tích hoàn thành"` (Outline variant) kèm icon hoàn tác.
  * Ghi chú có nhãn trạng thái tự lưu: `"Đang lưu..."` $\rightarrow$ `"Đã lưu"` $\rightarrow$ `"Lỗi lưu"`.

---

## 5. Màn hình Chi tiết Lộ trình (Curriculum Detail Screen)

- **URL:** `/curricula/:slug`
- **Mục tiêu:** Giúp người học xem cấu trúc tổng quan, các giai đoạn, tuần học và danh sách bài học mà không lộ trước đáp án trắc nghiệm.

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ ❮ Danh mục lộ trình                                                            │
│                                                                                │
│ Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng (Pilot)                        │
│ Phiên bản: v1.0.0 (Bản chính thức) • Chuẩn giọng: Anh-Anh (en-GB)              │
│ Tác giả: AI Specialist & Curriculum Lead • Nguồn: Giáo trình tự biên soạn       │
│                                                                                │
│ [ Đang học • Tiến độ: 20.0% ]                         [ ⚙️ Xem / Đổi lịch học ] │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│ ▼ Giai đoạn 1: Chuẩn hóa Ngữ âm & Ngữ pháp Nền tảng (2 tuần)                   │
│                                                                                │
│   ▼ Tuần 1: Chuẩn hóa Ngữ âm IPA Anh-Anh cốt lõi                 (5/6 buổi)    │
│     Mục tiêu: Nắm vững khẩu hình /iː/, /ɪ/, /p/, /b/, Schwa /ə/ và trọng âm từ  │
│                                                                                │
│     ├── [✓ ĐÃ HỌC] Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/          (25 phút)    │
│     ├── [● ĐANG HỌC] Buổi 2: Cặp phụ âm /p/ và /b/                (20 phút)    │
│     ├── [  CHƯA HỌC] Buổi 3: Quy tắc Trọng âm từ 2 âm tiết        (25 phút)    │
│     ├── [  CHƯA HỌC] Buổi 4: Âm lướt Schwa /ə/                    (20 phút)    │
│     ├── [  CHƯA HỌC] Buổi 5: Đánh giá & Tổng hợp tuần 1           (30 phút)    │
│     └── [ TÙY CHỌN ] Buổi 6: Hiện tượng Non-Rhoticity             (15 phút)    │
│                                                                                │
│   ▶ Tuần 2: Ngữ pháp cơ bản Hiện tại đơn & Shadowing             (0/5 buổi)    │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Màn hình Thiết lập Lịch & Xem trước Đổi lịch (Reschedule Screen)

- **URL:** `/enrollments/:enrollmentId/reschedule`
- **Mục tiêu:** Giúp người học điều chỉnh lại lịch học khi bị gián đoạn, xem trước chính xác ngày kết thúc mới trước khi xác nhận.

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│ ĐỔI LỊCH HỌC CÁ NHÂN                                                           │
│ Bạn có thể điều chỉnh lại ngày bắt đầu tiếp tục và các thứ trong tuần.         │
│ Dữ liệu các bài học đã hoàn thành sẽ được giữ nguyên 100%.                     │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│ 1. Ngày tiếp tục học lại:                                                      │
│    [ 2026-11-02 📅 ]                                                           │
│                                                                                │
│ 2. Các ngày bạn muốn học trong tuần:                                           │
│    [✓] Thứ Hai   [ ] Thứ Ba   [✓] Thứ Tư   [ ] Thứ Năm   [✓] Thứ Sáu   [ ] T7/CN│
│                                                                                │
│ 3. Mục tiêu học tập mỗi ngày:                                                  │
│    [ 30 phút/ngày ▾ ]                                                          │
│                                                                                │
│                             [ Xem trước lịch mới ]                             │
│                                                                                │
├────────────────────────────────────────────────────────────────────────────────┤
│ 📋 KẾT QUẢ XEM TRƯỚC (PREVIEW)                                                 │
│                                                                                │
│ • Các bài đã hoàn thành: Giữ nguyên lịch sử (Buổi 1 ngày 26/10).               │
│ • Buổi 2 (Đang học dở)   : Dời sang ➔ Thứ Hai, 02/11/2026                      │
│ • Buổi 3 (Trọng âm từ)   : Dời sang ➔ Thứ Tư, 04/11/2026                      │
│ • Buổi 4 (Âm Schwa)      : Dời sang ➔ Thứ Sáu, 06/11/2026                     │
│ • Buổi 5 (Tổng hợp tuần) : Dời sang ➔ Thứ Hai, 09/11/2026                      │
│                                                                                │
│ 🏁 Ngày dự kiến hoàn thành mới: 09/11/2026 (Kéo dài thêm 5 ngày so với lịch cũ) │
│                                                                                │
│           [ Hủy bỏ ]                       [ Xác nhận & Lưu lịch mới ]         │
└────────────────────────────────────────────────────────────────────────────────┘
```

- **Quy tắc kiểm soát xung đột (Optimistic Version Check):**
  * Nút `"Xác nhận & Lưu lịch mới"` gửi kèm `expectedVersion` nhận được từ bước Preview.
  * Nếu có xung đột phiên (ví dụ: đã hoàn thành bài ở tab khác), modal cảnh báo hiển thị: `"Lịch học đã thay đổi do có bài học mới được hoàn thành. Đang tải lại bản xem trước..."`.

---

## 7. Thiết kế Thích ứng (Responsive Breakpoints & Accessibility)

- **Điểm ngắt giao diện (Breakpoints):**
  * `sm` (640px): Điện thoại màn hình vừa/lớn. Bố cục 1 cột hoàn toàn.
  * `md` (768px): Tablet dọc. Sidebar thu gọn dạng drawer.
  * `lg` (1024px): Desktop tiêu chuẩn. Sidebar cố định 240px, khu vực học mở rộng.
  * `xl` (1280px): Màn hình lớn. Khóa max-width ở `1152px` (`max-w-6xl`) căn giữa màn hình để mắt người học không bị mỏi khi đọc dòng chữ quá dài.
- **Tiêu chuẩn Tiếp cận (Accessibility - a11y):**
  * Tương phản màu chữ tối thiểu `4.5:1` đối với văn bản thông thường và `3:1` cho tiêu đề lớn (chuẩn WCAG AA).
  * Hỗ trợ đầy đủ điều hướng bằng bàn phím (`Tab`, `Shift+Tab`, `Space`, `Enter`, `Escape`).
  * Tất cả biểu tượng trạng thái đều có chữ đi kèm (ví dụ: `[✓ Hoàn thành]`, `[● Đang học]`), không chỉ dùng màu sắc xanh/đỏ đơn thuần.
  * Font chữ hỗ trợ bảng ký tự quốc tế IPA đầy đủ (ưu tiên Inter, Noto Sans, font hệ thống dự phòng rõ nét).
