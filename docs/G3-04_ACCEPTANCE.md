# G3-04 — Nghiệm thu M1 web desktop

Ngày 26/10/2026 (Asia/Ho_Chi_Minh). **ĐẠT G3-04 và M1 cho web desktop**. Mobile hoãn theo yêu cầu người dùng, không được coi là đã kiểm chứng.

## Bằng chứng độc lập của Codex

| Kiểm tra | Kết quả | Log local |
|---|---|---|
| Full E2E Chromium, FE/BE/PostgreSQL dev thật | **14/14 đạt**, không retry, khoảng 2,2 phút | `frontend/review-g304-e2e.log` |
| Chạy lại network, hai tài khoản/hai tab, autosave, navigation, desktop sau sửa reviewer | **9/9 đạt**, không retry, khoảng 1,4 phút | `frontend/review-g304-repeat.log` |
| Desktop cuối, gồm pageerror assertion | **1/1 đạt**, 1440×900 và 1280×800 | `frontend/review-g304-desktop-final.log` |
| Backend Maven verify | **143 unit + 48 PostgreSQL integration**, không fail/error/skip, Spotless và BUILD SUCCESS | `backend/target/review-g304-round1.log` |
| FE regression | **98/98 tests**; lint không error, build/TypeScript đạt | `frontend/review-g304-final-regression.log` |

E2E chạy trực tiếp `localhost:5173` → `localhost:8080` → `english_learning_db`, đúng quyết định của người dùng. Backend JUnit giữ Testcontainers hiện có. Không dựng/xóa container cho browser E2E. Không đổi mật khẩu hoặc reset dữ liệu tài khoản cá nhân; seed/reset giới hạn danh sách 17 tài khoản E2E định danh trong SQL.

## Sáu tiêu chí M1

1. **M1-1:** UI login/chọn pilot/enroll, hoàn thành 2 nhiệm vụ và note; reload giữ state/text.
2. **M1-2:** Hoàn thành yêu cầu cuối của lesson; banner, bài kế và Today có dữ liệu hợp lệ.
3. **M1-3:** Thay đổi thật tăng version, cùng trạng thái/version mới là no-op, request version cũ nhận VERSION_CONFLICT; backend integration bổ sung kiểm chứng event/no-op/concurrency.
4. **M1-4:** Complete/undo rồi reload cập nhật status và tổng tiến độ, mẫu số hiệu lực pilot là 19. Quy tắc response/optional và completion được bảo vệ thêm bởi regression G3-02/G3-03; không có màn hình tuần mới trong task này.
5. **M1-5:** Abort lưu note giữ chữ, không báo saved, guard rời trang và retry hoạt động; reload có nội dung đã lưu.
6. **M1-6:** Fixture activity/lesson tồn tại ở revision khác bị từ chối, version enrollment không đổi.

Cases chính tại `frontend/e2e/m1-core-loop.e2e.ts`; cases owner/session/CAS ở `multi-account-and-sync.e2e.ts`; autosave/focus/navigation/desktop ở `navigation-and-desktop-ux.e2e.ts`.

## Các phần reviewer tự hoàn thiện

- Seed SQL có transaction và ON_ERROR_STOP, reset theo allowlist chính xác 17 usernames; không dùng LIKE prefix rộng. Không disable trigger/foreign key hoặc xóa/tạo lại cây curriculum published; fixture secondary đúng ID được tái dùng.
- Runner chọn container được cấu hình chính xác thay vì postgres đầu tiên; kiểm tra native exit code. SQL lỗi dừng trước test. Giữ UTF-8 qua pipeline Windows PowerShell để tên fixture tiếng Việt không bị hỏng. `-TestFilter` hỗ trợ file spec hoặc regex tên test.
- Repro browser thật cho thấy modal rời trang chưa nhận focus. Đã sửa focus ban đầu, giữ Tab trong modal, Escape ở lại, và khôi phục focus khi đóng; test bàn phím sau sửa đạt. Log tái hiện trước sửa: `frontend/review-g304-keyboard-repro.log`.
- Autosave test giữ response A thật đang pending, nhập B, xác nhận tối đa một write in-flight và B dùng version A trả về. Không dùng delay cố định để giả định A đã bắt đầu. Desktop test kiểm tra JavaScript page errors riêng với lỗi HTTP cố ý.
- Đã xem screenshots Today/catalog/workspace/focus desktop; nội dung IPA đọc được, không tràn ngang ở hai viewport đã test. Artifacts trong `frontend/e2e-screenshots/`, được Git ignore cùng traces/videos/session artifacts.

## Giới hạn

- M1 là vòng học pilot trên Chromium desktop, không phải toàn bộ sản phẩm G4–G7 hay mọi tổ hợp trình duyệt, kích thước và lỗi mạng.
- Warning còn: form.watch ở EnrollmentDialog, bundle khoảng 685 KB. Không chặn vòng học M1.
- Mobile chưa nghiệm thu. Chưa bắt đầu G4; bước tiếp theo là lịch cá nhân linh hoạt theo backlog.
- DB dev còn tài khoản/fixture E2E để debug. Fixture curriculum phụ được seed từ lượt cũ có tên mất dấu trong catalog; pilot và nội dung học chính hiển thị đúng. Không sửa nội dung revision đã publish để chữa dữ liệu fixture.
