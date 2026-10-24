# Review G0 và hướng giao việc tiếp theo

## Kết luận tái kiểm tra — cho phép chuyển G1-01

**Ngày 24/10/2026: đủ cơ sở triển khai G1-01. Không cần thêm một vòng G0-FIX chỉ để dựng nền tảng.** Phần nhận xét ban đầu phía dưới được giữ làm lịch sử, không phải yêu cầu mở lại toàn bộ G0.

Đã chạy lại kiểm tra gói: manifest/week/resources qua JSON Schema; 6 file bài học tồn tại, key và resource reference hợp lệ; pilot có 5 buổi bắt buộc, 1 tùy chọn và 19 nhiệm vụ bắt buộc hiệu lực. Tuần 2 đã chuyển ra ngoài manifest chính thức.

OpenAPI đã bổ sung read version, union null, session security, các trạng thái Hôm nay và endpoint submit phản tư. Đủ để tiếp tục nền tảng; chưa coi là đã kiểm chứng runtime.

Reviewer sửa trực tiếp hai điểm còn sót để khép vòng:

- Hiệu chỉnh cột quan sát nguồn của 26 tuần theo các ô đã đọc trong phiên trước, tách ý tưởng biên soạn khỏi quan sát thực tế.
- Sửa thông báo hai script để phân biệt kiểm tra cấu trúc/smoke check với kiểm chứng API và điều kiện publish. Script OpenAPI hiện chỉ kiểm tra văn bản/fixture local, không phải full schema validator.

Các việc còn lại có mốc xử lý, **không chặn G1-01**:

1. Trước khi tích hợp FE ở G1-03: cấu hình validator OpenAPI/example thực sự, type generation có lệnh tái lập được; không dùng smoke script làm bằng chứng đã validate đầy đủ.
2. Khi làm G3: khóa quy tắc save draft nhiều tab và submit phản tư, version, không cho checklist bypass policy; kiểm tra bằng integration test thay vì kéo dài mô tả G0.
3. Trước publish học liệu ở G2/G6: kiểm tra link và chất lượng nội dung, xử lý metadata quyền sử dụng chưa xác minh. Pilot hiện là draft phục vụ kỹ thuật, chưa được chứng nhận sư phạm.

Giao đúng task G1-01: khảo sát read-only WSL/DB, pin dependency, backend skeleton, config, migration identity/preferences, Clock, error format, healthcheck và checks phù hợp. Chưa làm auth đầy đủ/FE/model phát âm. Không reset hoặc đổi hạ tầng PostgreSQL hiện có.

---

Ngày review: 24/10/2026. Phạm vi: tài liệu G0, OpenAPI, JSON Schema, gói nội dung pilot và backlog trong workspace hiện tại. Đây là review; không sửa trực tiếp các đầu ra của AI triển khai.

## Kết luận

**G0 có đủ nhóm đầu ra, nhưng chưa đạt điều kiện khóa đặc tả.** Cần một lượt `G0-FIX` trước khi dùng OpenAPI và gói pilot làm nguồn chuẩn để code. Không cần làm lại toàn bộ tài liệu.

Hướng kiến trúc, sidebar, session/CSRF, tách revision/enrollment/progress, tiến độ theo nhiệm vụ bắt buộc và lựa chọn phát âm local nhìn chung bám kế hoạch. Các lỗi dưới đây ảnh hưởng tính đúng và khả năng triển khai, không chỉ là cách viết.

## Phát hiện cần sửa

### R1 — P1: Mapping bị thay đổi nội dung nguồn nhưng vẫn gắn nhãn đã quan sát

Vị trí: `docs/content-mapping.md:34`, `:39`, `:47`, `:61`.

Mapping ghi giai đoạn 1 từ tuần 1–4, tuần 2 học tiếp nguyên âm/phụ âm, tuần 14 giới thiệu bản thân. Nội dung đã đọc trong sheet ở phiên trao đổi trước là: giai đoạn 1 tuần 1, giai đoạn 2 tuần 2–13; tuần 2 Present Simple, tuần 3 Present Continuous, tuần 4 Past Simple; tuần 14 du lịch. Đây là nội dung khác nguồn, không thể ghi `SOURCE_OBSERVED`.

Tác động: lộ trình A về sau bị triển khai thành giáo trình khác trong khi mang nhãn bám nguồn.

