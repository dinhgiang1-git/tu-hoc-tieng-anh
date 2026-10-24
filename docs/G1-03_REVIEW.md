# Review G1-03 và xác nhận phần sửa G1-02

Ngày: 24/10/2026. Phạm vi: các sửa A1/A2/A3 từ G1-02; frontend shell/sidebar, API client và auth. Chỉ ghi nhận xét, không sửa code ứng dụng hoặc cấu hình cầu nối AI.

## Kết luận

Backend đã sửa đúng hướng ở mức code: login gọi composite session authentication strategy trước khi lưu SecurityContext; PATCH phân biệt field vắng mặt với null; bỏ Docker host cố định và thêm regression A1/A2. Chưa xác nhận runtime vì Docker không khả dụng trong lượt review.

Frontend có cấu trúc feature, API client chung, CSRF memory, TanStack Query, form login và layout responsive. Chưa chốt G1-03 hoàn thành: cần một lượt sửa giới hạn F1–F3 dưới đây, rồi chuyển G2-01 khi kiểm tra đạt. Không mở lại G0 hoặc làm cầu nối AI trong task này.

## F1 — P1: Menu mobile tự đóng ngay khi mở

Vị trí: `frontend/src/app/layouts/AppShell.tsx:39`; `frontend/src/app/layouts/AppSidebar.tsx:31–34`.

AppShell truyền `onClose={() => setMobileDrawerOpen(false)}`. Bấm mở drawer làm AppShell render lại và tạo callback mới; effect của AppSidebar phụ thuộc `onClose` chạy lại và lập tức đóng drawer dù pathname không đổi. Nhận xét dựa trên luồng state/effect, chưa demo bằng browser trong lượt này.

Sửa callback ổn định bằng useCallback trước các early return, hoặc thiết kế việc đóng theo thay đổi route để không phụ thuộc callback đổi mỗi render. Trong cùng component, drawer đóng hiện chỉ dịch ra ngoài màn hình nên link vẫn có thể nhận focus bằng Tab; cần ẩn khỏi focus khi đóng, quản lý focus khi mở/đóng và tên accessible. Có thể dùng Dialog/Sheet primitive đã chọn, không cần thêm bộ UI mới.

Nghiệm thu: ở 375px, mở menu và menu giữ mở; Escape/click nền/chọn route đều đóng đúng; Tab không đi vào drawer đang đóng và không thoát ra nền khi drawer mở. Desktop thu gọn vẫn hoạt động.

## F2 — P1: Đăng xuất thất bại bị biến thành thành công ở phía client

Vị trí: `frontend/src/features/auth/context/AuthContext.tsx:41–49`; `features/auth/api/auth.api.ts:26–36`.

AuthProvider nuốt mọi lỗi logout rồi xóa cache. Nếu POST logout bị 403 do CSRF cũ, lỗi mạng hoặc 5xx trước khi server hủy session, HttpOnly cookie/session trên server vẫn còn hợp lệ. UI có thể về login nhưng reload lại đăng nhập với tài khoản cũ. JavaScript không thể tự hủy HttpOnly session cookie. Việc xóa cache không chứng minh server đã logout.

Sửa: có trạng thái pending/error và retry rõ; chỉ báo logout hoàn tất khi server xác nhận 204, hoặc 401 đã xác nhận phiên không còn hợp lệ. Khi CSRF_INVALID, refresh token rồi cho thử lại có giới hạn; không lặp vô hạn hoặc tự replay mutation nghiệp vụ. Có thể ẩn dữ liệu cá nhân ngay khi bắt đầu logout, nhưng nếu chưa xác nhận server phải báo chưa đăng xuất được. Hủy query cá nhân đang chạy trước khi dọn cache để response cũ không ghi lại dữ liệu.

Nghiệm thu: logout thành công rồi reload/Back không vào trang bảo vệ; giả lập logout 403/network/500 không báo thành công giả và có cách retry. Double click không tạo nhiều luồng logout đồng thời.

## F3 — P2: Chưa xử lý đúng trạng thái lỗi xác thực và phiên hết hạn

Vị trí: `frontend/src/features/auth/context/AuthContext.tsx:21–29`; `lib/api/api-client.ts:96–109`; `app/query-client.ts`.

Query /me catch mọi lỗi và trả null, vì vậy network/500 bị coi là chưa đăng nhập, không có error/retry đúng ngữ cảnh. Ngược lại, 401 từ preferences hoặc mutation chỉ được throw, không có lifecycle chung để dừng request, bỏ user đã cache, xóa dữ liệu cá nhân và CSRF; app có thể tiếp tục hiển thị trang đã xác thực bằng user cache. Token cũ cũng không được làm mới khi CSRF_INVALID nên thông báo "thử lại" có thể tiếp tục gửi cùng token lỗi.

