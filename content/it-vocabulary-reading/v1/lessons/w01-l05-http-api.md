# Buổi 5: HTTP/API: request, payload và validation

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **endpoint** | — | điểm truy cập API, thường mô tả bằng method và URL | Send the invoice to the POST /invoices endpoint.<br>→ Gửi hóa đơn tới endpoint POST /invoices. |
| **request body** | — | phần thân dữ liệu của request | The request body contains the customer identifier.<br>→ Thân request chứa định danh khách hàng. |
| **response** | /rɪˈspɒns/ | thông điệp trả lời của server | The response includes the new record identifier.<br>→ Response chứa định danh bản ghi mới. |
| **status code** | — | mã trạng thái biểu thị kết quả xử lý HTTP | Check the status code before interpreting the response body.<br>→ Kiểm tra mã trạng thái trước khi diễn giải thân response. |
| **payload** | /ˈpeɪləʊd/ | dữ liệu được mang trong thông điệp; ở đây là JSON body | The payload contains an array of line items.<br>→ Payload chứa một mảng các mục hàng. |
| **validate** | /ˈvælɪdeɪt/ | kiểm tra dữ liệu theo các ràng buộc | Validate the amount before saving the invoice.<br>→ Kiểm tra số tiền theo ràng buộc trước khi lưu hóa đơn. |
| **deserialize** | — | chuyển dữ liệu tuần tự hóa thành cấu trúc/object | Deserialize the JSON body into an input object.<br>→ Chuyển JSON body thành đối tượng đầu vào. |
| **authentication** | /ɔːˌθentɪˈkeɪʃən/ | xác thực danh tính; khác kiểm quyền | Authentication establishes who the caller is.<br>→ Xác thực danh tính xác định người gọi là ai. |

**Phân biệt để đọc đúng:** Deserialize không đồng nghĩa validate: JSON đúng cú pháp vẫn có thể sai ràng buộc. Authentication là xác thực danh tính, authorization là kiểm quyền. Mã lỗi và thứ tự trong đoạn là hợp đồng API mẫu, không phải mọi API đều giống hệt.

## 3. Nhớ lại chủ động — 5 phút

Che nghĩa: diễn giải deserialize, validate, payload, authentication; đặt câu phân biệt request body với response. Ôn required và optional từ bài 2.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

The fictional Atlas Invoice API exposes the POST /invoices endpoint. The client sends a JSON request body, called the payload in this guide. Authentication runs before body processing. After successful authentication, the server deserializes the JSON into an input object and validates its fields. The amount must be positive and the currency field is required. Invalid JSON receives a 400 response. A well-formed payload that fails these validation rules also receives 400. A request without valid credentials receives 401 before an invoice is saved. A successful creation returns status code 201 and a response body containing the invoice identifier. Authentication identifies the caller; a separate permission check decides whether that caller may create invoices. Being authenticated alone is therefore not a promise of success.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.

Tham khảo tùy chọn: [MDN — Overview of HTTP](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Overview), mở đối chiếu khái niệm request/response ngày 04/11/2026. Chỉ dẫn liên kết, không tái bản nguồn.
