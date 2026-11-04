# Buổi 4: Spring vocabulary: bean, container và lifecycle

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **bean** | /biːn/ | đối tượng được container Spring quản lý trong ngữ cảnh Spring | The application registers the mail sender as a bean.<br>→ Ứng dụng đăng ký bộ gửi thư làm bean. |
| **container** | /kənˈteɪnə/ | bộ quản lý object; ở đây không phải Docker container | The container manages the registered objects.<br>→ Container quản lý các object đã đăng ký. |
| **inject** | /ɪnˈdʒekt/ | cung cấp dependency từ bên ngoài object | Inject a clock into the report service.<br>→ Cung cấp clock từ bên ngoài cho report service. |
| **scope** | /skəʊp/ | phạm vi/quy tắc quản lý instance của bean | Read the scope before assuming that requests share an instance.<br>→ Đọc scope trước khi cho rằng các request dùng chung instance. |
| **lifecycle** | — | vòng đời từ tạo, khởi tạo tới kết thúc | The guide describes the lifecycle of the worker.<br>→ Hướng dẫn mô tả vòng đời worker. |
| **instantiate** | /ɪnˈstænʃieɪt/ | tạo một instance | The factory instantiates a formatter.<br>→ Factory tạo một đối tượng formatter. |
| **resolve a dependency** | — | tìm/chọn dependency phù hợp để đáp ứng yêu cầu | Resolve a dependency before calling this constructor.<br>→ Tìm dependency phù hợp trước khi gọi constructor này. |
| **dependency injection** | — | cách object nhận dependency từ bên ngoài | Dependency injection separates object creation from this service's work.<br>→ Dependency injection tách việc tạo object khỏi công việc của service này. |

**Phân biệt để đọc đúng:** Container có nhiều nghĩa: ở đây là bộ quản lý object, không phải Docker. Tên scope application và trình tự callback trong đoạn là quy ước mô phỏng, không thay tài liệu Spring thực. Không suy thread-safe từ shared bean.

## 3. Nhớ lại chủ động — 5 phút

Nhớ lại instantiate, inject, resolve a dependency và lifecycle bằng 4 câu ngắn. Ôn instance và constructor từ bài 3.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

This teaching example uses Spring vocabulary, but the rules below describe a fictional training container, not every Spring configuration. A bean is an object managed by this container. The ReportService constructor declares a Clock dependency. To instantiate ReportService, the container must first resolve that dependency and then inject the selected Clock through the constructor. The example registers exactly one Clock candidate. A missing candidate causes startup to fail. The ReportService bean has application scope in this example: one instance is shared for the lifetime of the training application. Its lifecycle includes creation, an explicit start callback and a stop callback. Dependency injection describes how collaborators are supplied. It does not guarantee that a shared bean is thread-safe or immutable. Those properties depend on the implementation.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
