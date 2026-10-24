# ADR-004: Kiến trúc Luyện Phát âm Anh-Anh Tự chạy (Offline / Local Speech Engine)

**Ngày:** 24/10/2026  
**Trạng thái:** Accepted (Đã duyệt)  
**Người quyết định:** AI Specialist & Project Coordinator  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../../KE_HOACH_DU_AN.md) (Mục 17), [BE_CODE_CONVENTION.md](../../BE_CODE_CONVENTION.md)

---

## 1. Bối cảnh (Context)
Người dùng yêu cầu học phát âm theo chuẩn **tiếng Anh-Anh (`en-GB` / British English / Received Pronunciation)** và chỉ định: **ưu tiên sử dụng mã nguồn mở tự chạy, không mất phí API cloud (như Azure hay Google Cloud Speech)**.
Tuy nhiên:
- Các mô hình ASR mã nguồn mở phần lớn được huấn luyện theo giọng Anh-Mỹ (`en-US`).
- Việc chạy mô hình Deep Learning nhận dạng ngữ âm đòi hỏi tài nguyên máy tính (CPU/RAM) và có thể gây độ trễ.
- Độ chính xác của các thuật toán chấm điểm phát âm tự động dựa trên khoảng cách ngữ âm (edit distance) chỉ mang tính ước lượng (heuristic), đặc biệt đối với người Việt học tiếng Anh.

## 2. Quyết định (Decision)

### 2.1. Phân định Rõ ràng: Không chặn Vòng học Cốt lõi
- **Không đưa tính năng chấm điểm phát âm vào điều kiện hoàn thành bài học (DoD) của các giai đoạn G0–G3.**
- Vòng học cơ bản ở các giai đoạn đầu hỗ trợ người học nghe mẫu Anh-Anh chuẩn, thu âm lại giọng của mình để tự nghe lại (tự đối chiếu), và tự xác nhận hoàn thành (`PRACTICE` activity).
- Gói PoC phát âm nâng cao (PA-01 đến PA-04) được tách thành luồng việc thử nghiệm độc lập sau G3.

### 2.2. Lựa chọn Mô hình và Công nghệ PoC
- **Mô hình nhận dạng âm:** `facebook/wav2vec2-xlsr-53-espeak-cv-ft` (Apache-2.0 License).
- **Âm tham chiếu Anh-Anh:** eSpeak-NG với profile `en-gb` và bảng từ điển ngữ âm ngoại lệ đã được duyệt.
- **Kiến trúc Worker:** Python + FastAPI đóng gói trong Docker (chạy trên WSL), chỉ phục vụ nội bộ backend Spring Boot qua HTTP port riêng, không mở trực tiếp ra trình duyệt.
- **Cơ chế gọi:** Spring Boot tạo attempt trong DB $\rightarrow$ gọi worker Python bất đồng bộ ngoài transaction $\rightarrow$ ghi nhận kết quả và version mô hình vào DB $\rightarrow$ trả về FE.

### 2.3. Trải nghiệm Người học và Dữ liệu Đánh giá
- Nhãn hiển thị trên giao diện là: **"Gợi ý luyện phát âm — Thử nghiệm" (Experimental Pronunciation Feedback)**.
- Kết quả tập trung vào việc chỉ ra âm nào trong từ cần chú ý nghe lại, không đưa ra điểm số tuyệt đối kiểu "Bạn phát âm đúng 68%" để tránh tạo ấn tượng sai lệch về năng lực người học.
- Khi audio quá nhỏ, rè hoặc lỗi đường truyền: Trả về trạng thái `"Chưa đánh giá được"` và điểm số `null`. Tuyệt đối không tự động gán điểm 0.

## 3. Hệ quả (Consequences)
- **Tích cực:**
  * Hoàn toàn miễn phí, không phát sinh chi phí API cloud hàng tháng.
  * Đảm bảo tính nhất quán của chuẩn Anh-Anh (en-GB) từ bài học đến công cụ đối chiếu.
  * Không làm gián đoạn hoặc chặn tiến độ học tập bình thường của người dùng nếu máy tính tạm thời không đủ tải chạy model AI.
- **Thách thức:**
  * Cần chuẩn bị tập dữ liệu thử nghiệm (PA-02) gồm cả giọng người Anh và người Việt để cân chỉnh ngưỡng trước khi phát hành tính năng chấm.
  * Cần quản lý vòng đời file âm thanh tạm để không làm đầy ổ đĩa.
