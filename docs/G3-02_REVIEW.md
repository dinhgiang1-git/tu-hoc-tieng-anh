# G3-02 — review đầu tiên, chưa nghiệm thu

Ngày: 25/10/2026 (Asia/Ho_Chi_Minh). Phạm vi: docs/tasks/G3-02_HANDOFF.md.

## Bằng chứng

- Suite hiện có: `mvnw.cmd -o -B -ntp verify` đạt **113 unit + 37 PostgreSQL integration tests**, không failures/errors/skips, Spotless đạt. Log local: `backend/target/review-g302.log`.
- FE: **59/59 tests**, lint không error (warning form.watch cũ), production build và TypeScript đạt; bundle khoảng 579 KB có warning.
- Reviewer bổ sung **1 PostgreSQL IT và 1 FE test, cả hai fail**, tái hiện R1/R2 dưới đây. Log IT: `backend/target/review-g302-repro.log`. Source tại `docs/reviews/G302ReviewIT.java` và `docs/reviews/G302Review.test.tsx`.
- IT kế thừa fixture LearningProgressIT, dùng DB Testcontainers; Clock được spy để giữ cùng instant với updatedAt enrollment. Chạy `mvnw.cmd -o -B -ntp -Dspotless.skip=true -Dit.test=G302ReviewIT#review* test-compile failsafe:integration-test failsafe:verify`. Spotless chỉ tắt cho source repro tạm, không phải bằng chứng formatter đạt ở lượt repro.
- FE đặt repro vào `src/features/learning/__tests__`, chạy `npm test -- src/features/learning/__tests__/G302Review.test.tsx`.
- Đã chuyển hai repro ra khỏi suite chính và xóa đúng class tạm G302ReviewIT.class, không thay đổi app để che lỗi. Không thay đổi DB dev.

## R1 — P1: Tăng version phụ thuộc đồng hồ thay vì thay đổi tiến độ

`LearningProgressService.updateProgress` và `submitResponse` gọi `enrollment.setUpdatedAt(now)` rồi `saveAndFlush`. Khi status enrollment/completedAt không đổi và now bằng updatedAt hiện tại, Hibernate không thấy enrollment dirty nên **không UPDATE enrollment và không tăng @Version**, dù activity_progress và event đã thay đổi.

IT `reviewRealChangeAdvancesVersionWhenClockDoesNotAdvance` tạo enrollment thật, đóng băng Clock ở updatedAt hiện tại, hoàn thành một READING (chưa hoàn thành toàn bộ curriculum). HTTP 200 và progress đã ghi nhưng response version=1, expected=2. Đây là tái hiện PostgreSQL thật, không suy diễn từ Mockito. Clock cố định là cơ chế test chuẩn của dự án; hai thay đổi trong cùng độ phân giải thời gian cũng không được mất kiểm soát concurrency.

Sửa để mỗi thay đổi thực sự **luôn tăng enrollment version đúng một lần**, độc lập updatedAt. Response phải chứa version đã ghi; không tăng thủ công rồi để Hibernate cộng lần nữa, không force-increment trì hoãn đến commit mà dựng response version cũ. No-op không tăng version và không đổi timestamps/event. Áp dụng thống nhất cho complete/undo/submit.

Regression PostgreSQL bắt buộc: Clock không tiến vẫn tăng đúng version; no-op cùng version không tăng; request version cũ trả 409; hai mutation **khác activities** cùng expectedVersion tối đa một commit; submit đổi text cũng tăng version dù thời điểm không đổi. Giữ rollback nguyên tử của response/progress/event/schedule. Hoàn tất case race tạo enrollment mới với undo completed cũ theo handoff để kiểm chứng lock order/partial unique index, không chỉ test hai thao tác tuần tự.

## R2 — P1: FE mở mutation khi version/state còn cũ

`useLearningProgress.ts` bỏ qua ProgressResponse, onSuccess chỉ gọi `void invalidateQueries(...)`. Mutation kết thúc trước refetch activities, còn ActivityProgressPanel chỉ khóa theo mutation.isPending. Khi refetch chậm, nút kế tiếp bật lại với enrollmentVersion cũ; người dùng thao tác liên tiếp sẽ tự tạo VERSION_CONFLICT hoặc lặp thao tác ngoài ý muốn.

Reviewer FE trả lần đọc đầu version=1, mutation thành công trả version=2 và giữ lần refetch ở trạng thái pending. Nút activity thứ hai **không disabled**, UI vẫn hiển thị version=1. Test fail đúng thời điểm race, không chỉ test thứ tự gọi invalidation.

Sửa một trong các luồng nhất quán: cập nhật cache từ response server rồi đồng bộ đầy đủ, hoặc giữ pending đến khi refetch cần thiết hoàn tất. Không cho thao tác dựa trên version cũ khi đang sync; lỗi refetch phải hiển thị và có retry, không báo mutation đã đồng bộ khi chưa biết state. Invalidate/cập nhật các keys activities/summary/Today/detail/my-enrollments; không tự tính % từ số checkbox.

Giữ text người dùng khi conflict/network. Phân biệt `VERSION_CONFLICT` với `STATE_CONFLICT`, không coi mọi HTTP 409 là dữ liệu đã bị sửa ở tab khác. Sau conflict giữ khóa thao tác cho đến khi version mới được đọc, không tự submit lại.

Related lifecycle guard trong cùng luồng UI: LessonSummaryPage hiện không chờ/hiển thị lỗi enrollmentDetailQuery; status undefined làm isReadOnly=false. Khi detail pending/error vẫn bật các controls. Cần chờ trạng thái enrollment đáng tin cậy trước khi cho mutation; PAUSED/ABANDONED phải chỉ đọc thực sự. Backend vẫn giữ check riêng.

Test FE bằng API mocks/refetch deferred thật: mutation version=2 + đọc lại đang pending không gửi tiếp version=1; khi dữ liệu mới sẵn sàng thao tác kế tiếp dùng version=2; lỗi refetch có recovery; conflict giữ draft, không auto-resubmit; detail pending/error không mở mutation; paused/abandoned read-only; không phá undo trên COMPLETED hợp lệ.

## Điểm dừng và prompt sửa

Chỉ R1/R2, chưa chuyển G3-03. Không mở thêm vòng chỉnh tài liệu/style. Công nhân có tối đa **hai lượt sửa cho cùng finding**, đây là yêu cầu sửa lần thứ nhất; sau đó Codex tự xử lý phần còn lại.

```text
Đọc docs/G3-02_REVIEW.md và hai repro trong docs/reviews/G302Review*.
Chỉ sửa R1/R2: version enrollment phải tăng chính xác cho thay đổi thật dù
Clock đứng yên; FE phải đồng bộ version/state an toàn trước mutation tiếp,
giữ text khi conflict, phân biệt lỗi và chặn thao tác khi lifecycle chưa rõ.
Đưa regression vào suite chính, không sửa kỳ vọng để che bug.
Chạy PostgreSQL Testcontainers biệt lập cho fixed Clock/no-op/concurrent khác
activity/rollback và race create-vs-undo. FE test refetch deferred và lifecycle.
Giữ giới hạn G3-02, không chỉnh secrets/DB dev/migration cũ, không commit/push.
Chạy Maven verify, FE test/typecheck/lint/build, báo bằng chứng rồi dừng.
Tối đa hai lượt sửa cùng finding, còn lại Codex tự xử lý.
```
