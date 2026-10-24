# ADR-005: Quản lý Phiên bản Gói Nội dung, Phân tách Schema và Cơ chế Xác thực Dữ liệu (Content Package Versioning)

**Ngày:** 24/10/2026 (Cập nhật sau G0-FIX)  
**Trạng thái:** Accepted (Đã duyệt)  
**Người quyết định:** AI Specialist & Project Coordinator  
**Tài liệu liên quan:** [content-mapping.md](../content-mapping.md), [manifest.schema.json](../../content/schemas/manifest.schema.json), [week.schema.json](../../content/schemas/week.schema.json), [resources.schema.json](../../content/schemas/resources.schema.json), [curriculum.schema.json](../../content/schemas/curriculum.schema.json)

---

## 1. Bối cảnh (Context)
Trong giai đoạn đầu (v1), hệ thống chưa có giao diện quản trị viên (Admin CMS). Tuy nhiên, nội dung bài học không được đổ cứng vào mã nguồn Java hay các file migration SQL của cơ sở dữ liệu vì sẽ làm phình to migration và rất khó chỉnh sửa sư phạm.
Đồng thời, nguồn tham khảo bên ngoài có nhiều liên kết chưa được kiểm chứng bản quyền và độ ổn định. Cần một cơ chế lưu trữ nội dung dưới dạng Git-friendly, kiểm tra tự động trước khi nhập vào cơ sở dữ liệu, và quản lý phiên bản bất biến khi đã xuất bản.

Trong đợt rà soát G0, phát hiện:
- `manifest.json` tham chiếu tuần qua danh sách đối tượng `{ key, path }`, trong khi schema ban đầu mong đợi danh sách object tuần nhúng đầy đủ (inlined), gây lỗi khi validate độc lập.
- Gói `v1` chứa `week-02.json` ở dạng phác thảo dang dở (chưa đủ 6 buổi và thiếu file markdown), vi phạm nguyên tắc "gói xuất bản phải đầy đủ 100%".
- Dữ liệu tham khảo chưa tách bạch rõ ràng giữa nguồn gốc thực tế (`SOURCE_OBSERVED`) và nội dung tác giả tự biên soạn (`AUTHOR_COMPILED`).

## 2. Quyết định (Decision)

### 2.1. Cấu trúc Gói Nội dung và Phân tách Schema (Multi-Schema Architecture)
Để hỗ trợ validate độc lập từng file mà không bắt buộc phải gom thành một file JSON duy nhất, hệ thống tách thành 3 schema chuyên biệt và 1 schema tổng hợp:
```text
content/
  schemas/
    manifest.schema.json        # Xác thực file manifest.json (metadata, phases, danh sách week pointer {key, path})
    week.schema.json            # Xác thực từng file week-*.json (danh sách lessons, activities, passingPolicy)
    resources.schema.json       # Xác thực resources.json (từ điển liên kết ngoài, license, verificationStatus)
    curriculum.schema.json      # Schema tổng hợp cho representation đã bundle
  pilot-curriculum/             # Thư mục cho một lộ trình cụ thể
    v1/                         # Phiên bản revision chính thức (100% hoàn chỉnh)
      manifest.json             # Khai báo metadata, phases, tham chiếu week-01.json
      resources.json            # Thư viện liên kết ngoài
      weeks/                    # Cấu trúc từng tuần chính thức
        week-01.json            # Tuần 1: 6 lessons, 21 activities, 19 required
      lessons/                  # Nội dung bài học dạng Markdown
        w01-l01-vowels.md
        ...
        w01-l06-review.md
    drafts/                     # Thư mục chứa tuần/bài học nháp chưa đưa vào manifest
      week-02-sketch.json       # Phác thảo tuần 2, tách riêng khỏi v1
```

### 2.2. Nguyên tắc Xuất bản và Phân tách Nháp (Draft Separation)
- Thư mục phiên bản (ví dụ `v1/`) **chỉ chứa nội dung hoàn chỉnh 100%**: Tất cả file markdown tham chiếu phải tồn tại, tất cả `resourceKey` phải phân giải được trong `resources.json`, tất cả `key` phải duy nhất.
- Mọi tuần hoặc bài học đang trong quá trình biên soạn hoặc mới chỉ là phác thảo (sketch) bắt buộc phải lưu trong thư mục `drafts/` ngoài phiên bản chính thức và không được khai báo trong `manifest.json`.

### 2.3. Quy trình 4 bước Nhập và Xuất bản Nội dung (Content Pipeline)
Xây dựng công cụ CLI backend tại G2 (`CurriculumImportCli`) thực thi quy trình nghiêm ngặt:
1. **Validate:**
   - Dùng `manifest.schema.json` kiểm tra `manifest.json`.
   - Dùng `resources.schema.json` kiểm tra `resources.json`.
   - Lần lượt dùng `week.schema.json` kiểm tra từng file trong `weeks/`.
   - Kiểm tra toàn bộ đường dẫn markdown `contentPath` thực sự tồn tại trên ổ đĩa.
   - Kiểm tra toàn vẹn `resourceKey` và tính duy nhất của mã khóa `key`.
2. **Dry-run:** Giả lập quá trình đọc và phân tích cấu trúc, báo cáo số lượng giai đoạn, tuần, buổi, nhiệm vụ bắt buộc/tùy chọn, và cảnh báo các liên kết ngoài mang trạng thái `UNVERIFIED_DRAFT`.
3. **Import (Draft):** Nhập dữ liệu vào các bảng quan hệ trong một transaction duy nhất. Tạo một bản ghi `CurriculumRevision` ở trạng thái `DRAFT`. Nếu có bất kỳ lỗi nào, rollback 100%.
4. **Publish:** Chuyển trạng thái từ `DRAFT` sang `PUBLISHED` sau khi kiểm tra preview. **Một khi đã PUBLISHED, revision trở nên bất biến (immutable).**

### 2.4. Phân định Nguồn gốc và Trạng thái Xác minh
Mọi nội dung đều được phân định minh bạch:
- `SOURCE_OBSERVED`: Tiêu đề, mục tiêu, lộ trình rút ra trực tiếp từ tài liệu gốc.
- `AUTHOR_COMPILED`: Khung nội dung, lý thuyết, bài tập trắc nghiệm, phản hồi tự do do AI/tác giả biên soạn.
- `PROPOSED_PILOT`: Gói giáo trình thử nghiệm độc lập 1 tuần (`pilot-curriculum-v1`).
- `verificationStatus` của tài nguyên liên kết:
  - `VERIFIED`: Đã kiểm tra trực tiếp URL, bản quyền, tính nhúng và tính khả dụng.
  - `UNVERIFIED_DRAFT`: URL lấy từ tài liệu tham khảo nhưng chưa thực hiện kiểm tra bản quyền hay khả năng nhúng an toàn trong production.
- **Tiêu chuẩn sư phạm:** Tuyệt đối không đưa số liệu thống kê vô căn cứ (ví dụ "chiếm 85% trường hợp") hoặc quy định thời lượng mili-giây cứng nhắc khi không có nghiên cứu ngữ âm học đối chứng rõ ràng.

## 3. Hệ quả (Consequences)
- **Tích cực:**
  * Schema rõ ràng, có thể validate độc lập từng file bằng CLI hoặc công cụ CI/CD.
  * Không bao giờ xảy ra tình huống manifest trỏ tới tuần dở dang làm vỡ runtime của người học.
  * Minh bạch về bản quyền và trách nhiệm xác thực đường dẫn ngoài.
- **Thách thức:**
  * Cần duy trì tính đồng bộ giữa 3 schema khi có sự thay đổi cấu trúc dữ liệu.

