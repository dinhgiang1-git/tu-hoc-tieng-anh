# G3-04 — E2E web desktop và chốt mốc M1

## 1. Mục tiêu và baseline

Kiểm chứng vòng học dùng được hằng ngày trên **web desktop**, sửa lỗi thực sự phát hiện trong vòng học G1–G3, hoàn thiện bằng chứng M1. Codex điều phối/nghiệm thu; AI công nhân triển khai và báo cáo rồi dừng.

Baseline `e44edc0`: G3-03 đã nghiệm thu, 143 unit + 48 PostgreSQL integration + 98 frontend tests đạt. Đọc trước:

- `BE_CODE_CONVENTION.md`, `FE_CODE_CONVENTION.md`.
- `KE_HOACH_DU_AN.md` mục G3/M1, `docs/product-rules.md`.
- `docs/G3-03_ACCEPTANCE.md`, `docs/tasks/backlog.md`, `docs/api/openapi.yaml`.
- Luồng auth/session/CSRF, enrollment/Today, workspace/progress, notes, query cache và tests G3-02/G3-03.
- `docs/HUONG_DAN_CHAY.md`: phần mô tả chức năng hiện tại đã cũ, phải cập nhật theo code nghiệm thu.

**Thay đổi ưu tiên do người dùng chốt ngày 26/10/2026:** chỉ desktop trong đợt này. Hoãn mobile 375px, không sửa/tối ưu mobile, không dùng mobile làm điều kiện chặn M1 desktop. Desktop chính 1440×900; thêm kiểm tra cơ bản 1280×800, không mở ma trận đa trình duyệt rộng.

Hai file ngoài phạm vi còn từ trước: `docs/DAY2_REVIEW.md`, thư mục `backend/src/main/resources/META-INF/spring/`. Giữ nguyên, không đưa vào thay đổi G3-04.

## 2. Đầu ra bắt buộc

1. Bộ E2E bằng Playwright + Chromium chạy **browser thật → frontend thật → Spring Boot thật → PostgreSQL development thật đang dùng**.
2. Harness tạo dữ liệu và khởi chạy/dọn môi trường test có thể lặp lại bằng một lệnh; hướng dẫn Windows PowerShell phù hợp workspace hiện tại.
3. Regression cho các lỗi tìm được; chỉ sửa code cần thiết cho vòng học và UX desktop hiện có.
4. Bằng chứng 6 tiêu chí M1, hai tài khoản, hai tab, lỗi mạng và desktop; report `docs/G3-04_IMPLEMENTATION.md` và tài liệu chạy E2E.
5. Backlog chờ Codex nghiệm thu; không tự đánh dấu M1 hoàn thành trước review.

Không làm G4, quiz grading/bookmark/phát âm/admin, nội dung toàn bộ 26 tuần, offline storage hoặc thiết kế lại giao diện. Không thêm endpoint seed/reset public hoặc bypass auth để tests dễ qua. Không giải quyết bundle warning bằng đợt refactor lớn trong task này.

## 3. Chạy trực tiếp trên môi trường development — quyết định mới

Người dùng cho phép ngày 26/10/2026: **dùng trực tiếp DB development, ưu tiên tốc độ phát triển**. Mục này thay toàn bộ yêu cầu dựng PostgreSQL/container/profile biệt lập của bản handoff trước. Không cần thêm lớp hạ tầng chỉ để chạy E2E.

