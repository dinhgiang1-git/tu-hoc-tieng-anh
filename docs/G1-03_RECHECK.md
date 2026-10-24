# Kiểm tra lại G1-03 sau lượt sửa F1–F3

## Chốt phần FE G1-03 — 24/10/2026

Đã sửa trực tiếp hai nhánh session expiry và logout bằng cùng helper: chỉ giữ query có key **chính xác** `authKeys.me()` rồi set null, xóa mọi query khác gồm `authKeys.preferences()` và cả các key con của `/me`. Các ghi chú lỗi bên dưới là lịch sử review, không còn là blocker FE.

Kiểm chứng sau sửa:

- Suite chính: **24/24 tests đạt**, gồm query 401 với preferences thật, mutation 401, logout xóa preferences/key con, A hết phiên → B đăng nhập và không có dữ liệu A cả khi request preferences B đang pending hoặc lỗi.
- Bản tái hiện `docs/reviews/G1-03-real-preferences-repro.test.tsx`: **3/3 đạt**, đã dọn file chạy tạm khỏi frontend.
- Typecheck/lint/build đạt; build có cảnh báo chunk **528.89 kB**. Không đưa tối ưu bundle vào vòng sửa auth này.
- Không thay đổi backend. BE integration vẫn chờ Docker runtime để xác minh lại như đã ghi; không dùng DB dev. Không bổ sung bằng chứng browser E2E trong lượt này.

**Điểm dừng đã đạt cho phần FE. Chuyển G2-01 theo `docs/tasks/G2-01_HANDOFF.md`, không tiếp tục vòng sửa auth/style.**

---

## Kết quả kiểm tra mới nhất — 24/10/2026, 23:20

**R2 đạt; R1 còn đúng một lỗi lọc query key.** 401 từ mutation đã đưa user về anonymous. Typecheck/lint đạt, suite chính 23/23 đạt, build đạt với cảnh báo chunk 528.92 kB. Docker Engine vẫn không khả dụng qua named pipe, chưa xác minh lại BE integration tests. Không có browser E2E trong lượt này.

Tại `frontend/src/features/auth/context/AuthContext.tsx:56` và `:117`, cả expiry lẫn logout dùng `query.queryKey[0] !== "auth"` để chọn cache cần xóa. Nhưng `authKeys.preferences()` thực tế trả về `["auth", "preferences"]` (`auth.keys.ts:4`), nên dữ liệu preferences của tài khoản cũ được giữ lại. Không thể dùng “non-auth” làm đồng nghĩa cho dữ liệu cá nhân.

Đã chạy lại regression với **đúng key và payload preferences đang dùng**, seed `dailyTargetMinutes=90`, rồi GET preferences trả 401: user về null nhưng cache vẫn trả `{dailyTargetMinutes:90, focusedEnrollmentId:null}`. Test yêu cầu xóa cache thất bại. Hai case mutation 401 và login retry CSRF đã đạt. Test A→B trong suite chính đang seed `["personal", "account-a"]`, nên chưa bắt được trường hợp key thật này.

Bản tái hiện: `docs/reviews/G1-03-real-preferences-repro.test.tsx`. Đã chuyển file test tạm khỏi frontend; không sửa code ứng dụng.

**Chỉ giao sửa phần còn lại của R1:** sau expiry và logout thành công, xóa cả `authKeys.preferences()` cùng các cache cá nhân khác; nếu giữ query `/me`, chỉ giữ **chính xác key `/me`** rồi set null, không giữ cả prefix `auth`. Regression dùng `authKeys.preferences()` thật, kiểm tra cả logout và A hết phiên → B đăng nhập, bao gồm lúc response preferences mới còn pending/lỗi. Không sửa lại R2, F1/F2 hoặc mở rộng UI/DTO. Khi case này và các check hiện có đạt thì chốt FE G1-03, chuyển G2-01; giữ trạng thái xác minh BE runtime riêng nếu Docker chưa chạy.

---

Ngày: 24/10/2026. Chỉ review; không sửa code ứng dụng, không commit/push, không làm tiếp Agent Desk.

## Kết luận

Chưa chốt G1-03. Lỗi mobile drawer tự đóng đã được xử lý; logout đã có xác nhận server, retry CSRF giới hạn và báo lỗi. `/me` đã phân biệt 401 với lỗi mạng/500. Còn hai điểm thuộc **F3 đã yêu cầu từ lượt trước**, được tái hiện bằng ba kiểm thử hành vi bổ sung. Không mở thêm vòng sửa style, DTO hoặc kiến trúc ngoài phạm vi này.

## R1 — P1: Lifecycle hết phiên bỏ sót mutation và chưa xóa cache cá nhân

Vị trí: `frontend/src/features/auth/context/AuthContext.tsx:51–62`.

Listener chỉ đăng ký `getQueryCache()`, nên PATCH preferences trả 401 `AUTHENTICATION_REQUIRED` qua mutation không kích hoạt dọn phiên. User trong AuthContext vẫn tồn tại và trang bảo vệ vẫn hiển thị trạng thái đăng nhập. HomePage hiện đã dùng mutation này, không phải tình huống giả định cho tính năng tương lai.

Ngay cả với GET/query 401, đoạn xử lý chỉ cancel queries rồi set `/me` về null và clear CSRF. Cancel không xóa dữ liệu đã cache: cache cá nhân của tài khoản trước vẫn tồn tại. Đăng nhập tài khoản khác có thể lấy lại dữ liệu đó trong thời gian stale nếu dùng cùng query key. Test hiện có phát thủ công QueryCache event và chỉ kiểm tra user/CSRF, chưa kiểm tra mutation hoặc cache dữ liệu cá nhân.

