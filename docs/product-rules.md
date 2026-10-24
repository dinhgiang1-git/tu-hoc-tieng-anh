# Quy tắc nghiệp vụ và mô hình trạng thái (Product Rules & State Lifecycle)

**Ngày ban hành:** 24/10/2026  
**Trạng thái:** Đã khóa cho G0 — Chuẩn hóa nền tảng cho G1–G7  
**Tài liệu liên quan:** [KE_HOACH_DU_AN.md](../KE_HOACH_DU_AN.md), [BE_CODE_CONVENTION.md](../BE_CODE_CONVENTION.md), [FE_CODE_CONVENTION.md](../FE_CODE_CONVENTION.md)

---

## 1. Thuật ngữ và Khái niệm Cốt lõi

Hệ thống tự học tiếng Anh được thiết kế theo mô hình phân tầng chặt chẽ, tách rời giữa **Nội dung giáo trình (Curriculum)** và **Tiến độ cá nhân (Learner Progress)**:

```text
Lộ trình (Curriculum)
└── Phiên bản (Revision) [DRAFT | PUBLISHED | ARCHIVED]
    └── Giai đoạn (Phase)
        └── Tuần nội dung (CurriculumWeek)
            └── Buổi học (Lesson) [required: boolean, estimatedMinutes]
                ├── Nhiệm vụ (Activity) [type, required: boolean, completionPolicy]
                └── Tài liệu tham khảo (Resource qua ActivityResource)
```

### 1.1. Các thực thể nội dung (Curriculum Entities)
- **Curriculum (Lộ trình):** Thực thể cấp cao nhất đại diện cho một chương trình đào tạo (ví dụ: `curriculum-a` - Lộ trình tiếng Anh nền tảng 6 tháng; `pilot-curriculum` - Lộ trình thử nghiệm). Định danh bằng `id` (UUID) và `slug` bất biến.
- **CurriculumRevision (Phiên bản lộ trình):** Một bản đóng băng nội dung của lộ trình tại một mốc phát hành (ví dụ: `v1.0.0`). Người học luôn đăng ký học trên một revision cụ thể.
- **Phase (Giai đoạn):** Nhóm sư phạm lớn trong một revision (ví dụ: Giai đoạn 1: Phát âm & Nền tảng; Giai đoạn 2: Tích lũy ngữ pháp & từ vựng; Giai đoạn 3: Phản xạ giao tiếp).
- **CurriculumWeek (Tuần nội dung):** Đơn vị phân phối bài học theo kế hoạch sư phạm. **Lưu ý quan trọng:** Tuần nội dung không đồng nghĩa với một tuần lịch thực tế (7 ngày) của người học.
- **Lesson (Buổi học):** Đơn vị sắp xếp vào lịch học cá nhân (`plannedDate`). Chứa tiêu đề, mục tiêu sư phạm, thời lượng ước tính (`estimatedMinutes`), thứ tự trong tuần (`orderIndex`), và cờ bắt buộc (`required: boolean`).
- **Activity (Nhiệm vụ học tập):** Đơn vị học tập nhỏ nhất được ghi nhận trạng thái và hoàn thành. Mỗi buổi học chứa từ 1 đến nhiều activities.
- **Resource (Tài liệu):** Liên kết ngoài (video, bài đọc, audio) hoặc tài liệu đính kèm. Resource được gắn với activity; việc truy cập resource là hành động hỗ trợ, không tự động đánh dấu hoàn thành activity.

### 1.2. Các thực thể người học (Learner Entities)
- **AppUser:** Tài khoản người học, định danh duy nhất bằng `id` (UUID), `username`, `timezone` (chuỗi IANA, ví dụ `Asia/Ho_Chi_Minh`), `role` (`LEARNER`, `ADMIN`).
- **Enrollment (Lần đăng ký học):** Liên kết giữa người học và một `CurriculumRevision` cụ thể. Chứa thông tin lịch học cá nhân (`startDate`, `studyDays`, `dailyTargetMinutes`, `timeZone`), trạng thái học, và `version` (optimistic lock).
- **LessonSchedule:** Lịch dự kiến của từng buổi học trong enrollment (`plannedDate`, `originalDate`).
- **ActivityProgress:** Trạng thái thực tế của người học trên từng activity (`status`, `startedAt`, `completedAt`).
- **ActivityResponse:** Câu trả lời dạng text của người học cho các bài thực hành viết, ghi chú phản tư (`PRACTICE`, `REFLECTION`).
- **LearningEvent:** Bản ghi bất biến (event log) ghi nhận mỗi hành động học tập thực tế (`COMPLETED`, `UNDONE`, `SUBMITTED`) kèm timestamp UTC và local date tại thời điểm thao tác. Phục vụ thống kê, báo cáo và kiểm tra lịch sử.

