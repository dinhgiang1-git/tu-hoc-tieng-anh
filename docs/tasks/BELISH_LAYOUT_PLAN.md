# Kế hoạch tu sửa layout theo Belish

Ngày 09/11/2026. **Đã khảo sát nguồn, cập nhật skill và lập kế hoạch; chưa sửa giao diện ứng dụng.** Nguồn: [Belish Roadmap](https://belish.net/vi/roadmap), tổng quan, mở A1 và Cài đặt; chưa đăng nhập. Xem 670×900 và desktop 1440×900. Phiên nguồn đang Dark/blue và bật giao diện vẽ tay; không coi đây là mặc định toàn website. Light dưới đây là đề xuất, chưa khảo sát trên nguồn.

## 1. Hướng đã chọn

Sổ tay kiểu Belish: Itim, grid nhẹ, một viền bao mỗi card, thẻ ngang có tape pastel, marker vàng, active/action xanh. Nền main phủ kín vùng còn lại desktop, không khung sheet nổi/gutter ngoài; hành động học xuất hiện sớm. Giữ thương hiệu, chức năng và nội dung dự án.

**Chốt bổ sung theo ảnh và phản hồi 09/11/2026:** không dùng vạch màu cạnh heading, dải màu một cạnh card hoặc đường nhấn trang trí lặp lại. Chữ và spacing tạo thứ bậc; giữ ranh giới control/focus/progress có chức năng. Áp dụng [kế hoạch gỡ thanh màu ở mục 9](#9-fix-bỏ-thanh-màu-trang-trí) trước khi hoàn thiện các đợt còn lại; không đưa lại các vạch này khi triển khai BL-03.

**Bài học theo phương pháp:** [mục 10](#10-giao-diện-riêng-theo-phương-pháp-học) cụ thể hóa BL-03 và phần quiz: một vùng làm bài, mỗi phương pháp có cấu trúc riêng, ưu tiên quiz hiện có trước. Các phương pháp mới cần dữ liệu sẽ triển khai thành phần riêng sau khi có contract, không giả lập từ Markdown.

**Bỏ khoảng trống quanh main theo ảnh mới:** [mục 11](#11-main-phủ-kín-vùng-màn-hình-còn-lại) thay đề xuất khung giấy nổi trước đây. Nền phủ sát dưới topbar và cạnh sidebar tới mép phải/đáy; chỉ giữ padding nội dung bên trong, không bật fullscreen hoặc ẩn navigation.

Đọc AGENTS, FE convention, skill `nhai-ui-ux` và `references/belish-notebook.md`. Kế hoạch này thay hướng visual cũ trong `design-system/english-study/MASTER.md` cho đợt Belish; giữ ràng buộc copy gọn, dữ liệu và state của MASTER/handoff cũ. `THEME_MODE_FIX_PLAN.md` là lịch sử: đối chiếu source để biết lỗi nào đã sửa, không lặp lại toàn bộ danh sách lỗi cũ. Quy trình kiểm tra gọn của AGENTS ưu tiên hơn yêu cầu full suite mỗi lượt trong handoff cũ.

Lượt này chỉ cập nhật skill và kế hoạch. Triển khai FE ở lượt tiếp theo theo BL-01 đến BL-04; không đổi BE/auth/DB/schema/học liệu để tạo hình. Giữ React, TypeScript, Tailwind 3.4, TanStack Query, Lucide, font và primitives sẵn có. Mobile hoãn. Không thêm Premium, chat, bảng xếp hạng hoặc A1–C2 từ nguồn.

## 2. Hiện trạng và khoảng cách

Baseline Git `8a6e783` kèm nhiều thay đổi FE/BE chưa commit có sẵn. Đã đọc source liên quan; chưa render app local trong lượt lập kế hoạch này.

| Bằng chứng source | Chuyển đổi cần làm |
|---|---|
| Có Itim/grid/press, Warm/Cool/Dark trong styles | Tái dùng nền tảng, thêm recipe Belish và palette xanh; không chỉ đổi primary rồi giữ mọi layout cũ |
| Sidebar ở lần rà đầu là 224px/64px; shell có topbar, main cuộn và full-width wrapper | Sidebar mở 240–256px, topbar 64–72px, nền main phủ kín không khung sheet nổi; giữ collapse/auth/error |
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
│ Sidebar       │ Nền kẻ ô sát mép, content padding 16–24   │
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

## 10. Giao diện riêng theo phương pháp học

### Nguồn và quyết định sản phẩm

Khảo sát tiếp ngày 09/11/2026: [bài từ vựng A1 của Belish](https://belish.net/vi/vocabulary/a1-nen-tang/chapter-1-lesson-1), năm mode trong bài, và [bài Shadowing học thử](https://belish.net/vi/shadowing/cap-do-14/57990027-7fff-40dd-9edd-35275a9664c1?from=overview). Đã lật flashcard; chọn sai một câu quiz và chuyển câu; gõ đúng một từ rồi kiểm tra; xem câu khuyết Đọc hiểu, Luyện nghe, transcript/controls Shadowing, Nghe–Viết chính tả và Ghép câu. Phiên khách Dark, chưa kiểm lưu/hoàn thành/âm thanh thực tế/microphone. Chi tiết quan sát và giới hạn ở [skill learning-methods](../../.agents/skills/nhai-ui-ux/references/learning-methods.md).

Chốt cách làm: **chung shell/primitive, riêng layout và thao tác của mỗi phương pháp**. Bỏ cách trình bày mọi hoạt động thành một dãy card có nhãn enum và nút Hoàn thành. Phương pháp đang chọn là vùng chính; bộ chọn ở trước vùng làm bài, không đặt dưới bài đọc dài. Không ép bài nào cũng có đủ các mode hoặc bê quy tắc mở khóa Flashcard + một mode của nguồn sang dự án.

Quiz hiện có mặc định là bài kiểm tra nộp toàn bộ, giữ cách chấm này. Chỉ học từ Belish cách tập trung từng câu và bố trí lựa chọn; không sao chép phản hồi ngay vào endpoint hiện tại. Mục 9 về bỏ thanh màu vẫn ưu tiên; đổi phương pháp bằng composition và hành vi, không bằng dải màu hoặc nhiều lớp khung.

### Map phương pháp vào năng lực hiện tại

| Phương pháp | Layout mục tiêu | Nguồn dữ liệu hiện có / phần thiếu |
|---|---|---|
| Trắc nghiệm một lựa chọn | Một câu, lựa chọn lớn 2 cột khi ngắn/1 cột khi dài; trước/tiếp/để sau; rà soát và nộp cuối | `QuizDetailResponse` có questionId/prompt/options/order; không có đáp án đúng. `QuizResultResponse` trả đáp án/giải thích sau submit. Làm được bằng FE, giữ API |
| Đọc tài liệu | Mặt đọc 65–78ch và nhiệm vụ liên quan, không render toàn Markdown ở mỗi activity | Có lesson-level `markdownContent`; giữ một nơi đọc. Không tự cắt heading gán thành payload cho từng activity |
| Trả lời tự do / phản tư | Đề + editor + lưu nháp/nộp đúng policy | Có instructions/userResponse và completionPolicy; giữ hooks/version/blocker hiện có, không giả đúng/sai |
| Xem/nghe tài nguyên | Player hoặc liên kết học liệu đúng media type, hướng dẫn thực hành và action hợp lệ | Có resources VIDEO/AUDIO/ARTICLE/PDF. Chưa có transcript/timestamp cho controls theo câu; không dựng controls không chạy |
| Flashcard | Hai mặt term/IPA ↔ nghĩa/ví dụ; nhớ/chưa nhớ riêng | DTO hiện tại chưa có bộ từ có cấu trúc, state nhớ và policy hoàn thành; cần bổ sung dữ liệu trước |
| Gõ từ / câu khuyết | Prompt + input hoặc câu ngữ cảnh với lựa chọn; kiểm tra theo đáp án hợp lệ | Chưa có accepted answers/normalization/blank có cấu trúc. PRACTICE không tự xác định là dạng này |
| Chép chính tả | Audio + ô nhập, transcript chỉ mở theo policy | DICTATION đang bị frontend đánh dấu chưa hỗ trợ; cần chấm/lưu/progress thật, không mở khóa nút hoàn thành thủ công |
| Shadowing / ghép câu | Media + đoạn đang chọn và transcript; hoặc vùng câu đã ghép + ngân hàng token | Thiếu segments/timestamp/token occurrence/contract chấm. RECORDING chưa hỗ trợ; không làm điểm phát âm/microphone giả |

Không suy phương pháp bằng tên activity, slug curriculum hoặc regex Markdown. Quyết định renderer dựa vào capability/payload rõ ràng; `type` là loại nội dung, `completionPolicy` quyết hành động hoàn thành. Nếu muốn thêm discriminator, xác định contract trước khi viết consumer; không đổi enum/schema chỉ để dựng prototype đẹp.

### LM-01 — quiz hiện có, làm một luồng dùng được trước

Sửa `features/learning/components/QuizActivityView.tsx`, nối state với `ActivityRenderer.tsx` và workspace khi cần; API `quiz.api.ts` giữ endpoint/payload hiện tại. Chỉ tách `QuizQuestion`/navigation khi giảm lặp thực, không làm framework khảo thí.

```text
Tên nhiệm vụ                  Câu 2 / 8 · Đã trả lời 1 / 8

                  Đề bài đầy đủ

 [A  Lựa chọn]                    [B  Lựa chọn]
 [C  Lựa chọn]                    [D  Lựa chọn]

 [Câu trước]     [Để làm sau]                     [Câu tiếp]
                           cuối lượt → Rà soát / Nộp bài
```

- Lấy số lựa chọn/thứ tự từ API, không ép bốn option. Radio/fieldset/legend và label đủ vùng bấm; đáp án dài xuống dòng, không font 12px hoặc line-clamp. Số 1/2/A/B chỉ là nhãn UI, gửi optionId thật.
- `activeQuestionId` và answers theo questionId; chuyển câu chỉ điều hướng, không gửi bài/ghi tiến độ. “Để làm sau” giữ unanswered; phân biệt câu đang xem, đã trả lời và chưa trả lời. Cuối lượt chỉ nộp khi đủ câu theo contract hiện tại; có cách quay lại câu thiếu, sửa lựa chọn trước nộp.
- Một surface cho đề/đáp án, không mỗi câu một card lồng trong card quiz. Với câu ngắn ở 1280×720, prompt/lựa chọn/action nhìn thấy sớm; câu dài cuộn được, không cắt nội dung để ép vừa màn hình.
- Selected chỉ thể hiện lựa chọn, không đúng/sai trước submit. Kết quả/explanation lấy từ server; sau nộp có danh sách câu sai/cách quay lại xem từng câu, điểm authoritative và nút bắt đầu lượt mới rõ ràng.
- Giữ answers khi đổi câu/activity/theme, đóng mở notes và lỗi submit. Báo quiz dirty/pending lên unified blocker; hiện `QuizActivityView` giữ answers nội bộ và chưa có callback dirty riêng. Không bỏ qua phần này khi chỉ thêm navigator. Không unmount quiz đang làm để chuyển method; panel ẩn không Tab/đọc được.
- Nộp một lần cho toàn quiz, giữ expectedVersion, idempotency và read-only/409 recovery; request retry không được vô tình tạo attempt trùng hoặc tái dùng key cho payload khác. Chờ đồng bộ authoritative trước cho phép mutation tiếp theo. Không lấy answer key vào FE để giả phản hồi tức thì.
- Rút ngôn ngữ kỹ thuật đang hiện như `ON_SUBMIT` thành mô tả hành động/điều kiện dễ hiểu. Không thêm nhiều banner nhắc cùng một yêu cầu hoặc tự đổi passing score.

### LM-02 — workspace và phương pháp đã có

Hoàn thiện BL-03 tại `LessonWorkspacePage.tsx`: bộ chọn task/phương pháp theo activityId thật, một nhiệm vụ được chọn có vùng làm việc riêng; không chỉ anchor số sau toàn bài đọc. Renderer đọc, phản tư, tài nguyên và quiz chọn recipe phù hợp. Giữ một Markdown tham khảo và drawer notes; không ép note thành cột làm hẹp quiz/bài đọc. Giữ instance/draft khi chuyển view, focus mode và thứ tự Escape đã sửa.

Không tạo năm mode chỉ từ một activity text. Với READING không có câu hỏi, vẫn có giao diện đọc; với quiz chỉ có một lựa chọn/đáp án text, không suy ra chép chính tả hay ghép câu. Bộ chọn chỉ hiện những phương pháp thực sự dùng được trong bài.

### LM-03 — dữ liệu cho các phương pháp mới, rồi mới render

Tách khỏi LM-01/02. Trước triển khai chọn một nội dung test có quyền dùng và một phương pháp làm xuyên FE–BE–DB; không xây đồng loạt mọi engine. Ưu tiên bộ từ có cấu trúc và flashcard trước, rồi gõ từ/câu khuyết; audio theo đoạn/Shadowing/Ghép câu sau khi có dữ liệu thích hợp.

- Flashcard cần entryId, term, IPA nếu có, nghĩa, ví dụ/bản dịch và nguồn audio; định nghĩa rõ nhớ/chưa nhớ là trạng thái luyện hay completion. Không gọi đó là SRS nếu chưa có thuật toán/lịch ôn thật.
- Gõ từ/câu khuyết cần prompt, vị trí blank có cấu trúc, accepted answers/normalization và feedback được cấp quyền đúng thời điểm. Không parse Markdown published thành đáp án hoặc dùng edit-distance tùy ý để đánh dấu đúng.
- Nghe/Shadowing cần media và segmentId/start/end/transcript/translation theo dữ liệu thật; adapter phải hỗ trợ replay/seek trước khi hiện controls. Không chia đều thời lượng video để tạo timestamp, không lấy TTS làm bản ghi chuẩn.
- Ghép câu cần token theo occurrence và quy tắc câu hợp lệ; chọn/bỏ bằng bàn phím, không drag-only. Chép chính tả/recording phải có chính sách chấm/lưu/hoàn thành tương ứng, giữ trạng thái chưa hỗ trợ đến khi làm được thật.
- Chỉ thêm migration khi chức năng dữ liệu cần, không sửa migration đã áp dụng. Cập nhật OpenAPI/generated types nếu đổi contract; owner, version/transaction/idempotency và regression PostgreSQL liên quan theo AGENTS. Không bật nút/mode giả chấm điểm trong lúc contract chưa có.

### Kiểm tra và bàn giao

Codex giao công nhân tuần tự LM-01 → LM-02 → từng lát LM-03; không cùng sửa workspace/renderer với công nhân bỏ stripe. Đọc diff hiện có, không reset hoặc commit phần task khác. Mỗi lát một bàn giao ngắn, nghiệm thu trước khi commit/push.

- LM-01: build + lint, `QuizActivityView.test.tsx` và nhóm workspace/blocker bị tác động một lần. Regression có giá trị: đổi câu giữ answers; câu để sau không tính đã làm; payload cuối chứa đủ IDs/expectedVersion; không lộ đáp án trước nộp; pending gửi một lần, lỗi/409 giữ choices; kết quả và retry đúng contract.
- LM-02: build + lint; LessonWorkspace, notes/quiz/ActivityRenderer regression liên quan khi composition thay đổi. Thử giữ text/quiz/note khi đổi activity/theme/focus; Tab không lọt panel ẩn, Escape đóng layer trên cùng. Không mặc định chạy lại toàn BE nếu chỉ thay composition.
- Từng phương pháp mới LM-03: một luồng thật với fixture test cho phép, contract/owner/data/concurrency tests trực tiếp; integration PostgreSQL khi có SQL/transaction mới. Không dùng chỉ screenshot hoặc Mockito kết luận completion đúng.
- Desktop Light/Dark 1280×720 và 1440×900, câu/đáp án dài, số option khác bốn, keyboard và reflow. Không auto-advance khi chọn radio; không autofocus lúc background refetch; alert feedback không đọc lại toàn trang. Audio có lỗi/retry, transcript không rò vào accessible name bài chép.
- Sau khi chạm quiz thật: một luồng học cốt lõi trên account test, chọn/nộp → kết quả → xem lại câu sai; nếu ghi note thì lưu/reload đúng scope. Không reset DB hay tài khoản cá nhân. Full FE cuối milestone theo AGENTS; không chạy full suite sau mỗi edit.

Lượt này **chỉ cập nhật skill và kế hoạch**, không sửa FE/BE/DB hay học liệu. Chưa có phương pháp mới được triển khai hoặc nghiệm thu từ lượt khảo sát này. Khác biệt giữa dữ liệu hiện có và nguồn đã được ghi rõ để công nhân không giao một UI trông giống nhưng không học/chấm/lưu được.

Kiểm tra tài liệu: `quick_validate.py` đạt cho bản skill trong repo và bản chia sẻ; bản sao/reference/link nội bộ, UTF-8 và `git diff --check` đạt. Không chạy build/unit/E2E vì lượt này không thay đổi code ứng dụng.

## 11. Main phủ kín vùng màn hình còn lại

### Bằng chứng và phạm vi

Ảnh người dùng khoanh khoảng trống phía trên và cạnh trái của tờ giấy nội dung. Source hiện tại giải thích được khoảng này: `AppShell.tsx` đặt `p-3 md:p-5` trên main, bên trong lại có `belish-sheet ... p-5 md:p-7 lg:p-8`; `.belish-sheet` có border/radius/shadow và pseudo-element nét đôi. `w-full` hiện có chỉ làm sheet đầy phần bên trong padding, không xóa gutter. Nhận định này dựa vào ảnh và source, chưa phải đo computed geometry sau sửa.

Chốt: nền grid/paper bắt đầu ngay tại ranh giới main dưới chrome, cạnh sidebar và phủ đến mép phải/đáy. Giữ topbar, sidebar mở/thu và các action. Một lớp padding chữ/control bên trong nền (đề xuất 16px ở 1280, 24px ở màn lớn), không hai lớp lề; giữ gap giữa card và padding trong card. Khoảng nền trống phía dưới khi ít nội dung vẫn là vùng main sử dụng được; không kéo giãn card để lấp đầy hay tự thêm widget.

Lượt này cập nhật skill và kế hoạch, **chưa sửa FE**. Không gộp redesign Today, sửa màu, quiz hoặc auth vào việc bỏ gutter. Quy tắc mới ưu tiên hơn đề xuất sheet có khung ở các mục/lịch sử nghiệm thu cũ.

### Thay đổi cụ thể

| Điểm sửa | Hướng thực hiện |
|---|---|
| `app/layouts/AppShell.tsx`: main có outer padding và nền chrome | Bỏ padding/margin tạo gutter; đưa `paper-grid` vào main phủ kín. Giữ flex-1/min-w-0, min-h-0 theo chuỗi flex và overflow-y-auto của main |
| Wrapper chứa `Outlet` có `belish-sheet`, padding 20–32px | Bỏ class sheet/khung ngoài, chỉ một wrapper full-width/min-height với padding nội dung 16–24px; không max-width/mx-auto làm bó cả app. Không đổi key/mount của Outlet hoặc state auth/theme |
| `styles/globals.css`: `.belish-sheet` và `::before` | Rà các điểm dùng rồi gỡ recipe không còn dùng hoặc chuyển riêng đúng nơi còn cần. Không để border/shadow/inset âm tạo dải mép, viền kép, overflow hay scrollbar giả; không đụng `.belish-frame` của card/control |
| Wrapper trang con, đặc biệt Today và workspace | Rà padding/container/max-width chỉ ở lớp bố cục trang. Giữ `max-w-*` cho đoạn đọc, nội dung dialog hoặc form phù hợp; không replace toàn repo. Không chồng thêm padding page để bù wrapper vừa gỡ |

Hình cấu trúc đích, không phải code đã triển khai:

```text
topbar / banner (chiều cao thật theo layout)
┌ sidebar ───┬ main: paper-grid, flex-1, min-w-0, min-h-0, scroll ┐
│            │ wrapper: full width/min-height; padding 16–24     │
│            │ Outlet → nội dung trang                          │
└────────────┴───────────────────────────────────────────────────┘
```

Không dùng `w-screen`/100vw cho main cạnh sidebar, `h-screen` cho main dưới header, absolute/fixed phủ lên chrome hoặc phép trừ hardcode 256/64px. Header/banner và sidebar vẫn tham gia flex layout; nội dung dài cuộn ở main, không sinh scroll body thứ hai. Sidebar rộng/hẹp làm main tự nhận phần còn lại. Không dùng overflow-hidden để che lỗi tràn nội dung/focus.

### Giao việc và nghiệm thu

Một công nhân sửa shell/wrapper/CSS cùng đợt nhỏ, tuần tự với task đang chạm AppShell/styles. Đọc diff có sẵn; không reset hoặc commit các thay đổi chưa nghiệm thu. Không cần API/DB/migration/dependency mới. Codex tự sửa finding nhỏ rồi nghiệm thu/commit phần đạt.

- Chụp trước/sau cùng Today, mode, viewport, sidebar state. Kiểm 1280×720, 1440×900 và một desktop rộng tương đương ảnh người dùng; Light/Dark, sidebar mở/thu. Nền main liền tới các cạnh, không vòng nền chrome hoặc tờ giấy nổi bên trong.
- Đo geometry tại đầu trang: mép trái main khớp mép phải sidebar; mép trên khớp đáy vùng chrome/banner thực tế; mép phải/đáy khớp vùng còn lại trong viewport, cho phép scrollbar/đường phân vùng chức năng. Wrapper không chừa gutter ngoài; khoảng cách chữ tới cạnh là padding nội dung đã chốt.
- Today ít nội dung vẫn phủ kín chiều cao; curriculum/workspace dài cuộn bình thường. Thử mở/thu sidebar khi ở giữa trang, đổi route, mở drawer notes/IPA và focus mode; không mất state, không clip tape/focus và không có scrollbar ngang mới.
- Smoke read-only catalog, lịch, ôn tập, tiến độ, tùy chọn vì dùng chung shell. Login là layout độc lập, không ép form đăng nhập full-width theo quy tắc main của app.
- Build (đã gồm TypeScript) + lint lúc nghiệm thu. Chọn AppSidebar/TodayPage tests hiện có nếu DOM/props ảnh hưởng; không viết test chỉ so class padding hoặc mặc định full FE/backend/E2E cho thay đổi bố cục này. Một luồng Hôm nay → mở bài để xác nhận không ảnh hưởng điều hướng.
- Báo riêng geometry/render đã đo, checks đã chạy và điểm chưa xác minh. Chưa có ảnh sau sửa hoặc nghiệm thu UI trong lượt cập nhật tài liệu này.

## 12. Bỏ viền trong/ngoài chồng nhau — sửa trực tiếp

Ảnh phản hồi tiếp theo khoanh nét song song sát cạnh trên của card chính và card tiến độ. Nguyên nhân source: các recipe có border thật và `::before` vẽ thêm border 1px với inset lệch. Không phải hai card dữ liệu lồng nhau ở vị trí này.

Đã gỡ định nghĩa `::before` của `.belish-frame`, `.belish-sheet`, `.nhai-panel` và `.nhai-card-interactive` tại `frontend/src/styles/globals.css`; giữ border chính, shadow offset, tape `::after` và focus ring chức năng. Không thêm CSS override ẩn nét, không sửa logic học. Các kế hoạch bỏ stripe/gutter và phương pháp học vẫn là việc riêng, chưa được coi hoàn thành từ sửa này.

Skill và CSS ví dụ đã đổi sang một đường viền bao; quan sát nguồn có nét chồng được giữ dưới dạng bằng chứng lịch sử, không còn là recipe bắt buộc. Bản chia sẻ đồng bộ quy tắc này.

Kiểm tra thực tế Hôm nay bằng account audit đã có: Light/Dark, computed border card là 2px, `::before` content none/border 0px; tape còn, Light không tràn ngang. Build/lint chạy sau sửa CSS, kết quả kiểm tra được báo trong bàn giao. Không viết test class cho thay đổi CSS đơn giản, không chạy lại full FE/backend/E2E.

Sửa CSS ở working tree đang dựa trên hệ Belish chưa commit từ trước; không stage nguyên file cùng thay đổi task khác chưa nghiệm thu. Commit tài liệu/skill riêng không có nghĩa CSS ứng dụng đã được push.

## 13. Codex nghiệm thu bản cập nhật — 10/11/2026

**Đạt các thay đổi đã có về viền đơn, bỏ thanh màu, main sát mép và luồng workspace/quiz hiện tại trong phạm vi kiểm tra dưới đây.** Kết quả này cập nhật các finding BL-03 cũ ở mục 8: hiện đã có task canvas riêng, giữ panel mounted/inert và note drawer, không còn chỉ anchor phía dưới bài đọc. Không xác nhận LM-03 hoặc các phương pháp chưa có dữ liệu đã hoàn thành; không nghiệm thu thay đổi BE/auth ngoài phạm vi.

- Build production và lint đạt; cảnh báo còn lại là bundle JS >500kB và `react(incompatible-library)` của react-hook-form tại EnrollmentDialog. Full FE: **29 file, 169/169 tests đạt**. Không chạy full backend verify hoặc bộ Playwright rộng trong lượt này; browser kiểm thử trực tiếp bên dưới dùng app/DB development đang chạy.
- Source không còn recipe `::before` viền phụ, `.belish-sheet`, `.nhai-section-heading` hoặc dải `border-l-4`/`border-l-[6px]` trang trí đã liệt kê. Đo card trên web: pseudo trước không render, border chính vẫn có. Tape/focus và progress còn hoạt động; không thay bằng dải trang trí khác.
- Hôm nay Light 1440×900: topbar cao 64px; sidebar mở kết thúc tại x=256; main bắt đầu (256,64), kết thúc (1440,900), outer padding 0px, content padding 24px. Không gutter/khung sheet ngoài hoặc tràn ngang. Dark 1280×720: main từ x=256 hoặc x=68 khi sidebar thu, y=64 tới đáy 720, content padding 16px; không tràn. Dark 1920×1080 thu sidebar: main từ (68,64) tới (1920,1080), content padding 24px, không gutter.
- Trên account audit `e2e_readiness_20261028`: Hôm nay/lịch → bài pilot; một tabpanel hiển thị. Nhập response chưa gửi → đổi task → quay lại giữ nguyên; mở/đóng notes không mất response. Drawer rộng 416px ở viewport 1280, main không bị thu hẹp. Đã xóa text nháp UI thử nghiệm trước khi rời; không nộp hoặc ghi ghi chú của account này.
- Trên fixture `e2e_learner_quiz` và enrollment/lesson IDs có sẵn trong `frontend/e2e/quiz-desktop.e2e.ts`: quiz từng câu; chọn → câu tiếp, để làm sau, quay lại trả lời và đổi task vẫn giữ lựa chọn. Rà soát hiển thị đủ 4 answers trước nộp, không hiển thị đáp án đúng trước đó. Lượt nộp thực #1 đạt 1/4 (25%) và IN_PROGRESS; lượt làm lại #2 đạt 4/4 (100%). Reload xác nhận latest attempt 100%/Đạt và activity Đã hoàn thành, tổng task 1/2. Không hoàn thành giả task đọc còn lại, không reset DB hay sửa dữ liệu cá nhân.
- Đã thử các lớp focus/overlay: IPA trên notes và focus mode; Escape lần lượt đóng IPA, notes, rồi focus mode, chrome được gỡ inert khi thoát. Notes là drawer, không còn cột grid bóp bài.
- Điểm tinh gọn không chặn: quiz 1280×720 còn nhiều nội dung giới thiệu/nhắc điều kiện trước câu hỏi, phải cuộn để thấy hết lựa chọn/action; chưa cắt hoặc mất nội dung. Có thể giảm lặp khi hoàn thiện, không mở thêm vòng sửa vì đổi sở thích. Chưa đo native zoom 125%, toàn bộ contrast mọi route hoặc hành vi âm thanh/ghi âm/phương pháp mới.

Không sửa code trong lượt nghiệm thu này. Source ứng dụng vẫn là working tree đang trộn các task chưa tách, bao gồm auth/BE; không stage toàn bộ theo kết quả kiểm tra FE. Chỉ lưu/push bản ghi nghiệm thu này, chưa tuyên bố source ứng dụng đã được push hoặc toàn milestone hoàn thành.
