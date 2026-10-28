# G6-02 — Rà soát, biên soạn và xuất bản toàn bộ 26 tuần của Lộ trình A v1

## 1. Mục tiêu và Phạm vi

Biên soạn, chuẩn hóa và xuất bản chính thức toàn bộ **26 tuần của Lộ trình A v1** (`roadmap-a`, phiên bản `1.0.0`) dựa trên khung mục tiêu sư phạm trong `KE_HOACH_DU_AN.md` và `docs/content-mapping.md`:
1. **Cấu trúc Lộ trình 26 Tuần Hoàn Chỉnh:**
   - Gói nội dung đặt tại `content/roadmap-a/v1/`.
   - Tuân thủ nghiêm ngặt chuẩn JSON Schema (`manifest.schema.json`, `week.schema.json`, `resources.schema.json`).
   - Gồm 3 Giai đoạn sư phạm (3 Phases):
     * **Giai đoạn 1 (Tuần 1):** Chuẩn hóa Ngữ âm IPA & Nền tảng Ban đầu.
     * **Giai đoạn 2 (Tuần 2 – Tuần 13):** Tích lũy Ngữ pháp & Kỹ năng Nghe hiểu cốt lõi.
     * **Giai đoạn 3 (Tuần 14 – Tuần 26):** Phản xạ Giao tiếp Đời sống & Ứng dụng Thực tế.
   - Mỗi tuần gồm 3–6 bài học (`lessons`), có thứ tự rõ ràng (`orderIndex`), thời lượng ước tính (`estimatedMinutes`), bài học bắt buộc/tùy chọn (`required`), và đầy đủ bài đọc Markdown thực tế (`lessons/*.md`).
   - Mỗi bài học chứa các nhiệm vụ đa dạng:
     * `READING`: Lý thuyết ngữ pháp, ngữ âm, từ vựng chuẩn IPA Anh-Anh (`en-GB`).
     * `PRACTICE`: Luyện tập đặt câu, viết đoạn ngắn, luyện khẩu hình.
     * `REFLECTION`: Phản tư trải nghiệm học và tự đánh giá.
     * `EXTERNAL_RESOURCE`: Tham khảo học liệu uy tín (BBC Learning English, British Council, Cambridge, Oxford).
     * `QUIZ`: Bài kiểm tra trắc nghiệm định kỳ (Tuần 1, Tuần 8, Tuần 13, Tuần 26...) với câu hỏi, lựa chọn, đáp án đúng và giải thích sư phạm chi tiết.
2. **Kiểm tra & Xuất bản (Validation & Publishing):**
   - Vượt qua `content validate content/roadmap-a/v1` không có lỗi schema/file/key.
   - Vượt qua `content dry-run content/roadmap-a/v1` tổng hợp chính xác số phase, week, lesson, activity, resource và tính mã băm checksum.
   - Nhập vào cơ sở dữ liệu qua `content import content/roadmap-a/v1`.
   - Xuất bản chính thức trạng thái `PUBLISHED` qua `content publish roadmap-a 1.0.0`.
3. **Kiểm chứng Trải nghiệm Người học (Learner Experience Verification):**
   - Danh mục lộ trình (`/curricula`): Hiển thị Lộ trình A 26 tuần với đầy đủ mô tả, thời lượng và số tuần.
   - Chi tiết đề cương (`/curricula/{id}`): Hiển thị đầy đủ 3 giai đoạn và 26 tuần.
   - Đăng ký học (`Enrollment`): Học viên đăng ký Lộ trình A, sinh lịch học 26 tuần trên trang Hôm nay và Workspace.
   - Tiến độ học tập (`/progress`): Biểu đồ thanh 26 tuần hiển thị trực quan, phân chia rõ ràng theo từng tuần học.

---

## 2. Đặc tả Kỹ thuật Gói Nội dung `roadmap-a/v1`

### 2.1. File `manifest.json`
- `schemaVersion`: `"1.0.0"`
- `curriculumKey`: `"roadmap-a"`
- `version`: `"1.0.0"`
- `title`: `"Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng (Chuẩn Anh-Anh)"`
- `description`: `"Lộ trình tự học 26 tuần từ nền tảng phát âm IPA, ngữ pháp cốt lõi đến phản xạ giao tiếp tự tin chuẩn Anh-Anh (General British / Modern RP)."`
- `targetLocale`: `"en-GB"`
- `phases`: 3 phases:
  - `phase01`: `orderIndex: 1`, `weeks`: `[week-01]`
  - `phase02`: `orderIndex: 2`, `weeks`: `[week-02 .. week-13]`
  - `phase03`: `orderIndex: 3`, `weeks`: `[week-14 .. week-26]`

### 2.2. Danh sách 26 Tuần theo `docs/content-mapping.md`