---

## 2. Các trạng thái và Vòng đời (State Machines)

### 2.1. Vòng đời Phiên bản nội dung (`RevisionStatus`)

```text
[ DRAFT ] ──(validate & publish)──> [ PUBLISHED ] ──(deprecate)──> [ ARCHIVED ]
```

- **DRAFT:** Nội dung đang được biên soạn. Cho phép chỉnh sửa cấu trúc, nội dung Markdown, thêm/bớt bài học. Chưa cho phép người học đăng ký mới.
- **PUBLISHED:** Đã qua kiểm tra tự động và phê duyệt chất lượng. **Nội dung bất biến (immutable)**: không cho phép sửa đổi thứ tự, nội dung bài, cờ required, đáp án quiz. Người học đăng ký và học trên phiên bản này.
- **ARCHIVED:** Phiên bản cũ không còn nhận đăng ký mới. Người học đang học dở trên phiên bản này vẫn tiếp tục học bình thường; dữ liệu lịch sử không bị xóa.

### 2.2. Vòng đời Lần đăng ký học (`EnrollmentStatus`)

```text
          ┌──────────────────(resume)──────────────────┐
          ▼                                            │
    [ ACTIVE ] ──(pause)──> [ PAUSED ]                 │
        │                       │                      │
        │ (hoàn thành)          │                      │
        ▼                       ▼                      │
  [ COMPLETED ]           [ ABANDONED ] ◄──────────────┘
        │                 (kết thúc để học lại)
        │ (undo bài bắt buộc)
        ▼
  [ ACTIVE ] (chỉ khi không có enrollment ACTIVE/PAUSED khác của cùng lộ trình)
```

| Trạng thái | Ý nghĩa | Hành vi cho phép | Chuyển trạng thái tiếp theo |
|---|---|---|---|
| **ACTIVE** | Đang tham gia học lộ trình. | Học bài, đánh dấu hoàn thành, hoàn tác, sửa ghi chú, đổi lịch, nộp quiz. | `PAUSED`, `COMPLETED`, `ABANDONED`. |
| **PAUSED** | Tạm dừng lộ trình. | Xem lại nội dung và ghi chú đã học. Không tính quá hạn mới trên trang Hôm nay. Mutation học tập bị chặn (409 `STATE_CONFLICT`). | `ACTIVE` (qua Resume và đổi lịch mới), `ABANDONED`. |
| **COMPLETED** | Đã hoàn thành 100% nhiệm vụ bắt buộc hiệu lực. | Xem toàn bộ nội dung, sửa ghi chú, luyện lại quiz. Không xếp lịch học mới. | Tự động chuyển từ `ACTIVE`. Có thể quay lại `ACTIVE` nếu Undo nhiệm vụ bắt buộc (xem mục 5.2). |
| **ABANDONED** | Người học chủ động kết thúc lần học này để đăng ký lại từ đầu. | Chỉ xem lịch sử (read-only). Không thể kích hoạt lại. | Trạng thái kết thúc (Terminal state). |

**Ràng buộc cơ sở dữ liệu và nghiệp vụ:**
- Tại một thời điểm, một người học (`user_id`) chỉ được phép có **tối đa một** enrollment ở trạng thái `ACTIVE` hoặc `PAUSED` cho cùng một `curriculum_id`. Ràng buộc này được bảo vệ bởi Partial Unique Index trong PostgreSQL:
  ```sql
  CREATE UNIQUE INDEX uq_user_curriculum_active_idx 
  ON enrollment (user_id, curriculum_id) 
  WHERE status IN ('ACTIVE', 'PAUSED');
  ```
- Nếu người học muốn học lại từ đầu trong khi đang có enrollment dở dang, họ phải xác nhận chuyển enrollment hiện tại sang `ABANDONED` trước khi tạo enrollment mới.

