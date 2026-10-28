# Đánh giá mức sẵn sàng để tự học, tính năng và UI/UX

Ngày đánh giá: 28/10/2026, Asia/Ho_Chi_Minh. Baseline Git: `a78a875`, cộng thay đổi FE chưa commit đang có trong workspace.

## 1. Kết luận để quyết định

**Ứng dụng đã có vòng quản lý việc học hoạt động được. Lộ trình A hiện chưa đạt chất lượng để dùng làm giáo trình chính trong 26 tuần.** Bạn có thể dùng app để đọc tài liệu, lưu câu trả lời, ghi chú, theo dõi lịch và tiến độ. Tuy nhiên, cần bổ sung giáo trình thật và học liệu nghe dùng được trước khi tin vào kết quả học tập của khóa này.

Đây là đánh giá sản phẩm và nội dung, không phải xác nhận lại toàn bộ an toàn backend hoặc chứng nhận sư phạm. Chưa có bài đầu vào và mẫu nghe/nói của bạn nên chưa kết luận được trình độ cá nhân. “Bạn đã học được chưa” trong tài liệu này được hiểu là “app đã đủ để bạn học có ích chưa”.

Quyết định đề xuất: giữ nền tảng hiện tại; làm hoàn chỉnh 10 buổi đầu, sửa các thông tin gây hiểu nhầm, dùng thử rồi mới mở rộng. Không cần thêm thư viện đồ họa ở thời điểm này.

## 2. Bằng chứng đã thu thập

| Phạm vi | Đã kiểm tra | Giới hạn |
|---|---|---|
| Nguồn giáo trình | Quét và thống kê toàn bộ 26 JSON tuần, 130 Markdown, resources của `content/roadmap-a/v1`; đọc đối chiếu bài đầu/giữa/cuối và các câu quiz | Không coi tiêu đề, số lượng hoặc schema hợp lệ là bằng chứng chất lượng bài |
| Web thực tế | Đăng nhập; catalog; chi tiết A; đăng ký; Hôm nay; workspace; ghi chú; thực hành; hoàn thành; bookmark; ôn tập; tiến độ; lịch; preview/hủy đổi lịch; IPA; focus; sáng/tối | Xem trực tiếp ở viewport trình duyệt 1280×720; chưa phải ma trận 1440/1280, zoom và nhiều trình duyệt |
| Dữ liệu thử | Tạo đúng tài khoản `e2e_readiness_20261028`, đăng ký A và hoàn thành buổi đầu | Giữ lại fixture để tái hiện. Không sửa tiến độ, ghi chú hoặc mật khẩu tài khoản cá nhân |
| Tính bền vững | Reload workspace và quay lại sau khi sang trang khác | Câu trả lời, trạng thái hoàn thành và ghi chú audit được phục hồi |
| FE | Build production; lint; 5 file test liên quan | 41 test đạt; lint có cảnh báo cũ ở `EnrollmentDialog`; bundle JS 848.37 KB, gzip 246.60 KB |
| PDF | Đọc đủ 4 trang danh sách từ; đếm 416 trang sách ngữ pháp, render và xem 5 trang đầu | Sách ngữ pháp dạng scan, trích text không đáng tin; chưa đọc toàn bộ 416 trang |
| Nguồn ngoài | Tra cứu British Council level test; mở A1 listening/speaking, bài Ordering in a café và tài liệu MDN về voice language | Chưa xác minh toàn bộ 10 link trong gói; một video lỗi đã xác nhận trực tiếp |

Lệnh FE đã chạy, trong `frontend`: `npm run build`, `npm run lint`, `npm test -- src/features/learning/__tests__/TodayPage.test.tsx src/features/learning/__tests__/LessonWorkspacePage.test.tsx src/features/curricula/__tests__/Curricula.test.tsx src/features/review/__tests__/ReviewPage.test.tsx src/features/schedule/__tests__/SchedulePage.test.tsx`.

