# Buổi 7: React: component, state và luồng dữ liệu

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **component** | /kəmˈpəʊnənt/ | thành phần UI; trong React có thể là hàm trả JSX | The dashboard contains a filter component.<br>→ Dashboard chứa một component lọc. |
| **props** | /prɒps/ | dữ liệu đầu vào component nhận từ cha | The parent passes the product list through props.<br>→ Component cha truyền danh sách sản phẩm qua props. |
| **state** | /steɪt/ | dữ liệu component lưu qua các lần render | The selected category is stored in state.<br>→ Danh mục được chọn được lưu trong state. |
| **render** | /ˈrendə/ | tính mô tả UI; không đồng nghĩa luôn sửa DOM | React calls the component to render its next UI description.<br>→ React gọi component để tính mô tả UI tiếp theo. |
| **event handler** | — | hàm xử lý sự kiện | The event handler requests a state update.<br>→ Hàm xử lý sự kiện yêu cầu cập nhật state. |
| **derived value** | — | giá trị tính từ dữ liệu đã có | The visible count is a derived value from the filtered list.<br>→ Số mục hiển thị là giá trị tính từ danh sách đã lọc. |
| **source of truth** | — | nơi giữ dữ liệu chuẩn cho một thông tin | The parent state is the source of truth for the selected category.<br>→ State ở cha là nguồn dữ liệu chuẩn của danh mục được chọn. |
| **immutable update** | — | cập nhật bằng giá trị/object mới thay vì sửa dữ liệu hiện có | Use an immutable update to replace the selected item.<br>→ Dùng cập nhật bất biến để thay mục đã chọn. |

**Phân biệt để đọc đúng:** Props là đầu vào; state là dữ liệu component giữ. Derived value không tự cần state riêng. Render không đồng nghĩa ghi lại toàn bộ DOM. Ví dụ không phải quy tắc “không bao giờ dùng state thứ hai”.

## 3. Nhớ lại chủ động — 5 phút

Che bảng: mô tả props, state, event handler, derived value; vẽ mũi tên parent → FilterBar và handler → state. Ôn immutable/reference bài 3.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

The fictional ProductPanel component receives products through props. It stores only the selected category in state. The parent panel is the source of truth for that category and passes it to FilterBar. When the user selects a category, an event handler requests a state update. During render, ProductPanel calculates the visible products and their count. The count is a derived value, so the example does not store a second count state that could become inconsistent. FilterBar does not mutate the products prop. A separate ProductEditor component keeps an editable copy of products in its own state. When that editor changes a product, it uses an immutable update: a new array and a new object for the changed product. Rendering calculates a UI description; it does not mean every DOM node must change.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