- Tái dùng backend `http://localhost:8080`, frontend `http://localhost:5173` và PostgreSQL `english_learning_db` hiện có (hoặc URL dev thực tế đã cấu hình). Dùng cấu hình dev/.env hiện có; không in mật khẩu/token vào log hoặc commit.
- Harness gọn: kiểm tra health/URL → chuẩn bị fixtures cần thiết → chạy Playwright. Nếu app đang chạy thì tái dùng; không kill/restart chỉ để tạo môi trường riêng. Nếu phải khởi chạy helper thì khởi chạy ẩn, chỉ cleanup tiến trình do harness tạo, không đóng app người dùng.
- **Bỏ tạo/xóa container và volume khỏi đường chạy E2E mặc định.** Không cần port riêng, runId container, guard cấm DB dev hoặc fail-closed trước Flyway cho môi trường biệt lập. Kiểm tra đơn giản URL ứng dụng/DB đang dùng và ghi trong báo cáo để tránh nhầm môi trường.
- Tạo tài khoản test `e2e_<case>_<run>` và dữ liệu enrollment/note/progress riêng trong **cùng database**. Dùng script seed gọi rõ ràng, không chạy seed mỗi lần app dev khởi động. Không thêm endpoint seed/reset public hoặc bỏ auth/CSRF.
- Ghi lại các IDs do test tạo; reset/cleanup chỉ các bản ghi test đó. Không truncate toàn bảng, drop DB, reset Flyway, xóa container PostgreSQL chung hoặc đổi mật khẩu tài khoản cá nhân như `dinhgiang1`. Không coi việc cho phép dùng DB dev là yêu cầu xóa dữ liệu đang học.
- Tái dùng pilot đã publish nếu có; nếu chưa có thì import/publish bằng luồng hiện có, không sửa revision đã publish. Fixture cross-revision có curriculum key tiền tố `e2e_` để không lẫn nội dung thật. Seed preferences theo format backend đang dùng, ngày học không cố định trong quá khứ.
- Mỗi test tự chuẩn bị tài khoản/state cần thiết; chạy riêng một case không phụ thuộc test trước. Có thể giữ dữ liệu test sau run để debug; nếu có cleanup tự động thì dùng manifest IDs của run, không xóa hàng loạt theo tên không xác minh.
- Playwright/Chromium dùng bản cài hiện có; chỉ bootstrap dependency/browser khi thiếu. Không nâng dependencies khác. Tách E2E khỏi Vitest, không để `npm test` chạy browser specs.
- Startup/readiness có timeout hữu hạn, exit code chính xác. Restore env do script thay đổi; không tự sửa file .env dùng chung. Lệnh test nhận baseURL thực tế thay vì ép dựng server thứ hai.
- Với backend regression đã có, giữ cơ chế Testcontainers hiện tại; không chuyển suite JUnit sang truncate DB dev. Thay đổi này chỉ bỏ yêu cầu biệt lập cho browser E2E G3-04 để giảm setup, không yêu cầu viết lại hạ tầng test đã chạy tốt.

## 4. Sáu tiêu chí M1 — phải có bằng chứng tương ứng

| ID | Kịch bản | Kết quả phải kiểm chứng |
|---|---|---|
| M1-1 | Login UI → chọn pilot → đăng ký → mở workspace → hoàn thành 2 nhiệm vụ hợp lệ, ghi note → reload | Tiến độ, note, response/status còn đúng từ server; version sau reload dùng tiếp được. Không chỉ kiểm tra toast/DOM trước reload. |
| M1-2 | Còn đúng nhiệm vụ bắt buộc cuối của lesson → hoàn thành/nộp qua UI | Lesson hoàn thành, Today thống nhất, gợi ý bài kế đúng revision/thứ tự hoặc Today khi không có bài kế; không link null. |
| M1-3 | Gửi cùng trạng thái với version mới nhất; double click khi pending | Một lần tính hoàn thành, không event duplicate, không tăng version vô ích; retry version cũ bị 409 đúng contract. |
| M1-4 | Undo nhiệm vụ bắt buộc đã hoàn thành, rồi reload/Today | Tiến độ lesson/tổng giảm đúng, completion bỏ đúng, response text vẫn còn; optional không đổi mẫu số. Không tạo màn hình tiến độ tuần mới chỉ để thỏa câu chữ. |
| M1-5 | Mất mạng/response chậm lúc lưu note hoặc draft; retry/recover | Giữ chữ, không báo Đã lưu giả, không rời trang sau lưu lỗi; reload sau xác nhận lưu thành công còn dữ liệu đúng. |
| M1-6 | Dùng activity/lesson từ revision khác trong request có session+CSRF hợp lệ | 404 hoặc lỗi contract tương ứng; không ghi progress/response/event/version của dữ liệu bị nhắm tới. |