### 2.3. Vòng đời Nhiệm vụ học tập (`ActivityStatus`)

```text
[ NOT_STARTED ] ──(bắt đầu học / mở bài)──> [ IN_PROGRESS ] ──(xác nhận hoàn thành)──> [ COMPLETED ]
       ▲                                                                                     │
       └─────────────────────────────(hoàn tác / undo)───────────────────────────────────────┘
```

- **NOT_STARTED:** Chưa có tương tác nào được ghi nhận.
- **IN_PROGRESS:** Đã bắt đầu thực hiện (ví dụ: đã lưu bản nháp câu trả lời `PRACTICE`, đã bấm bắt đầu đọc).
- **COMPLETED:** Đã hoàn thành nhiệm vụ theo đúng chính sách hoàn thành (`CompletionPolicy`).
- **Quy tắc chuyển trạng thái:**
  * Thao tác chuyển sang `COMPLETED` đối với bài xác nhận thủ công là idempotent: gọi nhiều lần cùng trạng thái chỉ ghi nhận 1 lần thành công, không tạo thêm event trùng.
  * Thao tác `UNDONE` đưa trạng thái về `NOT_STARTED` hoặc `IN_PROGRESS` (nếu còn câu trả lời nháp).

### 2.4. Trạng thái suy diễn của Buổi học (`LessonStatus`) và Tuần học (`WeekStatus`)
Trạng thái của Buổi học và Tuần học được tính toán động (derived) từ tập các nhiệm vụ con:
- **Buổi học COMPLETED:** Khi và chỉ khi **toàn bộ** các nhiệm vụ bắt buộc (`activity.required == true`) trong buổi học đó đã đạt `COMPLETED`. Nếu buổi học không có nhiệm vụ bắt buộc nào (buổi học tùy chọn), buổi học được coi là hoàn thành khi người học hoàn thành các nhiệm vụ tùy chọn của nó hoặc chủ động hoàn tất.
- **Tuần học COMPLETED:** Khi và chỉ khi **toàn bộ** các buổi học bắt buộc (`lesson.required == true`) trong tuần đó đã đạt `COMPLETED`.

---

## 3. Các loại Nhiệm vụ (`ActivityType`) và Tiêu chí Hoàn thành

| Loại nhiệm vụ (`ActivityType`) | Mô tả & Trải nghiệm học | Tiêu chí hoàn thành (`CompletionPolicy`) | Giai đoạn hỗ trợ |
|---|---|---|---|
| `READING` | Bài đọc lý thuyết, giải thích ngữ âm, từ vựng, ngữ pháp. Có ví dụ minh họa và câu hỏi tự suy ngẫm. | Người học xác nhận thủ công (`MANUAL_CHECK`). | G0 / G3 |
| `EXTERNAL_RESOURCE` | Liên kết đến video Youtube, podcast, bài viết bên ngoài. Có hướng dẫn cụ thể việc cần làm trước khi xem. | Người học xác nhận thủ công (`MANUAL_CHECK`) sau khi xem xong. | G0 / G3 |
| `PRACTICE` | Bài tập luyện tập: viết câu theo mẫu, luyện đọc to IPA, shadowing, ghi chép từ vựng. | Người học có thể lưu câu trả lời nháp (`PUT /response`) và xác nhận thủ công (`MANUAL_CHECK_WITH_RESPONSE`). | G0 / G3 |
| `REFLECTION` | Câu hỏi phản tư cuối buổi: "Hôm nay bạn nhớ nhất điều gì? Còn điểm nào chưa rõ?". | Lưu nháp qua `PUT /response`. Hoàn thành bắt buộc qua `POST /response/submit` với text không rỗng (`RESPONSE_SUBMIT`), cập nhật response và progress nguyên tử kèm `expectedVersion`. Checklist `PUT /progress` trực tiếp bị từ chối 400 `RESPONSE_REQUIRED` nếu chưa có nội dung. | G0 / G3 |
| `QUIZ` | Trắc nghiệm chọn 1 đáp án (Single choice). | Server chấm tự động. Tuân theo chính sách bài: `ON_SUBMIT` (luyện tập) hoặc `PASS_SCORE` (vượt ngưỡng điểm). API checklist thủ công bị từ chối 403/409. | G5 |
| `DICTATION` / `RECORDING` | Nghe chép chính tả và thu âm giọng nói. | Theo đặc tả kỹ thuật chuyên biệt ở giai đoạn mở rộng. | Mở rộng (PA / P2) |

