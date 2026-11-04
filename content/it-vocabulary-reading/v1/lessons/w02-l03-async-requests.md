# Buổi 8: Async requests: promise, retry và cleanup

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **promise** | /ˈprɒmɪs/ | đối tượng biểu diễn kết quả bất đồng bộ sẽ hoàn tất hoặc thất bại | The loader returns a promise for the response data.<br>→ Loader trả promise cho dữ liệu response. |
| **resolve a promise** | — | giải quyết Promise với giá trị hoặc trạng thái theo đối tượng được truyền | The test helper resolves the promise with a fixed result.<br>→ Hàm hỗ trợ test làm promise hoàn tất với kết quả cố định. |
| **reject** | /rɪˈdʒekt/ | làm Promise thất bại với lý do lỗi trong ngữ cảnh này | Reject the promise if the local file cannot be opened.<br>→ Cho promise thất bại nếu không mở được tệp cục bộ. |
| **await** | /əˈweɪt/ | chờ kết quả Promise trong luồng async, không khóa toàn bộ UI | Await the request before using its result.<br>→ Chờ kết quả request trước khi dùng. |
| **retry** | /ˌriːˈtraɪ/ | thực hiện lại thao tác sau lần trước | Retry this read request at most once.<br>→ Thực hiện lại request đọc này tối đa một lần. |
| **timeout** | — | giới hạn chờ hoặc sự kiện hết thời gian chờ | A timeout means the client did not receive a result in time.<br>→ Timeout nghĩa là client không nhận kết quả kịp thời. |
| **cleanup** | — | dọn dẹp/giải phóng tác vụ hoặc tài nguyên không còn cần | The cleanup step cancels the subscription.<br>→ Bước cleanup hủy đăng ký nhận dữ liệu. |
| **race condition** | — | kết quả sai phụ thuộc thứ tự/timing của tác vụ đồng thời | An older response can cause a race condition if it overwrites newer data.<br>→ Response cũ có thể gây race condition nếu ghi đè dữ liệu mới. |

**Phân biệt để đọc đúng:** Resolve Promise khác resolve dependency ở bài 4. JavaScript resolve có thể nhận Promise/thenable khác và chưa lập tức fulfilled; ví dụ này dùng kết quả dữ liệu thường. Timeout không chứng minh server chưa xử lý; retry mutation cần hợp đồng an toàn riêng.

## 3. Nhớ lại chủ động — 5 phút

Nhớ retry, timeout, cleanup, race condition; giải thích hai nghĩa của resolve ở bài 4 và bài này. Tự tạo ví dụ query A trả chậm hơn query B.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

The fictional search client starts a request whenever the query changes. Each request returns a promise. The loader resolves the promise with results on success and rejects it on a network failure. The caller uses await inside an async function to read the result. The client applies a two-second timeout, but a timeout alone does not prove that the server did no work. For this read-only search endpoint, the client may retry once after a network failure. It does not apply that retry policy to payments. When the query changes again, cleanup marks the previous request as inactive. An inactive request may still finish, but its response is ignored. This prevents an older response from overwriting the current results, one possible race condition. Cleanup in this example does not guarantee server-side cancellation.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
