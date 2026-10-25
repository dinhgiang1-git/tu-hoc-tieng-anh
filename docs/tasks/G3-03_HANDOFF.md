# G3-03 — Không gian học, lưu nháp và ghi chú tự lưu

## 1. Mục tiêu và điểm bắt đầu

Triển khai màn hình học thật để người học đọc bài, chuyển nhiệm vụ, nhập/lưu nháp câu trả lời, nộp bài và ghi chú. Codex điều phối và nghiệm thu; AI công nhân triển khai rồi dừng.

Baseline: G3-02 đã nghiệm thu tại `6c784b4`: 113 backend unit + 42 PostgreSQL integration + 68 frontend tests. Đọc:

- `BE_CODE_CONVENTION.md`, `FE_CODE_CONVENTION.md`.
- `docs/product-rules.md`, `docs/wireframes.md` mục Learning Workspace.
- `docs/api/openapi.yaml`, hai bản generated types hiện có.
- `docs/G3-02_RECHECK.md`, `docs/tasks/G3-02_HANDOFF.md`.
- Code `LearningProgressService`, `EnrollmentRepository`, `ActivityProgressPanel`, `useLearningProgress`, `LessonSummaryPage`, `AppShell` và cơ chế auth/cache hiện có.

Lưu ý thực trạng: endpoint workspace/draft/notes mới có trong OpenAPI, chưa có implementation đầy đủ. Chưa có bảng/entity note trong migrations hiện tại. `SaveResponseDraftRequest` hiện thiếu expectedVersion, response draft thiếu enrollmentVersion; cần sửa contract trước khi code. Có hai file ngoài phạm vi `docs/DAY2_REVIEW.md` và thư mục `backend/src/main/resources/META-INF/spring/`; giữ nguyên, không đưa vào báo cáo thay đổi G3-03.

## 2. Phạm vi và đầu ra

1. GET workspace owner-checked, nội dung Markdown + nhiệm vụ + resources + trạng thái cá nhân/version.
2. PUT lưu nháp PRACTICE/REFLECTION với optimistic concurrency, không tự hoàn thành.
3. Backend module notes: list/create/update/delete theo contract; migration mới từ V007 hoặc số kế tiếp thực tế.
4. Frontend workspace thay trang tóm tắt trên route hiện hành, Markdown an toàn, activity renderer, ghi chú autosave, focus mode.
5. Contract/generated types, meaningful tests, cập nhật backlog và báo cáo kết quả thực chạy.

Không làm admin, quiz grading, phát âm/thu âm, bookmark, lịch/pause/resume G4, dashboard báo cáo hay hệ thống offline. Không đưa nút giả từ wireframe vào sản phẩm. Không đổi DB dev, secrets hoặc migrations V001–V006. Không nâng dependencies hàng loạt. Chỉ thêm thư viện Markdown cần thiết, ghi rõ lựa chọn/license và cập nhật lockfile.

## 3. Contract và backend

### 3.1 GET workspace

`GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId}` dùng LessonDetailResponse/ActivityWorkspaceDto hiện có làm nền:

- Owner từ principal; enrollment không thuộc user, lesson/activity sai revision → 404. Có thể đọc enrollment PAUSED/ABANDONED/COMPLETED và revision đã archive nếu thuộc enrollment.
- Trả Markdown thật, instructions, resources có attribution, response đã lưu, status và enrollmentVersion. Duyệt đúng thứ tự phase/week/lesson/activity hiện có; resources có thứ tự ổn định.
- Không trả quiz answer key, đáp án đúng, payload chấm điểm. QUIZ/RECORDING/DICTATION chưa hỗ trợ thì renderer chỉ đọc, không có nút hoàn thành giả.
- Không dùng endpoint nặng này cho Today/list; giữ projection và phép đo SQL G3-01. Tránh N+1 theo số activity/resource; đo SQL trên fixture nhiều activities/resources.
- Tiến độ lộ trình dùng summary server. Hoàn thành buổi dựa trên yêu cầu hiệu lực của G3-02, không dựa vào việc đã xem hết tab.
- Có thể bổ sung `nextLessonId` nullable nếu cần CTA bài kế; phải tìm trong revision enrollment, theo thứ tự thật, không suy ra từ UUID/số thứ tự cục bộ. Nếu chưa có dữ liệu đáng tin, CTA quay Today là đủ; không gửi link null.

