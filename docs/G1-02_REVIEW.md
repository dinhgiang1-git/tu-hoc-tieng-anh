# Review phần sửa G1-01 và triển khai G1-02

Ngày: 24/10/2026. Review có giới hạn: auth/session/CSRF, preferences, test isolation, môi trường build và tình trạng Git. Không yêu cầu làm lại G0 hoặc mở thêm nghiệp vụ lộ trình.

## Kết luận

Test đã được chuyển sang PostgreSQL Testcontainers, identity có login/logout, CSRF endpoint, principal/role, /me/preferences và bootstrap bằng biến môi trường. Formatter và 3 unit test đạt trong lần chạy reviewer.

**Còn hai lỗi code cần sửa khi hoàn thiện auth, rồi có thể chuyển G1-03.** Cấu hình Docker của test cũng cần portable; hiện chưa chạy được IT trong môi trường review nên chưa xác nhận auth runtime đạt. Không lặp lại review tài liệu hoặc mở rộng phạm vi ngoài các điểm dưới đây.

## A1 — P1: Login thủ công bỏ qua việc đổi session và làm mới CSRF

Vị trí: `backend/src/main/java/vn/englishlearning/identity/application/IdentityService.java:79–85`; đối chiếu `shared/security/SecurityConfig.java:57` và `identity/AuthAndIdentityIT.java:211–213`.

GET /auth/csrf tạo session trước đăng nhập. Sau xác thực, IdentityService giữ session đó bằng getSession(true), trực tiếp gắn SecurityContext và saveContext. Không gọi SessionAuthenticationStrategy hoặc đổi session ID/refresh CSRF trong luồng login này. Cấu hình migrateSession trong filter chain không tự thực hiện các bước đó cho một MVC controller tự xác thực và lưu context.

Tác động: session trước đăng nhập có thể tiếp tục là session đã đăng nhập, không đạt session fixation protection; token CSRF trước login vẫn được dùng. Test hiện chấp nhận fallback về cookie cũ và gửi lại token cũ sau login, nên không phát hiện thiếu sót này.

Sửa tối thiểu: đưa session authentication strategy vào luồng login thực sự, bảo đảm đổi session ID và xóa/rotate CSRF theo cùng cấu hình; lưu context sau khi hoàn tất session lifecycle. Có thể dùng composite strategy chuẩn Spring Security, không cần viết lại toàn bộ module.

Test đóng lỗi:

1. Lấy cookie/token trước login, login thành công nhận session ID khác.
2. Cookie session cũ không gọi được /me; cookie mới gọi được.
3. Lấy CSRF mới bằng cookie mới; token trước login bị từ chối trên mutation, token mới được chấp nhận.
4. Logout làm cookie đã đăng nhập không còn truy cập /me; luồng đăng nhập tiếp theo hoạt động.

Tham chiếu: [Spring Security — custom authentication/session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html). Nhận xét này dựa vào luồng code và cấu hình; chưa chạy regression HTTP vì Docker không khả dụng.

## A2 — P2: PATCH một field làm mất focusedEnrollmentId

Vị trí: `backend/src/main/java/vn/englishlearning/identity/application/IdentityService.java:171–175`; `identity/api/UpdatePreferencesRequest.java`.

Service luôn setFocusedEnrollmentId(request.focusedEnrollmentId(), now), kể cả khi JSON chỉ có dailyTargetMinutes. DTO hiện không phân biệt field không gửi với explicit null; vì vậy đổi mục tiêu học sẽ xóa lộ trình đang tập trung.

Sửa tối thiểu: biểu diễn sự hiện diện của field PATCH (absent/value/null), chỉ cập nhật khi field được gửi; explicit null mới là thao tác clear. Giữ validation cho field đã gửi. Khi enrollment module xuất hiện ở G3, kiểm tra owner/revision/state trước khi nhận focusedEnrollmentId; chưa cần xây module đó ở G1.

Test đóng lỗi: có focused enrollment ID → PATCH chỉ dailyTargetMinutes phải giữ ID; PATCH explicit null mới xóa. Empty PATCH không đổi dữ liệu ngoài metadata theo policy.

## A3 — Kiểm chứng môi trường: không hardcode Docker endpoint của một máy

Vị trí: `backend/src/test/resources/testcontainers.properties:1`.

