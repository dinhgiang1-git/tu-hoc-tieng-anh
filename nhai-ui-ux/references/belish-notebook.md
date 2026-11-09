# Belish — sổ tay vẽ phác và layout lộ trình

Đọc khi người dùng yêu cầu phong cách Belish; đây là biến thể của `nhai-ui-ux`, giữ thương hiệu, nội dung và chức năng của sản phẩm đích.

Với bài trắc nghiệm/từ vựng/đọc/nghe, đọc thêm [learning-methods.md](learning-methods.md): reference này chỉ quy định hình thức và roadmap; recipe tương tác theo phương pháp và khảo sát bài học bổ sung nằm ở file đó.

**Điều chỉnh theo phản hồi 09/11/2026:** không dùng vạch màu cạnh heading, dải màu một cạnh card hoặc đường nhấn trang trí ở đầu/cuối section. Heading phân cấp bằng chữ và khoảng cách; card không đổi độ dày/màu riêng một cạnh theo nhóm hoặc completion. Quy tắc này ưu tiên hơn ảnh nguồn và các recipe cũ. Bỏ padding dành cho vạch, không thay bằng vạch xám/gradient/gạch chân trang trí. Viền bao trung tính, focus ring, ranh giới control và progress có chức năng vẫn giữ. Trục roadmap dùng nét trung tính chỉ khi có trình tự thật. Marker tô nền chữ, tape và grid không đồng nghĩa với thanh màu ở mép; không tăng chúng để bù trang trí vừa bỏ.

## Bằng chứng và giới hạn