Log cục bộ: `frontend/review-readiness-build.log`, `frontend/review-readiness-lint.log`, `frontend/review-readiness-tests.log`. Không chạy full backend verify, PostgreSQL concurrency suite hoặc full Playwright ở lượt đánh giá này. Kiểm tra trình duyệt thủ công không được gọi là toàn bộ E2E suite đạt.

## 3. Các vấn đề cần sửa theo thứ tự

### P1 — 130 bài có cùng thân nội dung, chưa dạy đúng chủ đề

Toàn bộ 130/130 file Markdown có `Key Expression 1/2/3` và hai câu ví dụ chung giống nhau. Phần từ `## 2.` đến cuối của cả 130 file là cùng một nội dung. Tiêu đề, mục tiêu được chèn tên chủ đề và thời lượng thay đổi, nhưng phần giải thích/thực hành không thay đổi.

Ví dụ đối chiếu: `lessons/w01-l01-vowels.md`, `lessons/w02-l01-present-simple.md`, `lessons/w13-l05-stage2-comprehensive-quiz.md`, `lessons/w26-l05-final-graduation-assessment.md`, dưới `content/roadmap-a/v1`.

Đã xác nhận bài đầu được backend trả về trên web cũng chứa các placeholder này. Đây không chỉ là file nháp nằm ngoài ứng dụng. Người học mở bài về /iː/ và /ɪ/ nhưng không được giải thích sự khác nhau hay luyện phân biệt chúng.

**Cách sửa:** biên soạn từng bài có mục tiêu đo được, kiến thức riêng, ví dụ đúng trọng tâm, nhiệm vụ cụ thể, lời giải và học liệu đã nghe/xem. Ưu tiên 10 buổi đầu thay vì sinh lại hàng loạt 130 bài bằng một mẫu chung. Nội dung pilot tuần 1 chi tiết hơn và có thể làm điểm xuất phát, nhưng vẫn phải biên tập và kiểm tra âm mẫu.

### P1 — Học liệu nghe đầu tiên không dùng được

Video bài đầu `https://www.youtube.com/watch?v=3g5Y4fP3E7w` báo **Video unavailable / This video is unavailable** trong player. Các nguồn trong `resources.json` hiện có 10/10 trạng thái `UNVERIFIED_DRAFT`.

Chưa có căn cứ nói cả 10 link đều hỏng. Lỗi fetch của công cụ web đối với link khác cũng không đủ để kết luận link chết.

**Cách sửa:** chọn lại nguồn theo đúng bài; mở xem và nghe; xác nhận nội dung và quyền dùng trước khi đánh dấu verified. Mỗi bài nghe cần hướng dẫn nghe để làm gì và câu hỏi tương ứng. Có phương án chuyển sang nguồn dự phòng khi video mất. Nhúng YouTube không bảo đảm dùng offline hoặc nguồn tồn tại mãi.

### P1 — Lời giới thiệu đang hứa nhiều hơn khả năng thực tế

Catalog hiển thị “Thẩm định chuẩn sư phạm quốc tế”, “Học liệu BBC & Oxford RP”, “Khuyên học viên bắt đầu”. Bảng IPA khẳng định giọng SpeechSynthesis chất lượng cao chuẩn RP. Chi tiết giáo trình lại có cảnh báo nguồn chưa xác minh. Các thông tin này mâu thuẫn nhau.

Trong code, `getBritishVoice()` ưu tiên en-GB nhưng có fallback sang bất kỳ giọng tiếng Anh; `speakBritish()` không kiểm chứng RP. `IpaSoundboardModal` phát **từ ví dụ**, không phát âm vị độc lập. Ở các vị trí khác, `PronounceButton` còn nhận nguyên tiêu đề tiếng Việt rồi đọc bằng giọng tiếng Anh.

**Cách sửa:** dùng nhãn “Nghe từ ví dụ”, hiển thị giọng thực tế, báo thiếu en-GB thay vì âm thầm gọi fallback là RP; bỏ loa ở tiêu đề tiếng Việt. Ghi rõ đây là giọng tổng hợp tham khảo. Nếu chưa có bằng chứng thẩm định, bỏ lời khẳng định tương ứng. Không cần bỏ định hướng Anh-Anh, nhưng phải mô tả đúng mức kiểm chứng.

