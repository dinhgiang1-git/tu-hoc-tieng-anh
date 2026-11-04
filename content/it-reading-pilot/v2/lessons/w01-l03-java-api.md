# Buổi 3: Java/API: đầu vào, đầu ra và đối tượng

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **instance** | /ˈɪnstəns/ | đối tượng cụ thể được tạo từ lớp | Create one parser instance for this import job.<br>→ Tạo một đối tượng parser cho tác vụ nhập này. |
| **reference** | /ˈrefərəns/ | giá trị tham chiếu tới object, không phải bản sao object | Both variables hold a reference to the same object.<br>→ Hai biến giữ tham chiếu tới cùng một object. |
| **constructor** | — | hàm tạo dùng khi tạo đối tượng | The constructor receives a clock dependency.<br>→ Hàm tạo nhận dependency clock. |
| **method** | /ˈmeθəd/ | phương thức được khai báo trên kiểu | Call the parse method with the source text.<br>→ Gọi phương thức parse với văn bản nguồn. |
| **parameter** | /pəˈræmɪtə/ | tham số trong khai báo phương thức | The limit parameter must be positive.<br>→ Tham số limit phải dương. |
| **argument** | /ˈɑːɡjəmənt/ | giá trị truyền vào ở lần gọi cụ thể | In parse(text), text is the argument.<br>→ Trong parse(text), text là đối số được truyền. |
| **return value** | — | giá trị trả về từ lời gọi | The return value contains the parsed records.<br>→ Giá trị trả về chứa các bản ghi đã phân tích. |
| **immutable** | /ɪˈmjuːtəbəl/ | không thay đổi trạng thái sau khi tạo theo hợp đồng kiểu | This immutable result cannot be edited after creation.<br>→ Không thể sửa kết quả bất biến này sau khi tạo. |

**Phân biệt để đọc đúng:** Parameter ở khai báo; argument ở lời gọi. Reference không tự là deep copy. Không suy ra immutable chỉ vì dùng constructor injection; phải xét hợp đồng và cách bảo vệ dữ liệu.

## 3. Nhớ lại chủ động — 5 phút

Nhớ lại parameter, argument, reference, immutable; tự viết chữ ký method và lời gọi rồi chỉ rõ parameter/argument. Ôn override cấu hình: khác override method.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

Consider the fictional Java class ReportParser. Its constructor takes a FormatRegistry dependency and creates a parser instance. The method parse(String source, int limit) has two parameters. In the call parser.parse(rawText, 20), rawText and 20 are arguments supplied to those parameters. The method returns a ReportResult. This return value contains at most 20 records in this call. ReportResult is immutable by design: its public API does not allow callers to change its records, and its implementation protects the contained data from external modification. Assigning the result to another variable copies a reference, not the object itself. Both variables can refer to the same result. Constructor injection alone does not make the parser or its dependencies immutable.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