### 3.2 PUT response draft

`PUT /api/v1/enrollments/{enrollmentId}/activities/{activityId}/response`:

- Request `{responseText, expectedVersion}`; expectedVersion bắt buộc, không âm, là enrollment version. Response chứa text đã lưu, status, updatedAt, enrollmentVersion; chỉnh OpenAPI, 409 và generated types đồng bộ.
- Chỉ PRACTICE/REFLECTION với MANUAL_CHECK_WITH_RESPONSE/RESPONSE_SUBMIT. Sai owner/revision → 404; PAUSED/ABANDONED hoặc policy/type không phù hợp → STATE_CONFLICT. CSRF như mutation hiện có.
- `responseText` không null, tối đa 5000 ký tự; chuỗi rỗng được phép để xóa bản nháp. Giữ text nguyên bản, không trim mất nội dung; chỉ dùng trim để xác định blank.
- Activity chưa bắt đầu + text nonblank → IN_PROGRESS; các draft còn lại giữ trạng thái chưa hoàn thành hiện có. Không tự đặt COMPLETED, không thay completedAt của bài/buổi/lộ trình, không tăng số hoàn thành, không ghi SUBMITTED/COMPLETED event khi lưu nháp.
- Nếu activity đã COMPLETED: text khác bản đã nộp → STATE_CONFLICT; yêu cầu undo trước khi lưu nháp. Người học vẫn có thể dùng submit để cập nhật câu trả lời hoàn thành theo G3-02. Không thay ngầm bài đã nộp bằng draft chưa nộp.
- Kiểm tra version trước no-op. Cùng text hiện có và version hiện hành → no-op, giữ version/timestamps/events. Coi null chưa có progress và chuỗi rỗng là cùng bản nháp rỗng, không tạo row vô ích.
- Thay đổi thật phải tăng enrollment version đúng một lần dù Clock không tiến. Ghi text/status và version trong cùng transaction; thất bại rollback hết. Tái dùng/phân tách cơ chế CAS G3-02, không tự tạo cơ chế cạnh tranh thứ hai hoặc dùng dirty timestamp.
- Draft và complete/undo/submit trên cùng enrollment với cùng version: tối đa một commit, còn lại 409, không mất text hoặc trả 500 vì unique row. Bảo toàn test G3-02.

### 3.3 Module ghi chú

Module `notes` riêng, controller/application/domain/infrastructure theo convention. Notes có version riêng, không tăng enrollment version, không tạo learning progress/event, không đổi lịch.