---

## 4. Quy tắc Tính Tiến độ (Progress Formula)

### 4.1. Nhiệm vụ bắt buộc hiệu lực (Effectively Required Activity)
Để tránh sai lệch mẫu số tiến độ khi giáo trình có các bài học bổ trợ hoặc ngày nghỉ tự chọn, định nghĩa:
$$\text{isEffectivelyRequired} = \text{lesson.required} \land \text{activity.required}$$

- Nếu `lesson.required == false` (Buổi học tùy chọn / bài tập thêm): Mọi activity bên trong buổi học này **không** được đưa vào mẫu số của tiến độ lộ trình, dù bản thân activity đó có đặt cờ `required: true`.
- Nếu `lesson.required == true` nhưng `activity.required == false` (Nhiệm vụ tùy chọn trong bài chính): Activity này không tính vào mẫu số lộ trình.

### 4.2. Công thức tiến độ Lộ trình
$$\text{ProgressPercent} = \begin{cases} 
\text{null} & \text{khi } \text{TotalRequired} = 0 \\
\operatorname{round}\left(\dfrac{\text{CompletedRequired}}{\text{TotalRequired}} \times 100, 1\right) & \text{khi } \text{TotalRequired} > 0 
\end{cases}$$

- **Server là nguồn chuẩn duy nhất:** Frontend không tự tính phần trăm tiến độ bằng cách đếm ở local cache.
- **Trường hợp mẫu số bằng 0:** Trả `null`. Frontend hiển thị nhãn: `"Chưa có nội dung bắt buộc"` (không hiển thị 0% hay 100%).
- **Làm tròn:** Làm tròn số học đến 1 chữ số thập phân (ví dụ: `33.3%`, `100.0%`).
- **Tách bạch Tiến độ và Điểm số:** Tiến độ chỉ phản ánh khối lượng nhiệm vụ bắt buộc đã hoàn thành; không gộp điểm quiz hay điểm phát âm vào thanh phần trăm tiến độ lộ trình.

---

## 5. Lịch học cá nhân, Đổi lịch và Ưu tiên Trang Hôm nay

### 5.1. Thuật toán Xếp lịch Ban đầu (Initial Scheduling Algorithm)
Khi người học bắt đầu một enrollment:
- **Đầu vào:**
  * `startDate`: Ngày bắt đầu học (định dạng `YYYY-MM-DD`).
  * `timeZone`: Múi giờ IANA của người học (ví dụ: `Asia/Ho_Chi_Minh`).
  * `studyDays`: Tập các thứ trong tuần người học chọn học (ví dụ: `[MONDAY, WEDNESDAY, FRIDAY]`).
  * `dailyTargetMinutes`: Thời lượng mục tiêu mỗi ngày (ví dụ: 30 phút).
- **Quy tắc phân bổ:**
  1. Lấy danh sách toàn bộ các buổi học bắt buộc (`lesson.required == true`) theo đúng thứ tự sư phạm (`orderIndex`).
  2. Bắt đầu duyệt từ ngày `d = startDate`.
  3. Nếu thứ trong tuần của `d` nằm trong `studyDays`:
     - Gán buổi học bắt buộc tiếp theo vào ngày `d` (`plannedDate = d`, `originalDate = d`).
     - Chuyển sang buổi học kế tiếp.
  4. Tăng `d` lên 1 ngày và lặp lại cho đến khi xếp hết toàn bộ buổi học bắt buộc.
  5. Các ngày không nằm trong `studyDays` là **ngày nghỉ (rest day)**: Tuyệt đối không tạo lịch nhiệm vụ bắt buộc.
  6. Các buổi học tùy chọn (`lesson.required == false`): Không chiếm vị trí trong lịch bắt buộc; người học có thể mở học bất kỳ lúc nào dưới dạng tự do.

### 5.2. Các Trạng thái và Thứ tự Ưu tiên trên trang "Hôm nay" (Today States & Priority)

