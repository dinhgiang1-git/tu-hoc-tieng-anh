# Kế hoạch xây dựng webapp tự học tiếng Anh theo lộ trình

**Ngày lập:** 24/10/2026  
**Trạng thái:** Đề xuất triển khai v1 — dùng làm tài liệu giao việc cho AI lập trình  
**Phạm vi:** Lập kế hoạch sản phẩm, UI/UX, kiến trúc và các giai đoạn; chưa triển khai ứng dụng.

**Cách đọc:** mục 1–5 chốt sản phẩm và trải nghiệm; mục 6–10 dành cho thiết kế kỹ thuật; mục 11–12 là các giai đoạn và nghiệm thu; mục 13–16 hướng dẫn tổ chức, giao việc và mở rộng. Khi giao cho AI, bắt đầu từ prompt ở mục 14.4.

## 1. Định hướng đã chốt

Xây dựng một không gian tự học giúp người dùng biết **hôm nay học gì, học ngay ở đâu, đã hoàn thành phần nào và tiếp theo nên làm gì**.

Các yêu cầu đã được người dùng xác nhận:

- Bản đầu phục vụ nhu cầu tự học cá nhân; có thể mở cho nhiều người sau.
- Hỗ trợ nhiều lộ trình. Sheet được cung cấp là nguồn tham khảo cho **Lộ trình A**, không phải cấu trúc cố định của toàn ứng dụng.
- Backend: Java Spring Boot.
- Database: PostgreSQL trong Docker qua WSL đã có sẵn.
- Frontend: React + TypeScript, component `.tsx`, Tailwind CSS; bổ sung thư viện UI/đồ họa có mục đích rõ ràng.
- Giao diện đơn giản, thanh điều hướng bên trái, ưu tiên sự tập trung khi học.
- Chưa xây giao diện admin, nhưng thiết kế để bổ sung về sau.
- Chuẩn phát âm mục tiêu: **Anh-Anh (`en-GB`)**. Ưu tiên kiểm tra phát âm bằng **mã nguồn mở tự chạy, không mất phí API**; đây là lựa chọn được xác nhận bổ sung ngày 24/10/2026. Chi tiết và giới hạn ở mục 17.

Các mặc định đề xuất có thể điều chỉnh khi triển khai:

- Ngôn ngữ giao diện là tiếng Việt; nội dung tiếng Anh đi kèm giải thích phù hợp.
- Bản dùng được đầu tiên có bài học, nhiệm vụ, tài liệu, ghi chú, lưu tiến độ. Quiz ngắn bổ sung sau khi vòng học cơ bản hoạt động.
- Một tài khoản được tạo qua công cụ vận hành; chưa mở đăng ký công khai.
- Có thể đăng ký nhiều lộ trình, nhưng chỉ một lộ trình được chọn làm lộ trình đang tập trung trên trang Hôm nay.
- Bản đầu cần mạng để lưu tiến độ. Chế độ học ngoại tuyến đầy đủ là phạm vi sau.
- Thời lượng mỗi buổi do nội dung khai báo; mục tiêu phút/ngày ban đầu chỉ để gợi ý và cảnh báo tải học.

**Mốc đầu tiên cần đạt:** từ chọn lộ trình đến học một bài, hoàn thành nhiệm vụ, tải lại trang và tiếp tục học với tiến độ được giữ nguyên.

## 2. Những gì đã kiểm tra từ nguồn Lộ trình A

