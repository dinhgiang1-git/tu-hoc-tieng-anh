# Kế hoạch sửa light mode và dark mode

Ngày lập: 04/11/2026. Trạng thái: **đã rà source và ảnh người dùng; đây là kế hoạch, chưa sửa giao diện**.

## 1. Kết quả cần đạt

Hai chế độ sáng/tối nhất quán trên toàn bộ web desktop: nền trang, sidebar, card, tiêu đề, form, menu, modal, bài đọc và trạng thái học đều đọc rõ. Chuyển chế độ không làm mất câu trả lời, ghi chú hoặc tiến độ. Giữ phong cách sổ tay Nhai: chữ Itim, ô lưới nhẹ, viền rõ, bóng cứng, hiệu ứng nhấn và chiều rộng trang hiện có.

Phạm vi là frontend. Không đổi API, auth, DB, schema, dữ liệu học hoặc thêm thư viện theme. Những thay đổi đang dở ở các file liên quan phải được đọc diff trước khi triển khai; không ghi đè, reset hoặc commit phần việc khác. Quy trình kiểm tra gọn trong `AGENTS.md` thay yêu cầu chạy toàn bộ suite ở mỗi lượt trong handoff UI cũ.

## 2. Lỗi đã xác định và phần cần xác minh

| Ưu tiên | Hiện trạng và bằng chứng source | Hướng xử lý |
|---|---|---|
| P1 | `.dark` trong `tokens.css` chỉ đổi nhóm semantic, giữ nguyên `--nh-paper`, `--nh-ink`, `--nh-surface`, viền/focus/shadow Nhai của light | Hoàn thiện token cho từng mode và thống nhất quan hệ giữa hai nhóm biến |
| P1 | `.paper-grid` dùng `--nh-paper`; các heading mặc định dùng `--nh-ink`. Ảnh cho thấy nền giấy còn sáng, tiêu đề trên card tối gần như mất | Sửa màu tại nguồn token/role, kiểm riêng heading có utility màu và heading kế thừa mặc định |
| P1 | `input.tsx` dùng `bg-white` cùng `text-foreground`, khiến dark có thể dùng chữ sáng trên nền trắng | Dùng cặp màu surface/foreground theo vai trò; rà textarea và native controls tương tự |
| P1 | Chữ IPA và một số nhãn/link dùng `text-primary` vàng trên nền sáng; phép tính token chỉ đạt khoảng 1.72:1 | Tách màu chữ có tương phản khỏi màu vàng dùng cho nền nút; kiểm các vị trí thực tế trước khi thay |
| P1 | `ThemeToggle` giữ state riêng và áp dụng `.dark` trong effect; `index.html` chưa có bootstrap theme | Chọn theme trước lần vẽ đầu và dùng chung state trên mọi route |
| P2 | Đọc/ghi `localStorage` chưa bắt lỗi; lần mount đầu ghi cả theme suy từ hệ điều hành thành lựa chọn rõ ràng | Chỉ lưu khi người dùng chọn; fallback an toàn khi storage lỗi hoặc giá trị sai |
| P2 | Chưa theo dõi đổi thiết lập OS và sự kiện `storage`; toggle hiện chỉ mount trong `AppShell`, không có ở Login | Đồng bộ lifecycle ở tầng gốc; giữ theme khi đổi route, đăng xuất và mở nhiều tab |
| P2 | Focus ở Button/Input/Card hardcode `#243d99`, trong khi `.dark` có `--ring` vàng | Một role focus phù hợp cả nền sáng/tối, thống nhất CSS và primitive |
| P2 | Toaster chưa nhận resolved theme; Markdown có link/code dùng `text-primary`; các trang có màu trạng thái riêng | Kiểm thực tế overlay, nội dung học và trạng thái; không mặc định token primary dùng được cho mọi chữ/icon |
| P2 | Cool chỉ override một phần token; `.dark` dùng bảng màu Warm | Tách palette khỏi mode, kiểm cascade; không để Cool Dark rơi về hỗn hợp Warm/Cool |

Ảnh là bằng chứng về trải nghiệm lỗi; source giải thích được các cặp màu bị lệch. Các trường hợp chưa đo trên browser được coi là điểm phải kiểm tra, không báo đã nghiệm thu hay đã tìm đủ mọi lỗi.