Trang "Hôm nay" biểu diễn trạng thái qua trường `state` trong `TodayResponse` với 8 giá trị rõ ràng (xử lý R6):
- `NO_ENROLLMENT`: Người học chưa đăng ký lộ trình nào (hoặc chưa chọn lộ trình tập trung). `primaryAction = null`, `enrollmentId = null`. Giao diện hiển thị nút CTA chọn lộ trình.
- `PAUSED`: Lộ trình đang bị tạm dừng. `primaryAction = null`. Giao diện hiển thị thông báo tạm dừng và nút Tiếp tục học (Resume).
- `IN_PROGRESS`: Có buổi học đang làm dở (ít nhất 1 nhiệm vụ đã làm nhưng chưa xong toàn bộ bài bắt buộc). Hành động chính: Tiếp tục buổi học đang dở.
- `OVERDUE`: Có buổi học có `plannedDate < today` chưa hoàn thành. Hành động chính: Học bù bài quá hạn sớm nhất.
- `DUE_TODAY`: Buổi học có `plannedDate == today` chưa hoàn thành. Hành động chính: Bắt đầu học bài hôm nay.
- `REST_DAY`: Hôm nay là ngày nghỉ theo lịch cá nhân. Giao diện hiển thị thông điệp nghỉ ngơi kèm nút phụ học trước bài tiếp theo (`UPCOMING`).
- `UPCOMING`: Đã hoàn thành bài hôm nay sớm, gợi ý bài tiếp theo trong tương lai.
- `CURRICULUM_COMPLETED`: Đã hoàn thành 100% nhiệm vụ bắt buộc của toàn bộ lộ trình. `lessonId = null`, hiển thị thông điệp chúc mừng và gợi ý ôn tập.

Thứ tự ưu tiên khi chọn hành động chính:
1. `IN_PROGRESS` (buổi đang dở)
2. `OVERDUE` (buổi quá hạn sớm nhất)
3. `DUE_TODAY` (buổi đến hạn hôm nay)
4. `REST_DAY` / `UPCOMING` (học trước bài tiếp theo)
5. `CURRICULUM_COMPLETED` (tổng kết hoàn thành)

*Lưu ý:* Danh sách các bài quá hạn khác (nếu có từ 2 bài trở lên) được hiển thị thu gọn trong khu vực phụ bên dưới, không dồn cục vào hành động chính.

### 5.3. Quy tắc Đổi lịch học (Reschedule Policy - G4)
Khi người học bị gián đoạn (ốm, bận việc, nghỉ dài ngày):
- Người học chọn ngày tiếp tục mới và có thể cập nhật lại các thứ muốn học (`studyDays`).
- **Bảo toàn kết quả đã học:** Mọi buổi học đã `COMPLETED` giữ nguyên `plannedDate` thực tế và kết quả; không bao giờ bị xếp lại lịch.
- **Dời buổi chưa hoàn thành:** Chỉ các buổi học chưa hoàn thành mới được phân bổ lại vào các ngày học tiếp theo tính từ ngày tiếp tục mới.
- **Quy trình 2 bước bắt buộc:**
  1. `POST /schedule/preview`: Backend tính toán phương án mới, trả về danh sách thay đổi và `enrollmentVersion`.
  2. `POST /schedule/commit`: Gửi kèm `expectedVersion`. Nếu trong lúc xem preview mà ở tab khác người học đã bấm hoàn thành một bài, commit sẽ bị từ chối với mã lỗi 409 `VERSION_CONFLICT` để tránh ghi đè dữ liệu sai.

### 5.4. Tạm dừng và Tiếp tục (Pause & Resume Policy)
- Khi `PAUSED`: Toàn bộ nợ bài quá hạn ngừng hiển thị cảnh báo; tiến độ đóng băng an toàn.
- Khi `RESUME`: Người học bắt buộc phải xác nhận lịch học mới (tương tự quy trình Reschedule) để hệ thống tạo lại `plannedDate` cho các bài còn lại. Thao tác Resume và áp dụng lịch mới được thực thi trong cùng **một transaction duy nhất**.

---

## 6. Quy tắc Hoàn thành và Hoàn tác (Completion & Undo)