File đang cố định docker.host=tcp://127.0.0.1:2375. Lần chạy reviewer báo endpoint không lắng nghe. Docker desktop-linux named pipe cũng không khả dụng khi kiểm tra, vì vậy đây không chỉ là bằng chứng test ứng dụng thất bại: Docker daemon/môi trường hiện chưa sẵn sàng để chạy IT.

Cho Testcontainers tự phát hiện Docker mặc định; cấu hình đặc thù máy đi qua môi trường local được ghi trong README, không commit endpoint cố định. Không mở Docker TCP không mã hóa để làm test chạy. Giữ không fallback sang DB dev.

Khi Docker sẵn sàng, chạy verify với container riêng và báo kết quả. Không được nói IT xanh dựa vào unit tests hoặc report cũ. Không bỏ qua IT mặc định để qua build.

## Bằng chứng kiểm tra

JDK 21.0.10 đặt qua JAVA_HOME/PATH ở process của lệnh. Đã chạy:

```text
.\mvnw.cmd -o -B -ntp spotless:check verify
```

- Formatter đạt; 3 unit tests đạt.
- IT của cả hai class không khởi tạo được vì không tìm thấy Docker environment; verify kết thúc BUILD FAILURE. Không có test assertion auth nào được chạy thành công trong lượt này.
- Thử dùng endpoint named pipe từ context desktop-linux cũng không kết nối được daemon; dừng kiểm tra thay vì đổi hạ tầng máy.
- Không chạy hoặc ghi vào database dev. Test datasource trong code hiện được inject từ container.
- Lệnh đọc remote GitHub bị lỗi kết nối mạng/DNS ở môi trường thực thi; chưa xác minh được nội dung hoặc branch remote. Không suy ra repo không tồn tại từ lỗi đó.

## Tình trạng repository

URL người dùng cung cấp: `https://github.com/dinhgiang1-git/tu-hoc-tieng-anh.git`.

Workspace `D:/WorkSpace/tu-hoc-tieng-anh` hiện chưa có .git; git status và git remote báo not a git repository. Reviewer chưa init, tạo commit, gắn remote hoặc push; việc tạo repo trên GitHub chưa tự nối thư mục local.

Giao việc Git kế tiếp: init tại root, rà .gitignore và source secrets, kiểm tra remote/branch hiện có rồi gắn origin. Nếu remote đã có nội dung, phải fetch/đối chiếu trước khi tích hợp; không force push hoặc ghi đè lịch sử. Không đưa .env, target, test logs hoặc audio/model cache vào commit.

## Hướng chuyển giai đoạn có điểm dừng rõ

Giao một task sửa A1/A2 và portable Docker config trước khi tích hợp auth FE; có thể làm shell/sidebar G1-03 song song theo contract đã có. Khi regression trên DB biệt lập và verify đạt, không cần thêm vòng G0/G1-01 review, chuyển thẳng sang tích hợp FE auth.

Việc dependency mới chỉ nâng tới Spring Boot 3.4.3 chưa giải quyết trọn lựa chọn nhánh đang được duy trì đã nêu ở review trước. Ghi một task cập nhật baseline tương thích và ADR trước khi triển khai public; không dùng việc đổi version lặp lại làm vòng sửa vô hạn của UI skeleton.

Prompt giao AI:

```text
Đọc docs/G1-02_REVIEW.md. Sửa đúng A1/A2, thêm regression tests tương ứng.
Làm portable Testcontainers config theo A3; không đổi DB/volume hiện có,
không mở Docker TCP không mã hóa, không fallback test về DB dev.

Nếu Docker chưa sẵn sàng thì báo rõ, không báo verify thành công.
Khi regression và verify trên DB biệt lập đạt, chuyển G1-03:
React TSX/Tailwind shell, sidebar, API client, login/logout/CSRF flow.
Chưa làm catalog/bài học/AI phát âm.

Repo GitHub đã được người dùng cung cấp trong report. Chuẩn bị nối Git
tại workspace root, rà ignore/secret và trạng thái remote; không force push.
Chưa push mã nguồn khi chưa được người dùng giao bước xuất bản đó.
Cuối lượt báo checks, cách demo và hạn chế thực tế, không mở lại G0.
```