### Phép đo nền tảng ngày 04/11/2026

Đã đo computed style bằng Chromium ở 1440×900 với component thật và API giả lập, không đăng nhập tài khoản cá nhân hay ghi DB. Các tỷ lệ bên dưới dùng màu nền đặc, chưa cộng nét grid; chúng xác nhận lỗi chính nhưng không đại diện toàn ứng dụng.

| Cặp được đo trên browser | Tỷ lệ | Nhận xét |
|---|---:|---|
| Warm Light: tiêu đề card / nền card | 13.11:1 | Đạt cho cặp này; chưa kết luận mọi thành phần light đạt |
| Warm Dark: tiêu đề card vẫn tối / nền card tối | 1.04:1 | Không đọc rõ; `--nh-ink` chưa đổi theo mode |
| Warm Dark: tiêu đề trang sáng / nền paper vẫn sáng | 1.01:1 | Không đọc rõ; nhóm paper chưa đổi theo mode |
| Cool Dark: tiêu đề card / nền card; tiêu đề trang / paper | 1.08:1; 1.03:1 | Cùng lỗi cascade; Cool được bật thủ công để kiểm tra, chưa có bộ chọn trên UI |

Kiểm số học riêng từ token cho thấy vàng `text-primary` trên card sáng khoảng **1.72:1**, còn chữ dark foreground trên `bg-white` khoảng **1.09:1**. Đây là phép tính token, chưa phải đo input/IPA đang render. Các điểm cần kiểm thực tế gồm chữ IPA trong `IpaSoundboardModal.tsx`, nhãn/link trong `LessonWorkspacePage.tsx` và Input dùng nền trắng. Giữ vàng cho brand fill khi phù hợp, thêm role chữ/link/icon có tương phản thay vì làm tối toàn bộ màu thương hiệu.

## 3. Quyết định thiết kế và hành vi

### Màu và phong cách

- Warm là palette đang dùng, ưu tiên hoàn thiện Warm Light và Warm Dark trước. Light dùng giấy kem, chữ mực tối; Dark dùng giấy tối, chữ sáng ấm và card có mức sáng phân biệt với nền.
- Định nghĩa rõ vai trò: page/paper, surface/card, popover, foreground, muted, primary action, text/link/icon tương tác, border/input, focus, grid, shadow và success/warning/error. Màu vàng trang trí không tự trở thành màu chữ nhỏ trên giấy sáng.
- Giữ semantic tokens là API cho component. Nếu tiếp tục dùng `--nh-*`, chúng phải là alias hoặc lấy từ cùng nguồn màu theo mode; không duy trì hai bảng màu độc lập dễ lệch nhau.
- Heading mặc định theo foreground phù hợp surface. Các nền primary/accent phải dùng foreground đi cùng; tránh ép toàn bộ tiêu đề thành một màu bỏ qua nền của chúng.
- Grid có màu/opacity riêng theo mode. Giữ nhẹ sau chữ, không mang nền kem nguyên sang Dark. Bóng vẫn cứng; offset nhấn Warm 4px, Cool 2px giữ theo token, không đổi theo từng trang.
- Cool là biến thể palette, không phải tên khác của Dark. Chưa thêm nút chọn palette; xử lý tổ hợp selector để chế độ tối không phá palette đã có.

### Chuyển và lưu theme

Giữ nút chuyển **Sáng / Tối** hiện có, không cần thêm bộ chọn ba lựa chọn để sửa lỗi này.

1. Đọc giá trị `theme` hợp lệ (`light` hoặc `dark`) đã lưu; nếu không có hoặc không hợp lệ, dùng `prefers-color-scheme` làm fallback.
2. Áp dụng resolved mode lên `<html>` trước lần vẽ đầu, đồng thời đặt `color-scheme` để native control theo mode. Bootstrap và React dùng cùng quy tắc phân giải.
3. Theme state ở tầng gốc độc lập với auth và route; `ThemeToggle` chỉ đọc state và gọi thao tác đổi mode. Login vẫn nhận đúng theme khi không mount `AppShell`.
4. Khi người dùng bấm, cập nhật ngay và lưu lựa chọn. Storage lỗi không làm crash trang hoặc chặn chuyển theme trong phiên hiện tại.
5. Chỉ theo thay đổi OS khi chưa có lựa chọn rõ ràng. Không để OS ghi đè lựa chọn người dùng. Đồng bộ tab bằng sự kiện `storage`, xử lý cả xóa key/giá trị sai theo fallback đã chốt.
6. Đổi route, logout hoặc remount toggle không xóa sở thích. Đồng bộ Toaster và mọi overlay với resolved mode; không tạo nguồn state thứ hai trong từng trang.