Bằng chứng bổ sung:

- Thực thi mutation thật trong TanStack Query, đi qua `authApi.updatePreferences`, mock HTTP 401: yêu cầu user=null thất bại, vẫn nhận user A.
- Thực thi query thật trả 401, có sẵn cache cá nhân: user về null nhưng yêu cầu cache cá nhân bị xóa thất bại, dữ liệu vẫn còn.

Sửa giới hạn: một lifecycle chung nhận unauthorized từ cả request query và mutation được bảo vệ, phân biệt login sai thông tin, cancel/await request đang chạy và loại dữ liệu cá nhân cũ trước khi chuyển anonymous. Đừng chỉ đặt user=null. Thêm regression qua request/mutation thật, không chỉ notify cache bằng tay.

## R2 — P1: Login lỗi CSRF vẫn mắc kẹt với token cũ

Vị trí: `frontend/src/features/auth/api/auth.api.ts:17–24`; `frontend/src/lib/api/api-client.ts:108–109`; `frontend/src/features/auth/pages/LoginPage.tsx:46–47`.

Login chỉ refresh CSRF sau khi POST thành công. Khi POST trả 403 `CSRF_INVALID`, API client chỉ throw; trang login yêu cầu thử lại nhưng không clear/refresh token. Lần bấm tiếp theo tiếp tục dùng token cũ, nên không thoát được lỗi nếu không reload. Logout có recovery riêng không giải quyết được login hoặc mutation khác.

Bằng chứng bổ sung: mock endpoint CSRF trả token stale lần đầu và fresh từ lần hai; login lần đầu nhận 403, gọi login lần hai vẫn nhận 403 vì client không yêu cầu token mới.

Sửa giới hạn: clear/refresh CSRF khi nhận CSRF_INVALID, recovery hữu hạn và có lỗi rõ nếu lấy token mới thất bại. Cho người dùng thử lại với token mới; không tự replay mutation nghiệp vụ. Thêm regression cho login hai lần và PATCH lỗi CSRF mà không tự gửi lại PATCH.

## Bằng chứng chạy kiểm tra

- `npm run typecheck`: đạt.
- `npm run lint`: đạt.
- `npm test`: 18/18 kiểm thử hiện có đạt.
- Ba kiểm thử review bổ sung: **3/3 fail đúng các kết quả R1/R2 ở trên**. Bản tái hiện nằm tại `docs/reviews/G1-03-repro.test.tsx`. Đã chuyển khỏi frontend sau khi chạy, không để test tạm làm thay đổi suite ứng dụng.
- `npm run build`: đạt, vẫn có cảnh báo bundle JS **528.39 kB** sau minify. Không ghi “không warning”; cảnh báo không chặn task shell.
- JDK 21.0.10, `mvnw.cmd -o -B -ntp verify`: formatter và 3 unit tests đạt; integration tests không khởi tạo được Docker. Đã thử lại ngoài sandbox: Docker Desktop Linux Engine named pipe không tồn tại. Đây là giới hạn runtime lượt review, không phải assertion regression BE thất bại. Không dùng DB dev và không tự khởi động/đổi Docker hay WSL.
- Trước khi chạy lại, báo cáo build sẵn trên đĩa có 4+8 integration tests đạt. Đó là bằng chứng cũ trên đĩa; không gọi là verify đạt trong lượt kiểm tra này. Log mới: `backend/target/review-g1-03-recheck.log` và `backend/target/review-g1-03-recheck-elevated.log`.
- Không chạy browser E2E hoặc xác nhận UI 375px/1440px trong lượt này.

Để chạy lại bản tái hiện: chép `docs/reviews/G1-03-repro.test.tsx` vào `frontend/review-g1-03.tmp.test.tsx`, chạy `npm test -- review-g1-03.tmp.test.tsx` từ frontend. Khi sửa xong, đưa các case vào suite chính, không giữ file test trùng.

## Prompt giao AI thực hiện, có điểm dừng

```text
Đọc docs/G1-03_RECHECK.md. Chỉ hoàn tất R1 và R2 thuộc F3:
401 của query/mutation phải dọn phiên và cache cá nhân; login và request
lỗi CSRF phải có đường lấy token mới hữu hạn, không replay mutation nghiệp vụ.
Dùng docs/reviews/G1-03-repro.test.tsx làm tình huống tái hiện rồi đưa regression
vào suite chính. Kiểm tra A hết phiên -> B đăng nhập không thấy cache của A.

Không làm lại F1/F2, không sửa Agent Desk, không nâng dependency hoặc thiết kế
lại UI/DTO trong lượt này. Chạy typecheck, lint, test, build; ghi đúng warning.
Chạy BE verify trên PostgreSQL Testcontainers biệt lập nếu Docker đang sẵn sàng;
Docker không chạy thì ghi rõ, không dùng DB dev hoặc sửa môi trường để lách test.
Cập nhật backlog theo bằng chứng thật, không ghi F3 hoàn tất khi regression còn fail.

Điểm dừng: khi regression R1/R2 và các check đạt thì chốt phần FE G1-03,
chuyển G2-01; không tạo thêm vòng sửa tài liệu/style. Nếu BE verify chưa chạy
được thì ghi trạng thái xác minh còn thiếu riêng. Không commit/push.
```
