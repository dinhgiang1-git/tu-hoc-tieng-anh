# Kế hoạch tu sửa layout theo Belish

Ngày 09/11/2026. **Đã khảo sát nguồn, cập nhật skill và lập kế hoạch; chưa sửa giao diện ứng dụng.** Nguồn: [Belish Roadmap](https://belish.net/vi/roadmap), tổng quan, mở A1 và Cài đặt; chưa đăng nhập. Xem 670×900 và desktop 1440×900. Phiên nguồn đang Dark/blue và bật giao diện vẽ tay; không coi đây là mặc định toàn website. Light dưới đây là đề xuất, chưa khảo sát trên nguồn.

## 1. Hướng đã chọn

Sổ tay vẽ phác kiểu Belish: Itim, grid nhẹ, khung giấy nét đôi, thẻ ngang có tape pastel, marker vàng, active/action xanh. Main rộng theo vùng còn lại desktop; hành động học xuất hiện sớm. Giữ thương hiệu, chức năng và nội dung dự án.

**Chốt bổ sung theo ảnh và phản hồi 09/11/2026:** không dùng vạch màu cạnh heading, dải màu một cạnh card hoặc đường nhấn trang trí lặp lại. Chữ và spacing tạo thứ bậc; giữ ranh giới control/focus/progress có chức năng. Áp dụng [kế hoạch gỡ thanh màu ở mục 9](#9-fix-bỏ-thanh-màu-trang-trí) trước khi hoàn thiện các đợt còn lại; không đưa lại các vạch này khi triển khai BL-03.

Đọc AGENTS, FE convention, skill `nhai-ui-ux` và `references/belish-notebook.md`. Kế hoạch này thay hướng visual cũ trong `design-system/english-study/MASTER.md` cho đợt Belish; giữ ràng buộc copy gọn, dữ liệu và state của MASTER/handoff cũ. `THEME_MODE_FIX_PLAN.md` là lịch sử: đối chiếu source để biết lỗi nào đã sửa, không lặp lại toàn bộ danh sách lỗi cũ. Quy trình kiểm tra gọn của AGENTS ưu tiên hơn yêu cầu full suite mỗi lượt trong handoff cũ.

Lượt này chỉ cập nhật skill và kế hoạch. Triển khai FE ở lượt tiếp theo theo BL-01 đến BL-04; không đổi BE/auth/DB/schema/học liệu để tạo hình. Giữ React, TypeScript, Tailwind 3.4, TanStack Query, Lucide, font và primitives sẵn có. Mobile hoãn. Không thêm Premium, chat, bảng xếp hạng hoặc A1–C2 từ nguồn.

## 2. Hiện trạng và khoảng cách

Baseline Git `8a6e783` kèm nhiều thay đổi FE/BE chưa commit có sẵn. Đã đọc source liên quan; chưa render app local trong lượt lập kế hoạch này.

| Bằng chứng source | Chuyển đổi cần làm |
|---|---|
| Có Itim/grid/press, Warm/Cool/Dark trong styles | Tái dùng nền tảng, thêm recipe Belish và palette xanh; không chỉ đổi primary rồi giữ mọi layout cũ |
| Sidebar đang 224px/64px; shell có topbar, main cuộn và full-width wrapper | Sidebar mở 240–256px, topbar 64–72px, khung giấy nét phác; giữ collapse/auth/error |
| Catalog lưới hai cột, card có link con | Danh sách thẻ ngang có tape; static wrapper, link con; không nested control |
| Detail có phase → week → lesson bằng `details` | Giữ disclosure cho nội dung dài, thêm rail/mốc, hàng bài compact |
| Catalog summary không có số bài/thứ tự curriculum; enrollment summary không có curriculumId | Không bịa số bài, nối các curriculum thành A1→C2 hoặc gán tiến độ theo title |
| Catalog hiện ghép enrollment bằng `curriculumTitle === title` | Khi sửa layout, bỏ badge tiến độ gán theo title nếu chưa có khóa liên kết chuẩn; tránh hiển thị sai tên trùng |
| Outline có lesson/required; progress cá nhân đếm required activities | Số bài mô tả nội dung; phần trăm giữ mẫu số BE, không gắn nhãn “buổi hoàn thành” cho activity |

Đọc lại diff trước mỗi edit; không reset/stash/ghi đè công việc có sẵn. Không sửa/commit `docs/DAY2_REVIEW.md`, `backend/src/main/resources/META-INF/spring/` hoặc phần task khác chưa nghiệm thu. Skill trong `.agents/skills/nhai-ui-ux` và bản chia sẻ `nhai-ui-ux` phải đồng bộ cả entrypoint/reference.

## 3. Layout cụ thể

### Shell và Hôm nay

```text
┌ Thương hiệu hiện có ─────────────────── Theme / tài khoản ┐
│ Sidebar       │ Trang giấy kẻ ô, padding 24–32            │
│ 240–256px     │ Hôm nay                                   │
│ Active xanh   │ ┌ Việc học chính ───────┐ ┌ Tóm tắt gọn ┐ │
│               │ │ Tên bài / trạng thái │ │ Dữ liệu thật│ │
│               │ │ [Tiếp tục học]       │ └─────────────┘ │
│ Tùy chọn      │ └──────────────────────┘                 │
└───────────────┴───────────────────────────────────────────┘
```

Một vùng scroll chính, giữ sidebar thu gọn. Heading và CTA trong viewport 1280×720 ở state có bài. Marker chỉ cho mô tả hữu ích, không thêm slogan. Main dùng chiều ngang còn lại; riêng bài đọc giới hạn 65–78ch. Rail phụ xuống dưới khi chật. Giữ đủ state Today, error/null và date-only theo server.

### Catalog và chi tiết

Catalog gồm các curriculum độc lập, không nối thành một chuỗi bắt buộc. Mỗi thẻ ngang có tên, mô tả, locale và link; tape trang trí. Màu theo ID ổn định, không ngụ ý CEFR. Không gọi detail N lần để trang trí số bài. Nếu thiếu khóa liên kết enrollment chuẩn, chỉ hiển thị thông tin curriculum; tiến độ cá nhân vẫn ở màn có enrollmentId chuẩn.

```text
← Tất cả lộ trình       Tên lộ trình               [Đăng ký]
Số chặng · số buổi thật                          [Bảng IPA]

01 ─ ┌ Phase / chặng 1                          [thu/mở] ┐
│    │ 01  Tên buổi dài xuống dòng        25 phút        │
│    │ 02  Tên buổi                       Tùy chọn       │
│    └───────────────────────────────────────────────────┘
02 ─ ┌ Phase / chặng 2                          [thu/mở] ┐
```

Thứ tự phase/week/lesson từ API; trục trái mặc định cho giáo trình dài. Trục giữa/so le chỉ cho nhóm ngắn đang mở khi đủ rộng, không bắt buộc với toàn bộ giáo trình. Giữ DOM theo thứ tự học. Outline không có enrollment scope thì không gắn link học, nhãn khóa/hoàn thành giả. Giữ đăng ký và mục lục; “Tiếp tục học” chỉ xuất hiện khi có enrollmentId/lessonId/trạng thái hợp lệ từ dữ liệu sẵn có.

### Workspace và các màn khác

Giữ task navigator, một mặt giấy đọc, note drawer mặc định đóng và action đúng policy. Wrapper/theme không remount form hoặc reset quiz/note. Lịch giữ lưới/quy tắc ngày, đổi vỏ. Ôn tập giữ câu hỏi/action trước metadata. Tiến độ chỉ có số chuẩn BE. Login/Preferences/overlay dùng cùng primitives và cả hai mode. Workspace là thiết kế chuyển phong cách, chưa khảo sát workspace Belish.

## 4. Đợt triển khai và nghiệm thu

Codex giao AI công nhân từng đợt, nghiệm thu rồi commit/push phần đạt. Không giao hai công nhân cùng sửa primitive/layout. Codex tự sửa finding nhỏ; tối đa hai lượt giao sửa một finding theo AGENTS. Một bàn giao ngắn mỗi đợt, không tạo chuỗi tài liệu review.

| Đợt | File/phạm vi chính | Kết quả nhìn thấy |
|---|---|---|
| BL-01 — nền tảng + Today | `styles/tokens.css`, `globals.css`, Tailwind config khi cần; button/card/input/select, PageHeader, AppShell/AppSidebar, TodayPage | Một recipe sketch/tape/marker; semantic và nh cùng nguồn; shell/Today nhận ra Belish trong Light/Dark, giữ theme lifecycle |
| BL-02 — lộ trình | CurriculaPage, CurriculumDetailPage; component feature nhỏ có điểm dùng thật; EnrollmentDialog chỉ presentation | Catalog ngang, outline có mốc/disclosure; không gán enrollment theo title; không sửa thuật toán lịch/thêm endpoint vì hình thức |
| BL-03 — workspace | LessonWorkspacePage, wrapper ActivityRenderer, MarkdownContent, note/IPA khi cần đồng bộ | Chrome gọn/mặt giấy đọc; giữ draft/quiz/note, focus mode, blocker, 409/version và completion policy |
| BL-04 — đồng bộ | Schedule, Review, Progress, Preferences, Login, overlay/toast | Các route cùng recipe; keyboard/zoom/theme dùng được; một luồng học cốt lõi đạt |

Không thêm framework, Storybook, thư viện vẽ tay hoặc theme/font library. Hiệu ứng/variant thuộc primitive chung, trang quyết layout/nội dung. Recipe Belish shadow/press 2px thay offset Warm 4px trong phạm vi chuyển; không trộn mỗi trang một kiểu. Giữ mode đã lưu, không ép Dark. Tape/viền là pseudo-element không cản focus/click.

## 5. Checks sau thay đổi

Chạy từ `frontend`; mỗi nhóm một lần, chỉ chạy lại khi code đổi, failure hoặc nghi vấn cụ thể. Tên suite lấy theo file đang có, không thêm test chỉ để so class CSS.

| Đợt | Checks liên quan |
|---|---|
| BL-01 | Build + lint; AppSidebar/TodayPage tests và theme tests hiện có nếu sửa theme; render 1280×720, 1440×900 Light/Dark, sidebar mở/thu; Hôm nay → bài trên account test cho phép |
| BL-02 | Build + lint; Curricula tests, EnrollmentDialog nếu form đổi; catalog → detail → mở/đóng đăng ký; tên trùng, null progress, không phase và giáo trình dài |
| BL-03 | Build + lint; nhóm LessonWorkspace/ActivityRenderer/quiz/note bị tác động; nhập response → đổi bước → quay lại; giữ quiz/note khi đổi bước/theme/đóng drawer; lưu/reload trên test scope; regression blocker/409 hiện có |
| BL-04 | Build + lint và full FE suite cuối đợt; tests schedule/review/progress/preferences/auth bị tác động; một vòng học thực; zoom 125%, Tab/Escape/reduced motion. E2E rộng khi milestone/rủi ro cần |

Build dùng `npm run build` đã gồm TypeScript, không chạy thêm typecheck trùng. Lint lúc nghiệm thu `npm run lint`. Tái dùng FE/BE/DB development và fixture/allowlist, không reset dữ liệu cá nhân. BE verify/PostgreSQL integration chỉ khi có đổi business/data/auth/schema/SQL theo AGENTS; kế hoạch này không yêu cầu các thay đổi đó.

Ngay BL-01, token/primitive toàn cục đã tác động các trang chưa chuyển. Smoke read-only một workspace, lịch, ôn tập và Login để phát hiện mất chữ, tràn ngang hoặc control hỏng; không chờ BL-04 mới nhìn các consumer này. Chỉ mở rộng regression khi có lỗi/nghi vấn cụ thể, không mặc định chạy full suite.

## 6. Tiêu chí kết thúc

- Nhận ra Belish qua shell/thẻ/roadmap, vẫn giữ thương hiệu và nội dung dự án.
- Đo tương phản render Light/Dark: chữ thường ≥4.5:1, focus/ranh giới control quan trọng ≥3:1; gồm marker và active nav. Không sao chép chữ trắng trên xanh sáng mà bỏ đo.
- Không tràn ngang/cắt tape/bóng/focus desktop; tên dài/zoom không giấu action; static card không giả có thể click.
- Tab theo thứ tự học; giữ Escape/focus restore; decoration không tham gia accessibility tree hoặc cản thao tác.
- Không mất response/quiz/note/selection khi đổi bước/theme; giữ owner/version/quyền/policy và ngày lịch.
- Null không thành 0%, lỗi không thành empty; không ghép enrollment theo title; không sinh trạng thái khóa giả.
- Báo đúng checks đã/chưa chạy và phần chưa chuyển; không tuyên bố toàn app đạt sau BL-01.

## 7. Bàn giao lượt kế hoạch

Đã khảo sát nguồn thật, đọc computed font/màu/hình học desktop; rà shell/styles/catalog/detail, generated DTO, convention và handoff. Skill có reference Belish ở cả bản dùng trong repo và bản chia sẻ. UI UX Pro Max design-system trả Claymorphism/trẻ em không phù hợp nên không lưu/áp dụng; query hẹp Neubrutalism khớp cạnh/bóng cứng, còn nét phác và roadmap dựa trên quan sát nguồn.

Chưa chạy app local, build, lint, unit hoặc E2E vì lượt này chỉ sửa tài liệu. Chưa triển khai BL-01; chưa xác minh Light/workspace hoặc ghi tiến độ trên Belish. Đây là kế hoạch sẵn giao công nhân, không phải nghiệm thu UI đã sửa.

## 8. Codex nghiệm thu phần source hiện có — 09/11/2026

Lượt nghiệm thu sau khi có thay đổi Belish trong working tree: **nền tảng/shell/Hôm nay đạt các kiểm tra dưới đây, chưa nghiệm thu toàn bộ BL-01–04**. Không xác nhận các thay đổi BE/auth hoặc tính năng khác đang dở.

- `npm run build` đạt; cảnh báo bundle JS lớn hơn 500kB. `npm run lint` đạt với một warning `react(incompatible-library)` tại EnrollmentDialog (react-hook-form).
- AppSidebar + TodayPage: 2 file, 17 tests đạt. Theme context: 1 file, 6 tests đạt. Không chạy full FE/backend/E2E.
- Tái dùng app development, đăng nhập tài khoản audit có sẵn `e2e_readiness_20261028`. Xem Today Dark 1440×900 và Light/Dark 1280×720; tên bài, CTA, marker/tape rõ. CTA Dark 1280 có đáy khoảng 528px, nằm trong viewport 720px; không tràn ngang main/trang ở các trạng thái đã đo.
- Hôm nay → mở đúng bài pilot; sidebar thu gọn hoạt động. Smoke workspace Light/Dark, lịch Dark và ôn tập empty state; Login đã render trước đăng nhập. Đây là kiểm tra mở/đọc, chưa kiểm chu trình nộp bài/lưu note.
- Select lộ trình mở được; Escape đóng và trả focus về trigger. IPA mở/đóng bằng Escape, trả focus về nút Bảng IPA. Không gửi bài, sửa ghi chú, đổi enrollment hoặc lịch.
- Còn lại BL-02/03/04: roadmap chưa nghiệm thu; workspace hiện vẫn lặp hai H1 tên bài và Bài tiếp còn nổi bật trước hoàn thành. Giữ các điểm này trong BL-03; không coi việc thay token là đã hoàn thành workspace.
- Chưa kiểm zoom 125%, toàn bộ keyboard/reduced-motion hoặc đo đủ contrast render. Các kiểm tra đó vẫn cần trước khi nghiệm thu hoàn chỉnh, không suy ra từ build/tests xanh.

Sửa trực tiếp hai điểm tài liệu khi nghiệm thu: hướng dẫn chia sẻ Belish phải kèm reference, và smoke consumer chung ngay BL-01. Source FE/BE đang trộn nhiều task chưa có ranh giới bàn giao nên không stage/commit nguyên các file đó trong lượt này; chỉ commit phần tài liệu đã review.

### Thẩm định lại source cập nhật — tối 09/11/2026

**Chưa đạt toàn bộ BL-01–04.** Source đã chuyển catalog và outline sang thẻ ngang/rail; workspace đã bỏ H1 trùng và dùng nút outline cho Bài tiếp khi chưa hoàn thành. Hai điểm này trong báo cáo trước đã được xử lý, không còn là finding hiện tại.

- Chạy full FE: 29 file, 166/167 tests đạt. Một failure là G301Review còn đòi combobox cho trường hợp một enrollment, trong khi UI dùng CTA chọn trực tiếp. Đã cập nhật assertion theo affordance hiện tại, giữ test mutation chọn đúng enrollment.
- Tái hiện lỗi focus trên web: chế độ tập trung còn 10 control nền trong thứ tự Tab; Escape lúc IPA mở làm thoát focus mode phía sau. Đã sửa trực tiếp: đặt các sibling bị che thành inert trong thời gian focus, khôi phục khi thoát; không xử lý Escape của focus khi modal đang mở. Regression kiểm cả cô lập nền và Escape theo lớp. Xác minh lại browser: 0 control nền có thể Tab; Escape đóng IPA, giữ focus mode và trả focus về nút IPA.
- Sau sửa chạy đúng nhóm G301Review + LessonWorkspace: 2 file, 6/6 tests đạt. Build và lint sau sửa đạt; còn cảnh báo bundle JS lớn và react-hook-form như lượt trước. Không chạy lại toàn bộ FE sau sửa, không tuyên bố full suite cuối cùng đã đạt.
- Đo computed colors Hôm nay trên nền đặc: active nav/CTA 5.70:1, marker 10.85:1, chữ phụ Light 6.22–6.50:1, Dark 6.52–7.85:1; heading chính vượt 11:1. Chưa đo toàn app, opacity/disabled/overlay và ảnh hưởng grid không được coi là đã chứng nhận.
- Xem catalog Light 1440×900 và outline lộ trình A 130 buổi: thẻ ngang/tape và nhóm thu gọn hiển thị đúng, một H1; không tràn ngang ở trạng thái đã kiểm. Detail và form đăng ký Dark tại 1024×576 không tràn ngang; mở rồi hủy, không tạo enrollment. Đây là kiểm tra reflow viewport nhỏ, không phải bằng chứng zoom trình duyệt 125%.
- **BL-03 vẫn còn thiếu bố cục đã chốt:** navigator chỉ là anchor số đặt dưới toàn bộ bài đọc; các activity vẫn hiển thị liên tiếp, chưa có khu vực tập trung vào nhiệm vụ hiện tại. Ghi chú mở bằng một cột grid 21rem, chưa là drawer và còn làm hẹp bài. Đây là phần chức năng/layout đáng kể, cần triển khai trước khi xác nhận BL-03 hoàn thành; không đổi yêu cầu sang coi việc thêm anchor là đã đạt.
- Chưa chạy lại vòng nộp bài/ghi chú/quiz thực trên DB hoặc E2E rộng. Không sửa dữ liệu học, backend/auth hay nội dung published trong lượt thẩm định.

Các sửa source nhỏ ở lượt này còn trong working tree: workspace đang phụ thuộc phần IPA/Belish chưa commit từ trước, nên không commit riêng một patch tạo tham chiếu thiếu và không gom cả các task chưa nghiệm thu. Bản thẩm định không thay cho việc tách và nghiệm thu phần triển khai còn lại.

## 9. Fix bỏ thanh màu trang trí

### Phạm vi và quyết định

Người dùng cung cấp bốn ảnh cắt giao diện: thấy rõ vạch xanh cạnh heading và dải xanh lá ở mép card. Các ảnh còn cho thấy nhiều lớp khung trong bài/quiz; rà độ cần thiết trong vùng sửa, không tự suy ra yêu cầu xóa toàn bộ viền của ứng dụng. Đây là điều chỉnh sở thích thiết kế của người dùng, không phải bằng chứng xác định tác giả một giao diện.

Lượt này cập nhật skill và kế hoạch, **chưa sửa FE**. Skill Nhai/Belish và hướng dẫn local của UI UX Pro Max đã nhận quy tắc không dùng thanh nhấn trang trí mặc định. Bản skill chia sẻ phải đồng bộ. Các phần nghiệp vụ/BL-03 còn thiếu ở mục 8 giữ nguyên phạm vi.

### Điểm phải sửa đã xác định trong source

| Nơi sinh vạch/dải | Thay đổi cụ thể |
|---|---|
| `components/shared/PageHeader.tsx`: `pl-3.5 border-l-4 border-primary` | Bỏ border và padding dành cho vạch; căn title/description cùng mép vùng nội dung. Giữ action, thứ bậc heading và khả năng wrap |
| `styles/globals.css`: `.nhai-section-heading` có `border-left:4px` và padding | Bỏ recipe vạch ở nguồn; bỏ class/wrapper không còn vai trò tại Workspace (tên bài, nhóm nhiệm vụ), CurriculumDetail (Nội dung học), Preferences (mục tiêu). Không chỉ đổi màu vạch |
| `curricula/pages/CurriculaPage.tsx`: `CURRICULUM_ACCENTS`, `border-l-[6px]` và `accentClass` | Xóa map/class dải mép; giữ border bao đều, layout ngang và tape đang có. Không đổi dải màu thành dải trung tính, không tăng decoration khác |
| `learning/components/ActivityRenderer.tsx` và `ActivityProgressPanel.tsx`: completed → `border-l-[6px] border-l-success` | Bỏ accent cạnh và độ dày khác biệt. Giữ nhãn/icon hoàn thành đã có, trạng thái read-only và hành động đúng policy. Không thêm badge hoàn thành thứ hai |
| `review/components/ReviewBookmarkedCard.tsx`: note dùng `border-l-4 border-l-[hsl(var(--warning))]` | Bỏ cạnh vàng và bề dày lệch; phân biệt trích dẫn bằng nội dung/spacing hoặc nền nhẹ hiện có, không thêm khung ngoài mới |
| `learning/components/MarkdownContent.tsx`: blockquote có `border-l-4 border-primary` | Bỏ vạch màu, chỉnh padding/bo góc còn lại cho cân; giữ semantic blockquote, nội dung trích dẫn, giới hạn chiều dài dòng và sanitizer. Không sửa markdown published |

Rà thêm các pseudo-element có nền màu và class ghép động trong phạm vi thay đổi. Không thay toàn cục mọi `border-l`/`border-r`: đường phân vùng sidebar/tài khoản, bảng, input, đáp án đang chọn/sai, focus ring, progress và dữ liệu biểu đồ phải được phân loại theo chức năng trước. Trục roadmap trung tính biểu đạt thứ tự thật được giữ; không xóa kết nối dữ liệu chỉ vì là đường dọc. Vạch màu thuần trang trí ở bất kỳ cạnh nào cũng thuộc diện gỡ.

### Cách giao và thứ tự

Giao một công nhân xử lý các điểm trên trong một đợt nhỏ, tránh trùng file với công nhân BL-03. Đọc diff hiện có trước khi sửa; không reset/stash hoặc gom các task chưa nghiệm thu. Làm PageHeader/recipe dùng chung trước, rồi các consumer; sau đó gỡ map/class thừa. Không sửa API, DB, completion/version, autosave, auth hoặc đổi thư viện để bỏ một kiểu trang trí. Codex tự xử lý finding nhỏ khi nghiệm thu.

Trước/sau chụp cùng route, viewport, mode và state: Hôm nay; catalog/chi tiết; workspace có ít nhất một activity hoàn thành và một activity chưa hoàn thành; trích dẫn Markdown/ghi chú bookmark nếu có fixture phù hợp. Dùng test scope có sẵn, không hoàn thành bài hoặc ghi dữ liệu cá nhân chỉ để tạo ảnh.

### Kiểm tra và điểm kết thúc

- Xem desktop 1280×720 và 1440×900 ở Light/Dark; sidebar mở/thu. Không còn thanh màu ở heading/card/quote, không còn khoảng thụt dành cho thanh cũ; title/nội dung/action vẫn căn hợp lý, nhãn dài không tràn.
- Màn nhiều activity/quiz: không dùng thêm wrapper/đường viền thay cho thanh vừa gỡ. Giữ ranh giới câu hỏi, đáp án/input có chức năng; chỉ bỏ wrapper phụ khi source/render xác nhận không cần để nhóm tương tác. Không mở rộng thành redesign toàn bộ quiz.
- Cùng một activity trước/sau có cùng status/CTA; màu không là dấu hiệu duy nhất của hoàn thành hoặc selected/error. Focus ring bàn phím và thông tin tiến độ vẫn nhận ra được.
- `npm run build` (gồm TypeScript), `npm run lint` lúc nghiệm thu. Do chỉ đổi presentation, dùng kiểm tra render; nếu consumer/markup bị tác động thì chọn các file test hiện có tương ứng: `Curricula.test.tsx`, `ActivityProgressPanel.test.tsx`, `LessonWorkspacePage.test.tsx` (consumer ActivityRenderer), `ReviewPage.test.tsx` và `MarkdownContent.test.tsx`. Không thêm test chỉ so border/class, không bắt chạy full suite/E2E rộng cho việc gỡ stripe.
- Mở một luồng Hôm nay → bài; Tab tới action và mở/đóng ghi chú, giữ draft/state nếu có thay đổi wrapper. Chỉ chạy lại test khi thay đổi/failure/nghi vấn cụ thể; không viết lại test nghiệp vụ cho mỗi chi tiết CSS.
- Codex nghiệm thu rồi commit/push đúng phần đạt; báo checks đã/chưa chạy. Source FE vẫn chưa thay đổi bởi lượt cập nhật skill/kế hoạch này.

Kiểm tra lượt tài liệu này: validator chuẩn `quick_validate.py` đạt cho skill Nhai trong repo, bản chia sẻ và UI UX Pro Max; kiểm bản sao, liên kết file và `git diff --check` đạt. Không chạy build/unit/E2E vì chưa sửa FE; chưa có ảnh sau sửa giao diện.
