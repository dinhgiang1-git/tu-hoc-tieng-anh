# Buổi 6: Database: persist, flush, commit và rollback

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Trước khi mở bảng, nhớ lại 3 cụm buổi trước và viết nghĩa theo đúng ngữ cảnh.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **transaction** | /trænˈzækʃən/ | đơn vị xử lý dữ liệu có ranh giới bắt đầu/kết thúc | The import runs inside a transaction.<br>→ Quá trình nhập chạy trong một transaction. |
| **persist** | /pəˈsɪst/ | lưu bền/làm object được quản lý để lưu tùy API; chưa mặc định commit | The service asks the persistence layer to persist the new order.<br>→ Service yêu cầu tầng lưu trữ lưu đơn hàng mới. |
| **fetch** | /fetʃ/ | lấy dữ liệu từ nguồn | Fetch the current order before applying the update.<br>→ Lấy đơn hàng hiện tại trước khi áp dụng cập nhật. |
| **flush** | /flʌʃ/ | đồng bộ thay đổi đang chờ xuống DB; chưa đồng nghĩa commit | Flush pending changes before executing this query.<br>→ Đồng bộ thay đổi đang chờ xuống DB trước khi chạy query này. |
| **commit** | /kəˈmɪt/ | xác nhận hoàn tất transaction theo bảo đảm DB đang dùng | Commit the transaction after both updates succeed.<br>→ Commit transaction sau khi cả hai cập nhật thành công. |
| **roll back** | /rəʊl bæk/ | hoàn tác thay đổi transaction trong phạm vi DB quản lý | Roll back the transaction when the transfer fails.<br>→ Rollback transaction khi chuyển khoản thất bại. |
| **constraint** | /kənˈstreɪnt/ | ràng buộc dữ liệu DB/hệ thống thực thi | A unique constraint rejects duplicate order numbers.<br>→ Ràng buộc duy nhất từ chối số đơn hàng trùng. |
| **isolation** | /ˌaɪsəˈleɪʃən/ | mức/cơ chế kiểm soát tương tác giữa transaction đồng thời | Read the isolation rules before reasoning about concurrent updates.<br>→ Đọc quy tắc isolation trước khi suy luận về cập nhật đồng thời. |

**Phân biệt để đọc đúng:** Không đánh đồng persist/flush/commit. Roll back là động từ; rollback thường là danh từ/tính từ. Đoạn không dạy mặc định @Transactional và không bảo đảm rollback hoàn tác email hay HTTP request ngoài DB.

## 3. Nhớ lại chủ động — 5 phút

Không nhìn bảng: mô tả riêng persist, flush, commit; giải thích constraint và fetch. Ôn response/status code bài 5.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

In this fictional import workflow, one database transaction covers two new order records. The service first fetches the customer and then asks the persistence layer to persist both orders. A flush sends pending changes to the database, where a unique constraint checks the order numbers. A successful flush does not mean that the transaction has committed. The workflow attempts to commit only after all checks succeed. If a duplicate order number is detected during flush, the workflow rolls back the transaction. A separate transaction cannot read these uncommitted inserts under the isolation rules assumed in this example. Those rules are stated here for the exercise; real applications must check their database and isolation level. This passage does not describe Spring's default exception-to-rollback policy.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