Khảo sát [Belish Roadmap](https://belish.net/vi/roadmap) ngày 09/11/2026: tổng quan, mở A1 và Cài đặt; chưa đăng nhập. Xem ở 670×900 và 1440×900, đọc computed style DOM ở desktop. Phiên khảo sát đang Dark/blue và bật switch “Giao diện vẽ tay”. Không xác định đây là mặc định của mọi người truy cập. Chưa đổi cài đặt nguồn, khảo sát Light, mở workspace, lưu tiến độ hay thanh toán.

Nét phác, tape, marker và layout được quan sát qua ảnh. Chưa đọc cơ chế vẽ của nguồn. Border trong suốt/box-shadow none trên element gốc không có nghĩa ảnh không có viền trang trí: chúng có thể thuộc lớp khác. CSS và palette Light dưới đây là đề xuất triển khai, không phải mã nguồn Belish.

## Những gì tạo nên phong cách

| Đã quan sát | Quyết định áp dụng |
|---|---|
| Topbar ngang toàn trang, sidebar trái, main như trang giấy có khung | Chrome ổn định, một vùng cuộn nội dung, giữ route/menu hiện có |
| Itim, grid mờ, viền phác chồng nét | Dùng font sẵn có; decoration CSS mảnh, không làm méo chữ hoặc control |
| Active nav xanh; tên cấp màu riêng; mô tả đầu trang tô vàng | Một màu action; màu nhóm nếu cần ở nhãn/tape, không ở dải mép card; marker ngắn, không tô đoạn dài |
| Băng dính pastel hơi lệch trên đầu thẻ | Một pseudo-element không tương tác; không xoay cả card |
| Tổng quan có trục trái, mốc cấp và thẻ ngang | Chỉ có trục khi các nhóm thực sự có thứ tự; catalog độc lập dùng danh sách thẻ ngang |
| Chi tiết A1 có summary, CTA tiếp tục, các buổi so le quanh trục giữa | Chỉ so le nhóm ngắn trên desktop; giáo trình dài cần nhóm/thu gọn |

Không sao chép logo/mascot, chat nổi, Premium, footer quảng bá, streak, nhãn A1–C2 hoặc lời hứa đầu ra vào sản phẩm không có chúng. Trạng thái khóa/tiến độ ở nguồn không phải quy tắc backend của sản phẩm đích.

## Token và hình học

**Đo từ DOM trong phiên Dark/blue:**

| Vai trò | Giá trị đọc được |
|---|---|
| Nền body/main | `#28232b` |
| Nền lesson card | `#36303c` |
| Chữ chính | `#f1f3f7` |
| Primary fill | `#5194ff` |
| Border color mặc định | `#463e4e`, không đại diện toàn bộ nét phác nhìn thấy |
| Font body | `Itim, Arial, Helvetica, sans-serif`, 16px, 400 |
| Sidebar / topbar | 256px / 72px |
| Sidebar item | 232×44px, inset 12px; nav link 14px |
| Lesson title / CTA tiếp tục | 18px / 14px |
| Radius element chính | 6px |

Nguồn có chữ trắng trên primary xanh sáng. Khi triển khai phải đo lại tương phản; dùng chữ mực tối trên xanh sáng hoặc fill tối hơn cho nút chữ trắng. Link/text có token riêng.

**Đề xuất**, không phải số đo nguồn:

- Dark bám nền tím than ở trên. Light: paper `#faf7ef`, surface `#fffdf7`, ink `#28232b`, muted `#625969`. Giữ mode đã lưu, không ép Dark theo phiên khảo sát.
- Action fill `#5194ff`, on-action `#101c32`; action text/focus Light `#2457ad`, Dark `#9dbfff`. Marker `#f2d94e`, chữ `#28232b`. Đo tổ hợp thực dùng trước nghiệm thu.
- Grid 20px; nét Dark `rgba(241,243,247,.06)`, Light `rgba(40,35,43,.08)`; vùng đọc/form đặc màu.
- Sidebar mở 240–256px, thu 64–72px; topbar 64–72px; main padding 24–32px và dùng chiều ngang còn lại. Riêng đoạn bài đọc giới hạn 65–78ch.
- H1 30–34px, H2 22–26px, lesson title 18–20px; body 16–18px. Itim có face 400 thật; IPA dùng fallback hỗ trợ glyph khi cần.
- Viền chức năng 1–2px; nét phác phụ opacity thấp, lệch 1–3px. Shadow cứng 2px và press 2px cho biến thể này, thống nhất các primitive; không để vài trang giữ offset Warm 4px.

Palette và mode là hai trục riêng. Map vào semantic tokens hiện có; alias `--nh-*` lấy cùng nguồn, không lập bảng màu độc lập mỗi component. Không cần thêm UI chọn palette chỉ để triển khai phong cách.

## Decoration dùng chung

Một recipe frame/panel, một modifier tape. Pseudo-element không có nội dung đọc bởi screen reader và dùng `pointer-events:none`. Không cần thư viện vẽ tay. Ví dụ dựng mới:

```css
.notebook-panel {
  position: relative;
  min-width: 0;
  border: 1px solid var(--notebook-edge);
  border-radius: 6px;
  background: var(--notebook-surface);
  box-shadow: 2px 2px 0 var(--notebook-shadow);
  isolation: isolate;
}
.notebook-panel::before {
  content: "";
  position: absolute;
  inset: 2px -2px -2px 1px;
  border: 1px solid var(--notebook-sketch);
  border-radius: inherit;
  opacity: .4;
  pointer-events: none;
}
.notebook-panel[data-tape]::after {
  content: "";
  position: absolute;
  width: 54px;
  height: 18px;
  top: -10px;
  left: calc(50% - 27px);
  background: var(--notebook-tape);
  opacity: .65;
  transform: rotate(-3deg);
  pointer-events: none;
}
```

Định nghĩa các biến ở token theme; đưa recipe vào component của stack hiện tại. Chừa chỗ tape; tránh ancestor overflow cắt viền/bóng. Dialog đặt decoration ngoài scroll body. Không phủ nét phác lên input/chart/focus ring. Card tĩnh không nhún; interactive dùng press recipe chung; card có nút/link con không bọc bằng control khác. Giữ keyboard, disabled/loading và reduced-motion theo skill chính.

## Công thức bố cục

**Catalog:** title + marker mô tả hữu ích → thẻ ngang có viền bao trung tính, không có dải màu một cạnh. Mỗi thẻ có tên, mô tả 1–2 dòng, metadata có nguồn, link mở. Nếu API xác định thứ tự nhóm, có thể thêm rail trái 64–80px và đường nối trung tính. Các khóa độc lập không nối thành hành trình bắt buộc. Màu nhóm không tự biểu đạt proficiency.

**Chi tiết roadmap:** quay lại → title/metadata → CTA hợp lệ → các phase/chặng. Dùng ordered list cho thứ tự buổi, disclosure có label. Trục giữa/so le chỉ cho nhóm ngắn đang mở, khoảng 8–12 mục trở xuống và đủ ngang; đây là ngưỡng thiết kế đề xuất. Danh sách dài dùng trục trái, thẻ compact và thu gọn. DOM giữ thứ tự học, không CSS order làm lệch Tab.

**Workspace:** chưa quan sát ở nguồn; chỉ chuyển ngôn ngữ vỏ: header gọn, task rail, mặt giấy đọc và nút. Giữ draft/quiz/note mounted hoặc nâng state có kiểm chứng; một action đúng policy. Chuyển bước không gọi hoàn thành.

**Dữ liệu:** progress đúng mẫu số server; activity không đổi nhãn thành buổi, null không thành 0%. “Chưa học” không đổi thành “Khóa” khi BE không khóa. Không làm mờ cả card chưa học đến mức chữ mất tương phản. Không gán enrollment bằng title hoặc bịa CEFR/số bài.

## Nghiệm thu

- Nhận ra phong cách từ font, grid, cạnh phác, tape, marker và phân cấp layout; không cần thương hiệu nguồn.
- Desktop 1280×720, 1440×900, sidebar mở/thu, zoom 125%, tiêu đề dài; không tràn ngang hoặc cắt focus/tape.
- Một heading chính, CTA sớm; không hàng trăm card so le/khoảng trắng vô ích. Semantic list/heading/progress, Tab đúng thứ tự.
- Không có vạch màu ở heading/card, không còn thụt chữ do vạch cũ và không thêm dải trang trí thay thế; trạng thái vẫn hiểu được bằng nhãn/icon.
- Đo tương phản Light/Dark thực tế, cả marker và active nav. Decoration không cản click/focus.
- Giữ quyền, version, date-only và state khi đổi theme; test liên quan, build nếu sửa FE, lint lúc nghiệm thu theo AGENTS.

Reference này không chứng nhận UX nguồn hay giao diện ứng dụng đã triển khai.
