# Buổi 1: README: môi trường, dependency và bản build

**Dành cho:** người có tiếng Anh cơ bản, cần từ vựng chuyên ngành để đọc tài liệu IT. **Thời lượng gợi ý:** 35 phút. Mục tiêu là hiểu và dùng 8 mục từ/cụm trong ngữ cảnh, không học lại giao tiếp hoặc ngữ pháp cơ bản.

## 1. Ôn nhanh trước khi học — 3 phút

Bài đầu: thử đoán nghĩa theo ngữ cảnh trước khi xem bảng.

## 2. Tám mục từ/cụm chuyên ngành — 12 phút

Học theo cụm và nghĩa trong câu. IPA Anh-Anh là hỗ trợ tham khảo, không phải yêu cầu để qua bài; dấu — nghĩa là chưa cung cấp phiên âm cho mục đó. Không dùng cách đọc suy đoán cho tên API.

| Từ/cụm | IPA UK | Nghĩa trong ngữ cảnh | Ví dụ tự viết và bản dịch |
|---|---|---|---|
| **dependency** | /dɪˈpendənsi/ | thành phần mà phần mềm phụ thuộc vào | The parser is a dependency of the import tool.<br>→ Bộ phân tích là một thành phần phụ thuộc của công cụ nhập dữ liệu. |
| **prerequisite** | /ˌpriːˈrekwɪzɪt/ | điều kiện cần có trước khi làm một việc | A running database is a prerequisite for this test.<br>→ DB đang chạy là điều kiện tiên quyết của test này. |
| **repository** | /rɪˈpɒzɪtəri/ | kho lưu mã hoặc gói; ở đây là kho mã nguồn | Clone the repository before editing the source files.<br>→ Sao chép kho mã nguồn về máy trước khi sửa tệp nguồn. |
| **package** | /ˈpækɪdʒ/ | gói phần mềm/thư viện được phân phối | Install the logging package from the internal registry.<br>→ Cài gói ghi log từ kho gói nội bộ. |
| **build artifact** | — | tệp đầu ra của build, như JAR hoặc bundle | Upload the build artifact after the tests pass.<br>→ Tải tệp đầu ra build lên sau khi kiểm thử đạt. |
| **runtime** | — | môi trường/giai đoạn thực thi; ở đây là môi trường chạy | The runtime must support this language version.<br>→ Môi trường chạy phải hỗ trợ phiên bản ngôn ngữ này. |
| **environment variable** | — | biến môi trường cung cấp cho tiến trình | The launcher reads the API_URL environment variable.<br>→ Trình khởi chạy đọc biến môi trường API_URL. |
| **command-line argument** | — | đối số truyền khi gọi lệnh | Pass the file path as a command-line argument.<br>→ Truyền đường dẫn tệp dưới dạng đối số dòng lệnh. |

**Phân biệt để đọc đúng:** Repository cũng có thể là kho gói hoặc lớp truy cập dữ liệu; ở đây là kho mã nguồn. Artifact là đầu ra build, không phải tên khác của source code. INPUT_DIR là quy ước riêng của Atlas.

## 3. Nhớ lại chủ động — 5 phút

Không nhìn bảng: giải thích dependency, prerequisite, build artifact; viết câu phân biệt environment variable với command-line argument.

Ghi vào hoạt động “Nhớ lại thuật ngữ”. Tự nhớ trước, sau đó mở bảng kiểm tra và ghi mục còn nhầm cùng một ví dụ của bạn. Đây là tự đối chiếu, chưa có máy chấm chất lượng câu tự viết.

## 4. Đọc áp dụng — 10 phút

**Đoạn tự biên soạn cho ứng dụng giả định Atlas**, không phải trích đoạn tài liệu chính thức. Không xem đây là bảo đảm hành vi mặc định của Java, Spring, React hay sản phẩm thật. Khi áp dụng công việc, tra đúng phiên bản và hợp đồng hệ thống bạn dùng.

Atlas Importer is a fictional application used in this course. Its repository contains source files and a build script. A Java 21 runtime and access to the internal package registry are prerequisites for the build. The script downloads a parser package, which is a dependency of the importer. It then produces a build artifact named atlas-importer.jar. Before launching this artifact, provide the INPUT_DIR environment variable. The launcher reads that variable; Java itself does not assign a special meaning to this application-specific name. Supply the input file name as a command-line argument. The repository is not the build artifact: editing a source file does not change an artifact that has already been produced. Build again to include the edit.

Khoanh các cụm vừa học; xác định đối tượng, hành động và quy tắc/giới hạn đi kèm. Không cần dịch toàn bộ nếu đã hiểu ý.

## 5. Quiz và tóm tắt — 5 phút

Làm hoạt động quiz về câu mới và đoạn đọc. Giải thích xuất hiện sau khi nộp; bài này không có bảng đáp án.

Trong hoạt động tóm tắt, viết 3 ý tiếng Việt, giữ ít nhất 3 thuật ngữ IT. Rubric tự đối chiếu: (1) đúng chủ thể/hành động, (2) đúng điều kiện, default và phạm vi, (3) giữ cảnh báo, không thêm điều đoạn không nói. Chọn 2 mục cần ôn buổi sau. Phản hồi tự do chưa được máy chấm chất lượng; điểm quiz không chứng minh đã đọc thành thạo mọi tài liệu IT.

## Nguồn và phạm vi

Bảng từ, ví dụ, đoạn đọc và câu hỏi tự biên soạn ngày 04/11/2026, không sao chép tài liệu ngoài. IPA chưa được chuyên gia thẩm định toàn bộ. Không dùng bài này thay tài liệu cấu hình framework.