- Migration tạo bảng note: UUID id, owner user FK, enrollment/lesson/activity nullable theo contract, content TEXT, version BIGINT, createdAt/updatedAt TIMESTAMPTZ; index cho owner + scope + phân trang. Dùng Clock inject. Không gán ownership từ body.
- Scope hợp lệ: ghi chú cá nhân không có scope; hoặc enrollment thuộc user, có thể thêm lesson thuộc revision và activity thuộc lesson. Có lesson phải có enrollment; có activity phải có lesson. Reject scope thiếu/không nhất quán; tham chiếu không thuộc user/revision trả 404. Không cho PATCH đổi scope/owner.
- Workspace G3-03 dùng **một note cấp lesson** cho `(owner, enrollment, lesson)` với activity=null. Unique partial index đảm bảo không tạo hai bản ghi nếu hai tab cùng tạo. Không trộn note hai lần học cùng lesson hay chọn phần tử đầu của list không lọc scope.
- `GET /me/notes`: owner filter ở DB, filters enrollmentId/lessonId theo contract, page >= 0, size 1..100; thứ tự ổn định `updatedAt DESC, id DESC`. Filter scope sai owner không lộ dữ liệu. Trả PageResponse đúng generated contract, gồm enrollmentId trong NoteResponse để phân biệt scope.
- `POST /notes`: create nội dung nonblank tối đa 10000 ký tự, owner từ principal. Workspace chỉ create khi bắt đầu có text, không tạo note trắng khi mở trang. Cùng lesson scope đã tồn tại + text giống hệt → trả note hiện có; khác text → 409 STATE_CONFLICT kèm hướng tải lại, không ghi đè. Race tạo trùng được xử lý transaction an toàn, không tiếp tục query trong transaction PostgreSQL đã aborted.
- `PATCH /notes/{id}`: `{content, expectedVersion}` là **note version**, không âm. Cho phép content rỗng để người học xóa hết chữ, max 10000; sửa minLength hiện tại trong contract. Stale version → 409 VERSION_CONFLICT; text giống + version đúng là no-op. Thay đổi thật tăng đúng một lần dù Clock đứng yên; trả version persisted.
- `DELETE /notes/{id}` owner-checked, bổ sung expectedVersion bắt buộc ở query để tránh xóa nội dung vừa được sửa ở tab khác; 204 thành công, stale →409. UI workspace không cần nút xóa note riêng, chỉ cần cho xóa nội dung.
- Ghi chú cá nhân vẫn chỉnh sửa được khi enrollment PAUSED/ABANDONED/COMPLETED; đây không phải mutation tiến độ. UI khóa activity theo lifecycle nhưng không khóa việc ghi chú cá nhân. Nội dung note không render như HTML và không ghi nguyên văn vào log.

## 4. Frontend và UX

### 4.1 Không gian học

- Giữ route `/enrollments/:enrollmentId/lessons/:lessonId` và link Today hiện có. Thay LessonSummaryPage hoặc đổi route component có regression; không tạo hai đường học có hành vi khác nhau.
- Sidebar trái của app như hiện tại; desktop có danh sách nhiệm vụ gọn, vùng đọc chính, note panel có thể mở/thu. Mobile 375px chuyển vùng phụ sang tab/drawer, không ép ba cột; bảng IPA cuộn ngang trong vùng bảng, không kéo tràn toàn trang.
- Tên bài, nội dung và thao tác học là chính; bỏ version/UUID và thông tin giai đoạn triển khai khỏi UI người học. Trạng thái lưu ngắn, inline, không toast mỗi 700ms.
- `ActivityRenderer`/`MarkdownContent`/`NoteEditor` và hooks có trách nhiệm rõ; tái dùng logic G3-02. Không copy toàn bộ ActivityProgressPanel thành một bản độc lập. Shared module notes không import ngược learning page.
- Chuyển nhiệm vụ trước/sau không tự complete, không làm mất draft. Complete/undo/submit chỉ đổi UI sau server confirm; hoàn thành & tiếp chỉ điều hướng sau mutation thành công. Gợi ý kết thúc buổi/Today dựa trên trạng thái thật. Không nhân đôi tiến độ enrollment ở client.
- Focus mode thực sự ẩn AppSidebar, có nút thoát và hỗ trợ Escape; tích hợp layout context/outlet gọn, giữ auth guard hoạt động. Cleanup khi rời workspace; không sửa DOM toàn cục để ẩn sidebar.
- Keyboard labels/focus rõ, vùng thông báo trạng thái dùng aria-live phù hợp; không giật focus mỗi autosave. Có loading/empty/404/error/retry, kiểm tra lifecycle trước mutation.

### 4.2 Markdown an toàn

- Dùng parser Markdown qua component dùng chung, hỗ trợ GFM tables, code, list, IPA Unicode. Không `dangerouslySetInnerHTML` trực tiếp từ nội dung.
- Mặc định bỏ raw HTML. Nếu bật raw HTML vì lý do thực tế phải sanitize theo allowlist, có test trước/sau sanitization. Không cho script, event handlers, iframe nguy hiểm, javascript:/data: links chạy.
- Link tài liệu ra ngoài dùng http/https hợp lệ, `target=_blank` và `rel=noopener noreferrer`. Kiểm tra resources URL bên ngoài renderer Markdown nữa. Không tự tải/embed tài nguyên không cần thiết. Nội dung do import vẫn là dữ liệu không tin cậy.

