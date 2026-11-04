# Buổi 9: Debug và test: root cause và kết quả kiểm thử

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **stack trace** | — | dấu vết chuỗi lời gọi liên quan lỗi | Read the stack trace to locate the failing call.<br>→ Đọc stack trace để định vị lời gọi lỗi. |
| **root cause** | — | nguyên nhân gốc, khác triệu chứng bề mặt | The root cause was an invalid database address.<br>→ Nguyên nhân gốc là địa chỉ DB không hợp lệ. |
| **reproduce** | /ˌriːprəˈdjuːs/ | tái hiện lỗi bằng điều kiện/bước cụ thể | Reproduce the failure with a small input file.<br>→ Tái hiện lỗi bằng tệp đầu vào nhỏ. |
| **assertion** | /əˈsɜːʃən/ | phép kiểm tra kỳ vọng trong test | The assertion checks that the saved count equals two.<br>→ Assertion kiểm số lượng đã lưu bằng hai. |
| **fixture** | /ˈfɪkstʃə/ | dữ liệu/trạng thái chuẩn bị để chạy test | The fixture contains two active customers.<br>→ Dữ liệu chuẩn bị test chứa hai khách hàng đang hoạt động. |
| **mock** | /mɒk/ | đối tượng thay thế được lập trình hành vi cho test | The unit test uses a mock mail sender.<br>→ Unit test dùng đối tượng thay thế cho bộ gửi thư. |
| **integration test** | — | test kiểm tương tác giữa các thành phần trong phạm vi xác định | The integration test uses a real test database.<br>→ Test tích hợp dùng cơ sở dữ liệu kiểm thử thật. |
| **log level** | — | mức ghi log như DEBUG, INFO, WARN, ERROR | Use the DEBUG log level for this local investigation.<br>→ Dùng mức DEBUG cho lần điều tra cục bộ này. |

**Phân biệt để đọc đúng:** Mock test có giá trị trong phạm vi của nó; không dùng mock để khẳng định kết nối DB thật hoạt động. Root cause cần bằng chứng. Không suy từ stack trace đơn lẻ rằng đã xác định mọi nguyên nhân.

## 3. Nhớ lại chủ động — 5 phút

Che bảng: định nghĩa fixture, assertion, mock, reproduce bằng ví dụ khác; ôn constraint và transaction bài 6. Viết hai bước điều tra trước khi sửa.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

A fictional Atlas integration test fails while importing two records. Its fixture creates an empty database and supplies a file with two different order numbers. An assertion expects a saved count of two, but the actual count is zero. The developer first reproduces the failure with the same fixture. The stack trace points to a connection attempt. At DEBUG log level, a local diagnostic message shows that the configured database address has a misspelled host name. That address is the root cause in this investigation; the failed count assertion is a symptom. A unit test using a mock repository had passed because it never opened a database connection. Fixing the address and rerunning the relevant integration test checks the real interaction. Passing the mock-based unit test alone would not establish that the database connection works.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