Nguồn: [Google Sheet lộ trình tiếng Anh](https://docs.google.com/spreadsheets/d/1Yim4AAiIfHMmNTrPsTXQtGfAkhsMcsBc_6-eURCgFEU/edit#gid=0).

Đã đọc các ô mục tiêu/nội dung của 26 tuần và xem tab video; chưa kiểm tra từng liên kết tài liệu đích.

| Phần trong nguồn | Cấu trúc đã quan sát | Ý nghĩa đối với ứng dụng |
|---|---|---|
| Tuần 1 | IPA, 100 từ; chia nhiệm vụ theo ngày | Cần buổi học và nhiệm vụ nhỏ |
| Tuần 2–13 | Ngữ pháp, từ vựng, nghe, shadowing; mục tiêu theo tuần | Cần biên soạn cách chia buổi |
| Tuần 14–26 | Chủ đề giao tiếp, từ vựng, chép chính tả, nói, tổng ôn | Cần nhiều loại hoạt động |
| Tab video | 5 mục hướng dẫn phương pháp học | Thư viện tài liệu dùng lại |

Nguồn có 3 giai đoạn và 26 tuần. “6 tháng” là tên thời lượng tham khảo, không phải quy tắc luôn kết thúc sau 180 ngày.

### 2.1. Khoảng trống cần biên soạn

Sheet mô tả kế hoạch học, chưa phải bộ bài học tương tác hoàn chỉnh. Phần công việc nội dung phải có trong dự án:

- Chuyển mục tiêu tuần thành các buổi có thứ tự, đầu ra và thời lượng ước tính.
- Viết phần giải thích, ví dụ, câu hỏi và đáp án đã kiểm tra.
- Chọn tài liệu cụ thể thay vì để yêu cầu chung như “tìm một video phù hợp”.
- Chuẩn bị tập từ thật sự sử dụng được; số lượng mục tiêu không chứng minh đã có đủ từ và ví dụ.
- Với bài nghe/chép chính tả, cần audio cùng transcript được phép dùng nếu muốn chấm trong app.
- Với bài nói/viết tự luyện, cần hướng dẫn và tiêu chí tự đánh giá; chưa có cơ sở tự động kết luận thành thạo.
- Kiểm tra thứ tự kiến thức: bài nâng cao nhắc kiến thức chưa giới thiệu thì thêm phần bổ trợ, có nhãn nội dung biên soạn mới.

### 2.2. Cách sử dụng nguồn trong dự án

Giữ nguồn, tác giả được ghi trên tài liệu, ngày tham khảo và vị trí ô/tuần khi lập bản đồ nội dung. Nội dung mới cần ghi người/AI soạn và người kiểm tra.

Sheet hiện ở chế độ chỉ xem, có thông báo hạn chế sao chép và ghi chú của tác giả về sao chép. Vì vậy, kế hoạch **không phụ thuộc vào khả năng export hoặc tự động lấy toàn bộ sheet**. Chuẩn bị giáo trình biên soạn riêng dựa trên khung mục tiêu và liên kết nguồn; nếu muốn nhập nguyên văn thì xác nhận quyền sử dụng trước bước nhập/phát hành nội dung đó. Việc ghi nguồn không tự thay thế quyền sử dụng.

Tài liệu này chỉ đặc tả sản phẩm, không phải bản chép lại giáo trình. Các liên kết trong sheet còn cần kiểm tra ở giai đoạn nội dung.

## 3. Phạm vi sản phẩm và thứ tự ưu tiên

| Mức | Chức năng | Đầu ra người học nhận được |
|---|---|---|
| P0 — vòng học đầu tiên | Đăng nhập, hồ sơ tối thiểu | Dữ liệu thuộc đúng người |
| P0 | Danh sách/chi tiết lộ trình, đăng ký học | Chọn A và biết các mục tiêu |
| P0 | Giai đoạn → tuần → buổi → nhiệm vụ | Tìm được bài và hiểu thứ tự học |
| P0 | Hôm nay, tiếp tục bài đang học | Có một hành động bắt đầu rõ ràng |
| P0 | Bài học, link tài liệu, checklist, ghi chú | Học và lưu kết quả thực tế |
| P0 | Tiến độ bền vững, hoàn tác hoàn thành | Theo dõi đúng sau khi tải lại |
| P1 — hoàn thiện lộ trình | Ngày học cá nhân, bài quá hạn, đổi lịch | Tiếp tục khi bận hoặc nghỉ học |
| P1 | Quiz ngắn, xem lỗi, làm lại | Có phản hồi về kiến thức |
| P1 | Ôn câu sai và bài đánh dấu | Dễ quay lại phần cần củng cố |
| P1 | Thống kê cơ bản, toàn bộ nội dung A đã duyệt | Học xuyên suốt lộ trình |
| P1 | Responsive, xử lý lỗi, backup/restore | Dùng ổn định hằng ngày |
| P2 — mở rộng | Flashcard và ôn cách quãng | Nhớ từ lâu hơn |
| P2 | Player nghe, dictation, ghi âm | Luyện kỹ năng sâu hơn |
| P2 | Admin CMS, nhiều người dùng công khai | Quản lý nội dung và cộng đồng |
| P2 | AI hỗ trợ nói/viết, nhắc lịch, PWA | Cá nhân hóa nâng cao |

Chưa đưa vào bản đầu: thanh toán, lớp học, bảng xếp hạng, mạng xã hội, marketplace, chứng chỉ, chấm phát âm tự động như tiêu chí đạt bài, microservices. Có thể thực hiện bản thử chấm phát âm tự chạy sau G3 theo mục 17; việc này không chặn vòng học cốt lõi.

## 4. Mô hình học tập và quy tắc nghiệp vụ

### 4.1. Cấu trúc nội dung

```text
Lộ trình (Curriculum)
└── Phiên bản (Revision)
    └── Giai đoạn (Phase)
        └── Tuần nội dung (Week)
            └── Buổi học (Lesson)
                ├── Nhiệm vụ (Activity)
                ├── Nhiệm vụ (Activity)
                └── Tài liệu liên quan qua nhiệm vụ
```

- **Lộ trình** là sản phẩm nội dung, ví dụ A hoặc B.
- **Phiên bản** là tập nội dung cố định khi xuất bản, ví dụ A v1.
- **Tuần nội dung** là nhóm sư phạm; không buộc trùng một tuần lịch thực tế.
- **Buổi học** là đơn vị đưa vào lịch cá nhân, có `estimatedMinutes` và thứ tự.
- **Nhiệm vụ** là đơn vị nhỏ nhất được ghi nhận hoàn thành.
- **Tài liệu** là nguồn hỗ trợ; mở link không tự làm nhiệm vụ hoàn thành.

Hệ thống không hardcode 3 giai đoạn, 26 tuần, 7 ngày mỗi tuần hay tên “Lộ trình A”. Một lộ trình ngắn 2 tuần cũng phải hoạt động bằng cùng API và UI.

### 4.2. Các loại nhiệm vụ

| Loại | Bản đầu | Cách hoàn thành |
|---|---|---|
| `READING` | Nội dung ngắn có mục tiêu, ví dụ, câu hỏi tự suy nghĩ | Người học xác nhận |
| `EXTERNAL_RESOURCE` | Link bài đọc/video/audio; có mô tả việc cần làm | Người học xác nhận sau khi thực hiện |
| `PRACTICE` | Viết câu, luyện IPA, shadowing, luyện nói theo yêu cầu | Xác nhận; có thể lưu câu trả lời dạng text |
| `REFLECTION` | Ghi điều đã hiểu/chưa hiểu | Lưu phản hồi theo yêu cầu nhiệm vụ |
| `QUIZ` | Bổ sung ở G5; câu hỏi chọn một đáp án | Server chấm theo chính sách bài |
| `DICTATION` / `RECORDING` | Dành cho giai đoạn mở rộng | Có đặc tả riêng trước khi phát triển |

Phân biệt `activityType` với `skillTags`: một bài `PRACTICE` có thể mang nhãn speaking và pronunciation. Không sinh một loại nghiệp vụ mới chỉ vì khác nhãn kỹ năng.

### 4.3. Hoàn thành và mức độ hiểu bài

Trạng thái nhiệm vụ: `NOT_STARTED → IN_PROGRESS → COMPLETED`.

- Có thể bỏ đánh dấu hoàn thành đối với nhiệm vụ xác nhận thủ công; bài và thống kê cập nhật lại theo dữ liệu hiện hành. Quiz tuân theo chính sách riêng ở mục 4.4.
- Buổi hoàn thành khi mọi nhiệm vụ **bắt buộc** trong buổi hoàn thành.
- Tuần hoàn thành khi mọi buổi bắt buộc thuộc tuần hoàn thành.
- Hoạt động tùy chọn và ngày nghỉ không làm tăng mẫu số tiến độ.
- Nhiệm vụ bắt buộc hiệu lực đối với lộ trình là `lesson.required && activity.required`. Buổi tùy chọn có thể có nhiệm vụ cần làm để hoàn thành chính buổi đó, nhưng toàn bộ buổi vẫn nằm ngoài mẫu số lộ trình. Tuần chỉ gồm buổi tùy chọn hiển thị “Tùy chọn”, không làm điều kiện kết thúc lộ trình.
- Bản đầu chưa có `SKIPPED` để tránh nhầm với hoàn thành. Có thể hoãn lịch mà vẫn giữ trạng thái học.
- Mở trang, click video hoặc để tab mở không đồng nghĩa đã học xong.
- “Hoàn thành lộ trình” là hoàn thành các yêu cầu được định nghĩa, không phải chứng nhận trình độ CEFR/IELTS.

```text
Tiến độ lộ trình = số nhiệm vụ bắt buộc hiệu lực đã hoàn thành
                  / tổng số nhiệm vụ bắt buộc hiệu lực trong phiên bản đang học × 100
```

Không lấy trung bình phần trăm của các tuần. Server trả cả tử số và mẫu số; trường hợp mẫu số bằng 0 hiển thị “Chưa có nội dung bắt buộc”, không hiển thị 100%. Publish yêu cầu có ít nhất một buổi bắt buộc và mỗi buổi bắt buộc phải có ít nhất một nhiệm vụ bắt buộc hợp lệ.

Tách hai nhóm chỉ số:

- **Tiến độ:** nhiệm vụ/buổi đã làm, phần đang học, phần còn lại.
- **Kết quả luyện tập:** điểm quiz, câu sai, kết quả từng lần làm. Chưa dùng một điểm tổng hợp để tuyên bố “đã thành thạo”.

### 4.4. Chính sách quiz ở G5

- MVP quiz chỉ có câu chọn một đáp án; mỗi câu trọng số bằng nhau, không trừ điểm.
- Lưu nguyên tập câu hỏi của phiên bản, lựa chọn, thời điểm nộp và kết quả mỗi lần làm.
- Hai chính sách rõ ràng: `ON_SUBMIT` cho luyện tập; `PASS_SCORE` cho bài kiểm tra bắt buộc, có ngưỡng khai báo trong nội dung.
- Nếu bài dùng ngưỡng, ví dụ 70%, đó là lựa chọn biên soạn, không là mặc định ẩn của hệ thống.
- Điểm lần gần nhất và điểm cao nhất phải có nhãn riêng. Đã đạt một lần thì lần luyện lại thấp hơn không tự mất trạng thái hoàn thành.
- Với quiz tự chấm, mọi mutation qua API checklist đều bị từ chối, gồm complete và undo. Assessment service quản lý progress quiz; luyện lại không reset thành tích. V1 không có thao tác xóa kết quả quiz, học lại toàn lộ trình dùng enrollment mới.
- Sau khi nộp mới trả đáp án và giải thích; trước đó API learner không chứa answer key.
- Cho làm lại, giải thích lỗi, mở lại bài liên quan. Câu sai là gợi ý ôn tập, không gây khóa cứng toàn lộ trình.

### 4.5. Lịch học cá nhân

Thông tin đăng ký gồm ngày bắt đầu, timezone, các thứ muốn học, mục tiêu phút/ngày và phiên bản lộ trình.

Quy tắc v1:

1. Sắp các buổi bắt buộc theo thứ tự nội dung.
2. Mỗi ngày học được chọn nhận một buổi; ngày bắt đầu không thuộc ngày học thì dùng ngày phù hợp tiếp theo.
3. Ngày nghỉ không tạo nhiệm vụ bắt buộc. Hoạt động tùy chọn không được tự chen vào lịch.
4. Thời lượng vượt mục tiêu ngày thì hiển thị cảnh báo; hệ thống chưa tự cắt/chia nội dung.
5. Bỏ lỡ một buổi làm nó thành quá hạn; không tự dồn nhiều buổi vào ngày tiếp theo.
6. Người học được xem trước nội dung tương lai và học sớm; ngày dự kiến vẫn được giữ để đối chiếu.

Ví dụ: một tuần nội dung được biên soạn thành 6 buổi, người học chọn thứ Hai/Tư/Sáu thì nhóm nội dung đó trải qua 2 tuần lịch. UI vẫn hiển thị “Tuần nội dung 1”, kèm ngày dự kiến thực tế.

**Hôm nay:** ưu tiên buổi đang dở của lộ trình đang tập trung; tiếp theo là buổi quá hạn sớm nhất; sau đó buổi đến hạn hôm nay. Nếu không có, hiển thị buổi tiếp theo và lựa chọn học sớm. Chỉ đưa một buổi thành hành động chính; danh sách bài quá hạn nằm ở phần phụ.

**Đổi lịch ở G4:** chọn ngày tiếp tục và các thứ học → xem trước danh sách thay đổi/ngày kết thúc → lưu. Chỉ dời buổi chưa hoàn thành; giữ nguyên thứ tự, kết quả và ngày của buổi đã hoàn thành. Với ngày đã có buổi hoàn thành, mặc định không xếp thêm buổi khi tạo lại lịch.

**Tạm dừng:** giữ nguyên dữ liệu, không hiển thị nợ bài mới trong Hôm nay. Tiếp tục học phải qua bản xem trước lịch mới. Nút tạm dừng và tiếp tục không xóa hay khởi tạo lại enrollment.

### 4.6. Phiên bản nội dung

- Trạng thái revision: `DRAFT → PUBLISHED → ARCHIVED`.
- Draft được sửa; published bất biến ở cấp nội dung, thứ tự, điều kiện hoàn thành và đáp án.
- Enrollment luôn gắn một revision cụ thể. A v2 không tự thay nội dung hay mẫu số tiến độ của người đang học A v1.
- Archive ngăn đăng ký mới, vẫn cho enrollment cũ đọc nội dung đã học.
- Bắt đầu học lại tạo enrollment mới; không xóa lịch sử cũ. Chỉ cho một enrollment chưa kết thúc của cùng curriculum với cùng người dùng.
- Chuyển enrollment sang revision khác là tính năng tương lai, cần bản đồ tương ứng và xử lý tiến độ rõ ràng.
- Link hỏng: cung cấp phản hồi/lựa chọn tìm nguồn thay thế; sửa nội dung chính thức qua revision mới. Không âm thầm thay bài đã xuất bản.

### 4.7. Vòng đời enrollment

| Trạng thái | Ý nghĩa và chuyển trạng thái |
|---|---|
| `ACTIVE` | Được học và ghi tiến độ; có thể chuyển PAUSED hoặc ABANDONED |
| `PAUSED` | Giữ dữ liệu, cho xem nội dung; ghi tiến độ cần resume. Resume với lịch đã preview chuyển ACTIVE |
| `COMPLETED` | Tự đạt khi mọi nhiệm vụ bắt buộc hiệu lực đã hoàn thành; lưu mốc hoàn thành, cho luyện lại quiz và sửa ghi chú |
| `ABANDONED` | Chủ động kết thúc lần học dở để bắt đầu lại; giữ lịch sử đọc được, không sửa tiến độ |

Chỉ một enrollment `ACTIVE` hoặc `PAUSED` cho cùng `(user, curriculum)`, bảo vệ bằng partial unique index và transaction. Muốn học lại khi còn lần học dở phải chọn tiếp tục lần cũ hoặc kết thúc lần đó trước; không tự bỏ lần học đang có.

Undo nhiệm vụ thủ công trong enrollment `COMPLETED` tự mở lại `ACTIVE` trong cùng transaction, **chỉ khi không có enrollment ACTIVE/PAUSED khác của cùng lộ trình**. Nếu đã bắt đầu lần học mới, trả 409 kèm giải thích để không có hai lần học đang hoạt động. Giữ sự kiện hoàn thành cũ trong lịch sử, xóa mốc hoàn thành hiện hành và tính lại tiến độ. Quiz vẫn theo mục 4.4.

Các thao tác học khi PAUSED/ABANDONED trả lỗi trạng thái rõ ràng; giao diện hướng dẫn resume hoặc bắt đầu lần học mới. Khi lộ trình đang tập trung vừa hoàn thành, Hôm nay hiển thị trạng thái đã hoàn tất và lựa chọn ôn tập/chọn lộ trình khác.

## 5. Thiết kế UI/UX

### 5.1. Nguyên tắc giao diện

- Trang Hôm nay là trang mặc định sau đăng nhập.
- Mỗi màn hình có một hành động chính nổi bật.
- Nội dung học là trọng tâm; thông tin tiến độ và lịch đóng vai trò hỗ trợ.
- Có thể quay lại bài đang dở trong tối đa 2 thao tác từ trang Hôm nay.
- Tránh màn hình đầy biểu đồ, hiệu ứng ăn mừng kéo dài hoặc thông báo gây gián đoạn.
- Trạng thái hoàn thành phải có chữ/icon, không chỉ dựa vào màu.
- Cho xem các tuần tương lai. V1 dùng thứ tự gợi ý; chưa khóa cứng theo prerequisite.

### 5.2. Thanh điều hướng bên trái

```text
[Tên ứng dụng]
[Lộ trình đang tập trung ▾]

Hôm nay
Lộ trình
Ôn tập          ← xuất hiện khi G5 sẵn sàng
Ghi chú
Tiến độ

-------------------------
Cài đặt
[Tài khoản]
```

- Desktop: sidebar rộng khoảng 232–248 px, có thể thu gọn; icon thu gọn có tooltip.
- Tablet: sidebar thu gọn khi cần.
- Mobile: nút mở drawer bên trái, đóng sau điều hướng, hỗ trợ bàn phím và focus trap.
- Không hiện mục admin hoặc mục chức năng chưa hoạt động.
- Trang tài liệu dùng bộ lọc trong lộ trình/bài; chưa cần thêm một mục menu cấp cao nếu ít nội dung.

### 5.3. Các màn hình cần xây

| Màn hình | Nội dung chính | Hành động chính | Trạng thái cần có |
|---|---|---|---|
| Đăng nhập | Username, mật khẩu, lỗi xác thực | Đăng nhập | Loading, sai thông tin, mất kết nối |
| Thiết lập lần đầu | Tên, timezone, ngày học, mục tiêu phút | Lưu và chọn lộ trình | Validation, giá trị gợi ý |
| Hôm nay | Buổi tiếp tục, mục tiêu, tải học, bài quá hạn gọn | Bắt đầu/tiếp tục học | Chưa đăng ký, nghỉ, tạm dừng, đã xong |
| Danh sách lộ trình | Tên, mục tiêu, thời lượng tham khảo, tiến độ nếu đã học | Xem chi tiết | Rỗng, loading, lỗi |
| Chi tiết lộ trình | Mô tả, nguồn, giai đoạn/tuần, nội dung mẫu | Bắt đầu/tiếp tục | Phiên bản mới, đã archive, đã hoàn thành |
| Chi tiết tuần | Mục tiêu, buổi, thời lượng, trạng thái, tài liệu | Học buổi tiếp theo | Tuần tương lai, chưa có lịch |
| Không gian học | Nhiệm vụ hiện tại, nội dung, tài liệu, ghi chú | Hoàn thành và tiếp tục | Đang lưu, đã lưu, lỗi lưu, link hỏng |
| Quiz/kết quả | Câu hỏi, lựa chọn, giải thích sau nộp | Nộp bài/luyện lại | Chưa trả lời hết, submit lặp, mất mạng |
| Ôn tập | Câu sai, bài đã đánh dấu, link về nội dung | Ôn phần tiếp theo | Chưa có mục ôn, đã ôn xong |
| Ghi chú | Tìm kiếm theo nội dung/lộ trình/bài | Mở ghi chú | Rỗng, đang lưu, xung đột |
| Tiến độ | Tổng hoàn thành, tiến độ tuần, lịch sử quiz | Quay lại học | Chưa có dữ liệu, lọc lộ trình |
| Cài đặt/lịch | Ngày học, timezone, pause, xem trước đổi lịch | Lưu lịch mới | Lịch xung đột, ngày không hợp lệ |

### 5.4. Bố cục không gian học

```text
┌──────────────┬──────────────────────────────────────────────┐
│ Sidebar      │ Lộ trình > Tuần nội dung > Buổi học           │
│ thu gọn được │ Tiêu đề + mục tiêu ngắn + thời lượng dự kiến │
│              ├──────────────────────────────────────────────┤
│              │ Nhiệm vụ 2/4                                │
│              │                                              │
│              │ Nội dung / ví dụ / câu hỏi / tài liệu        │
│              │                                              │
│              │ [Ghi chú ▾]  [Đánh dấu để ôn]                │
│              ├──────────────────────────────────────────────┤
│              │ Trước        Đã lưu        Hoàn thành →     │
└──────────────┴──────────────────────────────────────────────┘
```

Chế độ tập trung ẩn sidebar và các khối phụ, giữ tiêu đề ngắn, nội dung và điều hướng bài. Trên mobile, phần thao tác có thể sticky nhưng không che nội dung hoặc bàn phím nhập liệu.

Ghi chú tự lưu sau một khoảng ngừng gõ, có nhãn “Đang lưu/Đã lưu/Chưa lưu được”. Khi rời trang với nội dung chưa lưu, đưa lựa chọn giữ lại hoặc thử lưu. Không hiển thị thành công nếu server chưa xác nhận.

### 5.5. Hướng thị giác đề xuất

| Thành phần | Đề xuất |
|---|---|
| Nền | Trắng ngà/xám rất nhạt; vùng đọc trắng |
| Màu chính | Xanh lá trầm hoặc teal; một màu cho CTA |
| Chữ | Xám đậm, phân cấp bằng cỡ/độ đậm thay vì nhiều màu |
| Typography | Font hỗ trợ đầy đủ tiếng Việt và IPA; kiểm tra Noto Sans cùng font dự phòng cho ký hiệu |
| Nội dung đọc | 16–18 px, line-height khoảng 1.6–1.75, bề rộng 65–75 ký tự |
| Thành phần | Bo góc vừa phải, border nhẹ, ít shadow |
| Trạng thái | Hoàn thành, đang học, chưa học phân biệt bằng icon và chữ |
| Chuyển động | Khoảng 120–200 ms cho phản hồi nhỏ; tôn trọng reduced motion |
| Biểu đồ | Bar theo tuần; tránh biểu đồ radar suy diễn trình độ |

Đây là hướng thiết kế, không phải giá trị bắt buộc trước khi thử màn hình thật. Chuẩn hóa thành CSS variables/design tokens để đổi theme mà không sửa từng component.

## 6. Công nghệ đề xuất

### 6.1. Frontend

| Thành phần | Lựa chọn | Vai trò |
|---|---|---|
| Framework | React + TypeScript + Vite | SPA phù hợp ứng dụng học sau đăng nhập |
| Styling | Tailwind CSS, plugin Vite theo tài liệu hiện hành | Layout và token nhất quán |
| UI primitives | shadcn/ui với một bộ primitives thống nhất | Sidebar, dialog, form, tabs, accordion |
| Điều hướng | React Router | Route lộ trình/bài/quiz, bảo vệ trang cần đăng nhập |
| Dữ liệu server | TanStack Query | Query, cache, invalidate sau mutation |
| Form | React Hook Form + Zod | Form lịch học, login, kiểm tra dữ liệu phía FE |
| Icon | Lucide React | Icon rõ nghĩa và đồng bộ |
| Biểu đồ | Recharts ở G6 | Biểu đồ tiến độ đơn giản |
| Animation | CSS trước; Motion for React khi thực sự cần | Chuyển trạng thái bài/dialog nhẹ |
| API types | Sinh từ OpenAPI hoặc duy trì contract có kiểm tra | Hạn chế lệch DTO FE/BE |
| Kiểm thử | Vitest, Testing Library, Playwright | Logic UI và hành trình học thật |

TSX là cú pháp component TypeScript; lựa chọn framework trong kế hoạch này là React. SPA chưa cần SSR/Next.js cho mục tiêu bản đầu.

Không thêm Redux/Zustand chỉ để chứa lại dữ liệu đã có trong TanStack Query. UI state nhỏ dùng state/context; chỉ thêm store khi có nhu cầu xuyên màn hình rõ ràng.

Tài liệu tích hợp: [Vite](https://vite.dev/guide/), [Tailwind Vite](https://tailwindcss.com/docs/installation/using-vite), [shadcn/ui Vite](https://ui.shadcn.com/docs/installation/vite), [TanStack Query](https://tanstack.com/query/latest/docs/framework/react/overview), [Motion React](https://motion.dev/docs/react).

### 6.2. Backend và dữ liệu

- Java 21 là baseline đề xuất; dùng bản Spring Boot ổn định còn được hỗ trợ, tương thích thư viện tại G1.
- Spring Web MVC, Spring Security, Bean Validation, Spring Data JPA.
- PostgreSQL đang chạy trong Docker; giữ major version hiện có nếu còn phù hợp và được hỗ trợ.
- Flyway quản lý schema; `ddl-auto=validate` ngoài môi trường thử nghiệm biệt lập.
- Maven Wrapper để thống nhất công cụ build.
- OpenAPI mô tả API; chọn công cụ sinh tài liệu tương thích đúng major Spring Boot đã chốt.
- JUnit và Testcontainers PostgreSQL cho các integration test liên quan SQL, FK, timezone, transaction.

Tại thời điểm kiểm tra, [trang yêu cầu Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) cho biết Java 21 nằm trong phạm vi tương thích. Khi bắt đầu G1, khóa chính xác JDK, Spring Boot, Node, Vite, React, Tailwind và thư viện liên quan trong một ADR/lockfile. Không trộn hướng dẫn cũ với dependency mới hoặc nâng cấp major DB chỉ để theo một ví dụ.

### 6.3. Kiến trúc tổng thể

```text
Trình duyệt
  └── React SPA (TSX + Tailwind)
        └── /api/v1 qua cùng origin
              └── Spring Boot — một ứng dụng, nhiều module theo nghiệp vụ
                    ├── identity
                    ├── curriculum + contentops
                    ├── enrollment + scheduling
                    ├── learning + assessment
                    ├── notes + review
                    └── reporting
                          └── PostgreSQL / Docker / WSL

Gói nội dung JSON + Markdown được quản lý bằng Git
  └── CLI validate / import / publish → application service → PostgreSQL
```

Frontend và backend có thể nằm cùng repository, phát hành độc lập nếu cần. Bản production đơn giản dùng reverse proxy phục vụ FE và chuyển `/api` về Spring Boot.

Backend dùng modular monolith: một deployment, chia package theo nghiệp vụ. Controller gọi application service; service điều phối transaction và quy tắc; repository xử lý persistence. Không trả trực tiếp JPA entity ra API.

Chưa cần Redis, broker, Kubernetes, GraphQL hoặc event sourcing. Event lịch sử nhỏ phục vụ báo cáo không biến hệ thống thành kiến trúc event sourcing.

## 7. Mô hình dữ liệu dự kiến

Chỉ tạo bảng khi giai đoạn cần đến; danh sách dưới đây là bản thiết kế tổng thể, không yêu cầu tạo mọi bảng ở G1.

### 7.1. Nội dung chung

| Bảng | Trường chính / trách nhiệm |
|---|---|
| `curriculum` | `id`, `slug`, tên, mô tả; định danh lộ trình xuyên phiên bản |
| `curriculum_revision` | Curriculum, version, trạng thái, source metadata, checksum, ngày publish |
| `phase` | Revision, stable key, thứ tự, mục tiêu |
| `curriculum_week` | Phase, revision, stable key, số/thứ tự tuần nội dung |
| `lesson` | Week, revision, stable key, title, mục tiêu, thứ tự, estimated minutes, required |
| `activity` | Lesson, revision, stable key, type, required, completion policy, nội dung |
| `resource` | Revision, stable key, title, URL, loại, nguồn/attribution |
| `activity_resource` | Liên kết nhiệm vụ với tài liệu thuộc cùng revision |
| `content_import` | Checksum, kết quả validate/import, thời gian, revision tạo ra |

Nội dung dài có thể lưu Markdown. Payload riêng theo loại hoạt động có thể dùng JSONB với schema validation, nhưng quan hệ, thứ tự, ownership và tiến độ dùng cột/bảng quan hệ. Không nhét toàn bộ lộ trình và tiến độ vào một JSON lớn.

### 7.2. Dữ liệu người học

| Bảng | Trường chính / trách nhiệm |
|---|---|
| `app_user` | Username chuẩn hóa, password hash, tên, timezone, role |
| `learner_preferences` | Mục tiêu phút/ngày, enrollment đang tập trung, tùy chọn UI |
| `enrollment` | User, curriculum/revision, ngày bắt đầu, timezone, trạng thái, ngày học, version |
| `lesson_schedule` | Enrollment, lesson, ngày gốc, ngày dự kiến hiện tại |
| `activity_progress` | Enrollment, activity, trạng thái, bắt đầu/hoàn thành, optimistic version |
| `activity_response` | Câu trả lời text cho thực hành/phản tư; thuộc enrollment và activity |
| `note` | Owner, enrollment, lesson/activity tùy chọn, nội dung, thời gian sửa, version |
| `learning_event` | Sự kiện thực tế complete/undo/submit; thời điểm và ngày địa phương tại lúc ghi |

### 7.3. Bảng bổ sung ở G5 trở đi

| Bảng | Mục đích |
|---|---|
| `quiz_question`, `quiz_option` | Câu hỏi, lựa chọn, đáp án/explanation thuộc revision |
| `quiz_attempt`, `quiz_answer` | Lịch sử làm bài, đáp án đã chọn, điểm server chấm |
| `review_bookmark` | Bài/nhiệm vụ người học đánh dấu để ôn |
| `review_resolution` | Enrollment + question, theo dõi câu sai đã được trả lời đúng khi ôn; không dùng chung giữa các lần học lại |
| `study_session` | Chỉ thêm nếu đo thời gian học thực sự; không cần cho checklist ban đầu |

### 7.4. Ràng buộc phải có

- Unique `(curriculum_id, version)` trên revision; stable key duy nhất trong đúng scope revision.
- Unique `(enrollment_id, activity_id)` cho progress và `(enrollment_id, lesson_id)` cho lịch.
- Partial unique `(user_id, curriculum_id)` khi enrollment ở trạng thái ACTIVE hoặc PAUSED; unique `(enrollment_id, question_id)` cho trạng thái xử lý câu ôn.
- Enrollment có user và revision hợp lệ; curriculum ID nếu lặp trên enrollment phải được ràng buộc khớp revision.
- Dùng FK/composite FK và kiểm tra service trong transaction để ngăn gắn activity/lesson từ revision khác vào enrollment.
- Mọi đọc/sửa dữ liệu người học phải kiểm tra owner từ session; không tin `userId` client gửi.
- Index chính: lịch theo `(enrollment_id, planned_date)`, progress theo enrollment/status, enrollment theo user, note theo owner/updated_at.
- Các bảng đã có lịch sử học không cascade-delete khi archive nội dung.
- Số thứ tự, thời lượng, trọng số và phần trăm có validation; không cho thời lượng âm.
- `created_at`, `updated_at` dùng `timestamptz`; ngày dự kiến dùng `date`.
- Lưu timezone IANA riêng, ví dụ `Asia/Ho_Chi_Minh`; không chỉ lưu chuỗi `UTC+7`.

### 7.5. Đồng thời, thời gian và tính nhất quán

- Server tính tiến độ; FE không cộng dồn số đã hoàn thành như nguồn dữ liệu chính.
- PUT trạng thái lặp với cùng mục tiêu là idempotent; không tạo thêm event hoàn thành khi trạng thái thực tế không đổi.
- Create enrollment và submit quiz có idempotency key: retry cùng key/payload trả kết quả cũ; key cũ với payload khác bị từ chối.
- Complete/undo dùng optimistic version. Request cũ không âm thầm ghi đè thao tác mới từ tab khác.
- Preview đổi lịch trả `enrollmentVersion`; commit kiểm tra version trong transaction. Mọi thay đổi ảnh hưởng lịch, kể cả completion, phải tham gia cùng cơ chế khóa/version của enrollment.
- Điểm quiz, cập nhật progress và event được commit cùng transaction.
- Sự kiện lưu `occurred_at`, `local_date`, `timezone_at_event`; đổi timezone sau này không viết lại lịch sử.
- Dùng `Clock` có thể inject để test ngày hôm nay, cuối tháng và qua nửa đêm.

## 8. API dự kiến và contract giao tiếp

Prefix `/api/v1`. Danh sách là contract định hướng; G0/G1 chốt request/response trước khi FE và BE tách việc.

| Nhóm | Endpoint tiêu biểu | Giai đoạn |
|---|---|---|
| Session | `GET /auth/csrf`, `POST /auth/login`, `POST /auth/logout`, `GET /me` | G1 |
| Hồ sơ | `PATCH /me/preferences` | G1/G4 |
| Nội dung | `GET /curricula`, `GET /curricula/{slug}`, `GET /revisions/{id}/outline` | G2 |
| Học bài | `GET /enrollments/{id}/lessons/{lessonId}` | G3 |
| Đăng ký | `POST /enrollments`, `GET /me/enrollments` | G3 |
| Hôm nay | `GET /me/today?enrollmentId=...` | G3 |
| Progress | `PUT /enrollments/{id}/activities/{activityId}/progress` | G3 |
| Thực hành | `PUT /enrollments/{id}/activities/{activityId}/response` | G3 |
| Ghi chú | `GET /me/notes`, `POST /notes`, `PATCH /notes/{id}`, `DELETE /notes/{id}` | G3 |
| Lịch | `GET /enrollments/{id}/schedule?from=...&to=...` | G4 |
| Dời lịch | `POST /enrollments/{id}/schedule/preview`, `POST /enrollments/{id}/schedule/commit` | G4 |
| Tạm dừng/kết thúc | `POST /enrollments/{id}/pause`, `POST /enrollments/{id}/resume`, `POST /enrollments/{id}/abandon` | G4 |
| Quiz | `POST /enrollments/{id}/quizzes/{activityId}/attempts`, `PUT /attempts/{id}/submission` | G5 |
| Ôn tập | `GET /me/review`, `POST /review-bookmarks`, `DELETE /review-bookmarks/{id}` | G5 |
| Kiểm tra lại | `POST /enrollments/{id}/review/questions/{questionId}/checks` | G5 |
| Tiến độ cơ bản | `GET /enrollments/{id}/summary` | G3 |
| Lịch sử/thống kê | `GET /me/activity?from=...&to=...`, mở rộng summary | G6 |

Quy tắc API:

- Enum và DTO có mô tả trong OpenAPI; FE dùng đúng contract.
- API lesson qua enrollment xác thực owner và revision; xem trước catalog dùng DTO riêng, không lộ answer key.
- Tất cả API `/me`, attempt, note, enrollment đều yêu cầu session. Truy cập đối tượng của người khác trả 404 nhất quán.
- Phân trang cho notes, lịch sử, catalog khi cần; outline trả metadata nhẹ, không tải mọi nội dung bài của 26 tuần.
- Lỗi thống nhất kiểu Problem Details: `status`, `code`, `detail`, `fieldErrors`, `traceId`; không trả stack trace.
- 401: cần đăng nhập; 403: thiếu quyền hoặc CSRF; 404: không thấy tài nguyên; 409: version/state xung đột; 400: input không hợp lệ.
- Request thay đổi state có idempotency/version theo mục 7.5; không retry POST không an toàn một cách mù quáng.
- Resume nhận kết quả preview và enrollment version; áp dụng lịch mới cùng chuyển ACTIVE trong một transaction. Không commit lịch rồi gọi resume bằng hai bước độc lập phía FE.
- Review có scope enrollment: endpoint tổng hợp `/me/review` trả enrollment ID cho từng mục; checks và bookmark phải xác minh question/activity thuộc revision đang học. Làm đúng câu ôn ghi resolution cho enrollment đó, không tự sửa điểm attempt hoặc hoàn thành quiz gốc. Sai lại trong attempt mới mở lại mục cần ôn tương ứng.
- Sau mutation, invalidate đúng cache Hôm nay/outline/summary. Không chỉ đổi trạng thái riêng trên nút hiện tại.

Ví dụ DTO do dự án đề xuất:

```json
{
  "enrollmentId": "uuid",
  "revisionId": "uuid",
  "nextLesson": {
    "id": "uuid",
    "title": "Nhận biết một nhóm nguyên âm",
    "estimatedMinutes": 25,
    "reason": "IN_PROGRESS",
    "completedRequiredActivities": 1,
    "totalRequiredActivities": 3
  },
  "progress": {
    "completedRequiredActivities": 8,
    "totalRequiredActivities": 120
  }
}
```

Các con số/tên trong JSON chỉ minh họa contract, không được trình bày như số liệu trích từ sheet.

## 9. Chuẩn bị và quản lý nội dung khi chưa có admin

### 9.1. Gói nội dung trong repository

```text
content/
  schemas/
    curriculum.schema.json
  curriculum-a/
    v1/
      manifest.json
      weeks/
        week-01.json
      lessons/
        vowel-introduction.md
      resources.json
  fixtures/
    curriculum-b-demo/
```

Stable key có nghĩa như `a.w01.s01.vowel-practice`; không dùng số dòng sheet làm ID. Dùng UUID cho bản ghi database, stable key để kiểm tra/import/đối chiếu nội dung.

Manifest khai báo schema version, curriculum key, revision, ngôn ngữ, nguồn, trạng thái biên soạn và checksum. Mỗi bài ghi rõ mục tiêu, hướng dẫn, nhiệm vụ, completion policy, tài liệu và độ ưu tiên.

### 9.2. Quy trình nhập và xuất bản

```text
Biên soạn nội dung được phép dùng
→ kiểm tra sư phạm và tài liệu
→ validate schema + quan hệ
→ dry-run xem báo cáo
→ import tạo draft trong transaction
→ kiểm tra preview
→ publish revision
```

CLI dự kiến có các thao tác `content validate`, `content import --dry-run`, `content import`, `content publish`. Đây là tên lệnh cần xây ở G2, chưa phải công cụ đã tồn tại.

- Cùng checksum được nhập lại không tạo bản sao; gói khác nội dung phải tạo revision draft mới hoặc cập nhật draft được chỉ định.
- Import lỗi giữa chừng rollback toàn bộ gói.
- Kiểm tra key trùng, tham chiếu thiếu, thứ tự, type không hỗ trợ, URL scheme, bài bắt buộc rỗng, quiz thiếu đáp án.
- Publish yêu cầu bài bắt buộc đủ nội dung, rule hợp lệ, nguồn đã rà soát và không còn placeholder.
- Tài liệu bên ngoài hỏng có thể được thay bằng nội dung tự biên soạn phù hợp trước khi publish; tránh hứa hoàn thành một bài mà người học không truy cập được.
- Không fetch tùy ý URL do người dùng nhập ở backend. Rà soát liên kết bằng quy trình kiểm tra có kiểm soát.
- Flyway dành cho schema; nội dung giáo trình đi qua importer, không đổ toàn bộ vào migration SQL.
- CLI gọi cùng application service mà admin UI sau này sẽ sử dụng.

### 9.3. Chiến lược nội dung để có bản dùng được sớm

1. Xây một **lộ trình thử nghiệm riêng** gồm 1–2 tuần biên soạn hoàn chỉnh, có ngày học bình thường, ôn tập và tài liệu ngoài.
2. Dùng nó để kiểm tra end-to-end trước khi nhập 26 tuần.
3. Lộ trình A đầy đủ chỉ publish sau khi toàn bộ nhiệm vụ bắt buộc đã sẵn sàng.
4. Không cho enrollment thật bám một revision đang được thêm tuần hằng ngày; điều đó làm mẫu số và mục tiêu học thay đổi.
5. Fixture B ngắn chỉ nằm ở môi trường test/development để chứng minh khả năng nhiều lộ trình.

## 10. Môi trường phát triển và bảo mật

### 10.1. Làm việc với PostgreSQL/Docker/WSL đã có

G1 thực hiện kiểm tra chỉ đọc: Docker context, nơi daemon chạy, container PostgreSQL, version, published port, volume và database hiện hữu. Ghi cấu hình cần dùng mà không ghi mật khẩu vào tài liệu.

Ưu tiên tạo database/user riêng cho ứng dụng trên PostgreSQL sẵn có khi phù hợp. Không tự thay compose hiện có, đổi major image, xóa volume hoặc dùng database của ứng dụng khác.

| Nơi chạy backend | Host DB trong cấu hình | Cần xác minh |
|---|---|---|
| Windows IDE | Thường là `localhost` + port publish của Docker/WSL | Test kết nối từ chính Windows |
| WSL host | Port publish nhìn từ WSL | Không suy đoán từ việc Windows kết nối được |
| Container cùng Docker network với DB | Tên service/container network alias + port nội bộ | Cùng network, credentials, healthcheck |

Không dùng `localhost` trong container BE để chỉ container DB. Trường hợp WSL NAT/mirrored khác nhau cần xử lý theo cấu hình thực tế; tham khảo [Microsoft WSL networking](https://learn.microsoft.com/en-us/windows/wsl/networking).

Frontend dev dùng Vite proxy `/api` đến BE. Production dùng cùng origin qua reverse proxy để đơn giản hóa cookie và CORS.

### 10.2. Tài khoản và session

- Spring Security; tạo tài khoản đầu tiên bằng CLI/bootstrap dùng secret từ biến môi trường, không hardcode password.
- Session cookie `HttpOnly`, `Secure` khi HTTPS, SameSite phù hợp. Không lưu access token trong localStorage.
- Có thể dùng session in-memory ở bản cá nhân một instance: restart BE yêu cầu đăng nhập lại, tiến độ trong DB vẫn còn. Nâng sang Spring Session JDBC khi cần giữ session qua restart hoặc nhiều instance.
- Giữ CSRF protection cho request thay đổi dữ liệu, kể cả login/logout theo cấu hình đã chọn. FE lấy token và làm mới sau login/logout khi cần.
- Logout xóa session và cache dữ liệu cá nhân trên FE.
- Kiểm thử hai tài khoản riêng biệt từ G1, dù giao diện chỉ phục vụ một người dùng thật lúc đầu.
- Phân quyền ít nhất `LEARNER` và `ADMIN` trong mô hình; API quản trị chưa mở khi chưa phát triển.

Tham khảo cách tích hợp SPA và CSRF trong [Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

### 10.3. Nội dung, dữ liệu và vận hành

- Render Markdown qua cơ chế an toàn; raw HTML bị vô hiệu hoặc sanitize. Chỉ cho URL scheme được hỗ trợ.
- Mở tài liệu ngoài ở tab mới có `noopener`; luôn có đường quay lại bài.
- `.env` và secret nằm ngoài Git; `.env.example` chỉ chứa placeholder.
- Log có trace ID và lỗi kỹ thuật, tránh ghi toàn bộ ghi chú/câu trả lời/mật khẩu.
- Healthcheck không công khai chi tiết nhạy cảm; backup dữ liệu trước thay đổi schema đáng kể.
- Backup PostgreSQL bằng công cụ phù hợp, có kiểm tra khôi phục vào database riêng. Tham khảo [PostgreSQL SQL dump](https://www.postgresql.org/docs/current/backup-dump.html).

## 11. Các giai đoạn thực hiện

Thời gian dưới đây là **ước lượng công sức** cho một người điều phối cùng AI lập trình, đã bao gồm kiểm tra nhưng chưa phản ánh tốc độ làm thực tế. Nội dung học là luồng việc riêng, có thể chiếm nhiều thời gian hơn code.

### G0 — Khóa đặc tả và mẫu nội dung (1–2 ngày)

**Mục tiêu:** AI triển khai có cùng cách hiểu về sản phẩm.

Công việc:

- Xác nhận thuật ngữ và quy tắc ở mục 4; ghi quyết định thay đổi vào ADR.
- Lập bảng đối chiếu nguồn → phase/week → lesson/activity; đánh dấu phần đã đọc, chưa kiểm tra link và nội dung cần biên soạn.
- Biên soạn một tuần mẫu; thiết kế sơ bộ thêm một tuần có ngữ pháp/nghe để thử tính linh hoạt.
- Vẽ wireframe Hôm nay, chi tiết lộ trình, không gian học, đổi lịch; dùng nội dung thực tế ngắn.
- Định nghĩa DTO/OpenAPI bản đầu, schema content và danh sách task.

Đầu ra: `docs/product-rules.md`, `docs/content-mapping.md`, `docs/api/openapi.yaml`, ADR đầu tiên, gói nội dung mẫu.

**Nghiệm thu:** mỗi activity có đầu ra và điều kiện hoàn thành; phân biệt được nội dung tham khảo với nội dung tự soạn; người đọc có thể diễn giải chính xác lịch khi chọn 3 ngày/tuần.

### G1 — Nền tảng kỹ thuật và tài khoản (2–3 ngày, sau G0)

**Backend:** project Spring Boot, database migration tối thiểu, login/logout/session/CSRF, user/preferences, error format, healthcheck.

**Frontend:** Vite React TSX, Tailwind, UI primitives, router, query client, login, layout sidebar, states dùng chung.

**Hạ tầng:** kiểm tra DB hiện có; cấu hình dev, Maven Wrapper, package lock, `.env.example`, CI build/typecheck/test. Chốt phiên bản dependency và cách kết nối WSL.

Đầu ra: FE/BE chạy được theo README từ môi trường sạch; tài khoản cá nhân tạo được bằng công cụ đã mô tả.

**Nghiệm thu:** login thành công; logout xóa cache; request ghi thiếu CSRF bị từ chối; user A không truy cập dữ liệu riêng của B; BE kết nối đúng DB và migration chạy lặp an toàn.

### G2 — Nội dung và danh mục lộ trình (2–4 ngày, sau G1)

**Backend:** bảng curriculum/revision/phase/week/lesson/activity/resource; importer validate/dry-run/import/publish; API catalog/outline.

**Frontend:** danh sách lộ trình, chi tiết, accordion giai đoạn/tuần, trạng thái nội dung sẵn sàng, hiển thị nguồn.

**Nội dung:** nhập lộ trình thử nghiệm 1–2 tuần; fixture B cấu trúc khác A.

**Nghiệm thu:** import lặp không trùng; gói lỗi rollback; revision published không sửa được; A/B hiển thị từ API; outline không tải toàn bộ Markdown; chỉ hiện revision đủ điều kiện đăng ký.

### G3 — Vòng học có lưu tiến độ (3–5 ngày, sau G2)

**Backend:** enrollment, lịch ban đầu đơn giản, today, progress, response, note; tính tiến độ và kiểm tra owner/revision.

**Frontend:** đăng ký lộ trình, Hôm nay, không gian học, checklist, ghi chú, tiếp tục bài, hoàn tác. Các nhiệm vụ chưa hỗ trợ renderer phải bị chặn từ publish.

**Giới hạn:** dùng lịch mặc định đã đặc tả; chưa cần preview đổi lịch phức tạp hoặc quiz tự chấm.

**Nghiệm thu bắt buộc:**

1. Chọn lộ trình → bắt đầu → hoàn thành 2 nhiệm vụ → reload → tiến độ và ghi chú còn đúng.
2. Hoàn thành nhiệm vụ cuối làm buổi hoàn thành và gợi ý đúng buổi tiếp theo.
3. Gửi cùng trạng thái hai lần chỉ được tính một lần.
4. Undo cập nhật buổi/tuần/tổng tiến độ đồng bộ.
5. Mất mạng lúc lưu hiển thị lỗi và cho retry; không báo đã lưu giả.
6. ID activity từ lộ trình/revision khác bị từ chối.

**Mốc M1:** đã có thể dùng thử hằng ngày với lộ trình pilot. Ưu tiên đạt mốc này trước khi mở rộng tính năng.

### G4 — Lịch cá nhân linh hoạt (2–4 ngày, sau G3)

**Backend:** chọn ngày học, tính ngày kết thúc, truy vấn lịch theo khoảng, overdue, pause/resume, preview/commit đổi lịch, version conflict.

**Frontend:** form lịch ngắn, danh sách lịch theo tuần, khối bài quá hạn, preview lịch cũ/mới. Chưa cần kéo thả calendar.

**Nghiệm thu:**

- Lịch 3 ngày/tuần giữ nguyên nội dung và thứ tự, không ép 26 tuần nội dung thành 26 tuần lịch.
- Nghỉ một tuần rồi tiếp tục không mất progress/note/quiz history.
- Đổi lịch giữ nguyên buổi đã hoàn thành.
- Preview ở tab A, complete ở tab B, commit ở A trả xung đột và yêu cầu tính lại.
- Test qua nửa đêm, cuối tháng, timezone khác; ngày nghỉ không xuất hiện như thất bại.

### G5 — Quiz và ôn tập tối thiểu (3–5 ngày, sau G3; nên hoàn tất G4 trước phát hành)

**Backend:** câu hỏi/lựa chọn, attempt/submission, chấm điểm, completion policy, bookmark, truy vấn câu sai, kiểm tra lại câu ôn.

**Frontend:** quiz chọn một đáp án, nộp/xem giải thích, làm lại, danh sách cần ôn, quay lại bài nguồn.

**Nội dung:** bộ câu hỏi ít nhưng có đáp án và giải thích được kiểm tra; ưu tiên các bài pilot trước.

**Nghiệm thu:** đáp án không lộ trước submit; server chấm đúng; submit lặp không tạo điểm/lịch sử trùng; rule ON_SUBMIT/PASS_SCORE đúng; ôn đúng câu sai cập nhật danh sách; lần luyện lại thấp hơn không tự xóa thành tích đạt trước đó.

G5 chưa có thuật toán spaced repetition. Danh sách ôn sắp theo chưa xử lý rồi đến lần sai gần nhất; flashcard/SRS cần một đặc tả riêng sau.

### G6 — Hoàn thiện A và báo cáo học tập (2–4 ngày kỹ thuật, sau G4/G5)

**Backend:** mở rộng summary đã có từ G3, thêm activity history theo owner/enrollment/date; thống kê nguồn dữ liệu hiện hành; dữ liệu kiểm chứng cho các trường hợp đặc biệt.

**Frontend:** trang tiến độ, bar theo tuần, lịch sử quiz, nguồn bài học, tìm ghi chú, làm rõ trạng thái đã xong/chưa có dữ liệu. Hiển thị thư viện tài liệu theo ngữ cảnh bài.

**Nội dung:** hoàn thiện và rà soát đủ 26 tuần trước publish Lộ trình A v1. Từng bài bắt buộc phải học được, không còn link chờ điền hoặc câu hỏi chưa có đáp án.

**Nghiệm thu:** tổng tiến độ khớp dữ liệu nhiệm vụ; hoạt động tùy chọn không làm sai tổng; ngày hoạt động dựa trên sự kiện thực tế; publish A v2 thử nghiệm không đổi enrollment A v1; fixture B đi hết vòng đăng ký/học mà không sửa UI.

Thời gian học chỉ được hiển thị là số đo thực tế nếu đã có study session. Nếu chưa, hiển thị **thời lượng dự kiến của nội dung**, không dùng nó làm “phút bạn đã học”.

### G7 — Hoàn thiện vận hành và phát hành cá nhân (2–3 ngày, sau G6)

Công việc:

- E2E các hành trình chính trên desktop/mobile, test bàn phím và nội dung IPA.
- Kiểm tra loading/error/empty, auth hết hạn, mất mạng khi lưu, link ngoài bị lỗi.
- Kiểm tra query chậm/N+1; phân trang lịch sử; lazy-load bài và biểu đồ.
- Dockerfile/deploy config cho app; tài liệu kết nối DB hiện có; HTTPS/cookie đúng khi triển khai ngoài localhost.
- Backup và restore thử vào database riêng; kiểm tra khôi phục user, enrollment, progress, note và quiz.
- Ghi hướng dẫn cập nhật app, nhập nội dung, rollback và xử lý migration lỗi.

**Nghiệm thu:** chạy được theo README trên môi trường sạch; phục hồi được dữ liệu thật của bản thử; không có lỗi nghiêm trọng trong các kịch bản mục 12; dùng thử liên tục ít nhất 7 ngày để ghi nhận vướng mắc học tập.

**Mốc M2:** chức năng lộ trình hoàn chỉnh để tự học ổn định, với A đã được biên soạn/duyệt đầy đủ. Khoảng dùng thử 7 ngày là thời gian quan sát, không gộp vào 2–3 ngày công của G7.

### Tổng hợp phụ thuộc và ước lượng

```text
G0 → G1 → G2 → G3 → G4 ─┐
                 └→ G5 ─┴→ G6 → G7

Biên soạn/duyệt nội dung A: bắt đầu từ G0, song song, phải xong trước M2.
```

- Tổng ước lượng kỹ thuật/phân tích G0–G7: **17–30 ngày công**, chưa phải cam kết ngày hoàn thành.
- Biên soạn và rà soát 26 tuần: dự trù riêng, ban đầu khoảng **5–15+ ngày công**, hiệu chỉnh sau khi đo thời gian làm hai tuần pilot. Nếu tự sản xuất audio/video hoặc nhiều quiz, có thể tăng đáng kể.
- Nếu cần rút ngắn: ưu tiên M1, lịch cơ bản, nội dung pilot và chất lượng lưu tiến độ; dời biểu đồ nâng cao/animation sang sau.
- G4 và G5 chỉ làm song song khi đã chốt contract completion, enrollment version và quyền sở hữu dữ liệu; tránh hai AI cùng sửa quy tắc lõi.

## 12. Kiểm thử và tiêu chí hoàn thành toàn tính năng

### 12.1. Các kịch bản kiểm thử có giá trị cao

| Nhóm | Kịch bản | Kết quả mong đợi |
|---|---|---|
| Ownership | A gọi API note/enrollment/attempt của B | Không đọc/sửa được |
| Nội dung | Import cùng gói 2 lần | Không trùng dữ liệu |
| Transaction | Import lỗi ở bài cuối | Không còn gói nhập dở |
| Version | Publish A v2 khi A v1 đang học | V1 giữ nguyên bài và mẫu số |
| Revision FK | Hoàn thành activity ở revision khác | Bị từ chối |
| Completion | Bài có required và optional | Chỉ required quyết định hoàn thành |
| Completion | Buổi optional chứa activity required | Không đưa vào mẫu số tiến độ lộ trình |
| Enrollment | Undo sau hoàn thành, trước/sau khi bắt đầu lần mới | Mở lại an toàn hoặc trả 409 theo mục 4.7 |
| Retry | Complete hoặc submit lại do timeout | Một thay đổi/một attempt tương ứng |
| Concurrency | Complete/undo từ hai tab | Phát hiện xung đột, không mất thao tác âm thầm |
| Schedule | Bỏ ngày, nghỉ dài, học sớm, đổi lịch | Giữ thứ tự, không mất lịch sử |
| Timezone | Học gần 00:00, thay timezone | Ngày hoạt động đúng quy tắc đã chốt |
| Quiz | Không đạt, đạt, làm lại thấp hơn | Điểm và completion đúng policy |
| Review | Hai lần học lại cùng revision, cùng câu hỏi | Trạng thái ôn độc lập theo enrollment |
| Notes | Lưu lỗi, chuyển bài, session hết hạn | Không báo thành công giả hoặc mất text âm thầm |
| External media | Trang nguồn không cho embed | Có link mở trực tiếp và hướng dẫn tiếp tục |
| Backup | Restore vào DB kiểm thử | Khôi phục đủ dữ liệu cá nhân |
| Extensibility | Fixture B có số tuần/buổi khác | Hoạt động qua cùng API/UI |

### 12.2. Mức kiểm thử

- Unit test: tính tiến độ, thứ tự Hôm nay, lịch học, completion policy, normalization chấm quiz.
- Integration test trên PostgreSQL: constraints, transaction, owner queries, idempotency, optimistic locking, migrations.
- Component test: form, trạng thái lưu, điều hướng activity, xử lý auth/conflict.
- E2E: login → enroll → học → note → complete → reload; đổi lịch; quiz; đối chiếu hai user.
- Kiểm tra trực quan và accessibility: viewport 360/768/1440 px, không tràn ngang, focus rõ, dialog đóng bằng Escape, font IPA không lỗi, màu chữ đủ tương phản.

Mục tiêu hiệu năng ban đầu để đo ở G7: các API thường dùng p95 khoảng dưới 500 ms trên môi trường phát triển được ghi cấu hình, với bộ dữ liệu thử có vài nghìn activity. Đây là ngân sách kiểm thử đề xuất, không là kết quả đã đạt. Không tải toàn bộ nội dung 26 tuần để mở trang Hôm nay.

### 12.3. Definition of Done cho mỗi task

- Đạt acceptance criteria của task và quy tắc nghiệp vụ liên quan.
- BE validation/authorization đầy đủ; không chỉ dựa vào UI.
- Migration và contract được cập nhật nếu thay dữ liệu/API.
- UI có trạng thái loading, empty, error phù hợp.
- Test tập trung vào hành vi/rủi ro thực tế, không viết test chỉ lặp implementation.
- Chạy check phù hợp; báo rõ check nào chưa chạy và lý do.
- Không để mock data thành nguồn production; fixture được tách theo môi trường.
- README/ADR/backlog cập nhật tương ứng; có hướng dẫn demo để người điều phối kiểm tra.

## 13. Cấu trúc repository đề xuất

```text
tu-hoc-tieng-anh/
  KE_HOACH_DU_AN.md
  README.md
  frontend/
    src/
      app/                 # router, providers, layout
      features/
        auth/
        curricula/
        learning/
        scheduling/
        assessment/
        notes/
        review/
        progress/
      components/ui/
      lib/
      styles/
  backend/
    pom.xml
    mvnw, mvnw.cmd
    src/main/java/.../
      identity/
      curriculum/
      contentops/
      enrollment/
      scheduling/
      learning/
      assessment/
      notes/
      review/
      reporting/
      shared/              # chỉ các thành phần thực sự dùng chung
    src/main/resources/db/migration/
    src/test/
  content/
  infra/                   # cấu hình ứng dụng, không ghi đè hạ tầng DB có sẵn
  docs/
    adr/
    api/
    tasks/
    content-mapping.md
    test-plan.md
    runbook.md
```

Trong mỗi module backend, chia controller/application/domain/infrastructure khi quy mô cần; không tạo nhiều lớp rỗng chỉ để đủ tên. Frontend chia feature và component dùng chung; không có một component lớn xử lý toàn bộ lộ trình.

## 14. Cách giao việc cho AI khác

### 14.1. Quy trình điều phối

1. Đưa file này cho AI đọc và yêu cầu báo các điểm mâu thuẫn trước khi code.
2. Chọn một giai đoạn; tách thành task có phạm vi, dependency và nghiệm thu rõ.
3. Khóa contract API/data cho phần chuẩn bị giao song song.
4. Mỗi lượt giao một phần có thể demo và review. Tích hợp sau mỗi task, không chờ hoàn thành tất cả FE hoặc BE.
5. Chạy kiểm thử hành vi chính, cập nhật `docs/tasks/backlog.md` và ADR trước khi chuyển giai đoạn.
6. Người điều phối kiểm tra cả trải nghiệm học và quy tắc nghiệp vụ, không chỉ việc build thành công.

### 14.2. Mẫu task

```markdown
# Task G3-02 — Hoàn thành nhiệm vụ và lưu tiến độ

## Mục tiêu
Người học đánh dấu/hoàn tác một nhiệm vụ và dữ liệu vẫn đúng sau reload.

## Đầu vào
- KE_HOACH_DU_AN.md, nhất là mục 4, 7 và 8.
- OpenAPI đã chốt; G3-01 enrollment đã hoàn thành.

## Phạm vi
- API progress, kiểm tra owner và revision, transaction/version.
- Kết nối nút hoàn thành trên FE và invalidate dữ liệu liên quan.

## Ngoài phạm vi
- Quiz, thay đổi lịch, thay đổi published curriculum.

## Nghiệm thu
1. Complete → reload vẫn completed.
2. Retry không cộng đôi.
3. Undo cập nhật tiến độ bài/tuần/lộ trình.
4. User khác hoặc activity khác revision bị từ chối.
5. Mất mạng hiển thị lỗi và cho thử lại.

## Đầu ra
- Code, migration/contract nếu cần, test hành vi phù hợp.
- Danh sách check đã chạy, cách demo, điểm còn hạn chế.

## Ràng buộc
- Không hardcode Lộ trình A hoặc user ID.
- Không ghi đè Docker/DB hiện có.
- Không tự mở rộng sang tính năng ngoài phạm vi.
```

### 14.3. Các gói giao việc ban đầu

| Task | Nội dung | Phụ thuộc |
|---|---|---|
| G0-01 | Quy tắc học, trạng thái, content mapping, pilot | Tài liệu này |
| G0-02 | Wireframe, design tokens, API contract | G0-01 |
| G1-01 | Khảo sát WSL/DB, backend skeleton, migrations | G0 |
| G1-02 | Login/session/CSRF và kiểm thử owner | G1-01 |
| G1-03 | FE shell/sidebar/login, nối API | Contract + G1-02 để tích hợp |
| G2-01 | Content schema + importer + publish | G1-01 |
| G2-02 | API và UI catalog/outline | G2-01 |
| G3-01 | Enrollment + lịch đầu + Hôm nay | G2 |
| G3-02 | Completion/undo + summary | G3-01 |
| G3-03 | Lesson workspace + notes/response | G3-01, contract G3-02 |
| G3-04 | E2E vòng học và sửa các điểm vướng | G3-02/03 |

### 14.4. Prompt giao giai đoạn đầu

> Đọc KE_HOACH_DU_AN.md và khảo sát repository trước. Thực hiện G0: chốt các quy tắc nghiệp vụ, lập mapping nội dung và chuẩn bị pilot, wireframe bằng tài liệu, ADR và contract API dự kiến. Những nội dung đề xuất phải phân biệt với dữ liệu đã xác minh từ nguồn. Chưa xây các tính năng G1 trở đi. Khi hoàn thành, liệt kê đầu ra, điểm cần người điều phối quyết định và task tiếp theo đủ nhỏ để triển khai.

Khi giao giai đoạn sau, thay rõ ID/phạm vi và chỉ cho AI sửa các quyết định kiến trúc đã chốt khi có lý do được ghi vào ADR.

## 15. Mở rộng sau khi hoàn thiện chức năng lộ trình

| Tính năng sau này | Điểm nối đã chuẩn bị | Phần vẫn cần làm mới |
|---|---|---|
| Admin CMS | Import/publish dùng application service, draft/revision/role | UI editor, preview, audit, workflow duyệt |
| Nhiều người dùng | Ownership theo user, session, enrollment riêng | Đăng ký, khôi phục tài khoản, hạn chế abuse, vận hành |
| Lộ trình B/C | Catalog và nội dung không hardcode | Biên soạn nội dung, chất lượng học liệu |
| Flashcard/SRS | Activity tags, review module | Card state, thuật toán/version, lịch ôn và quota |
| Dictation | Activity type, attempt model | Media/transcript, quy tắc chấm, player |
| Ghi âm/nói | Practice response có ranh giới module | Quyền microphone, object storage, retention |
| Phát âm Anh-Anh tự chạy | `PronunciationProvider` theo mục 17, gắn activity/enrollment | Thử engine mã nguồn mở, kiểm chứng giọng người Việt, đóng gói worker |
| AI hỗ trợ học | Activity/attempt/response rõ nguồn | Consent dữ liệu, giới hạn chi phí, đánh giá chất lượng |
| PWA/offline | API/version/idempotency | Cache policy, hàng đợi đồng bộ, conflict resolution |
| Nhắc học | Schedule/timezone rõ ràng | Opt-in, scheduler, kênh gửi và chống trùng |

“Có thể mở rộng” nghĩa là dữ liệu và ranh giới trách nhiệm không khóa đường phát triển. Không có nghĩa những tính năng này đã sẵn sàng chỉ bằng bật một cờ.

## 16. Những quyết định cần rà lại tại các mốc

- **G0:** mức độ chi tiết của mỗi buổi, ai duyệt nội dung tiếng Anh, quiz nào bắt buộc, thời lượng học mong muốn. Mặc định hiện tại cho phép tiến hành pilot.
- **G1:** nơi chạy BE (Windows/WSL/container), thông số PostgreSQL hiện có, phiên bản dependency chính xác.
- **G3:** dùng thử có giúp bắt đầu học nhanh và quay lại bài dễ dàng không; sửa flow trước khi thêm nhiều tính năng.
- **G6:** nội dung A đầy đủ và hợp lệ để phát hành chưa; các link có truy cập được không; không đánh đồng xong code với xong giáo trình.
- **G7:** tiếp tục chạy cá nhân/local hay triển khai hosting; thay đổi vận hành và tài khoản theo lựa chọn thực tế.

**Thứ tự ưu tiên thực hiện:** dữ liệu đúng → học và lưu được → lịch phù hợp → phản hồi/ôn tập → nội dung đầy đủ → vận hành ổn định.

## 17. Bổ sung: luyện phát âm Anh-Anh bằng công cụ tự chạy

### 17.1. Quyết định và trạng thái

- Người dùng đã chọn Anh-Anh và xác nhận “mã nguồn mở tự chạy, ưu tiên không mất phí API”.
- Thiết kế mặc định không yêu cầu Azure hay dịch vụ cloud trả phí. Không tự chuyển sang cloud khi engine local gặp lỗi.
- **Lựa chọn triển khai bản thử:** xây adapter nhận dạng âm dựa trực tiếp trên Wav2Vec2 XLSR-53 + eSpeak NG British, phục vụ qua Python API nội bộ. Mimora là nguồn tham khảo cách triển khai, không là dependency bắt buộc. Đây là lựa chọn nền tảng cho PoC, chưa phải bộ chấm production đã được kiểm chứng; chi tiết ở mục 17.8.
- Spring Boot tiếp tục làm backend chính: tài khoản, bài học, lịch, tiến độ, quyền truy cập, lịch sử luyện và API cho FE. Python chỉ xử lý suy luận âm thanh.
- Phạm vi ban đầu là đọc từ/câu có sẵn. Chưa đưa chấm nói tự do, kết luận trình độ hoặc phán định lỗi phát âm chắc chắn vào bản đầu.

### 17.2. Các công nghệ cho bản thử

| Thành phần | Đề xuất | Vai trò |
|---|---|---|
| Thu âm | React + MediaRecorder | Thu, dừng, nghe lại; xử lý việc từ chối microphone |
| API chính | Spring Boot | Xác thực, giới hạn audio, lấy câu chuẩn từ activity, điều phối và lưu attempt |
| Speech worker | Python + FastAPI, Docker trong WSL | Chạy model local sau API nội bộ |
| Phone recognizer | PyTorch/Transformers với `facebook/wav2vec2-xlsr-53-espeak-cv-ft` làm ứng viên | Nhận dạng chuỗi âm từ audio |
| Âm tham chiếu | eSpeak NG/phonemizer theo British profile và từ điển ngoại lệ đã duyệt | Sinh/kiểm tra chuỗi âm mong đợi |
| Scoring | Căn chỉnh chuỗi phonetic tokens, xử lý biến thể và hiệu chỉnh trên tập thử riêng | Tạo gợi ý ôn; tham khảo Mimora, không mặc định kế thừa độ tin cậy của nó |
| Audio chuẩn | Bản ghi Anh-Anh có quyền dùng; hoặc model TTS offline phù hợp | Nghe mẫu; duyệt các mẫu đưa vào bài học |
| Persistence | PostgreSQL; file audio lưu riêng nếu người học chọn giữ | Lưu metadata, kết quả, phiên bản engine và lịch sử |

[Mimora](https://github.com/vikonix/Mimora) là ứng dụng desktop mã nguồn mở MIT, công bố hỗ trợ biến thể British. Cần tách/đóng gói engine thành service cho webapp; không coi nó là API web sẵn có. Tác giả mô tả cách chấm là heuristic, phụ thuộc giọng và microphone.

[Model card của Meta](https://huggingface.co/facebook/wav2vec2-xlsr-53-espeak-cv-ft) ghi license Apache-2.0, đầu vào 16 kHz, đầu ra là phonetic labels. Model nhận dạng âm chưa tự cung cấp thang điểm phát âm đã kiểm chứng. Giấy phép mã ứng dụng, dependency, model weights và audio mẫu phải được ghi riêng, giữ attribution khi tái sử dụng.

### 17.3. Luồng xử lý đề xuất

```text
React: nghe mẫu → thu âm → nghe lại → gửi chấm
  → Spring Boot: kiểm tra owner/activity, tạo attempt, lấy reference en-GB
    → worker Python: kiểm tra audio, chuẩn hóa, suy luận và đối chiếu
      → Spring Boot: lưu kết quả kèm engine/model/calibration version
        → React: gợi ý từ cần luyện, nghe lại đoạn, thử thêm lần nữa
```

Worker chỉ phục vụ backend qua mạng nội bộ, không tự truy cập bảng người dùng. Gói model được tải và pin phiên bản khi chuẩn bị môi trường; mục tiêu sau đó là suy luận được khi ngắt Internet. Không kèm LLM sinh câu hoặc dịch tự động trong worker đầu tiên vì bài học đã có câu chuẩn.

Thời lượng audio thử nghiệm đề xuất 5–20 giây/lượt. Giới hạn request, timeout, số job đồng thời và dung lượng file theo kết quả đo trên máy thật. Nếu cần xử lý bất đồng bộ, dùng bảng job cùng một worker trước; chưa cần message broker riêng.

### 17.4. Anh-Anh phải nhất quán ở cả chuỗi xử lý

- Gắn `targetLocale=en-GB` vào activity/attempt và reference profile có version.
- Kiểm tra giọng mẫu, từ điển/phonemizer, quy tắc so âm và calibration. Đổi giọng đọc TTS sang British chưa đủ nếu reference phones vẫn là Anh-Mỹ.
- Cho phép các cách đọc Anh-Anh hợp lệ trong nội dung đã duyệt; không ép mọi biến thể thành một chuỗi âm duy nhất.
- Không dùng một nhãn locale để khẳng định mô hình đã được huấn luyện/chấm chuẩn cho locale đó.
- Từ điển IPA hiển thị để học khác với kết luận về âm thực tế người học đã phát ra. Không tự gán nhãn “bạn đã đọc /t/ thay /θ/” nếu dữ liệu chưa hỗ trợ kết luận đó.

Lưu ý khi chọn engine khác: [registry của OpenPronounce](https://raw.githubusercontent.com/Halleck45/OpenPronounce/main/openpronounce/languages.py) đang đặt English reference là `en-us` tại thời điểm kiểm tra. Không lấy bản mặc định rồi gọi đó là bộ chấm Anh-Anh. Forced alignment chỉ hỗ trợ định vị đoạn âm theo nội dung; nó không tự chứng minh âm được phát ra đúng.

### 17.5. Phản hồi học tập và dữ liệu

- Ở bản thử, nhãn UI là “Gợi ý luyện phát âm — thử nghiệm”; ưu tiên từ cần nghe lại, audio mẫu và bản ghi của người học.
- Không xem độ giống waveform hoặc độ khớp transcript là tỷ lệ “phát âm chính xác”. Không dùng điểm chưa kiểm chứng để khóa bài/không cho hoàn thành.
- Audio không có tiếng nói, quá nhỏ hoặc lỗi xử lý trả trạng thái “Chưa đánh giá được”, không cho điểm 0 như một lỗi học tập.
- Tách `recording/submission completed` khỏi `pronunciation quality`; activity luyện tập có thể hoàn thành bằng việc thực hành dù chưa đủ điều kiện chấm.
- Lưu attempt theo owner/enrollment/activity, reference version, engine/model/calibration version, thời gian xử lý, trạng thái và kết quả. Các score không được hỗ trợ để `null`, không mặc định 0.
- Retry cùng attempt/idempotency key không tạo thêm lần luyện. Một job chỉ có một kết quả cuối; kết quả lỗi được giữ để chẩn đoán phù hợp.
- Không gộp điểm khác phiên bản engine/calibration thành đường biểu diễn cải thiện mà không có nhãn cảnh báo tính so sánh.
- Audio tạm được xóa theo thời hạn cấu hình sau xử lý. Chỉ lưu lâu dài để nghe lại khi có lựa chọn rõ trong app; người học có thể xóa bản ghi.

### 17.6. Gói việc bổ sung sau G3

| Task | Việc cần làm | Điều kiện nghiệm thu |
|---|---|---|
| PA-01 | Pin ứng viên, kiểm tra license/model, chạy British profile với audio mẫu được phép dùng | Nêu chính xác thành phần và cấu hình tạo reference Anh-Anh; chạy local sau khi tải model |
| PA-02 | Tập thử có người đọc Anh-Anh, người Việt, lỗi cố ý và audio nhiễu | Có nhãn người đánh giá, số báo sai/bỏ sót và giới hạn được ghi rõ; không chỉ tự chấm TTS |
| PA-03 | Đóng gói FastAPI và adapter `PronunciationProvider` ở Spring Boot | Worker không cần Internet lúc suy luận, không lộ API công khai, xử lý timeout/retry |
| PA-04 | UI nghe mẫu/thu âm/phản hồi/luyện lại và lưu attempt | Không mất bài đang học; lỗi audio không biến thành điểm sai; hoạt động trên trình duyệt mục tiêu |

Tập thử cần có các cặp âm dễ nhầm, phụ âm cuối, câu đọc thiếu/thừa từ và cách đọc Anh-Anh hợp lệ. Tách dữ liệu dùng điều chỉnh ngưỡng khỏi dữ liệu kiểm chứng; nếu có thể, tách cả người nói. Ghi precision/recall của gợi ý lỗi cùng số lượng mẫu, latency và RAM trên chính máy WSL. Các kết quả này mới là cơ sở quyết định có phát hành chức năng chấm hay chỉ giữ chế độ nghe–thu–nghe lại.

Không cần mặc định mua GPU. Thử CPU trước và đo thời gian thực tế; nếu không đáp ứng thì giảm model/tải hoặc xử lý nền. “Không phí API” vẫn có chi phí máy, điện, lưu trữ và công tích hợp. Gói PA là phạm vi bổ sung, **chưa nằm trong 17–30 ngày công của kế hoạch lõi**; ước lượng lại sau PA-01/02 thay vì cam kết chất lượng và thời gian khi chưa thử.

### 17.7. Đánh giá mức tin cậy trước khi chọn Mimora

Rà soát ngày 24/10/2026: **Mimora vẫn là ứng viên nghiên cứu, chưa được chọn làm bộ chấm chính thức.** Số star là tín hiệu về mức quan tâm, không thay thế bằng chứng chất lượng chấm. Chưa chạy ứng dụng, audit đầy đủ hoặc xác minh kết quả trên người Việt trong quá trình lập kế hoạch này.

- README công bố British và mô tả scoring là heuristic, phụ thuộc giọng/microphone. Tuyên bố hỗ trợ locale không tự chứng minh độ chính xác khi phát hiện lỗi.
- [Script hiệu chỉnh cá nhân](https://github.com/vikonix/Mimora/blob/main/pronunciation/phoneme/calibrate.py) điều chỉnh một mốc điểm từ các lượt luyện đã được hệ thống lọc. Đây không phải kiểm chứng độc lập bằng giáo viên chấm; cũng không đại diện cho toàn bộ quy trình hiệu chỉnh mô hình.
- Dùng model nền tảng có tài liệu không tự xác nhận chất lượng công thức quy đổi điểm, ngưỡng phát hiện lỗi hoặc trải nghiệm dạy phát âm của ứng dụng ghép model đó.
- Trước PA-03, cần báo cáo PA-02 cho thấy mức báo sai/bỏ sót trên dữ liệu giữ riêng và các biến thể Anh-Anh được chấp nhận. Nếu chưa đủ bằng chứng, tiếp tục coi kết quả là thử nghiệm và phát hành phần nghe mẫu/thu âm/nghe lại trước.
- Tích hợp qua adapter có contract của dự án, pin phiên bản; không để cấu trúc nội dung/tiến độ phụ thuộc trực tiếp vào API nội bộ của Mimora.

### 17.8. Lựa chọn sau khi đối chiếu Vosk và Wav2Vec2

**Chọn hướng Wav2Vec2 nhận dạng âm làm nền cho PoC.** Baseline là `facebook/wav2vec2-xlsr-53-espeak-cv-ft`; dùng `facebook/wav2vec2-lv-60-espeak-cv-ft` làm đối chứng nếu cần. Chưa có benchmark của dự án chứng minh model nào tốt hơn cho người Việt học Anh-Anh. Cả hai được công bố là model nhận dạng phonetic labels, không phải bộ chấm phát âm hoàn chỉnh: [XLSR-53](https://huggingface.co/facebook/wav2vec2-xlsr-53-espeak-cv-ft), [LV-60](https://huggingface.co/facebook/wav2vec2-lv-60-espeak-cv-ft).

- Vosk/vosk-browser có giá trị cho tính năng transcript offline hoặc đối chiếu nội dung câu đọc. Không đưa confidence từng từ thành điểm phát âm và chưa thêm Vosk vào PoC nếu chưa có nhu cầu transcript riêng. [Vosk](https://alphacephei.com/vosk/)
- Với reference Anh-Anh, dùng profile British của eSpeak NG và ngoại lệ được duyệt. Theo danh sách voice của công cụ, `en` là British; identifier thực tế phải kiểm tra bằng `espeak-ng --voices` ở phiên bản đã pin. `targetLocale=en-GB` của app cần ánh xạ sang identifier của từng thư viện, không mặc định chúng giống nhau. [eSpeak NG languages](https://github.com/espeak-ng/espeak-ng/blob/master/docs/languages.md)
- CMUdict định hướng US English nên không làm reference Anh-Anh mặc định. Chuyển ARPAbet sang IPA chỉ đổi hệ ký hiệu, không đổi mục tiêu accent. [CMUdict](https://github.com/cmusphinx/cmudict)
- Căn chỉnh bằng Levenshtein/Needleman–Wunsch là bước tìm khác biệt giữa các chuỗi, không tự xác định lỗi phát âm chắc chắn. Cần tokenize âm, xử lý âm ghép/dấu phụ, weak forms và biến thể hợp lệ trước khi so sánh; không tính edit distance theo từng ký tự Unicode rồi gọi là điểm chuẩn.
- Lỗi quan sát được có thể đến từ recognizer hoặc reference. PoC trả “khác biệt âm nghi ngờ/cần nghe lại”, giữ trạng thái không đánh giá được; chưa quy đổi edit distance thành phần trăm đúng hoặc xác suất phát âm đúng.
- Nếu cần gắn kết quả với từng từ/đoạn nghe lại, phải có bước căn chỉnh thời gian/từ được kiểm tra riêng; chuỗi phonetic labels đầu ra không tự bảo đảm các liên kết này.
- Tự xây phần ghép nối giúp kiểm soát dependency và dữ liệu, nhưng không loại bỏ công việc kiểm chứng giống khi dùng Mimora. PA-02 vẫn là cổng quyết định trước khi triển khai chức năng chấm cho người học.

Stack được giao cho AI: **React thu âm → Spring Boot quản lý attempt → Python/FastAPI + PyTorch/Transformers chạy XLSR-53 → reference British + căn chỉnh/hiệu chỉnh → phản hồi gợi ý luyện tập**. Mimora tiếp tục được dùng để đọc/tham khảo có attribution nếu tái sử dụng mã, không bắt buộc cài toàn bộ ứng dụng desktop.