| Tuần | Key | Tiêu đề & Trọng tâm | Số bài |
|:---|:---|:---|:---|
| **Tuần 1** | `roadmap-a.w01` | Chuẩn hóa Ngữ âm IPA Anh-Anh cốt lõi (/iː/, /ɪ/, /p/, /b/, /ə/, trọng âm) | 5 bài |
| **Tuần 2** | `roadmap-a.w02` | Hiện tại đơn (Present Simple) & Phát âm đuôi -s/-es (/s/, /z/, /ɪz/) | 5 bài |
| **Tuần 3** | `roadmap-a.w03` | Hiện tại tiếp diễn (Present Continuous) & Động từ chỉ trạng thái (Stative Verbs) | 5 bài |
| **Tuần 4** | `roadmap-a.w04` | Quá khứ đơn (Past Simple), Động từ bất quy tắc & Phát âm đuôi -ed (/t/, /d/, /ɪd/) | 5 bài |
| **Tuần 5** | `roadmap-a.w05` | Tương lai đơn (Future Simple) & Be Going To (Kế hoạch vs Dự đoán) | 5 bài |
| **Tuần 6** | `roadmap-a.w06` | Câu hỏi Wh- Questions & Ngữ điệu câu hỏi chuẩn British | 5 bài |
| **Tuần 7** | `roadmap-a.w07` | Danh từ đếm được / không đếm được & Từ định lượng (Much, Many, A lot of) | 5 bài |
| **Tuần 8** | `roadmap-a.w08` | Ôn tập giữa kỳ Ngữ pháp & Trắc nghiệm tổng hợp các thì cơ bản (Quiz 15 câu) | 5 bài |
| **Tuần 9** | `roadmap-a.w09` | Mạo từ A, An, The & Trường hợp Zero Article trong đời sống | 5 bài |
| **Tuần 10** | `roadmap-a.w10` | Tính từ, Trạng từ & So sánh hơn / So sánh nhất | 5 bài |
| **Tuần 11** | `roadmap-a.w11` | Động từ khuyết thiếu (Modal Verbs: Can, Could, Must, Have to, Should) | 5 bài |
| **Tuần 12** | `roadmap-a.w12` | Hiện tại hoàn thành (Present Perfect) & Giới từ chỉ nơi chốn / thời gian (In, On, At) | 5 bài |
| **Tuần 13** | `roadmap-a.w13` | Tổng ôn tập Giai đoạn 2 & Đánh giá năng lực Ngữ pháp nền tảng (Quiz 20 câu) | 5 bài |
| **Tuần 14** | `roadmap-a.w14` | Giao tiếp Du lịch: Sân bay, Thủ tục hải quan & Mua vé tàu xe tại Anh | 5 bài |
| **Tuần 15** | `roadmap-a.w15` | Giao tiếp Ẩm thực: Đặt bàn, Gọi món nhà hàng & Văn hóa Tiền tip | 5 bài |
| **Tuần 16** | `roadmap-a.w16` | Giao tiếp Mua sắm: Hỏi kích cỡ, Giá tiền, Đổi trả & Small Talk khi thanh toán | 5 bài |
| **Tuần 17** | `roadmap-a.w17` | Khách sạn & Lưu trú: Check-in, Phản ánh sự cố phòng & Yêu cầu hỗ trợ | 5 bài |
| **Tuần 18** | `roadmap-a.w18` | Sức khỏe & Y tế: Mô tả triệu chứng bệnh, Mua thuốc & Thăm khám bác sĩ | 5 bài |
| **Tuần 19** | `roadmap-a.w19` | Small Talk & Thời tiết: Nghệ thuật bắt chuyện tự nhiên kiểu Anh | 5 bài |
| **Tuần 20** | `roadmap-a.w20` | Sở thích, Thói quen & Thể thao: Kể về phong cách sống và đam mê | 5 bài |
| **Tuần 21** | `roadmap-a.w21` | Điện ảnh & Giải trí: Bình luận phim, Diễn đạt cảm xúc yêu thích / không thích | 5 bài |
| **Tuần 22** | `roadmap-a.w22` | Giao tiếp Công việc: Viết Email công việc chuẩn mực & Văn hóa lịch thiệp | 5 bài |
| **Tuần 23** | `roadmap-a.w23` | Công nghệ & Mạng xã hội: Bàn luận về thói quen số & Tác động của công nghệ | 5 bài |
| **Tuần 24** | `roadmap-a.w24` | Tranh luận & Bày tỏ Quan điểm: Các cụm từ diễn đạt ý kiến lịch sự kiểu British | 5 bài |
| **Tuần 25** | `roadmap-a.w25` | Kế hoạch tương lai & Mục tiêu cá nhân: Trình bày tầm nhìn bản thân | 5 bài |
| **Tuần 26** | `roadmap-a.w26` | Tổng kết Lộ trình: Bài kiểm tra Tổng hợp Cuối khóa (Quiz 30 câu) & Kế hoạch Tự học Tiếp diễn | 5 bài |

---

## 3. Tiêu chí Nghiệm thu (DoD)

1. **Tính Hợp lệ Tuyệt đối của Gói Nội dung:**
   - Lệnh `java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content validate content/roadmap-a/v1` trả về mã 0 (SUCCESSFUL, 0 errors).
   - Lệnh `java -jar backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar content dry-run content/roadmap-a/v1` in ra tóm tắt đầy đủ 3 phases, 26 weeks, 130 lessons, và các activities hợp lệ.
2. **Thực thi Nhập & Xuất bản (Import & Publish):**
   - Import thành công vào DB dev: `content import content/roadmap-a/v1`.
   - Publish thành công: `content publish roadmap-a 1.0.0`.
3. **Kiểm chứng API & Giao diện:**
   - Catalog: `GET /api/v1/curricula` hiển thị "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng (Chuẩn Anh-Anh)" ở trạng thái `PUBLISHED`.
   - Outline: `GET /api/v1/curricula/{id}/outline` trả về đúng 26 tuần và toàn bộ bài học.
   - Thử nghiệm đăng ký và học bài đầu tiên của Lộ trình A trên môi trường dev, kiểm tra `/progress` render biểu đồ 26 tuần mượt mà.
4. **Bảo toàn Dữ liệu:**
   - Không ảnh hưởng đến dữ liệu `pilot-curriculum` và các bài học của học viên hiện hữu.