Không gắn theme vào enrollment, dữ liệu đăng nhập hay query cache. Không tự thêm backend preference cho một lựa chọn hiển thị cục bộ.

## 4. Ba đợt triển khai

### TM-01: Sửa token và lỗi nhìn thấy trong ảnh

**File chính:** `frontend/src/styles/tokens.css`, `frontend/src/styles/globals.css`, `frontend/tailwind.config.js`, `components/ui/input.tsx`; consumer màu sai tại `components/shared/PageHeader.tsx`, `features/curricula/pages/CurriculaPage.tsx` và chữ IPA/nhãn thiết yếu nếu token/role chưa đủ. Các đường dẫn bắt đầu bằng `components/` hoặc `features/` tính từ `frontend/src/`; đường dẫn bắt đầu bằng `src/` tính từ `frontend/`.

- Lập bảng token Light/Dark, hoàn thiện nhóm Nhai còn thiếu và thứ tự cascade với Cool.
- Sửa paper/grid, heading, card, viền, focus và shadow để chuyển mode cùng nhau.
- Đo cặp màu nền–chữ của tiêu đề trang, tiêu đề card, mô tả, trạng thái đăng ký và CTA; kiểm cả mode sáng.
- Rà trước các cặp light nguy cơ thấp tương phản như chữ/link/code/icon vàng trên nền kem. Chỉ sửa điểm có vai trò hoặc phép đo cụ thể, không thay palette tùy thích.
- Sửa nền Input cố định ngay trong đợt này để dark không còn chữ sáng trên ô trắng; giữ xử lý form, giá trị nhập và validation hiện có.

**Đạt khi:** trang Lộ trình giống ảnh đã đọc rõ toàn bộ tiêu đề, nền và card thuộc đúng mode; chuyển qua lại không còn mảng giấy sáng ngoài chủ đích. Bóng và hiệu ứng nhấn vẫn đúng ở cả hai mode.

### TM-02: Theme chung, khởi tạo và persistence

**File chính:** `frontend/index.html`, `src/app/providers.tsx`, `src/components/shared/ThemeToggle.tsx`; thêm module/store/provider theme nhỏ trong `src/app` hoặc `src/lib` khi cần. `AppShell.tsx` chỉ đổi điểm nối; Login không sở hữu logic theme riêng.

- Đưa resolver và state theme ra khỏi lifecycle của nút trong AppShell.
- Bootstrap trước render, storage guard, theo OS khi chưa chọn, đồng bộ nhiều tab và `color-scheme` theo quyết định ở trên.
- Nối Toaster với resolved mode. Giữ label/icon của toggle phản ánh đúng hành động tiếp theo.
- Thêm test hành vi cho resolver/khởi tạo/persistence; tránh test chỉ kiểm tên class hoặc chép lại implementation.

**Đạt khi:** mở trực tiếp Login hoặc route học bằng light/dark đã lưu đều đúng; reload không chớp sai theme trong kiểm tra quay/chậm tải; đăng xuất/đăng nhập và mở hai tab không làm lệch state.

### TM-03: Primitive, nội dung học và kiểm tra toàn phạm vi

**File chính:** `components/ui/input.tsx`, `button-variants.ts`, `card.tsx`, `select.tsx` và primitive form khác có lỗi; `features/learning/components/MarkdownContent.tsx`, `IpaSoundboardModal.tsx`, các modal/drawer hiện có; component trạng thái/biểu đồ trong `features/analytics`, Review và Schedule khi có bằng chứng lỗi.

- Đổi hardcoded background/text/focus thực sự sai role sang token; không xóa mọi màu trạng thái đang dùng đúng.
- Kiểm dropdown mở, modal đăng ký, IPA, notes drawer, tooltip/toast và focus trên từng surface.
- Kiểm Markdown: heading, đoạn văn, bảng, link, inline code, code block, quote và selection. Giữ sanitization và hành vi đọc hiện có.
- Kiểm màu progress/chart/legend và nhãn trạng thái, có chữ/icon hỗ trợ thay vì chỉ dựa màu.
- Thử rest, hover, active, focus-visible, selected, loading, disabled và reduced-motion. Disabled không dịch/nhấn; mode không làm thay đổi recipe tương tác.