### 6.1. Đánh dấu Hoàn thành (Complete)
- Áp dụng cho `READING`, `EXTERNAL_RESOURCE`, `PRACTICE`, `REFLECTION`.
- Request gửi: `PUT /activities/{activityId}/progress` với `status: "COMPLETED"`, `expectedVersion: long`.
- Backend kiểm tra quyền sở hữu (`owner == principal`), đối chiếu `activityId` thuộc đúng `revisionId` của enrollment.
- Backend thực hiện nguyên tử trong 1 transaction:
  1. Ghi nhận `ActivityProgress` trạng thái `COMPLETED` và `completedAt = now()`.
  2. Ghi một bản ghi bất biến vào `learning_event`.
  3. Kiểm tra nếu tất cả activity bắt buộc trong lesson đã xong $\rightarrow$ đánh dấu lesson hoàn thành.
  4. Kiểm tra nếu tất cả activity bắt buộc trong revision đã xong $\rightarrow$ chuyển enrollment sang `COMPLETED`.
  5. Tăng `enrollmentVersion`.
  6. Trả về `ProgressResponse` với số đếm và phần trăm mới nhất.

### 6.2. Hoàn tác Hoàn thành (Undo)
- Người học bấm bỏ tích hoàn thành khi bấm nhầm hoặc muốn học lại bài đó.
- Request gửi: `PUT /activities/{activityId}/progress` với `status: "IN_PROGRESS"` hoặc `"NOT_STARTED"`.
- **Xử lý đặc biệt khi Enrollment đang `COMPLETED`:**
  * Nếu người học undo một nhiệm vụ bắt buộc trong enrollment đã `COMPLETED`: Hệ thống tự động chuyển enrollment quay trở lại trạng thái `ACTIVE` trong cùng transaction, **với điều kiện duy nhất: người học chưa bắt đầu một enrollment ACTIVE hoặc PAUSED khác cho cùng lộ trình này**.
  * Nếu đã có một enrollment mới đang active, hệ thống từ chối mutation với lỗi 409 `STATE_CONFLICT` kèm thông báo: `"Bạn đã bắt đầu một lượt học mới cho lộ trình này. Không thể mở lại lượt học cũ."`.
  * Xóa mốc `completedAt` của enrollment và tính lại tiến độ tương ứng.
- **Ngoại lệ:** Hoạt động `QUIZ` (G5) do Assessment service quản lý theo chính sách điểm số, tuyệt đối **không** cho phép undo thủ công qua API checklist.

---

## 7. Tính nhất quán, Đồng thời (Concurrency) và Xử lý Thời gian

### 7.1. Kiểm soát Đồng thời Lạc quan (Optimistic Concurrency Control)
- Mọi mutation ảnh hưởng đến tiến độ, trạng thái enrollment hoặc lịch học đều phải gửi kèm trường `expectedVersion`.
- Trường này khớp với `version` hiện hành của bảng `enrollment`.
- Nếu có 2 tab trình duyệt cùng thao tác hoặc mạng chậm dẫn đến request đến sau mang version cũ:
  * Backend rollback transaction và trả về lỗi 409 `VERSION_CONFLICT` theo chuẩn RFC 7807:
    ```json
    {
      "type": "about:blank",
      "title": "Conflict",
      "status": 409,
      "code": "VERSION_CONFLICT",
      "detail": "Dữ liệu học tập đã được cập nhật từ một phiên khác. Vui lòng làm mới trang.",
      "instance": "/api/v1/enrollments/8f9a2b.../activities/c4d1.../progress",
      "traceId": "7c8e1a90...",
      "fieldErrors": []
    }
    ```
  * Frontend nhận mã `VERSION_CONFLICT`, không tự ý ghi đè version mới mà gọi query refetch để đồng bộ giao diện và hiển thị thông báo nhẹ cho người học.

**Quy tắc Hợp đồng Đọc phục vụ Tải lại trang (Read Contract after Reload - xử lý R4):**
Để đảm bảo người học khi mở lại bài trực tiếp hoặc tải lại trang (reload) luôn có `expectedVersion` hợp lệ để gửi mutation:
- `GET /api/v1/enrollments/{id}`: Trả về `EnrollmentDetailResponse` chứa `version`.
- `GET /api/v1/enrollments/{id}/lessons/{lessonId}`: Trả về `enrollmentVersion`.
- `GET /api/v1/me/today`: Trả về `enrollmentVersion`.
- `GET /api/v1/enrollments/{id}/summary`: Trả về `enrollmentVersion`.
Frontend luôn đọc `enrollmentVersion` từ các endpoint đọc này để cung cấp vào `expectedVersion` cho các mutation cập nhật tiến độ, commit lịch và submit phản tư. Thao tác tải lại trang không làm mất khả năng kiểm soát đồng thời.