Yêu cầu sửa: đối chiếu lại nguồn đã truy cập hợp lệ, ghi vị trí ô/tuần cho quan sát; không xác minh được thì ghi rõ chưa xác minh. Nội dung điều chỉnh sư phạm phải nằm ở cột đề xuất riêng. Pilot tự biên soạn có thể giữ cấu trúc khác A, nhưng không được ghi là cấu trúc quan sát từ A.

### R2 — P1: Manifest không hợp lệ với schema đang cung cấp

Vị trí: `content/pilot-curriculum/v1/manifest.json:24`; `content/schemas/curriculum.schema.json:78`; `docs/adr/ADR-005-content-package-versioning-and-verification.md` phần pipeline.

Manifest chứa week dưới dạng object `{ "$ref": "weeks/week-01.json" }`, trong khi schema yêu cầu mỗi object tuần có key/title/orderIndex/weekNumber/lessons. `$ref` trong dữ liệu JSON không tự được validator giải tham chiếu như `$ref` nằm trong JSON Schema.

Đã tái hiện bằng PowerShell `Test-Json`: false, thiếu các required fields tại `/phases/0/weeks/0` và `/phases/0/weeks/1`.

Yêu cầu sửa: chọn contract package rõ ràng. Hoặc manifest schema chấp nhận file reference và từng week có schema riêng; hoặc đặc tả bước load/bundle reference trước khi validate schema của mô hình đã resolve. Cần bằng chứng validate đúng representation đầu vào, kiểm tra path trong package, file tồn tại, key trùng và resource reference. Không chỉ đổi required thành optional để làm validator im lỗi.

### R3 — P1: Pilot đăng ký hai tuần nhưng thiếu năm file bắt buộc

Vị trí: `content/pilot-curriculum/v1/weeks/week-02.json:14`; các dòng 51/88/126/163; `docs/tasks/backlog.md:117`.

Các file được tham chiếu nhưng không tồn tại:

- `lessons/w02-l01-present-simple.md`
- `lessons/w02-l02-ending-s.md`
- `lessons/w02-l03-listening-intro.md`
- `lessons/w02-l04-shadowing.md`
- `lessons/w02-l05-week2-review.md`

Manifest mô tả hai tuần hoàn chỉnh; backlog G2 yêu cầu import 11 buổi và catalog hai tuần. Điều đó không tương thích pipeline kiểm tra file và publish đủ nội dung.

Yêu cầu sửa: **ưu tiên giữ pilot publishable một tuần hoàn chỉnh**, chuyển tuần 2 vào khu phác thảo ngoài manifest nhập chính thức, sửa count/DoD tương ứng. Hoặc hoàn thiện đủ năm bài nếu muốn giữ gói hai tuần. G0 ban đầu chỉ đòi một tuần hoàn chỉnh và thiết kế sơ bộ tuần tiếp theo; không cần tăng việc biên soạn để che vấn đề packaging.

### R4 — P1: Không có read contract lấy enrollment version sau reload

Vị trí: `docs/api/openapi.yaml:994`, `:1017`, `:1075`, `:1196`; đối chiếu `:1146`.

Progress PUT yêu cầu expectedVersion của enrollment, nhưng các GET lesson/today/summary và danh sách enrollment không trả version. Version chỉ có ở response tạo enrollment và thay lịch; chưa có GET enrollment detail. Mở lại bài trực tiếp hoặc reload khiến FE không có version hợp lệ để gửi mutation.

Yêu cầu sửa: cung cấp `enrollmentVersion` ở read DTO của workspace/summary hoặc thêm GET enrollment detail rõ ràng. Chọn một nguồn và quy tắc refetch thống nhất, không hardcode version hoặc bỏ concurrency check. Nghiệm thu bằng flow đọc bài sau reload → lấy version → complete → nhận version mới; hai tab version cũ trả 409.

### R5 — P1: Nullable viết theo OpenAPI 3.0 trong tài liệu 3.1

Vị trí: `docs/api/openapi.yaml:1`, `:1167`, `:1206` và các `nullable: true` khác.

Schema khai báo OpenAPI 3.1.0 nhưng dùng `type: number` + `nullable: true`. Theo ngữ nghĩa JSON Schema trong OAS 3.1, cấu hình này vẫn không chấp nhận null. Trường tiến độ không có mẫu số và các field nullable khác có thể bị runtime validator từ chối hoặc sinh type sai.