### P1 về kỳ vọng học — Hoàn thành nhiệm vụ không phải thành thạo

Gói A có 406 hoạt động: 130 READING, 126 PRACTICE, 130 REFLECTION, 16 EXTERNAL_RESOURCE, 4 QUIZ. Chỉ có 16 câu quiz, 4 câu mỗi mốc ở tuần 1, 8, 13, 26. Không có activity LISTENING, RECORDING hoặc DICTATION trong gói này; điều đó không có nghĩa không được tự luyện nghe ngoài app.

Thử thực tế: nộp văn bản cho PRACTICE được ghi nhận hoàn thành, nhưng không có sửa ngữ pháp, giải thích đúng/sai hay đánh giá âm thanh. `ActivityRenderer` vẫn xác định RECORDING/DICTATION là chưa được hỗ trợ. Nút nghe là text-to-speech; không phải nhận diện hay chấm phát âm.

**Cách sửa:** vẫn cho phép tự xác nhận, nhưng phân biệt “đã làm” và “đã kiểm tra”. Bổ sung quiz theo bài, nghe hiểu và tự đối chiếu bài nói. Ẩn điểm năng lực khi chưa đo được; không đổi 100% tiến độ thành thông báo đã thành thạo Anh-Anh.

### P2 — Nhãn “6 tháng / 26 tuần” không khớp lịch cá nhân mặc định

Đăng ký ngày 28/10/2026 với mặc định Thứ Hai/Tư/Sáu, một bài mỗi ngày: lịch cuối hiển thị **25/08/2027**. 130 buổi ở 3 buổi/tuần cần khoảng 43–44 tuần, trong khi 5 buổi/tuần mới gần 26 tuần. Không coi đây là lỗi thuật toán xếp ngày; vấn đề nằm ở cách giới thiệu và kỳ vọng.

**Cách sửa:** gọi là “26 chặng / 130 buổi”, hiển thị lịch dự kiến ngay trong form đăng ký. Nếu vẫn dùng tên 26 tuần, ghi rõ tương ứng nhịp 5 buổi/tuần. Không thay lựa chọn ngày cá nhân một cách tự động.

### P2 — Bảng IPA thiếu hành vi modal cơ bản

Mở bảng từ Hôm nay, nhấn Escape: bảng không đóng, focus vẫn ở nút mở ngoài modal. Code chỉ có role/aria-modal và click backdrop; chưa có focus trap, focus ban đầu, Escape hay trả focus. Với người dùng bàn phím, đây là hành vi khó dùng thật sự.

**Cách sửa:** dùng một Dialog dùng chung có quản lý focus; tái sử dụng cách đang làm trong enrollment/blocker. Nếu cần thư viện cho nhiều modal, chọn một primitive dialog thống nhất sau khi kiểm tra dependency; không trộn thêm nhiều bộ UI. Có thể sửa modal hiện tại bằng code sẵn có trước.

### P2 — Nhãn lưu và số liệu lịch sử cần đối soát

Ngay sau nộp PRACTICE/REFLECTION thành công, thẻ hiển thị “Đã hoàn thành” và “Cập nhật câu trả lời” nhưng đồng thời có “Đã lưu nháp”. Reload thì dòng nháp biến mất. Nên dùng riêng “Đã nộp” và “Đã lưu nháp”.

Trong trang tiến độ của tài khoản audit, lịch sử ngày có 3 dòng hoàn thành từ đọc, thực hành, phản tư, nhưng badge hiển thị “1 nhiệm vụ xong”. Chưa truy nguyên backend ở lượt này: cần đối soát ý nghĩa `completedActivitiesCount` và event trả về, không sửa số bằng cách đếm DOM. Nếu con số thực tế chỉ đếm một loại event, đổi nhãn hoặc cách tính cho đúng.

## 4. Đánh giá từng tính năng theo giá trị học tập