### 7.2. Idempotency Key cho các tác vụ nhạy cảm
- Áp dụng cho: Tạo enrollment mới (`POST /enrollments`), Tạo/Nộp Quiz attempt (`POST /attempts`), Commit đổi lịch (`POST /schedule/commit`).
- Header yêu cầu: `Idempotency-Key: <UUID>`.
- Scope của key: `(userId, operation, key)`.
- Backend lưu hash payload và kết quả. Nếu client bị rớt mạng sau khi server đã commit thành công và client retry lại cùng key + payload $\rightarrow$ Server trả lại ngay kết quả cũ mà không thực thi lại logic. Nếu cùng key nhưng khác payload $\rightarrow$ Trả 409 `IDEMPOTENCY_CONFLICT`.

### 7.3. Xử lý Thời gian và Múi giờ
- **Ngày lịch (`LocalDate`):** Dùng kiểu `YYYY-MM-DD` cho ngày bắt đầu, ngày học dự kiến (`plannedDate`), ngày hoàn thành nhiệm vụ theo giờ địa phương (`localDate`). Tuyệt đối không convert sang UTC rồi làm lệch ngày lịch.
- **Thời điểm diễn ra (`Instant`):** Dùng ISO-8601 UTC có đuôi `Z` (ví dụ `2026-10-24T10:15:30Z`) cho `createdAt`, `updatedAt`, `occurredAt`.
- **Múi giờ người học (`ZoneId`):** Lưu định danh IANA chuẩn (ví dụ `Asia/Ho_Chi_Minh`, `Europe/London`). Không dùng offset cố định như `UTC+7` vì không phản ánh được quy tắc giờ mùa hè (DST) nếu người học ở nước ngoài.
- **Đồng hồ hệ thống:** Backend inject bean `java.time.Clock` vào tất cả service, cho phép kiểm thử chính xác các ca biên qua nửa đêm (midnight rollover) và cuối tháng.

---

## 8. Chuẩn Phát âm Anh-Anh (`en-GB`) và Phân định Kết quả

Theo mục 17 của Kế hoạch dự án và định hướng đã chốt:
1. **Chuẩn mục tiêu:** Tiếng Anh - Anh (`en-GB` / British English / Received Pronunciation).
2. **Kiến trúc phân tích:** Tự chạy mã nguồn mở (Self-hosted open source), không phụ thuộc vào cloud API trả phí (như Azure/Google Speech).
3. **Mô hình nền tảng PoC:** Wav2Vec2 XLSR-53 (`facebook/wav2vec2-xlsr-53-espeak-cv-ft`) kết hợp từ điển ngữ âm Anh-Anh (eSpeak-NG British profile `en-gb`). Chạy trên worker Python FastAPI nội bộ sau Spring Boot.
4. **Phân định rõ ràng trong học tập:**
   - Hoạt động luyện phát âm trong G0–G3 là dạng `PRACTICE` (Nghe mẫu $\rightarrow$ Tự đọc/thu âm $\rightarrow$ Tự xác nhận).
   - Module phân tích âm tự chạy (PoC sau G3) chỉ đóng vai trò **"Gợi ý luyện tập thử nghiệm" (Experimental Pronunciation Feedback)**.
   - **Tuyệt đối không dùng điểm phân tích âm để chặn hoàn thành bài học:** Nhiệm vụ thực hành phát âm được ghi nhận hoàn thành khi người học hoàn thành lượt luyện đọc/thu âm, không phụ thuộc vào việc thuật toán chấm đạt bao nhiêu điểm.
   - Các trường hợp âm thanh không rõ, nhiễu mic hoặc engine chưa đánh giá được phải trả trạng thái `"Chưa đánh giá được"` kèm điểm `null`, tuyệt đối không gán điểm 0.