Main journey phải dùng thao tác UI thật cho login/enroll/học/note. Có thể dùng API/fixture chuẩn bị phần prerequisites dài (ví dụ hoàn thành trước các yêu cầu khác để test nhiệm vụ cuối), nhưng phải mô tả rõ phần setup, giữ thao tác đang nghiệm thu trên UI. Không hoàn thành cả workflow bằng direct SQL rồi gọi đó là E2E UI.

## 5. Kịch bản bổ sung cần có

### 5.1 Phân quyền và cache hai tài khoản

- Browser contexts độc lập cho A/B; session A không dùng làm session B. B không đọc/sửa workspace, draft, note, progress hoặc delete note của A bằng UUID đã biết.
- A học/lưu note → logout → login B trong cùng context: không còn text/cache của A nhấp nháy hoặc gửi callback queued dưới session B. B → logout → A kiểm tra dữ liệu A vẫn nguyên.
- Kiểm tra request mutation thiếu/sai CSRF bị từ chối; không tắt CSRF để tests chạy. Không lưu session cookie/token vào Git.

### 5.2 Hai tab và tính đồng bộ

- Hai pages chung session đọc cùng enrollmentVersion; tab A hoàn thành, tab B gửi version cũ → 409, giữ draft B; user chọn đồng bộ rồi tự thao tác lại, không auto-submit.
- Note CAS riêng: A/B đọc cùng note version, A sửa rồi B sửa → conflict giữ nội dung B; thử dùng bản server hoặc giữ bản local sau khi đọc version thật. Không tự `version + 1`.
- Sau mutation thành công, trì hoãn request đọc workspace/detail: các action khác vẫn bị khóa. Cho một lần refetch lỗi → có retry, editor không unmount/mất text; phục hồi xong mới thao tác tiếp.
- Nếu kiểm tra concurrency trực tiếp bằng API trong browser request context thì vẫn dùng backend thật; chứng minh version/count/event DB bằng API hoặc DB assertions scoped E2E, không hardcode mock response thành công.

### 5.3 Autosave và điều hướng

- A đang save → gõ B trước khi debounce gửi B → chỉ gửi tuần tự, version mới đúng; saved chỉ khi B đã được xác nhận. Reload thấy B, không chỉ kiểm tra call count.
- Gõ note/response → bật/thoát focus, thu/mở note panel: chữ còn nguyên.
- Link Today/Bài tiếp với draft: ở lại, bỏ thay đổi, lưu note rồi rời. Save lỗi phải ở lại; khi còn response draft chưa lưu, không cho hành động lưu note rồi rời âm thầm bỏ response.
- Đổi lesson/enrollment hoặc logout lúc request bị trì hoãn: callback cũ không ghi nhầm note/version/status/cache vào scope mới.
- Kiểm tra note loading/error/retry; timeout POST đã commit nhưng response mất rồi retry không tạo hai note hoặc ghi đè bản khác âm thầm.
- Browser beforeunload là cảnh báo rời trang, không coi là cơ chế chắc chắn lưu khi đóng tab.

### 5.4 Desktop UX và nội dung

- 1440×900: sidebar trái, Today, danh mục/đăng ký, workspace, note panel/focus; không overflow ngang toàn trang, controls không bị che, scroll tới nội dung và note thuận tiện. Thêm smoke 1280×800.
- Markdown IPA/GFM table đọc được; link học liệu http/https có noopener/noreferrer, URL nguy hiểm không clickable. Raw HTML/XSS không chạy.
- Bàn phím: Tab/focus thấy rõ, form labels, submit, retry, modal, Escape thoát focus; modal không để focus lạc xuống nền trong khi quyết định bỏ/lưu draft.
- Không UUID/version kỹ thuật/tên giai đoạn triển khai trong thông báo người học. QUIZ/RECORDING/DICTATION chưa hỗ trợ không có nút complete sai policy.
- Console/page errors không có lỗi ngoài dự kiến. Phân biệt request 409/403 cố ý trong test với JavaScript exception không được xử lý; không blanket-ignore lỗi browser để đạt suite.

## 6. Thiết kế test, sửa lỗi và lưu bằng chứng

