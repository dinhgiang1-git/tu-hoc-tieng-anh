# ADR-002: Mô hình Học tập, Trạng thái và Chính sách Tính Tiến độ

**Ngày:** 24/10/2026  
**Trạng thái:** Accepted (Đã duyệt)  
**Người quyết định:** AI Specialist & Project Coordinator  
**Tài liệu liên quan:** [product-rules.md](../product-rules.md), [KE_HOACH_DU_AN.md](../../KE_HOACH_DU_AN.md)

---

## 1. Bối cảnh (Context)
Nguồn tham khảo (Google Sheet) có cấu trúc 3 giai đoạn và 26 tuần. Tuy nhiên, nếu hardcode cố định "Lộ trình A", "3 giai đoạn", hay "26 tuần", hệ thống sẽ mất khả năng mở rộng sang các lộ trình thử nghiệm ngắn (1–2 tuần) hoặc các lộ trình chuyên sâu khác.
Ngoài ra, cần một định nghĩa sư phạm và toán học chính xác về "Tiến độ học tập", "Buổi học hoàn thành", và cơ chế xử lý khi người học muốn hủy (Undo) một nhiệm vụ đã hoàn thành trước đó.

## 2. Quyết định (Decision)

### 2.1. Mô hình Phân cấp Thực thể (Entity Hierarchy)
Tách bạch tuyệt đối giữa Nội dung giáo trình và Tiến độ cá nhân:
- `Curriculum` $\rightarrow$ `CurriculumRevision` (`DRAFT` | `PUBLISHED` | `ARCHIVED`) $\rightarrow$ `Phase` $\rightarrow$ `CurriculumWeek` $\rightarrow$ `Lesson` $\rightarrow$ `Activity` & `Resource`.
- Người học luôn gắn với một `CurriculumRevision` cố định. Khi có phiên bản mới (`v2`), những người đang học `v1` không bị thay đổi nội dung hay mẫu số tiến độ.

### 2.2. Quy tắc Nhiệm vụ Bắt buộc Hiệu lực và Công thức Tiến độ
- Một nhiệm vụ chỉ được tính vào mẫu số tiến độ lộ trình khi và chỉ khi:
  $$\text{isEffectivelyRequired} = \text{lesson.required} \land \text{activity.required}$$
- Nếu `lesson.required == false` (Buổi học tùy chọn / bài tập mở rộng): Toàn bộ các nhiệm vụ con của buổi này **không** được tính vào mẫu số lộ trình.
- Phần trăm tiến độ lộ trình do **Server tính toán duy nhất**:
  $$\text{ProgressPercent} = \frac{\sum \text{Completed Effectively Required Activities}}{\sum \text{Total Effectively Required Activities}} \times 100$$
- Nếu tổng số nhiệm vụ bắt buộc bằng 0: Trả về `null`. Frontend hiển thị `"Chưa có nội dung bắt buộc"` (không hiển thị 0% hay 100%).

### 2.3. Vòng đời Enrollment và Ràng buộc Trạng thái
- Trạng thái: `ACTIVE`, `PAUSED`, `COMPLETED`, `ABANDONED`.
- Ràng buộc: Một người học chỉ được phép có tối đa một enrollment ở trạng thái `ACTIVE` hoặc `PAUSED` cho cùng một `curriculum_id` (áp dụng PostgreSQL Partial Unique Index).
- **Quy tắc Undo:**
  * Cho phép người học bỏ tích hoàn thành cho các nhiệm vụ xác nhận thủ công (`READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`).
  * Không cho phép undo kết quả `QUIZ` tự chấm (do Assessment service quản lý theo điểm số).
  * Nếu undo nhiệm vụ trong một enrollment đã `COMPLETED`: Tự động mở lại `ACTIVE` chỉ khi người học chưa bắt đầu một enrollment `ACTIVE/PAUSED` mới nào khác cho lộ trình đó. Nếu đã có enrollment mới, từ chối mutation với mã lỗi 409 `STATE_CONFLICT`.

### 2.4. Hoàn thành Nhiệm vụ Phản tư (Reflection Submission Policy - xử lý R7)
- Nhiệm vụ `REFLECTION` tách biệt giữa lưu nháp (`PUT /response`, không đổi trạng thái activity) và nộp hoàn thành (`POST /response/submit`, yêu cầu text không rỗng và `expectedVersion`).
- Thao tác nộp lưu response và chuyển activity sang `COMPLETED` nguyên tử trong một transaction, tăng `enrollmentVersion`.
- Checklist `PUT /progress` trực tiếp trên `REFLECTION` bị từ chối 400 `RESPONSE_REQUIRED` nếu chưa có nội dung.

### 2.5. Phạm vi Lộ trình Thử nghiệm (Pilot Scope - xử lý R3)
- Lộ trình thử nghiệm `pilot-curriculum/v1` được phát hành với **1 tuần hoàn chỉnh (Tuần 1: 6 buổi, 21 nhiệm vụ, 19 nhiệm vụ bắt buộc hiệu lực)**, đảm bảo toàn bộ tệp Markdown tồn tại trên đĩa và gói hợp lệ 100% để publish.
- Tuần 2 được lưu tại khu vực phác thảo riêng (`content/pilot-curriculum/drafts/week-02-sketch.json`), không nằm trong manifest nhập chính thức của v1.0.0.

## 3. Hệ quả (Consequences)
- **Tích cực:**
  * Đảm bảo tính toán tiến độ chính xác tuyệt đối, không bị lệch khi thêm các bài học tùy chọn hoặc bài đọc mở rộng.
  * Hỗ trợ đa lộ trình (Curriculum Pilot 1 tuần và Curriculum A 26 tuần đều chạy chung 1 kiến trúc).
  * Ngăn chặn tình trạng người dùng có nhiều phiên học dở dang gây xung đột dữ liệu.
  * Đảm bảo gói giáo trình publishable không có liên kết tệp gãy.
- **Thách thức:**
  * Logic chuyển đổi trạng thái giữa `COMPLETED` và `ACTIVE` khi undo cần kiểm tra giao dịch (transactional integrity) cẩn trọng.