| Tính năng | Giá trị đang có | Phần nên bổ sung |
|---|---|---|
| Tài khoản, lưu tiến độ | Đăng nhập được, buổi đã hoàn thành còn sau reload | Không cần mở rộng social/admin lúc này |
| Hôm nay | Có một hành động chính, cập nhật bài tiếp sau hoàn thành | Thêm “hôm nay đã học gì” và lượng ôn đến hạn; dùng tiếng Việt cho ACTIVE |
| Đăng ký | Chọn ngày, số phút, timezone rõ; tạo lịch thật | Xem trước ngày kết thúc và nhịp học; đừng gợi ý khóa chưa sẵn sàng |
| Workspace | Markdown, ghi chú bên cạnh, autosave, response, bookmark, focus | Sắp thành bước học ngắn; đưa âm mẫu/câu hỏi gần ví dụ; giảm cuộn |
| Ghi chú | Lưu và phục hồi thực tế, không mất nội dung khi quay lại | Đổi “700ms” thành “Tự động lưu”; trạng thái lưu/lỗi nhìn rõ |
| Quiz | Có hạ tầng chấm server và ôn câu sai trong source | Nội dung quá ít; chưa chạy lại luồng quiz thực tế trong lượt này |
| Ôn tập | Bookmark thực tế xuất hiện sau đánh dấu; câu sai có phần riêng | Không phải SRS hiện tại: cần ngày ôn, mức nhớ và lịch tái xuất hiện |
| Lịch | Buổi xong được đánh dấu; preview/hủy hoạt động | Modal preview 129 dòng quá dài; summary + vài dòng đầu + xem toàn bộ |
| Pause/resume/abandon | Có chức năng và giao diện trong code | Chưa thực hiện lại lượt này; để “Kết thúc” xa hành động thường dùng |
| Tiến độ | Sau buổi đầu có 1/130 bài, 3/390 nhiệm vụ, 0.8%, streak 1 ngày | Phân biệt thời lượng nội dung với thời gian học đo được và năng lực |
| Phát âm | Có từ ví dụ để nghe nhanh | Chưa có thu âm/nghe lại/chấm âm vị; chất lượng voice chưa được nghe thẩm định |
| Giáo trình | Có khung chủ đề và importer/versioning | Cần nội dung thật, thẩm định link, đáp án và năng lực đầu ra |

Tổng thời lượng ước tính của 130 buổi là **2.970 phút (~49,5 giờ)**. Đây là tổng metadata trong gói, không phải số giờ bạn đã thực học hay bằng chứng đạt một bậc CEFR.

## 5. Đánh giá UI/UX chi tiết trên desktop

### Giữ lại những điểm đang tốt

- Sidebar trái có nhãn rõ và thứ tự hợp lý: Hôm nay → Lộ trình → Lịch → Ôn tập → Tiến độ → Tùy chọn.
- Một nút bắt đầu học nổi bật; màn Hôm nay sau khi đăng ký không biến thành dashboard quá nhiều biểu đồ.
- Màu và card dùng design token tương đối nhất quán; có sáng/tối. Không cần thay cả design system.
- Ghi chú nằm cạnh bài; chế độ tập trung mở được và Escape thoát được trong thử nghiệm.
- Trạng thái hoàn thành và khả năng quay lại bài đều hiển thị được. Luồng đăng ký → học → ghi chú → hoàn thành → lịch/tiến độ chạy qua được.

### Điều hướng và thứ bậc thông tin

- Catalog đang giống trang quảng bá: hero dài, badge RP lặp lại, lời khẳng định thẩm định không có bằng chứng. Dùng thẻ ngắn: phù hợp ai, cần biết trước gì, học xong làm gì, nhịp học, trạng thái nội dung.
- Mục lục mở mặc định toàn bộ 26 tuần và 130 buổi, khiến trang rất dài. Mở chặng hiện tại/đầu tiên; chặng khác thu gọn, có tìm bài theo tên.
- Loa xuất hiện cạnh gần mọi tiêu đề tiếng Việt gây nhiễu và không hỗ trợ phát âm tiếng Anh. Giữ loa cạnh từ/câu tiếng Anh có thể nghe được.
- Cụm “Tuần 1: Tuần 1: ...” lặp nhãn. Chỉ render số tuần một lần; dịch “Phase” và enum trạng thái sang nhãn người học.
- Trên Hôm nay, tiêu đề lộ trình, dropdown lộ trình và card tiến độ đang nhắc cùng một tên rất dài. Chỉ cần một tên và một thao tác đổi lộ trình.

