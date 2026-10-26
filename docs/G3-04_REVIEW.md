# G3-04 — Review lần đầu: chưa nghiệm thu

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). Phạm vi **web desktop**, mobile vẫn hoãn. Baseline G3-03: `e44edc0`; handoff: `docs/tasks/G3-04_HANDOFF.md`.

## Kết quả kiểm tra độc lập

- Backend `mvnw.cmd -o -B -ntp verify`: **143 unit + 48 PostgreSQL integration tests**, không failures/errors/skips, BUILD SUCCESS, Spotless đạt. Log local `backend/target/review-g304.log`.
- FE `npm test`: **98/98 tests**; lint không error (warning form.watch đã biết), build/TypeScript đạt. Log `frontend/review-g304-unit.log`.
- `npx --no-install playwright test --list`: nhận diện **13 tests trong 3 files**. Đây chỉ là discovery, **không phải 13 tests đã chạy/đạt**.
- Không chạy `scripts/run-e2e.ps1` hiện tại vì chưa xác minh được ownership tài nguyên mà script tự xóa, và còn khả năng reuse nhầm server đang chạy. Không chỉnh DB dev hoặc code sản phẩm trong lượt review.
- Chưa có `docs/G3-04_IMPLEMENTATION.md`; tài liệu chạy và backlog chưa được cập nhật cho G3-04. Không có bằng chứng đủ để chốt M1.

## R1 — P1: harness chưa bảo đảm biệt lập và cleanup đúng tài nguyên

`scripts/run-e2e.ps1` đặt tên container cố định `english-learning-e2e-postgres`, chạy `docker rm -f` tên đó ngay từ đầu, rồi cleanup cũng xóa theo tên. Chưa kiểm tra ID/label/run identity trước xóa, có thể xóa phiên E2E khác hoặc container trùng tên không do lần chạy này tạo. Đồng thời chưa kiểm tra cổng 15432/8088/5174 trước launch; health 200/UP không chứng minh server do chính harness tạo. Vite thiếu strictPort có thể đổi cổng trong khi script probe server cũ.

Environment cũ như SERVER_PORT/E2E_DB_*/SPRING_DATASOURCE_URL có thể làm app đi khác cổng/DB mà harness nghĩ. Script chỉ cấp Vite env, không truyền cấu hình backend E2E cố định theo container mới. Không restore env sau chạy; readiness PostgreSQL nằm ngoài try/finally chính nên exception có thể để container sót. Dùng `-NoNewWindow` thay yêu cầu khởi chạy helper ẩn của handoff.

`E2EDataBootstrapRunner.verifySafetyCheck` chỉ blacklist một port/DB name, không xác minh đúng database/container dành riêng lần chạy. Quan trọng: kiểm tra ở CommandLineRunner là **sau Flyway và bootstrap khác**, quá muộn để ngăn mutation vào DB sai. Profile `e2e` đơn thuần không bảo đảm target an toàn.

**DoD R1:**

- Tên/label runId duy nhất, giữ container ID trả về, cleanup chỉ ID thuộc đúng run. Không pre-delete tài nguyên không do run sở hữu.
- Kiểm tra cổng riêng hoặc cấp cổng động; strictPort; không reuse server dev/cũ; readiness kết hợp process ownership và xác nhận đúng fixture/profile sau startup.
- Cấp rõ backend DB URL/user/password/profile/port cho container mới, loại bỏ ảnh hưởng env kế thừa và .env dev; restore env sau run. Guard target trước mọi Flyway/seed/write; không chỉ check trong runner sau startup. Missing/mismatch config phải fail closed.
- try/finally bao toàn bộ tài nguyên từ lúc tạo; lỗi startup/test/interrupt phải cleanup scoped. Helpers Windows khởi chạy ẩn, chỉ kill cây tiến trình do harness tạo. Exit code lỗi phải đúng, log riêng cho backend/frontend/test.
- Không endpoint reset/seed public hoặc production auth bypass. Nếu giữ fixture bootstrap trong main sources phải chứng minh nó không chạy được với profile dev/prod và target sai không bị migration/seed; ưu tiên test-only harness khi phù hợp.
- Có negative checks cho cổng bận, cấu hình DB sai và container cùng tên không thuộc run: dừng an toàn, không kill/xóa dịch vụ khác.

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

**DoD R3:** hoàn thiện assertions theo 6 tiêu chí M1 và các case đã giao, tạo `docs/G3-04_IMPLEMENTATION.md` có mapping criterion → spec → lệnh/kết quả/artifact. Cập nhật `docs/HUONG_DAN_CHAY.md` và hướng dẫn harness chạy lặp lại. Chạy full E2E trên môi trường biệt lập an toàn, sau đó nhóm race/network/autosave/navigation thêm một lượt; báo failures/retries thật. Không lấy list/discovery hay 98 Vitest để thay bằng chứng E2E.

## Điểm dừng / prompt sửa

Đây là yêu cầu sửa **lượt 1/2** cho R1–R3. Chỉ desktop, không mobile/G4. Nếu chưa chạy được môi trường, báo rõ chỗ chặn; không ghi M1 hoàn thành. Codex chưa commit implementation G3-04.

```text
Sửa G3-04 theo docs/G3-04_REVIEW.md, lượt 1/2.
Ưu tiên R1: harness sở hữu tài nguyên theo runId, không pre-delete/reuse nhầm,
cấu hình E2E fail closed trước migration/seed, cleanup và ports an toàn.
Sửa R2: E2E dùng selectors UI thật và mỗi test tự setup độc lập, chạy riêng được.
Sửa R3: kiểm chứng đúng cross-revision ID tồn tại, count/progress/undo,
autosave A in-flight/B, conflict/recovery và evidence theo handoff desktop.
Chạy regression + full E2E thật + nhóm race/network lần hai, cập nhật
docs/G3-04_IMPLEMENTATION.md, hướng dẫn chạy và backlog chờ Codex nghiệm thu.
Không sửa secrets, DB/compose dev, migration cũ; không commit/push, không G4.
Tối đa 2 lượt sửa cùng finding, sau đó Codex tự xử lý.
```