### 4.3 Draft phản hồi và đồng bộ tiến độ

- Mỗi activity có local draft riêng, nút Lưu nháp rõ ràng, trạng thái dirty/saving/saved/error. Submit luôn dùng text hiện tại và enrollmentVersion đã xác nhận. Không có autosave response trong G3-03 để tránh hai hàng đợi cùng enrollment; autosave chỉ áp dụng note.
- Các mutation draft/progress/submit phải cùng khóa đồng bộ trong workspace; request tiếp dùng version mới. Có response server không được ghi đè chữ người học gõ sau khi request được gửi.
- Success cập nhật/invalidate workspace, activities, detail, summary, Today, my-enrollments đúng keys. Giữ khóa khi đọc lại state lỗi, có retry; không nuốt lỗi invalidate rồi báo toàn bộ đã đồng bộ.
- 409 giữ draft, tải version/state mới để người dùng xem và chọn thử lại; không tự gửi lại bằng version mới. Network timeout có thể đã commit: refetch để xác nhận trước lần thao tác kế; không báo Đã lưu chỉ vì bắt đầu request.
- Detail refresh lỗi khi đã có dữ liệu không unmount editor làm mất draft. Giữ regression G3-02 về lifecycle/sync. Note version và enrollment version tuyệt đối không dùng lẫn.

### 4.4 Autosave note — tiêu chí bắt buộc

- `useAutosaveNote`, debounce mặc định 700ms cấu hình một nơi; khóa state theo user/enrollment/lesson/note. Không gửi POST/PATCH mỗi phím, không gửi khi chỉ mount/refetch.
- Mỗi note tối đa một write request đang chạy. Gõ A → gửi A; tiếp tục gõ B khi A pending → giữ B và chỉ gửi sau A bằng version A trả về, coalesce các bản trung gian. Response A không được đổi textarea B về A.
- Chỉ hiện Đã lưu khi nội dung local mới nhất khớp snapshot server đã xác nhận và không còn queued write. A thành công trong khi B chưa lưu vẫn hiện chưa lưu/đang lưu.
- Response cho note/bài cũ không được cập nhật draft/status bài mới. Clear/cancel debounce trên scope change; abort không được coi là rollback server. Không dùng fire-and-forget async trong effect cleanup để hứa lưu trước khi rời trang.
- Lỗi mạng: giữ text, hiện Chưa lưu được + Thử lại, không tự retry vô hạn. Nếu POST timeout, đọc lại đúng lesson scope để tránh tạo duplicate; nếu không chắc bản nào đã lưu, yêu cầu người học đối chiếu, không tự overwrite.
- Conflict note: dừng queue, giữ local draft, hiển thị bản server hoặc cách xem lại; lựa chọn dùng bản server hay lưu lại bản local phải do người học xác nhận với version vừa đọc. Không tự gộp/đè text.
- Chuyển bài/rời route khi còn dirty/pending/error: chặn điều hướng nội bộ và cho chọn ở lại, lưu rồi rời, hoặc bỏ thay đổi. Lưu rồi rời chỉ đi sau khi xác nhận lưu bản mới nhất; lỗi phải ở lại. Browser refresh/close dùng beforeunload khi có nguy cơ mất draft, không hứa lưu chắc chắn khi đóng tab.
- Đổi tab nhiệm vụ trong cùng lesson không mất note/draft. Logout/session 401 dùng lifecycle auth hiện có, dừng queue và không để callback user cũ ghi cache user mới. Không thêm localStorage/IndexedDB cho nội dung cá nhân trong task này.

## 5. Kiểm thử và bằng chứng

Backend dùng PostgreSQL Testcontainers biệt lập:

