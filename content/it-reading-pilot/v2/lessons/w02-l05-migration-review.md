# Buổi 10: Migration notes: deprecated, breaking change và ôn tích lũy

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **deprecated** | — | không còn khuyến khích dùng; chưa có nghĩa đã xóa | The old export method is deprecated but still available.<br>→ Method export cũ không còn được khuyến khích dùng nhưng vẫn còn. |
| **removed** | /rɪˈmuːvd/ | đã loại bỏ khỏi phiên bản/phạm vi đang nói | The legacy endpoint was removed in version three.<br>→ Endpoint cũ bị loại bỏ ở phiên bản ba. |
| **breaking change** | — | thay đổi khiến cách dùng cũ có thể không tương thích | Renaming a required field is a breaking change for existing clients.<br>→ Đổi tên trường bắt buộc là thay đổi phá vỡ tương thích với client hiện có. |
| **backward-compatible** | — | tương thích ngược với cách dùng cũ trong phạm vi được nêu | This optional response field is backward-compatible with our existing client.<br>→ Trường response tùy chọn này tương thích ngược với client hiện có của chúng ta. |
| **replacement** | /rɪˈpleɪsmənt/ | API/thành phần thay thế được đề xuất | Use the documented replacement for the old method.<br>→ Dùng phương án thay thế được tài liệu chỉ rõ cho method cũ. |
| **migrate** | /maɪˈɡreɪt/ | chuyển code/dữ liệu/cấu hình sang cách hoặc phiên bản mới | Migrate the client configuration before upgrading the server.<br>→ Chuyển cấu hình client sang định dạng mới trước khi nâng server. |
| **supported** | /səˈpɔːtɪd/ | được hỗ trợ trong phiên bản/môi trường cụ thể | Only the listed runtime versions are supported.<br>→ Chỉ các phiên bản runtime được liệt kê được hỗ trợ. |
| **workaround** | — | cách xử lý tạm để tránh vấn đề, chưa chắc sửa root cause | The workaround disables the affected optimisation.<br>→ Cách xử lý tạm tắt tối ưu hóa bị ảnh hưởng. |

**Phân biệt để đọc đúng:** Deprecated khác removed. Backward-compatible phải xét phạm vi client/API cụ thể. B only if A: B đòi hỏi A; A đúng chưa đủ bảo đảm B. Workaround không tự là bản sửa root cause.

## 3. Nhớ lại chủ động — 5 phút

Che bảng: so sánh deprecated/removed và workaround/root cause. Ôn override, optional, authentication, runtime ở bài trước. Viết cảnh báo migration dùng ít nhất ba cụm.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

These are original migration notes for the fictional Atlas SDK, not a real product release. In version 2.0, sendLegacy is deprecated but still available. Its replacement is sendBatch. Version 3.0 removes sendLegacy, so clients that call it must migrate before upgrading to 3.0. Renaming the required destination property to serverUrl in 3.0 is a breaking change. timeoutMs remains optional with a default value of 3000; a command-line value overrides the file value. Omitting serverUrl causes startup failure. Java 21 is the only supported runtime for this training release. Requests are accepted only if authentication succeeds; a separate permission check must also pass. Here, "this requirement" refers to successful authentication, not to permission approval. A temporary workaround disables batching to avoid a known response-order issue. It does not repair the issue. Test the replacement in a staging environment before deploying the upgrade.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