### Trải nghiệm đọc và làm bài

- Workspace lặp h1 ở đầu trang và trong Markdown, chiếm chỗ của nội dung. Giữ một h1, thân bài bắt đầu bằng mục tiêu ngắn và ví dụ.
- Tại 1280×720, bài đọc phải đi qua nhiều phần mở đầu trước khi đến nhiệm vụ. Cần nhìn thấy bước học hiện tại, thao tác chính và tiến độ buổi mà không cuộn quá nhiều.
- Cột ghi chú chiếm khoảng một phần ba bề rộng nhưng chỉ có textarea nhỏ. Cho thu gọn dễ thấy; ở chế độ tập trung có thể mặc định đọc 1 cột và mở ghi chú khi cần, nhớ lựa chọn cá nhân.
- Chữ nội dung nên 16–18px, line-height khoảng 1.6–1.75, chiều dài dòng khoảng 65–80 ký tự. Không dùng cỡ 10–11px cho hướng dẫn cần đọc hoặc thông tin quyết định học.
- IPA nên có font fallback hỗ trợ đủ ký tự và cỡ đọc tương đương từ mẫu. Không dùng phông trang trí hoặc monospace nhỏ làm nguồn phân biệt những âm rất gần nhau.
- Bài dài nên có bước “Học mẫu → Nghe → Thực hành → Kiểm tra”; ưu tiên hiển thị câu hỏi gắn với kiến thức, thay vì biến trải nghiệm thành tích checkbox.

### Màu, chuyển động, tương tác

- Dark mode tạo cảm giác tập trung nhưng nhiều đường viền và chữ phụ ít nổi. Cần đo contrast các token quan trọng trước khi tuyên bố WCAG; lượt này chưa đo đủ.
- Light mode đọc dễ theo ảnh quan sát; tiêu đề dùng font đậm khá nhiều. Hạn chế font display ở heading lớn, thân bài dùng sans đơn giản.
- Confetti được gọi khi mount một bài đã hoàn thành, nên có thể ăn mừng lại khi chỉ mở xem. Chỉ kích hoạt lúc chuyển từ chưa xong sang xong; tôn trọng reduced motion.
- Focus mode hữu ích; cần kiểm tra tiếp thứ tự Tab có còn đi vào sidebar nằm phía sau overlay không. Không kết luận accessibility hoàn chỉnh chỉ vì nhìn không còn sidebar.
- Nút Kết thúc lộ trình nên đưa vào menu phụ và ghi rõ “Dừng lộ trình”/ý nghĩa; giữ xác nhận có hậu quả cụ thể. Không để cạnh thao tác đổi lịch như một lựa chọn ngang hàng thường dùng.
- Video phải hiện nguồn thực tế, không gắn badge “BBC Learning English” cố định cho mọi YouTube URL. Có lựa chọn mở nguồn và báo lỗi rõ khi player không phát được.

### Tốc độ

Build hiện tạo một JS chunk 848,37 KB, gzip 246,60 KB, có cảnh báo chunk >500 KB. Đây là dấu hiệu nên lazy-load theo route hoặc phần nặng (quiz/Markdown/confetti), không đủ để nói app đang chậm vì chưa đo cold-load/LCP/INP. Không thêm animation/chart library để xử lý vấn đề chủ yếu là nội dung và thứ bậc thông tin.

## 6. Cách dùng tài liệu trong tailieu