**Đạt khi:** một vòng học cốt lõi hoạt động và lưu đúng; các màn dùng chung không còn chữ/nền không đọc được hoặc overlay sáng/tối lạc mode.

## 5. Kiểm tra và tiêu chí nghiệm thu

Mục tiêu tương phản theo [WCAG 1.4.3](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html): chữ thường tối thiểu **4.5:1**, chữ lớn đủ điều kiện tối thiểu **3:1**. Theo [WCAG 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html), dấu hiệu cần thiết để nhận biết control/trạng thái/đồ họa phải đạt **3:1** với màu kề phù hợp. Không áp ngưỡng này lên ô lưới hoặc bóng chỉ có tính trang trí; không tuyên bố tuân thủ toàn bộ WCAG từ vài phép đo.

Đo computed color và nền thực tế, tính cả lớp nền tổ tiên và alpha compositing. Đo cả chữ mờ, placeholder và link hover. Disabled được kiểm về khả dụng/nhận biết, không lấy ngoại lệ tương phản của disabled để bỏ qua chữ hoạt động.

| Nhóm kiểm | Ca tối thiểu |
|---|---|
| Khởi tạo | Storage light, storage dark, thiếu key với OS sáng/tối, key sai, storage bị chặn |
| Đồng bộ | Bấm toggle, reload, direct Login, route nội bộ, logout/login, OS đổi khi chưa chọn và sau đã chọn, hai tab |
| Trang trong ảnh | Lộ trình có dữ liệu, tiêu đề dài; đo heading, description, metadata và CTA trên Light/Dark |
| Luồng học | Hôm nay → mở bài → nhập phản hồi/ghi chú → đổi mode → lưu → reload; dùng tài khoản test, kiểm draft không mất |
| Overlay/form | Dropdown, đăng ký, IPA, notes, toast; input/textarea/validation, Escape và focus return |
| Trang còn lại | Tiến độ, Ôn tập, Lịch, Tùy chọn, Login/đăng ký; trạng thái loading/empty/error cần thiết |
| Palette/layout | Warm Light/Dark ở desktop 1440px và khoảng 1900px; sidebar mở/thu; smoke Cool cascade, không mở scope mobile |

Sau mỗi thay đổi, chạy nhóm test trực tiếp liên quan một lần. Nghiệm thu FE dùng `npm run build` (đã gồm TypeScript) và `npm run lint`; không chạy thêm typecheck trùng. Browser kiểm Light/Dark trực tiếp và phép đo tương phản, không bắt viết test cho từng màu/CSS. Chỉ chạy lại check khi có edit, failure hoặc nghi vấn mới.

Không mặc định chạy full Playwright hoặc mọi backend test cho thay đổi màu. Nếu triển khai theme provider làm lộ regression lan rộng hoặc đổi shared infrastructure đáng kể, mở rộng kiểm tra theo bằng chứng và quy tắc milestone trong `AGENTS.md`. Không sửa dữ liệu học cá nhân để dựng state; tái dùng app/DB development và tài khoản test đã xác định.

## 6. Thứ tự bàn giao

Ưu tiên TM-01 để giải quyết ngay lỗi đọc trong ảnh, sau đó TM-02 và TM-03. Codex nghiệm thu và commit/push riêng phần đạt; không gom các thay đổi auth/UI cũ chưa được nghiệm thu.

Bàn giao cuối ngắn gọn: file đã sửa, ảnh cùng route/viewport/state ở Light và Dark, cặp màu đã đo, check đã/chưa chạy và lỗi còn lại. Không tạo thêm báo cáo review trung gian. Kế hoạch này chưa phải kết quả sửa hoặc xác nhận toàn ứng dụng đã đạt tương phản.

Sau triển khai và kiểm chứng, ghi hợp đồng token Light/Dark cùng kinh nghiệm đã xác nhận vào skill Nhai theo yêu cầu lưu quy tắc dùng lại của người dùng; không biến các giá trị palette chưa đo thành chuẩn đã đạt.
