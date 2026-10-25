# Review G3-01 — chưa nghiệm thu

Ngày: 25/10/2026, Asia/Ho_Chi_Minh. Phạm vi: docs/tasks/G3-01_HANDOFF.md.

## Kết quả kiểm tra

- Suite hiện có: `mvnw.cmd -o -B -ntp verify` đạt 74 unit tests + 23 PostgreSQL Testcontainers integration tests, không failures/errors/skips. Log `backend/target/review-g301.log`.
- FE: 52/52 tests, lint không error (1 warning react incompatible-library tại form.watch), TypeScript và production build đạt; bundle khoảng 567 KB còn warning.
- Reviewer bổ sung: **3/3 BE tests fail, 1/1 FE test fail**, tái hiện R1/R2/R4 bên dưới. BE dùng fixture/mock, không kết nối DB; tắt Spotless riêng khi chạy source repro tạm, không coi đó là bằng chứng formatter đạt. Log BE: `backend/target/review-g301-repro.log`.
- Source repro được giữ tại `docs/reviews/G301ReviewTest.java` và `docs/reviews/G301Review.test.tsx`; đã chuyển khỏi suite chính và xóa đúng class tạm G301ReviewTest.class. Không thay code ứng dụng để che lỗi. Để chạy lại, đặt BE vào cùng package với TodayQueryServiceTest và FE vào thư mục __tests__ của learning; BE chọn `-Dtest=G301ReviewTest#review*`.
- Không thay đổi hoặc seed DB dev; integration chỉ dùng PostgreSQL tạm.

## R1 — P1: Today dùng sai điều kiện hoàn thành buổi và ưu tiên PAUSED

`TodayQueryService.java:197` kiểm tra completedAct == totalAct trên **mọi** activity, kể cả optional. Theo product-rules, chỉ toàn bộ activity bắt buộc cần hoàn thành để đóng buổi bắt buộc. Khi buổi 1 có một activity bắt buộc đã xong, một optional chưa xong, buổi 2 đến hạn, thực tế service vẫn chọn IN_PROGRESS cho buổi 1 thay vì DUE_TODAY buổi 2.

Đã tái hiện bằng `reviewOptionalActivityDoesNotKeepLessonOpen`: expected DUE_TODAY, actual IN_PROGRESS. Sửa completion/unfinished selection nhất quán với ProgressPolicy, không sửa fixture để biến optional thành required. Kiểm tra thêm buổi không có activity bắt buộc theo policy đã thống nhất, không để optional giữ nợ bài.

Service còn suy diễn CURRICULUM_COMPLETED trước khi kiểm tra PAUSED (dòng 134–157). `reviewPausedMustTakePriorityOverCompletedCounts` chứng minh PAUSED + fixture progress đủ trả CURRICULUM_COMPLETED thay vì PAUSED. Đưa guard trạng thái enrollment trước suy diễn action, giữ primaryAction=null/overdue=[] khi paused.

## R2 — P1: Có một enrollment nhưng mất focus thì người dùng bị kẹt

`TodayPage.tsx:116` chỉ hiển thị selector khi enrollments.length > 1. Với một enrollment ACTIVE và focusedEnrollmentId=null, API đúng trả NO_ENROLLMENT, nhưng UI chỉ dẫn sang catalog; đăng ký lại cùng curriculum lại bị STATE_CONFLICT. Không có cách chọn enrollment đã có trên trang này.

Reviewer FE fixture một enrollment + null focus không tìm được combobox. Cần có lựa chọn/CTA chọn lại enrollment khi đã có từ một enrollment; selector null focus phải có placeholder để chọn được item duy nhất. Không tự đổi focus của user bằng việc chọn ngẫu nhiên ở backend.

Xử lý lỗi preferences/list/switch-focus trên UI, không bỏ qua lỗi và giả như chưa đăng ký. Regression phải thao tác chọn enrollment, xác minh PATCH preferences và Today đổi đúng dữ liệu; refresh giữ focus.

## R3 — P2: LessonSummaryPage chưa phải tóm tắt bài được chọn

`LessonSummaryPage.tsx` chỉ query enrollment, rồi hiển thị startDate và dailyTargetMinutes của **lộ trình** cùng lessonId lấy từ URL. Không tải/kiểm tra lessonId thuộc revision, không có title/thời lượng buổi/plannedDate; sửa lessonId thành bất kỳ chuỗi nào vẫn hiện màn hình thành công.