Yêu cầu sửa: giữ OAS 3.1 và dùng union type, ví dụ `type: [number, "null"]`; xử lý object/ref nullable bằng schema composition phù hợp. Đồng bộ `required` với quyết định field luôn có nhưng nullable hay có thể absent. Validate các response mẫu có null và sinh TypeScript types để kiểm tra consumer.

Tham chiếu: [OpenAPI 3.1 Schema Object](https://spec.openapis.org/oas/v3.1.0.html#schema-object). Chưa chạy full OpenAPI linter trong lượt review này; phát hiện dựa vào schema và kiểm tra tối thiểu bằng JSON Schema, không phải kết quả lint toàn file.

### R6 — P2: Contract Hôm nay không biểu diễn đầy đủ các trạng thái wireframe

Vị trí: `docs/api/openapi.yaml:1017`, `:1043`.

TodayResponse bắt buộc enrollmentId/revisionId và PrimaryAction bắt buộc lessonId/title/counts, kể cả type `CURRICULUM_COMPLETED`. Chưa có biểu diễn rõ cho chưa đăng ký, chưa chọn lộ trình hoặc PAUSED. Muốn tuân schema, BE/FE dễ phải tạo lesson ID giả hoặc diễn giải 404 không nhất quán.

Yêu cầu sửa: discriminated union cho state Hôm nay; các trạng thái không có bài không bắt buộc lesson. Quy định rõ response khi không có focused enrollment. Thêm example cho no enrollment, paused, rest/upcoming, in-progress và completed.

### R7 — P2: Lưu phản tư có thể thay progress nhưng không có version/rule rõ

Vị trí: `docs/product-rules.md:115`, `:192`, `:217`; `docs/api/openapi.yaml:1172`.

REFLECTION định nghĩa hoàn thành qua RESPONSE_SUBMIT, nhưng phần Complete cho phép thao tác checklist chung; SaveResponseRequest chỉ có responseText, không có version. Nếu lưu phản tư làm hoàn thành activity thì đây là mutation progress không tuân rule expectedVersion; nếu chỉ lưu draft thì chưa có contract submit rõ ràng. Hai tab lưu text cũng có nguy cơ ghi đè.

Yêu cầu sửa: tách rõ save draft/submit hoặc định nghĩa một operation có version và transaction cập nhật response/progress. Checklist không được bypass yêu cầu text của REFLECTION. Chốt retry và response version; ghi rõ cách xử lý xóa text sau khi hoàn thành. Thêm example/test cùng lúc response save và reschedule.

### R8 — P2: Học liệu chưa thể coi là đã duyệt và có resource sai ngữ cảnh

Vị trí: `content/pilot-curriculum/v1/weeks/week-02.json:96`; `content/pilot-curriculum/v1/resources.json:13`; `docs/content-mapping.md:25`; `content/pilot-curriculum/v1/lessons/w01-l03-word-stress.md:20`.

Bài nghe giới thiệu bản thân lại trỏ resource luyện /iː/–/ɪ/. Resource gắn AUTHOR_COMPILED, và tài liệu định nghĩa nhãn này tương đương đã kiểm duyệt, dù chưa có bằng chứng kiểm tra link/đáp án. Bài học có các con số như quy tắc trọng âm đúng hơn 85% và yêu cầu hình học/thời gian phát âm cứng không kèm nguồn. Điều này không đủ để gọi giáo trình đã được duyệt.

Yêu cầu sửa: tách provenance khỏi verification/review status; link phải khớp hoạt động và ghi trạng thái kiểm tra thực tế. Bỏ số liệu không có căn cứ hoặc dẫn nguồn phù hợp; nội dung AI tự soạn không tự chuyển thành REVIEWED. Không gán tên tổ chức/người duyệt hoặc giấy phép nguồn khi chưa có thông tin xác nhận. Lượt review này không kết luận mọi URL đều chết hoặc đã audit đầy đủ nội dung ngữ âm.

## Các phần contract cần rà thêm trong cùng lượt sửa

- Khai báo cookie session security scheme và security requirements trong OpenAPI, chỉ public các operation đã quyết định; mô tả CSRF header không thay thế session scheme.
- `IdempotencyKeyHeader` hiện `required: false` tại dòng 637, trong khi product-rules yêu cầu header. Chọn semantics và đồng bộ; với operation đã yêu cầu key thì schema phải required.
- Bổ sung response 403 CSRF/permission ở các unsafe operation; API pause/resume/abandon nếu đưa vào G4 cần ghi là deferred hoặc thêm contract tương ứng, không gọi toàn bộ API đã hoàn chỉnh.
- Rà field preferences timeZone/fullName và enrollment dailyTargetMinutes để frontend có thể thực hiện đúng wireframe, không chỉ cập nhật một phần không đọc lại được.
- Chưa có bằng chứng G0 được người điều phối duyệt: README nên ghi “đã tạo đầu ra, chờ review/sửa” thay cho trạng thái hoàn thành chắc chắn.
- Chọn British accent reference cụ thể tại phần nội dung; không tự đồng nhất mọi en-GB với RP và không dùng một voice ID chưa kiểm tra cho mọi phiên bản eSpeak.

## Kiểm tra đã thực hiện và giới hạn

- Đọc các tài liệu product rules, content mapping, wireframes, ADR/backlog; xem OpenAPI, schema và gói pilot.
- Parse thành công 5 file JSON trong content hiện tại.
- Manifest validate trực tiếp với schema: **không đạt**; đã ghi chi tiết ở R2.
- Kiểm tra local contentFile references: **thiếu 5 file**, liệt kê ở R3.
- Đối chiếu schema/API với các flow reload, nullable progress, completed dashboard và response submission.
- Chưa có code ứng dụng để build/E2E. Chưa chạy full OpenAPI validator/typegen, chưa kiểm tra tất cả liên kết media hoặc duyệt chuyên môn toàn bộ giáo trình.
- Workspace hiện chưa là Git repository (`git status` báo not a git repository); chưa có diff/commit để đối chiếu.

## Prompt giao lại cho AI: G0-FIX

```text
Đọc docs/G0_REVIEW.md cùng kế hoạch và hai file convention.

Thực hiện một lượt G0-FIX, xử lý R1–R8 và rà soát contract liên quan.
Chưa triển khai G1, chưa cài model phát âm, chưa thay đổi Docker/database.

Ưu tiên pilot publishable một tuần; tuần 2 giữ ở khu phác thảo riêng,
trừ khi có lý do rõ để hoàn thiện gói hai tuần ngay.
Không tự tạo dữ liệu nguồn, giấy phép, tác giả hoặc người duyệt.
Nguồn chưa xác minh phải được ghi đúng trạng thái.

Đầu ra cần có:
1. Tài liệu/schema/API/content được sửa nhất quán.
2. Bảng đối chiếu từng R1–R8: đã sửa thế nào, file nào, kiểm tra nào.
3. Bằng chứng schema validation và kiểm tra reference file/key/resource.
4. OpenAPI validation + sinh TypeScript types thử, có response examples
   cho nullable, reload/version và các trạng thái Hôm nay.
5. Báo rõ việc chưa chạy/chưa xác minh, không tự đánh dấu đã duyệt.

Chỉ tạo công cụ validation nhẹ nếu cần cho nghiệm thu; chưa dựng app.
Không đổi hoặc xóa source/convention để né các lỗi review.
```

## Sau khi G0-FIX đạt: giao G1-01

Không cần giao tất cả G1–G3 một lượt. Bước đầu là hạ tầng phát triển và backend skeleton, theo task G1-01 đã có, với giới hạn:

- Khảo sát read-only Java/Maven/Docker context, nơi chạy BE, PostgreSQL version/port/database/volume; không in mật khẩu.
- Chốt và pin phiên bản dependency tương thích; tạo Maven Wrapper, backend skeleton, config profiles và `.env.example`.
- Dùng database/user riêng phù hợp với hạ tầng hiện có. Không reset container, đổi major image hoặc xóa volume; ghi rõ thay đổi cần thiết trước khi thực hiện.
- Migration identity/preferences; các FK đến enrollment chưa tồn tại để bổ sung đúng migration sau, không tạo toàn bộ nghiệp vụ trước.
- Clock injection, format lỗi chung, healthcheck và formatter/test setup. Auth đầy đủ ở G1-02.
- Nghiệm thu startup, migration chạy lặp, test liên quan, error response và không có secret trong source/log. Ghi đầy đủ lệnh đã chạy và kết quả.
- Khởi tạo Git nếu task triển khai được giao bao gồm việc quản lý repository; trước đó rà `.gitignore`/secret, không push remote khi chưa được yêu cầu.

Sau G1-01 được kiểm tra mới giao G1-02 (session/CSRF/identity), rồi G1-03 (FE shell và auth integration).