1. Workspace nội dung/resources/thứ tự đúng, reload có response/version; owner/revision isolation; quiz không lộ đáp án; không N+1 tăng theo số activity.
2. Draft save/reload/clear, validation null/length, policy/lifecycle, không tự complete/event, no-op không đổi timestamp/version, fixed Clock vẫn tăng version cho thay đổi thật.
3. Race draft-vs-submit/progress cùng version trên PostgreSQL: tối đa một commit; inject failure sau save để kiểm chứng rollback text/status/version.
4. Notes CRUD/filter/pagination/owner/scope, hai enrollments cùng lesson không lẫn; PATCH/DELETE stale bị từ chối, no-op và fixed Clock. Create cùng lesson scope đồng thời không duplicate/500; note không tăng enrollment version. Blank update và lifecycle theo quyết định trên.

Frontend dùng API mocks/deferred responses, fake timers chỗ debounce:

5. Markdown table/IPA; script/raw HTML/onerror/javascript:/data: link không thực thi; resource link an toàn.
6. Activity chuyển qua lại giữ draft; save/submit đúng text/version, pending chống gửi trùng; conflict/network/refetch lỗi giữ nội dung và có recovery; note và progress không tranh version.
7. Autosave 699ms chưa gửi, 700ms gửi; A pending + gõ B + A resolve không mất B, B dùng version mới; status không hiện Đã lưu sớm. Error/retry/conflict và POST response mất được xử lý, không duplicate note.
8. Scope switch/unmount/session change không gửi queued draft cho nhầm note/user; route blocker và beforeunload hoạt động. Background lifecycle query error không mất nội dung đang nhập.
9. Focus mode vào/thoát/Escape và navigation/auth không hỏng. Regression G3-01/G3-02 giữ đạt.

Chạy `mvnw.cmd -o -B -ntp verify`; FE `npm test`, `npm run typecheck`, `npm run lint`, `npm run build`; `node scripts/validate-openapi-examples.js` chỉ là smoke check văn bản, không gọi là full schema validation. Cập nhật cả `docs/api/generated-types.ts` và `frontend/src/generated/api-types.ts` bằng generator hiện có; không dùng any để né type.

Nếu có browser tools, kiểm tra desktop 1440px/mobile 375px trên app thật và nêu bằng chứng; không có thì báo chưa kiểm tra. Không dùng DB dev để tạo bằng chứng, không sửa tài khoản local.

## 6. Thứ tự triển khai và điểm dừng

1. Chốt contract/schema theo quyết định trên, migration note và service/test backend.
2. Workspace/Markdown/ActivityRenderer, tích hợp progress/draft thay logic sao chép.
3. NoteEditor/useAutosaveNote và điều hướng an toàn, focus mode/responsive.
4. Chạy toàn bộ checks, cập nhật backlog **chờ Codex nghiệm thu**, ghi file `docs/G3-03_IMPLEMENTATION.md` với file/API/migration đổi, số tests thực chạy, quyết định kỹ thuật, giới hạn chưa kiểm chứng; dừng, không tự làm G3-04.

Công nhân tối đa hai lượt sửa cho cùng finding sau review; Codex tự xử lý phần còn lại. Không mở thêm AI reviewer tự sinh yêu cầu mới. Không commit/push — Codex chịu trách nhiệm khi nghiệm thu.

## 7. Prompt gửi AI công nhân

```text
Triển khai G3-03 trong D:/WorkSpace/tu-hoc-tieng-anh theo
docs/tasks/G3-03_HANDOFF.md. Đọc toàn bộ handoff và tài liệu đầu vào trước.
Baseline G3-02 đã nghiệm thu tại 6c784b4. Làm workspace Markdown an toàn,
lưu nháp phản hồi có enrollment expectedVersion, notes có version riêng,
autosave serialize 700ms giữ text khi mạng lỗi/conflict và chặn rời trang
khi chưa lưu. Tái dùng G3-02, giữ auth/lifecycle/cache và regression.
Chỉ G3-03, không G3-04/G4/G5/phát âm/admin. Không sửa secrets/DB dev,
migration cũ hoặc các file ngoài phạm vi; không commit/push.
Chạy PostgreSQL Testcontainers + FE checks, ghi bằng chứng thật tại
docs/G3-03_IMPLEMENTATION.md, backlog chờ Codex nghiệm thu rồi dừng.
Tối đa hai lượt sửa cùng finding sau review, còn lại Codex tự xử lý.
```