Sửa bằng dữ liệu chỉ đọc có owner/revision check; lấy tên, thời lượng thật và lịch buổi. Lesson ngoài revision/không tồn tại phải là not-found. Nếu cần endpoint summary nhẹ thì cập nhật contract/types trước; không làm workspace/checklist/Markdown G3-03. Existing enrollment ở revision ARCHIVED vẫn phải xem được summary, không phụ thuộc duy nhất public outline chỉ trả PUBLISHED.

Bỏ UUID và mã giai đoạn triển khai khỏi nội dung sản phẩm; thông báo ngắn phần học chi tiết chưa sẵn sàng là đủ. Đây là yêu cầu màn hình tóm tắt trong handoff, không yêu cầu mở rộng toàn bộ lesson workspace.

## R4 — P2: Today/list enrollment có N+1 và tải nội dung nặng

`TodayQueryService.java:102` gọi findByLessonIdOrderByOrderIndexAsc trong stream mỗi lesson. `EnrollmentQueryService.java:104` lặp tương tự trong từng enrollment. Cả hai dùng entity Lesson/Activity, mang theo Markdown/instructions/payload không cần cho dashboard.

`reviewTodayDoesNotFetchActivitiesOncePerLesson` capture đúng 3 lần gọi theo 3 lessons trong fixture; đây là tái hiện ở mock, chưa phải phép đo SQL thật. Source repository/entity xác nhận cần đổi sang batch/projection/count-query. Số query không được tăng theo lesson count; không chỉ che bằng cache hoặc đổi tên method mà vẫn lặp query.

Regression integration cần đếm query với dữ liệu lớn hơn pilot và xác minh không SELECT Markdown/payload. Dùng projection số đếm/trạng thái chung cho Today và my-enrollments khi phù hợp để hai nơi không tính tiến độ lệch nhau.

## R5 — P2: Thiếu bằng chứng concurrency và rollback cho enrollment

EnrollmentAndTodayIT mới có 2 tests: workflow tuần tự và NO_ENROLLMENT. Các case POST replay tuần tự đang tốt, nhưng chưa chạy hai request đồng thời và chưa inject lỗi sau khi bắt đầu ghi enrollment/schedule để chứng minh rollback/focus/idempotency success được dọn. Đây là DoD đã giao, không suy diễn rằng code khóa user đã được kiểm chứng chỉ vì các tests khác qua.

Bổ sung PostgreSQL biệt lập: cùng key/cùng payload đồng thời chỉ một enrollment/bộ lịch, key khác đồng thời cùng curriculum trả 201/409 không 500; lỗi giữa schedule write rollback enrollment/schedules/focus/idempotency; đăng ký mới draft/archived bị chặn; existing archived vẫn đọc; timezone London DST/cuối tháng với Clock cố định. Không mở G3-02 để tạo fixture.

## Prompt sửa — một lượt có điểm dừng

```text
Đọc docs/G3-01_REVIEW.md. Chỉ sửa R1–R5 trong G3-01.
Dùng docs/reviews/G301ReviewTest.java và G301Review.test.tsx làm repro,
đưa regression đúng vào suite chính. Sửa completion chỉ theo activity bắt buộc,
ưu tiên PAUSED; chọn lại enrollment khi có một item nhưng focus null;
summary lấy dữ liệu bài thật và kiểm tra revision/owner; projection/batch tránh
N+1 và payload nặng; bổ sung test concurrency và rollback enrollment thật.

Giữ owner/CSRF/idempotency hiện có, không sửa migration V001–V004. Nếu V005 đã
áp dụng ở DB được giữ thì dùng migration tiếp theo khi thay schema. Không đọc
secrets, không seed/import/publish/chỉnh DB dev, không làm G3-02/G3-03/G4,
không commit/push. Chạy Maven verify và FE test/typecheck/lint/build;
ghi số test thực tế, cập nhật backlog rồi dừng cho Codex nghiệm thu.

Tối đa 2 lượt sửa cùng finding, sau đó Codex tự xử lý. Không mở vòng style/doc
mới. Đạt các regression và checks trên thì chốt G3-01, không thêm phạm vi.
```
