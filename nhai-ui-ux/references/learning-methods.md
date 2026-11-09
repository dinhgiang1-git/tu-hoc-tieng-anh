# Giao diện theo phương pháp học

Đọc khi xây bài học tương tác hoặc tham chiếu Belish vocabulary. Dùng cùng hệ chữ/màu/primitive; mỗi phương pháp có cấu trúc, thao tác và phản hồi phù hợp mục tiêu. Không dùng thanh màu trang trí cạnh heading/card, không nhân khung lồng nhau để tạo cảm giác nhiều chức năng.

## 1. Bằng chứng từ nguồn

Khảo sát ngày 09/11/2026, desktop 1280×720, chưa đăng nhập, phiên Dark và giao diện vẽ tay. Các nguồn đã mở:

- [Bài từ vựng A1](https://belish.net/vi/vocabulary/a1-nen-tang/chapter-1-lesson-1): năm mode Flashcard, Trắc nghiệm, Gõ từ vựng, Đọc hiểu, Luyện nghe. Đã lật flashcard, thử một lựa chọn sai ở quiz rồi tiếp câu sau; gõ một từ đúng và kiểm tra; xem câu khuyết của Đọc hiểu và giao diện nghe có ô nhập.
- [Shadowing](https://belish.net/vi/shadowing) → [bài học thử A1](https://belish.net/vi/shadowing/cap-do-14/57990027-7fff-40dd-9edd-35275a9664c1?from=overview): đã xem video, bản chép theo đoạn, controls trước/phát lại/tiếp, auto-pause, tốc độ, chọn mode Nghe–Viết chính tả và submode Ghép câu.

Chỉ thử tương tác công khai trong phiên khách. Không đăng nhập, hoàn thành cả lượt học, kiểm lưu server, mua Premium, kiểm microphone hoặc chấm phát âm. Chưa xác minh chất lượng/ngôn ngữ âm thanh, độ chính xác timestamp hay hiệu quả sư phạm. Mode “Đọc hiểu” của bài từ vựng đã xem là **câu khuyết chọn từ**, không phải bằng chứng nguồn có bài đọc dài trong mode đó. Bản chép Shadowing được quan sát, chưa xác nhận phát/loop có chạy chính xác bằng âm thanh.

## 2. Điều học và điều điều chỉnh

| Quan sát | Áp dụng |
|---|---|
| Một bộ chọn mode cho cùng bài, mỗi mode thay nội dung làm việc | Có phương pháp được chọn rõ, một vùng tương tác chính; đặt bộ chọn trước vùng làm bài để không phải cuộn xuống mới đổi được |
| Quiz từng câu, đề lớn, lựa chọn ngắn theo lưới 2×2, phản hồi đúng/sai rồi Tiếp tục | Từng câu giúp tập trung; lựa chọn dài dùng một cột. Thời điểm phản hồi phụ thuộc chính sách thật, không sao chép auto-grade vào bài kiểm tra cuối lượt |
| Flashcard lật để thấy nghĩa và câu ví dụ, nút nhớ/chưa nhớ riêng | Tách xem lời giải khỏi tự đánh giá; lật hoặc phát âm không tự đánh dấu đã thuộc |
| Gõ từ có nghĩa làm prompt và một ô nhập | Giao diện recall khác multiple choice; chỉ chấm khi có answer policy, không coi textarea bất kỳ là bài gõ từ |
| Đọc hiểu đặt từ cần điền vào một câu ngữ cảnh | Ngữ cảnh ở cạnh câu hỏi; không đổi mọi READING thành bài khuyết |
| Luyện nghe có phát lại và đầu vào; Shadowing có bản chép theo đoạn | Nghe/viết và bắt chước phát âm cần vùng làm việc khác nhau; dữ liệu audio/transcript/timestamp là điều kiện để có controls thật |
| Ghép câu có ngân hàng từ và vùng câu đã chọn | Dùng chọn/bỏ từ có thể thao tác bằng bàn phím; không bắt kéo thả |

Không học theo các hạn chế nhìn thấy: giữ khoảng trắng lớn khiến đáp án nằm dưới fold, bảng từ và bản dịch có thể làm lộ lời giải khi luyện, tên âm thanh có transcript trong accessible name của bài nghe, nhãn đùa khi sai, quá nhiều khung và chat nổi cạnh lựa chọn. Đây là đánh giá từ giao diện đã thấy, chưa là kiểm toán accessibility đầy đủ của nguồn. Không dùng loại từ/IPA, thứ tự từ, số lượng câu hoặc quy tắc mở khóa của nguồn làm dữ liệu chuẩn cho sản phẩm khác.

## 3. Hợp đồng giao diện chung

- Header gọn: tên nhiệm vụ, phương pháp, vị trí thật trong bài. Trọng tâm đầu màn là prompt và action, không là banner/thống kê/chính sách kỹ thuật.
- Bộ chuyển chỉ có phương pháp được hỗ trợ bởi dữ liệu hiện có. Cùng bài có thể chỉ có hai phương pháp; không dựng đủ năm tab từ ảnh nguồn. Chọn phương pháp không gửi mutation hoàn thành, không reset lượt đang làm âm thầm.
- Shared primitive: Button, Field, lựa chọn, feedback, progress. Feature sở hữu renderer riêng cho từng phương pháp; không dựng engine generic phức tạp hay mỗi phương pháp một bộ Button.
- State có scope `(enrollmentId, lessonId, activityId, attempt)` hoặc scope tương ứng của sản phẩm. Giữ selection, text, vị trí câu, mặt thẻ, playback position hợp lý khi đổi view/theme; reset chỉ khi người dùng bắt đầu lượt mới hoặc scope đổi. Thông báo trước hành động làm mất phần chưa nộp.
- Bố cục ưu tiên desktop 1280×720/1440×900. Prompt/lựa chọn/action nhìn thấy sớm với câu ngắn; văn bản dài được cuộn tự nhiên, không cắt câu/đáp án để giữ chiều cao cố định.
- Một lớp surface chính; control lựa chọn có viền khi cần. Phân cấp bằng chữ/spacing. Feedback dùng nhãn/icon và màu có chức năng, không dải mép card. Giữ focus, reduced-motion và mode đã lưu.
- Loading/error/empty/read-only/pending là state thật; lỗi không xóa câu trả lời. Lấy điểm, quyền, version, completion từ server. Không lộ đáp án trong data/DOM/ARIA trước khi policy cho phép.

## 4. Recipe riêng cho từng phương pháp

### Trắc nghiệm một lựa chọn

```text
Kiểm tra kiến thức             Câu 2 / 8 · Đã trả lời 1 / 8

              Đề bài đầy đủ, dễ đọc

 [A  Lựa chọn]                 [B  Lựa chọn]
 [C  Lựa chọn]                 [D  Lựa chọn]

 [Câu trước]   [Để làm sau]                      [Câu tiếp]
```

Không ép đúng bốn đáp án; lấy số/thứ tự từ dữ liệu. Hai cột khi nhãn ngắn và đủ rộng, một cột với câu dài/zoom. Toàn vùng nhãn kích hoạt được; radio có group name theo questionId, fieldset/legend hoặc radiogroup được gắn label. Trạng thái idle/selected/correct/incorrect/disabled khác nhau, có chữ/icon khi feedback; không chỉ màu.

Chọn một phương án không tự chuyển câu. Câu trước/tiếp giữ lựa chọn; “để làm sau” chỉ đổi vị trí, không tính là đã trả lời hoặc tự ghi sai. Có cách xem câu chưa làm, quay lại sửa trước nộp. Chỉ hiện bộ điều hướng số câu khi đủ ích; không mặc định grid lớn ngay đầu bài.

**Bài kiểm tra:** chọn → điều hướng → rà soát → nộp toàn bộ → kết quả/explanation chuẩn server. Trước nộp không đổi lựa chọn thành xanh/đỏ, không có đáp án đúng trong markup. Nộp pending chặn gửi trùng và khóa chỉnh, lỗi giữ answers; 409 giữ draft và đồng bộ theo contract. Kết quả cho phép xem từng câu sai/đúng và làm lại bằng thao tác rõ ràng.

**Luyện nhận biết có phản hồi ngay:** chọn/kiểm tra → phản hồi → chủ động Tiếp tục. Chỉ có khi endpoint/contract cho phép chấm từng item và xác định điểm/attempt; không gọi submit toàn quiz cho mỗi câu, không tải answer key để giả chấm phía FE. Source dùng phản hồi ngay, nhưng đó không phải mặc định cho quiz của mọi app.

### Flashcard — ghi nhớ và tự đánh giá

Một thẻ trung tâm. Mặt trước term/IPA hoặc prompt; mặt sau nghĩa, ví dụ và bản dịch theo dữ liệu. Nút “Xem nghĩa/Lật thẻ” truy cập được; nút phát âm riêng, không làm lật vô ý. “Chưa nhớ/Đã nhớ” chỉ ghi nhận khi chủ động chọn; không dùng làm điểm năng lực hoặc SRS nếu không có backend hỗ trợ. Mặt không hoạt động không được đọc/Tab; reduced-motion đổi mặt trực tiếp. Không lấy bảng từ đầy đủ đặt ngay cạnh bài recall đang cần giấu đáp án.

### Gõ từ — nhớ lại chủ động

Prompt nghĩa hoặc ngữ cảnh → một input có label → Kiểm tra → feedback/cách sửa → Tiếp tục. Giữ IME, không chấm lúc đang composition. Quy tắc khoảng trắng/chữ hoa/dấu câu/biến thể đáp án phải có nguồn và nhất quán server; không tự bỏ mọi dấu hay chấp nhận gần đúng. Chưa có answer contract thì chỉ có bài trả lời tự do, không giả “Chính xác”.

### Đọc hiểu/ngữ pháp — câu khuyết hoặc đoạn đọc

Câu khuyết: giữ nguyên ngữ cảnh và vị trí blank, lựa chọn gần câu; không sửa nội dung bằng cách tách chuỗi tùy tiện. Đọc dài: đoạn đọc 65–78ch và câu hỏi bên cạnh nếu đủ ngang hoặc ngay dưới, giữ đoạn đọc khi chuyển câu. Bản dịch/gợi ý có thể disclosure trong chế độ luyện; bài kiểm tra theo policy ẩn đáp án không để bản dịch tiết lộ. Đọc tài liệu không có câu hỏi vẫn là READING, không tự tạo quiz.

### Nghe/chép chính tả

Player replay/tốc độ khi được nguồn hỗ trợ → prompt không chứa transcript → input/câu đang làm → Kiểm tra → feedback. Transcript có nút mở sau kiểm tra hoặc theo policy luyện. Accessible name của nút phát mô tả hành động, không chứa chính câu cần chép. Audio chưa tải/không phát được có retry và nguồn thay thế hợp lệ. Không tự phát âm thanh; phân biệt âm mẫu thật và TTS tham khảo.

### Shadowing và ghép câu

Shadowing: player + câu/đoạn đang chọn, trước/phát lại/tiếp, tốc độ/loop chỉ khi có media adapter và timestamp. Bản chép/IPA/bản dịch có thể bật tắt theo dữ liệu; không tạo timestamp giả từ chia đều thời lượng video. Không gắn điểm phát âm khi chưa có ghi âm/chấm thật; xin microphone chỉ khi người dùng chủ động dùng ghi âm.

Ghép câu: vùng câu đã ghép + ngân hàng token; chọn/bỏ/đổi vị trí bằng controls bàn phím. ID token theo từng occurrence để hai từ giống nhau không mất một bản. Đáp án hợp lệ và chấm phải do dữ liệu/contract, không dùng renderer quiz một lựa chọn để giả bài ghép.

### Thực hành trả lời và phản tư

Đề cụ thể + editor/textarea + trạng thái lưu + hành động theo policy. Phản tư không có đúng/sai giả. Giữ draft/autosave/409/blocker hiện có; không ép loại này vào giao diện flashcard hoặc màn bốn lựa chọn.

## 5. Điều kiện dữ liệu và nghiệm thu

Trước triển khai, map từng recipe vào dữ liệu thật. Flashcard cần term/meaning/example; gõ từ cần accepted answers và normalization; nghe cần audio, đáp án/feedback có quyền truy cập phù hợp; shadowing cần segments có ID/timestamp; quiz cần options và result sau nộp. Không suy mode từ regex title/Markdown hoặc coi PRACTICE nghĩa là flashcard. Thiếu contract thì làm phương pháp đang có trước, tách phần data thành task rõ; không render tab hoạt động giả.

Nghiệm thu bằng một nội dung thật mỗi phương pháp đã triển khai; xem câu dài, số options khác bốn, unselected/selected/result/pending/error/read-only, giữ draft khi đổi câu/method/theme và hành vi bàn phím. Với quiz kiểm nộp toàn bộ đúng một lần, IDs/version/key đúng, không có đáp án trước nộp. Test business/concurrency/SQL theo AGENTS; không thêm suite chỉ kiểm màu/class. Báo riêng phương pháp đã chạy, mới thiết kế, còn thiếu backend và phần nguồn chưa kiểm.
