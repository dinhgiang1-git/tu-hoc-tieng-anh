# G3-04 — Review lần đầu: chưa nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). Phạm vi **web desktop**, mobile vẫn hoãn. Baseline G3-03: `e44edc0`; handoff: `docs/tasks/G3-04_HANDOFF.md`.

## Kết quả kiểm tra độc lập

- Backend `mvnw.cmd -o -B -ntp verify`: **143 unit + 48 PostgreSQL integration tests**, không failures/errors/skips, BUILD SUCCESS, Spotless đạt. Log local `backend/target/review-g304.log`.
- FE `npm test`: **98/98 tests**; lint không error (warning form.watch đã biết), build/TypeScript đạt. Log `frontend/review-g304-unit.log`.
- `npx --no-install playwright test --list`: nhận diện **13 tests trong 3 files**. Đây chỉ là discovery, **không phải 13 tests đã chạy/đạt**.
- Không chạy `scripts/run-e2e.ps1` hiện tại vì chưa xác minh được ownership tài nguyên mà script tự xóa, và còn khả năng reuse nhầm server đang chạy. Lượt review trước chưa chỉnh DB dev hoặc code sản phẩm; từ quyết định mới bên dưới, E2E được phép ghi dữ liệu test trực tiếp vào DB dev.
- Chưa có `docs/G3-04_IMPLEMENTATION.md`; tài liệu chạy và backlog chưa được cập nhật cho G3-04. Không có bằng chứng đủ để chốt M1.

## R1 — Đã thay đổi yêu cầu: dùng trực tiếp DB dev, không dựng môi trường riêng

**Quyết định mới của người dùng ngày 26/10/2026:** chấp nhận dùng trực tiếp database development để tăng tốc. Bỏ yêu cầu dựng container/profile/cổng E2E riêng, runId ownership cho container, guard cấm DB dev và negative tests phục vụ môi trường biệt lập. Không dùng các yêu cầu cũ này để chặn nghiệm thu.

Thay R1 bằng đường chạy đơn giản:

- Tái dùng app dev và DB hiện có, health-check đúng baseURL; seed tài khoản/dữ liệu test tiền tố `e2e_` bằng script gọi rõ ràng.
- Bỏ tạo/xóa container khỏi harness mặc định; không chạy `docker rm -f` tên cố định. Không cần startup/shutdown một bộ FE/BE/DB mới mỗi lượt.
- Mỗi case tự setup và ghi manifest IDs; cleanup/reset chỉ dữ liệu test do case/run tạo. Không truncate/drop DB, xóa dữ liệu đang học, đổi tài khoản cá nhân hoặc sửa migration cũ.
- Không in secrets, không public seed/reset endpoint, không bỏ auth/CSRF. Tái dùng .env local, không commit credentials.
- Không kill app người dùng; nếu harness có launch helper thì chỉ cleanup tiến trình con của mình. Backend regression JUnit giữ Testcontainers hiện có, không cần viết lại.

Đây là **đơn giản hóa yêu cầu theo ủy quyền mới**, không phải lỗi cần công nhân đầu tư thêm hạ tầng. Handoff mục 3 mới là nguồn chuẩn. R2/R3 về test đúng và bằng chứng thật vẫn cần hoàn tất.

## R2 — P1: E2E không khớp UI và phụ thuộc thứ tự test

Sai lệch trực tiếp từ source hiện tại:

- Các specs tìm `[data-testid="activity-card"]`, trong khi component thực tế là `activity-card-${activityId}`. Locator này trả rỗng, không đến được thao tác học.
- Nút đăng ký tìm `Đăng ký học lộ trình này`, UI là `Đăng ký học`. M1-1 còn chờ dialog chứa `Thiết lập lịch học`, title thực là `Đăng ký lộ trình học`.
- Banner complete/bài kế/modal guard dùng chuỗi và role không có ở page hiện tại. Workspace blocker dùng data-testid riêng, chưa có role=dialog; phải kiểm tra accessibility thực, sửa hợp lý nếu thiếu, không đổi text sản phẩm chỉ để hợp test đoán trước.
- Sau reload note, editor khởi tạo trạng thái idle; test yêu cầu nhãn Đã lưu ngay sau reload dù chưa thực hiện write, thay vì kiểm tra nội dung và dữ liệu server đã persist.

`m1-core-loop.e2e.ts` dùng enrollmentId/lesson1Id/lesson2Id biến chung được gán ở M1-1, các test sau dựa vào biến và state đó. Chạy riêng `-g M1-2`, hoặc test đầu lỗi/restart worker sẽ dùng undefined ID. `learner_nav` và `learner_a` cũng dùng chung state qua nhiều specs mà không có setup độc lập; lỗi ở test trước tạo lỗi dây chuyền. workers=1 không sửa được sự phụ thuộc này.

