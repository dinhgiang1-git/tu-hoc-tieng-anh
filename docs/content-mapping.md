# Bản đồ đối chiếu nội dung và Chiến lược biên soạn (Content Mapping & Authoring Strategy)

**Ngày lập:** 24/10/2026 (Cập nhật sau G0-Review: 24/10/2026)  
**Trạng thái:** Bản chuẩn hóa G0-FIX — Đã sửa R1 và R8  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../KE_HOACH_DU_AN.md), [product-rules.md](./product-rules.md), [G0_REVIEW.md](./G0_REVIEW.md)

---

## 1. Nguyên tắc sử dụng nguồn và Phân định quyền sở hữu trí tuệ

Nguồn tài liệu tham khảo ban đầu: [Google Sheet lộ trình tiếng Anh 6 tháng](https://docs.google.com/spreadsheets/d/1Yim4AAiIfHMmNTrPsTXQtGfAkhsMcsBc_6-eURCgFEU/edit#gid=0).

### 1.1. Tình trạng pháp lý và giới hạn sử dụng nguồn
1. Nguồn Google Sheet hiện ở chế độ **Chỉ xem (View Only)**, có thông báo hạn chế sao chép từ tác giả sheet.
2. Ứng dụng **tuyệt đối không scrape, export hoặc sao chép nguyên văn** các nội dung/tài liệu từ sheet vào cơ sở dữ liệu.
3. Dự án sử dụng Google Sheet như một **khung mục tiêu sư phạm tham khảo (Reference Pedagogical Framework)**. Toàn bộ bài giảng, ví dụ, câu hỏi trắc nghiệm, bài tập thực hành và câu hướng dẫn đều là **giáo trình tự biên soạn mới (Author-Compiled Curriculum)** hoặc khai thác các nguồn học liệu mở có giấy phép phù hợp (như BBC Learning English, British Council).
4. **Không tự nhận đã kiểm duyệt hoặc đã duyệt:** Toàn bộ nội dung do AI hoặc tác giả soạn thảo ở giai đoạn này là bản nháp thử nghiệm (`UNVERIFIED_DRAFT`), cần qua các vòng kiểm tra thực tế và rà soát sư phạm trước khi coi là hoàn chỉnh.

### 1.2. Hệ thống Nhãn Nguồn gốc (Provenance) & Trạng thái Xác minh (Verification Status)
Để tránh nhầm lẫn giữa **Nguồn gốc nội dung** và **Mức độ kiểm duyệt thực tế** (xử lý R8), hệ thống tách bạch hai thuộc tính:

#### A. Thuộc tính Nguồn gốc (Provenance Tags):
- `SOURCE_OBSERVED`: Tiêu đề, mục tiêu, chủ đề được quan sát trực tiếp từ các ô trong sheet nguồn.
- `SOURCE_UNVERIFIED_LINK`: Liên kết ngoài (URL Youtube, bài đọc, web) có trong sheet nhưng chưa kiểm tra đích.
- `AUTHOR_COMPILED`: Nội dung do tác giả/AI tự biên soạn mới dựa trên khung tham chiếu.
- `PROPOSED_PILOT`: Nội dung thuộc Lộ trình thử nghiệm riêng (Pilot Curriculum), hoàn toàn độc lập với Lộ trình A.

#### B. Thuộc tính Trạng thái Xác minh (Verification Status):
- `UNVERIFIED_DRAFT`: Bản nháp chưa qua kiểm định đường link hoặc thẩm định sư phạm đầy đủ.
- `LINK_CHECKED`: Đã kiểm tra đường link còn sống (HTTP 200) và không bị chặn nhúng (no-embed).
- `PEDAGOGICALLY_REVIEWED`: Đã được người điều phối/giáo viên rà soát chất lượng sư phạm và đáp án.

---

## 2. Bảng đối chiếu tổng thể 26 Tuần của Lộ trình A

Sheet nguồn Lộ trình A gồm 3 giai đoạn:
- **Giai đoạn 1 (Tuần 1):** Chuẩn hóa phát âm IPA & 100 từ vựng cơ bản.
- **Giai đoạn 2 (Tuần 2 – Tuần 13):** Ngữ pháp các thì cốt lõi, từ vựng theo chủ đề, nghe hiểu và shadowing.
- **Giai đoạn 3 (Tuần 14 – Tuần 26):** Chủ đề giao tiếp đời sống, nghe chép chính tả (dictation), luyện nói và tổng ôn.

Dưới đây là bảng đối chiếu chi tiết, phân định rõ giữa **Quan sát thực tế từ nguồn** và **Đề xuất biên soạn giáo trình của ứng dụng**:

> Cột quan sát được hiệu chỉnh theo các ô đã đọc trực tiếp trong phiên làm việc trước. Cột đề xuất là ý tưởng biên soạn độc lập, chưa phải phân bổ cuối cùng; rà soát sự phù hợp khi biên soạn A ở G6. Liên kết học liệu chưa được coi là đã kiểm tra.

### Giai đoạn 1: Chuẩn hóa Ngữ âm IPA & Nền tảng Ban đầu (Tuần 1)

| Tuần | Quan sát thực tế từ nguồn (`SOURCE_OBSERVED`) | Đề xuất biên soạn của ứng dụng (`AUTHOR_COMPILED`) | Trạng thái xác minh |
|---|---|---|---|
| **Tuần 1** | IPA và 100 từ vựng; có nhiệm vụ từng ngày (B10/C10). | Chuyển thành các buổi học có đầu ra cụ thể: chia nhóm nguyên âm đơn, phụ âm bật hơi, âm lướt Schwa và quy tắc trọng âm cơ bản; bổ sung audio chuẩn Anh-Anh (`en-GB`). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |

### Giai đoạn 2: Tích lũy Ngữ pháp & Kỹ năng Nghe hiểu (Tuần 2 – Tuần 13)

| Tuần | Quan sát thực tế từ nguồn (`SOURCE_OBSERVED`) | Đề xuất biên soạn của ứng dụng (`AUTHOR_COMPILED`) | Trạng thái xác minh |
|---|---|---|---|
| **Tuần 2** | Hiện tại đơn (Present Simple), từ vựng, nghe và shadowing (C14). | Biên soạn bài phân biệt thói quen vs chân lý, quy tắc phát âm đuôi -s/-es theo chuẩn Anh-Anh (/s/, /z/, /ɪz/). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 3** | Hiện tại tiếp diễn (Present Continuous), từ vựng, nghe và shadowing (C15). | Biên soạn bài phân biệt thì tiếp diễn vs hiện tại đơn; lưu ý động từ chỉ trạng thái (stative verbs); bài tập nghe ngắn. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 4** | Quá khứ đơn (Past Simple), từ vựng, nghe và shadowing (C16). | Biên soạn quy tắc phát âm đuôi -ed chuẩn Anh-Anh (/t/, /d/, /ɪd/); bảng 30 động từ bất quy tắc hay gặp nhất kèm câu mẫu. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 5** | Tương lai đơn (Future Simple) (C17). | Biên soạn bài tập tình huống phối hợp thì (hành động đang xảy ra thì hành động khác xen vào). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 6** | Câu hỏi Who/What/Where/When/Why/How (C18). | Biên soạn bài tập so sánh quyết định tức thì vs kế hoạch có từ trước; bài nghe ngữ điệu câu. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 7** | Danh từ đếm được và không đếm được (C19). | Biên soạn bài so sánh Present Perfect vs Past Simple; lý giải vì sao người Việt hay nhầm do tiếng Việt không có thì hoàn thành. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 8** | Số nhiều của danh từ (C20). | Xây dựng bài tổng hợp nhận diện thì và bài quiz trắc nghiệm ngắn 15 câu có giải thích. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 9** | Tính từ sở hữu (C21). | Biên soạn bảng quy tắc mạo từ có ví dụ thực tế; bài tập điền mạo từ ngữ cảnh. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 10** | Đại từ nhân xưng (C22). | Biên soạn bài tập phân biệt tính từ đuôi -ed/-ing và các dạng so sánh bất quy tắc (good/better/best). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 11** | So sánh hơn và so sánh nhất (C23). | Biên soạn bài tập phân biệt nghĩa vụ chủ quan (Must) vs khách quan (Have to); lời khuyên lịch sự (Should). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 12** | Hiện tại hoàn thành (Present Perfect) (C24). | Minh họa mô hình tam giác giới từ In/On/At từ rộng đến hẹp; 15 phrasal verbs phổ biến nhất. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 13** | Ôn tập ngữ pháp và từ vựng các tuần trước (C25). | Xây dựng bài kiểm tra trắc nghiệm tổng hợp 30 câu có giải thích chi tiết. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |

### Giai đoạn 3: Phản xạ Giao tiếp & Ứng dụng Thực tế (Tuần 14 – Tuần 26)

| Tuần | Quan sát thực tế từ nguồn (`SOURCE_OBSERVED`) | Đề xuất biên soạn của ứng dụng (`AUTHOR_COMPILED`) | Trạng thái xác minh |
|---|---|---|---|
| **Tuần 14** | Du lịch (C30). | Biên soạn bài hội thoại tại sân bay, mua vé tàu, hỏi đường; bài nghe thông báo nhà ga chuẩn British accent. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 15** | Ẩm thực (C31). | Mẫu câu mặc cả lịch sự, hỏi kích cỡ, đổi trả hàng; bài tập nghe giá tiền và số đếm. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 16** | Sở thích (C32). | Mẫu câu đặt bàn, gọi món chuẩn Anh ("Could I have the bill, please?"); văn hóa tip tại Anh. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 17** | Công việc (C33). | Bài tập điền thông tin check-in, phản ánh sự cố phòng (máy lạnh hỏng, thiếu khăn). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 18** | Giáo dục (C34). | Từ vựng triệu chứng thông thường (headache, cough, sore throat); hội thoại với dược sĩ/bác sĩ. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 19** | Mua sắm (C35). | Đoạn văn đọc hiểu ngắn 150 từ về thời tiết nước Anh; mẫu câu bắt chuyện về thời tiết (small talk). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 20** | Sức khỏe (C36). | Bài nghe chép chính tả ngắn 1 phút về thói quen rèn luyện sức khỏe. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 21** | Giao thông (C37). | Thảo luận về bộ phim yêu thích; mẫu câu diễn đạt cảm xúc và nhận xét tích cực/tiêu cực. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 22** | Môi trường (C38). | Viết email công việc ngắn đơn giản; mẫu câu chào hỏi và cảm ơn trang trọng. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 23** | Văn hóa (C39). | Từ vựng công nghệ đời thường; bài đọc về tác động của mạng xã hội đến thói quen học tập. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 24** | Công nghệ (C40). | Mẫu câu nêu quan điểm chuẩn British ("In my view...", "I'm afraid I have to disagree..."). | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 25** | Giải trí (C41). | Dàn ý gợi ý người học nói ghi âm về kế hoạch 1 năm tới. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |
| **Tuần 26** | Ôn tập và tổng hợp (C42). | Bài test trắc nghiệm tổng hợp 50 câu; hướng dẫn duy trì thói quen tự học sau khi kết thúc lộ trình. | SOURCE_OBSERVED (mục tiêu); UNVERIFIED_DRAFT (đề xuất) |

---

## 3. Lộ trình Thử nghiệm (Pilot Curriculum) — Độc lập và Tự chủ

Để phục vụ kiểm chứng kỹ thuật trong các giai đoạn G1–G3 mà không phải chờ đợi biên soạn đủ 26 tuần, dự án xây dựng một **Lộ trình Thử nghiệm riêng biệt (`pilot-curriculum`, version `v1.0.0`)**.

> [!IMPORTANT]
> **Lưu ý nguyên tắc (xử lý R1):** Lộ trình Thử nghiệm là gói giáo trình **tự biên soạn độc lập (`PROPOSED_PILOT`)**, KHÔNG PHẢI là bản trích xuất hay sao chép cấu trúc từ Lộ trình A. 
> - **Gói nhập chính thức (Publishable Package):** Gồm duy nhất **Tuần 1 hoàn chỉnh**, đầy đủ 6 bài học, 20 nhiệm vụ và toàn bộ file Markdown tồn tại trên đĩa.
> - **Bản phác thảo Tuần 2 (Draft Sketch):** Được lưu tại thư mục phác thảo riêng (`content/pilot-curriculum/drafts/week-02-sketch.json`) để kiểm chứng khả năng mở rộng sang các dạng bài ngữ pháp và nghe/shadowing, **không đưa vào manifest nhập chính thức của v1**.

### Cấu trúc Tuần 1 của Pilot Curriculum (1 tuần hoàn chỉnh - Publishable):
- **Key:** `pilot.w01`
- **Mục tiêu:** Nắm vững khẩu hình cặp nguyên âm /iː/ và /ɪ/, cặp phụ âm /p/ và /b/, âm lướt Schwa /ə/, và quy tắc trọng âm từ 2 âm tiết chuẩn Anh-Anh (Modern Received Pronunciation / General British).
- **Phân bổ:** 6 buổi học (5 buổi bắt buộc, 1 buổi tùy chọn), 20 nhiệm vụ (18 bắt buộc, 2 tùy chọn):
  1. **Buổi 1 (pilot.w01.l01) - 25 phút [Bắt buộc]:** Cặp nguyên âm đơn /iː/ và /ɪ/ (`READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`).
  2. **Buổi 2 (pilot.w01.l02) - 20 phút [Bắt buộc]:** Cặp phụ âm bật hơi /p/ và /b/ (`READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`).
  3. **Buổi 3 (pilot.w01.l03) - 25 phút [Bắt buộc]:** Quy tắc trọng âm từ 2 âm tiết (`READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`).
  4. **Buổi 4 (pilot.w01.l04) - 20 phút [Bắt buộc]:** Âm lướt Schwa /ə/ (`READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`).
  5. **Buổi 5 (pilot.w01.l05) - 30 phút [Bắt buộc]:** Đánh giá thực hành & Tổng hợp tuần 1 (`READING`, `PRACTICE`, `REFLECTION`).
  6. **Buổi 6 (pilot.w01.l06) - 15 phút [Tùy chọn - lesson.required: false]:** Hiện tượng Non-Rhoticity trong tiếng Anh-Anh (`READING`, `EXTERNAL_RESOURCE`).

---

## 4. Minh chứng Diễn giải Lịch học khi Người học chọn 3 ngày/tuần

Theo tiêu chí nghiệm thu của G0: **"Người đọc có thể diễn giải chính xác lịch khi chọn 3 ngày/tuần"**.

### Kịch bản thử nghiệm:
- **Người học:** Nguyễn Văn A
- **Lộ trình đăng ký:** `pilot-curriculum` (Phiên bản v1.0.0 — gồm 5 buổi học bắt buộc: Buổi 1 đến Buổi 5; Buổi 6 là tùy chọn).
- **Ngày bắt đầu học (`startDate`):** `2026-10-26` (Thứ Hai).
- **Múi giờ:** `Asia/Ho_Chi_Minh`.
- **Các ngày học đã chọn (`studyDays`):** `[MONDAY, WEDNESDAY, FRIDAY]` (3 buổi/tuần).

### Bảng phân bổ lịch học của hệ thống:

| Ngày lịch (`plannedDate`) | Thứ trong tuần | Trạng thái ngày | Buổi học được xếp lịch | Tên bài học | Tính chất |
|---|---|---|---|---|---|
| **2026-10-26** | Thứ Hai | Ngày học | **Buổi 1** (pilot.w01.l01) | Cặp nguyên âm đơn /iː/ và /ɪ/ | Bắt buộc |
| **2026-10-27** | Thứ Ba | **Ngày nghỉ** | *(Không xếp lịch)* | Nghỉ ngơi / Tự ôn tập | Không có bài bắt buộc |
| **2026-10-28** | Thứ Tư | Ngày học | **Buổi 2** (pilot.w01.l02) | Cặp phụ âm /p/ và /b/ | Bắt buộc |
| **2026-10-29** | Thứ Năm | **Ngày nghỉ** | *(Không xếp lịch)* | Nghỉ ngơi / Tự ôn tập | Không có bài bắt buộc |
| **2026-10-30** | Thứ Sáu | Ngày học | **Buổi 3** (pilot.w01.l03) | Trọng âm từ 2 âm tiết | Bắt buộc |
| **2026-10-31** | Thứ Bảy | **Ngày nghỉ** | *(Không xếp lịch)* | Nghỉ ngơi cuối tuần | Không có bài bắt buộc |
| **2026-11-01** | Chủ Nhật | **Ngày nghỉ** | *(Không xếp lịch)* | Nghỉ ngơi cuối tuần | Không có bài bắt buộc |
| **2026-11-02** | Thứ Hai | Ngày học | **Buổi 4** (pilot.w01.l04) | Âm lướt Schwa /ə/ | Bắt buộc |
| **2026-11-03** | Thứ Ba | **Ngày nghỉ** | *(Không xếp lịch)* | Nghỉ ngơi / Tự ôn tập | Không có bài bắt buộc |
| **2026-11-04** | Thứ Tư | Ngày học | **Buổi 5** (pilot.w01.l05) | Đánh giá & Tổng hợp tuần 1 | Bắt buộc |

### Diễn giải sư phạm và kỹ thuật:
1. **Tuần nội dung trải dài qua 2 tuần lịch thực tế:** Mặc dù mang tên "Tuần nội dung 1", nhưng vì người học chỉ học 3 ngày/tuần, 5 buổi bắt buộc của tuần này trải dài từ ngày `2026-10-26` (Thứ Hai tuần 1) đến ngày `2026-11-04` (Thứ Tư tuần 2).
2. **Giao diện hiển thị đúng nhãn:** Tiêu đề sư phạm là `"Tuần nội dung 1: Ngữ âm IPA Anh-Anh cốt lõi"`, nhưng bên cạnh hiển thị rõ ngày dự kiến thực tế của từng buổi (`26/10/2026`, `28/10/2026`,...).
3. **Xử lý ngày nghỉ (Thứ Ba, Thứ Năm, Thứ Bảy, Chủ Nhật):** Hệ thống không tạo bất kỳ nhiệm vụ bắt buộc nào cho những ngày này. Trên trang "Hôm nay", người học thấy thông điệp chào mừng ngày nghỉ kèm gợi ý học sớm bài tiếp theo nếu muốn.
4. **Vị trí của Buổi 6 (Tùy chọn):** Buổi 6 (`lesson.required: false`) không chiếm slot vào ngày Thứ Sáu `2026-11-06`. Người học có thể mở học bất kỳ lúc nào qua mục Danh mục lộ trình mà không làm tăng hay giảm mẫu số tiến độ bắt buộc.