Sửa: chỉ /me 401 mới ánh xạ thành anonymous; network/5xx có trạng thái lỗi và retry. Có điểm xử lý unauthorized chung cho request cá nhân, không áp dụng nhầm INVALID_CREDENTIALS của login thành lỗi hết phiên. Khi phiên hết hạn, cancel request, clear dữ liệu cá nhân/CSRF và chuyển login theo policy. CSRF lỗi phải có refresh/recovery hữu hạn, không tự gửi lại thao tác nghiệp vụ không an toàn. Không nuốt AbortError thành anonymous.

Nghiệm thu: /me 500 hiện lỗi/retry; /me 401 về login; đang ở trang bảo vệ mà request cá nhân trả 401 thì cache/user cũ được dọn; login sai vẫn hiện thông báo sai thông tin. Token cũ sau hết phiên có đường khôi phục để login tiếp.

## Bằng chứng kiểm tra

- Frontend: `npm run typecheck`, `npm run lint` exit 0.
- `npm run build` exit 0; có cảnh báo JS chunk 522.72 kB sau minify. Đây là việc tối ưu sau, không chặn shell. Không ghi build "không warning".
- Backend, JDK 21.0.10: `mvnw.cmd -o -B -ntp spotless:check verify`. Formatter đạt, 3 unit tests đạt. Verify thất bại vì cả hai class IT không khởi tạo được Testcontainers do không tìm thấy Docker environment; các assertion regression A1/A2 chưa được chạy. Log: `backend/target/review-g1-03.log` (file build được ignore).
- Không chạy app lên database dev, không đổi Docker/WSL/container/volume. Không thực hiện browser E2E hoặc xác nhận ảnh UI trong lượt này.
- Git local đã init và origin đúng URL người dùng cung cấp. Source vẫn untracked, chưa có bằng chứng đã commit/push; reviewer không commit/push.

## Việc nhỏ ghép vào task kế tiếp, không tạo vòng review riêng

- DTO auth hiện viết tay; OpenAPI UserResponse dùng `fullName`, `timeZone`, còn BE/FE dùng `displayName`, `timezone`. Chốt một tên theo contract, sinh lại types bằng script có dependency được pin và dùng generated DTO trong auth API. Runtime decoder hiện mặc định chỉ ép kiểu; tránh ghi đã validate response khi chưa có decoder thật.
- HomePage đang là trang kiểm tra kỹ thuật: cần error/retry cho GET/PATCH preferences thay vì fallback 30 phút khi lỗi; dọn chữ API/CSRF và các nút chưa có hành động khi chuyển sang màn học thật. Không yêu cầu thiết kế lại ở lượt sửa này.
- G2-01 phải dùng migration V003 hoặc số kế tiếp còn trống: V002 đã được dùng cho identity. G3 cũng chọn số kế tiếp thực tế, không theo số cũ trong backlog.
- Giữ task dependency baseline đã ghi trước đây cho trước public release; không mở thêm vòng nâng phiên bản trong G1-03.

## Prompt giao AI triển khai — một lượt sửa có điểm dừng

```text
Đọc docs/G1-03_REVIEW.md. Chỉ sửa F1–F3: drawer mobile, logout thật,
và auth/error/session lifecycle. Không làm cầu nối AI, không mở lại G0,
không triển khai catalog/bài học/phát âm trong lượt sửa.

Thêm kiểm chứng hành vi có ý nghĩa cho drawer và các case auth:
logout thành công/thất bại, /me 401 so với 500/network, phiên hết hạn,
CSRF recovery hữu hạn. Không mock tất cả thành success rồi báo E2E đạt.
Chạy typecheck/lint/build và BE verify trên Testcontainers biệt lập nếu
Docker sẵn sàng. Nếu Docker không dùng được thì ghi rõ, không fallback DB dev.
Cập nhật backlog theo đúng bằng chứng, báo checks và các case chưa xác nhận.

Khi các tiêu chí F1–F3 và regression BE đạt, chốt G1 rồi chuyển G2-01.
Không cần một vòng chỉnh tài liệu/style nữa. Với G2 chọn migration số
kế tiếp vì V002 đã tồn tại; đồng bộ DTO generated và contract khi làm API.
Không commit/push nếu chưa được người dùng giao bước đó.
```