**DoD R2:** sử dụng locator theo role/label hoặc testid thật, helpers login/enroll lấy dữ liệu đúng trạng thái; fixture/setup riêng từng test. Chạy được từng M1 test riêng lẻ, toàn suite và nhóm chọn lọc lần hai. Đọc text/assertion từ hành vi thực, không optional-if im lặng bỏ qua thao tác đang nghiệm thu. Không sửa kỳ vọng để che lỗi app. Giữ browser FE/BE thật, chỉ fault-inject chọn lọc.

## R3 — P1: tên test đang rộng hơn điều được kiểm chứng, thiếu bằng chứng M1

Các gaps đã nằm trong handoff, không phải yêu cầu mở rộng:

- M1-6 tạo một UUID không tồn tại (`...0999`) rồi kiểm tra 404. Điều này chỉ chứng minh missing resource, chưa chứng minh activity **tồn tại nhưng thuộc revision khác** bị từ chối. Fixture secondary đã có nhưng không được dùng. Cần lấy ID fixture thật, xác minh nó tồn tại trong setup và state mục tiêu không đổi sau request trái quyền/revision.
- M1-4 không assert số tiến độ trước/sau; chỉ check text phản tư và một nhãn tổng quát. Comment nói mẫu số pilot 21 sai: yêu cầu hiệu lực pilot là 19 (lesson.required && activity.required). Phải kiểm tra summary/Today/count/lesson completion trước/sau và optional không đổi mẫu số, không hardcode số sai trong app.
- Autosave serialization chỉ fill A rồi lập tức fill B trước khi gửi, không giữ A in-flight. Như vậy mới chứng minh debounce/coalesce trước write, chưa chứng minh serialize A pending → B dùng version mới. Cần delay request thật có kiểm soát, xác nhận A bắt đầu, nhập B, đo không có write chồng và reload ra B.
- Chưa có note CAS hai tab/đọc bản server/overwrite an toàn; chưa có trì hoãn và lỗi reconcile progress; chưa có callback cũ khi chuyển scope/logout hoặc POST đã commit nhưng mất response. Dùng các case bounded trong handoff; không mock toàn API.
- Context A/B có một phần owner/CSRF nhưng cần chứng minh không đọc/sửa progress/draft/note của A, cache A/B và return A vẫn đúng bằng state thật. Logout helper fallback POST không có CSRF, không kiểm tra kết quả; không được coi goto login là logout thành công.
- Desktop test mới check overflow/links; chưa có screenshots các màn chính, keyboard/modal focus hay report. Hiện còn `waitForTimeout(300)`; thay bằng điều kiện layout ổn định. Không blanket-ignore console/page exception.

**DoD R3:** hoàn thiện assertions theo 6 tiêu chí M1 và các case đã giao, tạo `docs/G3-04_IMPLEMENTATION.md` có mapping criterion → spec → lệnh/kết quả/artifact. Cập nhật `docs/HUONG_DAN_CHAY.md` và hướng dẫn harness chạy lặp lại. Chạy full E2E trên app/DB development với dữ liệu test riêng, sau đó nhóm race/network/autosave/navigation thêm một lượt; báo failures/retries thật. Không lấy list/discovery hay 98 Vitest để thay bằng chứng E2E.

## Điểm dừng / prompt sửa

Đây là yêu cầu sửa **lượt 1/2** cho R1–R3. Chỉ desktop, không mobile/G4. Nếu chưa chạy được môi trường, báo rõ chỗ chặn; không ghi M1 hoàn thành. Codex chưa commit implementation G3-04.

```text
Sửa G3-04 theo docs/G3-04_REVIEW.md, lượt 1/2.
R1 đã thay đổi theo người dùng: tái dùng trực tiếp FE/BE/DB dev, bỏ dựng/xóa
container riêng. Chỉ seed/reset dữ liệu test e2e_, không xóa dữ liệu cá nhân.
Sửa R2: E2E dùng selectors UI thật và mỗi test tự setup độc lập, chạy riêng được.
Sửa R3: kiểm chứng đúng cross-revision ID tồn tại, count/progress/undo,
autosave A in-flight/B, conflict/recovery và evidence theo handoff desktop.
Chạy regression + full E2E thật + nhóm race/network lần hai, cập nhật
docs/G3-04_IMPLEMENTATION.md, hướng dẫn chạy và backlog chờ Codex nghiệm thu.
Được dùng DB dev cho dữ liệu test; không xóa dữ liệu thật, sửa secrets/compose
chung hoặc migration cũ; không commit/push, không G4.
Tối đa 2 lượt sửa cùng finding, sau đó Codex tự xử lý.
```
