# Kế hoạch tu sửa layout theo Belish

Ngày 09/11/2026. **Đã khảo sát nguồn, cập nhật skill và lập kế hoạch; chưa sửa giao diện ứng dụng.** Nguồn: [Belish Roadmap](https://belish.net/vi/roadmap), tổng quan, mở A1 và Cài đặt; chưa đăng nhập. Xem 670×900 và desktop 1440×900. Phiên nguồn đang Dark/blue và bật giao diện vẽ tay; không coi đây là mặc định toàn website. Light dưới đây là đề xuất, chưa khảo sát trên nguồn.

## 1. Hướng đã chọn

Sổ tay vẽ phác kiểu Belish: Itim, grid nhẹ, khung giấy nét đôi, thẻ ngang có tape pastel, marker vàng, active/action xanh. Main rộng theo vùng còn lại desktop; hành động học xuất hiện sớm. Giữ thương hiệu, chức năng và nội dung dự án.

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