- **Sách Giải thích ngữ pháp Tiếng Anh – Mai Lan Hương:** phù hợp làm sách tra cứu theo chủ đề. Mục lục có danh từ, đại từ, mạo từ, thì, so sánh, lượng từ, bài tập và đáp án. Không ép người mới đọc tuần tự 416 trang; không coi đây là giáo trình luyện nghe/nói.
- **PDF “100 từ phổ biến”:** bốn trang, số thứ tự thực tế kết thúc ở 96, có từ lặp theo từ loại. Có các ví dụ cần biên tập, như “I have a bad news” và “I go to school bus”; lần lượt nên sửa thành “I have some bad news” và “I go to school by bus”. Không nhập nguyên bộ như dữ liệu chuẩn đã xác minh.
- Tận dụng chủ đề và ý tưởng bài tập; viết lời giải và ví dụ riêng, kiểm tra lại ngữ âm. Việc có file PDF không tự chứng minh quyền phân phối công khai toàn bộ sách. Bản đề xuất không chép toàn văn hay đưa PDF lên web.

Nguồn ngoài đã kiểm tra: [British Council level test](https://learnenglish.britishcouncil.org/level), [A1 Listening](https://learnenglish.britishcouncil.org/free-resources/listening/a1), [A1 Speaking](https://learnenglish.britishcouncil.org/free-resources/speaking/a1), [Ordering in a café](https://learnenglish.britishcouncil.org/free-resources/listening/a1/ordering-cafe), [phonemic chart](https://www.teachingenglish.org.uk/teaching-resources/teaching-secondary/teaching-tools/phonemic-chart-english-teachers?hasFlash=true&page=1), [MDN voice language](https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesisVoice/lang). Trang level test cho chỉ báo gần đúng; nhãn ngôn ngữ voice không phải chứng chỉ giọng RP.

## 7. Kế hoạch sửa để sớm học được thật

| Đợt | Làm gì | Điều kiện kết thúc |
|---|---|---|
| R1: Sự trung thực và thao tác | Bỏ claim chưa kiểm chứng; sửa loa/voice fallback; IPA modal; nhãn đã nộp; ngày kết thúc ở đăng ký | Người học hiểu được giới hạn, dùng bàn phím được, không nghe tiêu đề Việt bằng giọng Anh |
| R2: 10 buổi học thật | Biên soạn theo tài liệu lộ trình kèm theo; học liệu phát được, bài tập có đáp án/giải thích; không placeholder | Người mới thực hiện được từng nhiệm vụ mà không phải đoán đề; kiểm tra nội dung tất cả 10 buổi |
| R3: Dùng thử và phản hồi | Học 5 buổi thực tế, ghi lại điểm vướng; sửa thứ bậc workspace; đối soát số liệu lịch sử | Một người học tự hoàn tất buổi mà không cần được hướng dẫn qua chat |
| R4: Luyện và ghi nhớ | Nghe có transcript/ẩn đáp án; thu âm nghe lại; lịch ôn đơn giản | Phân biệt tự đánh giá, chấm quiz, và chưa được chấm phát âm |
| R5: Mở rộng khóa | Xuất bản từng cụm bài đã kiểm chứng, không đợi/sinh hàng loạt 26 tuần | Nội dung tiếp nối có cùng tiêu chuẩn chất lượng, học liệu vẫn hoạt động |

Kỹ thuật xuất bản: revision đã publish không sửa tại chỗ; không đổi migrations đã áp dụng. Có thể tạo curriculum mới `foundation-en-gb` cho lộ trình đề xuất, để A hiện tại là bản khung/thử nghiệm; không tự chuyển enrollment hoặc tiến độ cũ. Nội dung mới phải qua validate → dry-run → import → kiểm tra → publish theo công cụ đang có. Nhãn draft trong nguồn không tự bảo đảm nội dung sẽ bị chặn publish; cần trạng thái hiển thị trung thực.

Tiêu chí kiểm tra gọn cho mỗi đợt: FE build + lint khi có FE, test nhóm bị ảnh hưởng một lần, một luồng thật xuyên FE–BE–DB. Chỉ chạy DB integration mới khi sửa transaction/SQL/quyền/concurrency. Nội dung cần người đọc/nghe kiểm tra thật, không thêm nhiều test chỉ để đếm file và coi đó là chất lượng giảng dạy.

Thứ tự đề xuất: **R1 → R2 → R3**, rồi mới quyết định tiếp R4. Không cần nâng thư viện, admin, mobile, nhận diện âm vị hoặc gamification lớn để bắt đầu học có ích.