- Dùng locator role/label hoặc data-testid có chủ đích; chờ trạng thái/request/response, không dùng sleep cố định để chữa flakiness. Timeout hữu hạn, assertions theo dữ liệu thật.
- Chỉ intercept có chọn lọc để mô phỏng abort/delay/response mất. Các success path phải đi tới BE thật; báo rõ case nào có fault injection. Không mock toàn bộ `/api` rồi gọi là full E2E.
- Screenshot desktop các màn chính và focus; traces/screenshots failure để debug trong thư mục Git-ignored. Không commit traces/storageState chứa session cookie, secrets, dữ liệu cá nhân. Report có đường dẫn artifacts và test command thực chạy.
- Tìm lỗi → thêm regression tái hiện → sửa vừa đủ → chạy lại case và bộ regression liên quan. Không thay expected để che bug, bỏ test, hoặc retry nhiều lần đến xanh rồi báo ổn định.
- Chạy một lượt full E2E; sau khi ổn, chạy lặp lại **nhóm race/network/autosave/navigation** thêm một lượt để xác nhận không phụ thuộc timing. Báo từng lượt và failures/retries; không nhất thiết chạy lại mọi suite nhiều lần.
- Backend `mvnw.cmd -o -B -ntp verify`; FE `npm test`, `npm run typecheck`, `npm run lint`, `npm run build`; thêm lệnh E2E rõ ràng (ví dụ `npm run test:e2e` hoặc script gốc quản lý cả harness). Báo số tests thực tế, không cộng số tests từ báo cáo cũ.
- Nếu browser/app dev không chạy được: nêu đúng chỗ chặn, không gọi M1 đã đạt. DB dev được phép; test mô phỏng không thay bằng chứng E2E thật.

## 7. Tài liệu và điểm dừng

- `docs/HUONG_DAN_CHAY.md`: cập nhật chức năng G3-03 hiện có, cách chạy bình thường và link hướng dẫn E2E riêng. Tái dùng PostgreSQL và cấu hình dev hiện có; không sửa compose chung để phục vụ harness.
- `docs/G3-04_IMPLEMENTATION.md`: URL dev/fixture IDs và cách seed/cleanup, file và dependencies đổi, lệnh chạy, số tests, bảng mapping M1-1..6 → spec/assertion/kết quả, desktop screenshots, các lỗi đã sửa, giới hạn chưa kiểm chứng, cleanup.
- Backlog G3-04 ghi **chờ Codex nghiệm thu**, desktop-only theo yêu cầu mới. M1 đầy đủ web desktop chỉ chốt sau review; mobile deferred được ghi riêng, không đánh dấu mobile đạt.
- Không commit/push, không bắt đầu G4; Codex chịu trách nhiệm nghiệm thu và Git. Hai lượt sửa tối đa cho cùng finding, sau đó Codex tự xử lý.

## 8. Prompt gửi công nhân

```text
Triển khai G3-04 theo docs/tasks/G3-04_HANDOFF.md tại
D:/WorkSpace/tu-hoc-tieng-anh. Baseline G3-03 đã nghiệm thu e44edc0.
Ưu tiên web desktop 1440x900 và smoke 1280x800, chưa làm mobile.
Làm E2E Chromium trực tiếp với FE/BE/DB development hiện có theo mục 3 mới,
kiểm chứng 6 tiêu chí M1, hai tài khoản/hai tab, autosave/network/conflict,
navigation guards và UX desktop. Sửa lỗi thực tế kèm regression, không mock
toàn bộ API để gọi là E2E. Không endpoint seed/reset production hoặc bypass auth.
Tạo cách chạy test tái lập, cập nhật docs/HUONG_DAN_CHAY.md và report
docs/G3-04_IMPLEMENTATION.md bằng bằng chứng thật. Backlog chờ Codex review.
Được tạo dữ liệu test e2e_ trong DB dev; không xóa dữ liệu cá nhân, sửa secrets,
compose dùng chung, migration cũ hoặc file ngoài phạm vi;
không commit/push, không G4. Tối đa 2 lượt sửa cùng finding rồi Codex tự xử lý.
```
