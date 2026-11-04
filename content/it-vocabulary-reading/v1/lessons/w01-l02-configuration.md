# Buổi 2: Cấu hình: property, default và override

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **property** | /ˈprɒpəti/ | mục cấu hình có tên và giá trị | The timeout property controls how long the client waits.<br>→ Mục cấu hình timeout điều khiển thời gian client chờ. |
| **default value** | — | giá trị dùng khi không có giá trị khác theo quy tắc được nêu | The documented default value is five seconds.<br>→ Giá trị mặc định được tài liệu nêu là năm giây. |
| **override** | /ˌəʊvəˈraɪd/ | ghi đè giá trị khác theo quy tắc ưu tiên | A command-line setting can override the file value.<br>→ Thiết lập dòng lệnh có thể ghi đè giá trị trong tệp. |
| **omit** | /əʊˈmɪt/ | không đưa vào/không cung cấp; hậu quả tùy quy tắc | Do not omit a required connection string.<br>→ Đừng bỏ không cung cấp chuỗi kết nối bắt buộc. |
| **enable** | /ɪˈneɪbəl/ | bật/cho phép chức năng | Enable request logging in the test environment.<br>→ Bật ghi log request trong môi trường kiểm thử. |
| **disable** | /dɪˈseɪbəl/ | tắt/ngăn chức năng | Disable automatic retries for this operation.<br>→ Tắt tự thử lại đối với thao tác này. |
| **required** | /rɪˈkwaɪəd/ | bắt buộc theo hợp đồng đang xét | The tenant identifier is required for this endpoint.<br>→ Định danh tenant là bắt buộc với endpoint này. |
| **optional** | /ˈɒpʃənəl/ | không bắt buộc cung cấp; không hàm ý default cụ thể | The diagnostic label is optional.<br>→ Nhãn chẩn đoán là tùy chọn. |

**Phân biệt để đọc đúng:** Omit chỉ là không cung cấp: không tự suy ra default, null hay an toàn. Override ở bài này là ghi đè cấu hình, khác override a method. Optional không đồng nghĩa nullable.

## 3. Nhớ lại chủ động — 5 phút

Che bảng: nêu nghĩa omit, override, default value; so sánh required với optional bằng ví dụ khác. Ôn dependency và runtime của bài 1.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

The fictional Atlas client reads three properties: serverUrl, timeoutMs and requestLog. serverUrl is required. If you omit it, startup fails with a configuration error. timeoutMs is optional and has a documented default value of 3000. A command-line value can override the value of timeoutMs in the configuration file. For example, if the file contains 5000 and the command line supplies 1000, the effective timeout is 1000 milliseconds. requestLog is disabled by default. Set it to true to enable request logging, or false to disable it. An optional property does not necessarily accept null or an empty string. This client rejects those values for timeoutMs. Omitting a property and supplying an invalid value are different operations.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
