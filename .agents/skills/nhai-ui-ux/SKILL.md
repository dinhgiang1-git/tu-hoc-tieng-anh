---
name: nhai-ui-ux
description: >-
  Thiết kế hoặc cải thiện website theo phong cách sổ tay Nhai hoặc Belish: nền kẻ ô, chữ Itim, viền mực, bóng cứng, thẻ màu và layout lộ trình. Dùng khi người dùng yêu cầu phong cách Nhai/Belish hoặc cung cấp skill làm design brief; giữ stack và nội dung của dự án.
---

# Nhai UI/UX — skill độc lập

## Cách sử dụng và phạm vi

Đọc toàn bộ file này trước khi thiết kế. Dùng nó làm đặc tả có thể triển khai, không cần repo, file CSS, ảnh tham chiếu hay skill khác. Không cần truy cập lại website nguồn. Các URL ở cuối chỉ ghi xuất xứ, không phải dependency.

**Biến thể Belish (bổ sung 09/11/2026):** khi người dùng yêu cầu giống Belish hoặc đưa `belish.net/vi/roadmap` làm tham chiếu, đọc thêm [references/belish-notebook.md](references/belish-notebook.md). Reference quy định palette, nét phác, băng dính, marker và bố cục roadmap; các lựa chọn đó ưu tiên hơn mặc định Warm/Cool và công thức bìa sách ở đây. Giữ hợp đồng semantics, state và bảo toàn dữ liệu. Với biến thể này cần mang theo thư mục `references/` khi chia sẻ skill; các chế độ Nhai cũ vẫn tự chứa trong file này.

Trong môi trường hỗ trợ skill, đặt file này trong thư mục `nhai-ui-ux/`. Với AI chỉ nhận tài liệu, đính kèm file này; nếu yêu cầu Belish thì đính kèm thêm `references/belish-notebook.md`, rồi yêu cầu:

> Đọc skill này và làm website [mục đích] cho [đối tượng]. Dùng thương hiệu/nội dung [của tôi], giữ stack [hiện tại hoặc stack được yêu cầu]. Áp dụng chế độ [danh mục/dashboard/landing page/gói dịch vụ] phù hợp và kiểm tra theo tiêu chí cuối file.

Giữ mục tiêu, nội dung, chức năng và stack của người dùng. Không tự chuyển sản phẩm thành app ngoại ngữ, không tự thêm trả phí, tài khoản, quảng cáo hoặc mascot. Với dự án hiện có, xác định các trang/component dùng chung bị ảnh hưởng trước khi sửa; giữ các luồng hoạt động ngoài phạm vi yêu cầu.

### Đọc nhanh và thứ tự ưu tiên

Lần đầu đọc toàn file để hiểu hệ thống. Khi thực thi: chọn theme và cấu trúc theo mục 3, thiết lập component dùng chung theo mục 4.1–4.2 trước khi ghép trang, dùng công thức mục 4.3 và CSS mục 6; chỉ áp dụng mục 5 nếu có bán gói dịch vụ. Mục 1–2 giải thích nguồn và quyết định, không phải nội dung phải hiển thị trên website. Checklist mục 8 là điểm kết thúc công việc trong phạm vi đã giao.

Ưu tiên theo thứ tự: yêu cầu rõ ràng của người dùng → chức năng và khả dụng → quy ước/stack dự án → quy tắc phong cách → ví dụ minh họa. Có xung đột thì giữ ý định người dùng và giải thích lựa chọn ảnh hưởng hình thức. Không thêm trang hoặc luồng chỉ để dùng đủ mọi component trong skill.

Mặc định khi thiếu lựa chọn: dùng theme **Warm**, giữ stack hiện tại; nếu dự án mới và người dùng không chỉ định công nghệ, chọn giải pháp đơn giản đáp ứng chức năng. Nội dung tiếng Việt, icon nét đơn nhất quán, nền giấy ở shell và nền đặc ở panel. Chỉ dùng Cool khi được yêu cầu hoặc dự án đã có thương hiệu xanh phù hợp. Dữ liệu quan trọng còn thiếu thì làm phần độc lập trước; đánh dấu phần cần dữ liệu, không tự bịa. Có thể dùng nội dung mẫu được gắn rõ là mẫu khi người dùng yêu cầu prototype.

### Chọn cấu trúc theo nhiệm vụ

| Nhiệm vụ | Cấu trúc khởi đầu | Tránh |
|---|---|---|
| Danh mục/thư viện | Tiêu đề, search/filter nếu cần, nhóm thẻ, trạng thái rỗng | Buộc dùng 6 cột cho thẻ dài |
| Dashboard | App shell, tác vụ chính, chỉ số cần thiết, danh sách/bảng | Đặt texture lên dữ liệu hoặc tự bịa biểu đồ |
| Portfolio | Nav gọn, giới thiệu ngắn, dự án có ảnh, liên hệ thật | Thay ảnh dự án bằng bìa khóa học |
| Landing page | Giá trị, CTA chính, nội dung/bằng chứng thật, CTA cuối nếu cần | Sidebar và pricing không liên quan |
| Một gói dịch vụ | Một pricing card + giải thích/FAQ bên cạnh trên desktop | Tự dựng nhiều hạng gói |

Ví dụ yêu cầu có thể gửi nguyên văn cho AI khác:

> Dùng file skill này làm portfolio cho nhiếp ảnh gia, theme Warm. Giữ Vue của dự án, dùng ảnh và nội dung hiện có. Có danh mục dự án và trang chi tiết, không thêm hội viên. Kiểm tra trên mobile và desktop.

> Dùng file skill này đổi giao diện dashboard quản lý công việc, theme Cool. Giữ API, route và chức năng hiện có. Ưu tiên đọc dữ liệu; chỉ dùng texture ở vùng trang trí.

> Dùng file skill này làm trang một gói dịch vụ, theme Warm. Giá và quyền lợi lấy từ dữ liệu tôi cung cấp. Có chọn thời hạn và tính tổng; dừng ở prototype nếu chưa có backend thanh toán.

## 1. Cơ sở và mức độ chắc chắn

- **Đã quan sát:** trang chủ `nhaitopik.com` ở trạng thái chưa đăng nhập, ảnh desktop và quan sát mobile ban đầu; computed styles/CSS cho font, màu, viền, bóng, layout và breakpoint.
- **Đã quan sát bổ sung:** trang chủ và `nhaihsk.com/membership` ngày 29/10/2026 trong cửa sổ Helium 1280 × 758 px, chưa đăng nhập. Đã cuộn danh mục trang chủ, đọc pricing/FAQ và chọn thời hạn 12 tháng, thấy tổng tiền cập nhật. Công cụ browser tự động trước đó bị chặn; lần này xem bằng UI trình duyệt thông thường, không đổi cơ chế bảo vệ trang. Chưa đo computed styles của HSK; màu/texture/kích thước HSK bên dưới là quan sát trực quan hoặc giá trị đề xuất, không phải token chính xác.
- **Thiết kế bổ sung:** cách giảm mật độ, trình bày giá, trạng thái form, accessibility, cách dùng ảnh và chuyển phong cách sang ngành khác là khuyến nghị của skill. Chưa có nghiên cứu người dùng hoặc A/B test chứng minh cải thiện chuyển đổi.

Chưa kiểm tra mobile HSK, các trạng thái menu con, đăng nhập hoặc thanh toán; chưa xác minh font HSK bằng CSS. Giữ một hệ thống thống nhất thay vì trộn component của hai nguồn tùy tiện. Các nhận xét UX là đánh giá chuyên môn từ giao diện quan sát, không phải kết quả nghiên cứu người dùng.

## 2. Điểm giữ và điểm bù trừ

| Quan sát/đánh giá từ Nhai TOPIK | Giá trị mang lại hoặc rủi ro UX | Quyết định áp dụng |
|---|---|---|
| Chữ viết tay, giấy ô, thẻ như bìa sách | Nhận diện thân thiện, dễ nhớ | Giữ làm ngôn ngữ chung |
| Viền đen và bóng cứng | Phân tách rõ thành phần, cảm giác bấm được | Giữ nhưng chỉ thành phần tương tác mới chuyển động |
| Màu theo nhóm và số thứ tự lớn | Dễ phân loại danh mục, nhận ra trình tự | Giữ nhãn chữ cùng màu; số lớn chỉ khi có ý nghĩa |
| Lưới 3/4/6 cột cho bìa rất ngắn | Hiệu quả với nhiều cuốn sách; chật nếu có mô tả/giá/CTA | Tách lưới bìa ngắn khỏi lưới nội dung dài |
| Nền ô, màu bìa, banner và nhiều điểm nhấn cùng xuất hiện | Có nguy cơ cạnh tranh chú ý trên màn hình nhỏ | Giảm trang trí ở vùng ra quyết định; một thông báo ưu tiên mỗi vùng |
| Nút xanh dùng chữ trắng nhỏ | Cần kiểm tra tương phản thay vì mặc định đạt | Ưu tiên chữ tối trên xanh sáng; nếu chữ trắng thì chọn xanh tối đủ tương phản |
| Navigation ứng dụng ở sidebar | Hợp công việc lặp lại; có thể quá nặng cho một landing page | Dùng sidebar cho app, nav ngang gọn cho trang giới thiệu/bảng giá độc lập |

Đây là đánh giá thiết kế, không phải đo lường hành vi thực tế của người dùng nguồn.

### Bổ sung từ Nhai HSK và cách bù trừ

| Quan sát thực tế HSK | Đánh giá | Quyết định tổng hợp |
|---|---|---|
| Giấy kem, đỏ đất/vàng ấm, viền mực, bóng lệch màu taupe | Cảm giác sổ tay ấm và có cá tính hơn việc chỉ đổi màu xanh | Thêm biến thể Warm HSK nhất quán; không trộn ngẫu nhiên cả hai bảng màu |
| Texture sờn trên thẻ, nét trang trí và hình nền mờ | Có chất liệu nhưng dễ làm rối khu vực chữ nhỏ | Texture rất nhẹ ở mảng bìa; không phủ lên giá, form hoặc đoạn văn |
| Thẻ danh mục ngang, tiêu đề lớn + dải thống kê dưới | Dễ biết lượng nội dung trước khi mở | Dùng cho khóa học/dự án cần metadata; không ép thành bìa đứng |
| Rail icon + nhãn rất nhỏ, một số mục dưới vùng nhìn | Tiết kiệm chiều ngang nhưng khó quét/đọc nhãn dài | Ưu tiên sidebar rộng có nhãn 14 px; rail chỉ khi nhãn ngắn, đủ đọc và có cơ chế truy cập mọi mục |
| Pricing một gói bên trái; miễn phí và FAQ bên phải | Giảm số quyết định, giải thích ngay giá trị nâng cấp | Một gói thì dùng một card + vùng giải đáp, không dựng giả ba gói |
| Giá cơ sở, nút thời hạn, tổng tiền và CTA rõ | Lựa chọn và kết quả gần nhau | Giữ toàn bộ cụm chọn thời hạn → tổng thu → CTA liền nhau, đủ gần quyền lợi |
| Có khoảng trống lớn giữa quyền lợi và bộ chọn | Có thể tăng chiều cao, đẩy CTA xuống dưới màn hình | Dùng gap theo nội dung; tránh chiều cao cố định tạo khoảng trống vô nghĩa |
| Giải thích không tự gia hạn, tính năng miễn phí vẫn giữ; FAQ nói ai không cần mua | Minh bạch, giảm mua nhầm | Đặt điều kiện và đối tượng không phù hợp cạnh giá, dùng chính sách thật của sản phẩm mới |
| Một dòng hỗ trợ có giọng đùa, chưa nêu phạm vi cụ thể | Thân thiện nhưng không đủ làm cam kết dịch vụ | Viết rõ kênh/phạm vi hỗ trợ; giữ giọng ấm, không thay chính sách bằng câu đùa |

Khi chọn 12 tháng, nguồn hiển thị tổng `412.000đ`, giá trước giảm `468.000đ`, nhãn tiết kiệm 12%; giá cơ sở vẫn là 39.000đ/tháng. Đây chỉ là dữ liệu minh chứng tại thời điểm khảo sát, không phải giá để dùng trong dự án mới. Bản triển khai mới cần quy tắc làm tròn đồng nhất giữa nhãn giá, tổng tiền và backend; phân biệt giá cơ sở với giá quy đổi sau giảm.

## 3. Ngôn ngữ hình ảnh

### Màu và hình khối — bộ Cool TOPIK đo được

| Token | Giá trị mặc định | Vai trò |
|---|---|---|
| Primary | `oklch(67.47% .1726 259.49)` | Xanh periwinkle, nút/selection/trang trí |
| Text | `oklch(14.5% 0 0)` | Chữ gần đen |
| Border | `oklch(13.979% .00002 271.152)` | Cạnh và bóng |
| Surface | `oklch(98% 0 0)` | Panel gần trắng |
| Secondary | `oklch(97% 0 0)` | Nền sidebar/phụ |
| Paper | `color-mix(in oklch, var(--nh-primary) 15%, white)` | Giấy xanh nhạt |
| Grid | Dọc `#8080804d`, ngang `#8080808f` | Nét 1 px, chu kỳ 20 px |
| Highlight | `#ffd94a` | Marker vàng, dùng hạn chế |
| Notice | `oklch(93.6% .032 17.717)` | Thông báo hồng nhạt |
| Border width | `2px` | Thành phần chính |
| Shadow | `2px 2px 0 0 var(--nh-border)` | Không blur |
| Radius | `5px` | Nút, panel, thẻ |
| Shell radius | `18px` | Khung ngoài desktop |

Giữ quan hệ màu sáng + cạnh tối + bóng sắc nét. Không thay bằng glassmorphism, glow hoặc bóng mờ rộng. Không đặt đoạn văn dài trực tiếp trên lưới tương phản cao; panel nội dung phải đặc màu. Dashboard, bảng dữ liệu và giá nên dùng nền trơn ở bên trong.

Màu danh mục quan sát được gồm `#0076BE`, `#0a6e3a`, `#E11D2A`, `#C8462E`. Chọn một tập phù hợp sản phẩm, giữ ánh xạ nhất quán. Không dùng màu danh mục làm trạng thái thành công/lỗi một cách mâu thuẫn. Một nhóm điều khiển chỉ có một hành động ưu tiên nổi bật; nhóm lựa chọn như các gói dịch vụ có thể có CTA lặp cùng cấu trúc.

### Biến thể Warm HSK — tổng hợp từ quan sát hình ảnh

Nếu người dùng nói thích Nhai HSK hoặc không chọn theme, ưu tiên Warm theo mặc định ở đầu file. Cool dành cho yêu cầu phong cách TOPIK/xanh hoặc thương hiệu hiện có phù hợp. Công bố lựa chọn ngắn gọn và dùng nhất quán; không tự đổi theme giữa các trang. Đây là quy tắc lựa chọn thiết kế, không phải theme switch đã kiểm chứng ở nguồn.

Palette **đề xuất gần cảm giác HSK, chưa đo từ CSS**: paper `#faf5ec`, surface `#fffaf2`, panel phụ `#f2e8d9`, ink `#302d27`, border `#39342d`, đỏ đất `#b84d43`, vàng `#e3bd68`, shadow `4px 4px 0 #b8ac96`, grid `rgba(97,81,54,.13)` chu kỳ 20 px. Kiểm tra tương phản thật; với nút vàng dùng chữ tối, với đỏ có thể cần làm tối hơn nếu chữ trắng nhỏ. Không mặc định mọi tổ hợp của palette đã đạt chuẩn.

Giữ radius nhỏ và cạnh rõ của TOPIK; lấy sự ấm/chất liệu của HSK, giảm watermark và texture ở vùng đọc. Bóng Warm được đề xuất 4 px thay vì quy tắc 2 px của Cool: không trộn hai kiểu shadow tùy tiện trong cùng theme. Hover dịch bằng đúng offset shadow của theme. Một nét marker hoặc chi tiết giấy lệch nhẹ là đủ; không xoay toàn bộ card/input. Dùng decoration nguyên bản, không lấy logo, mascot, bản đồ hay thông điệp riêng của nguồn vào sản phẩm khác.

### Typography

- Itim là font đặc trưng cho tiếng Việt/Latin. Nguồn quan sát dùng fallback Nanum Gothic cho chữ Hàn. Dùng font phù hợp hệ chữ khi có tiếng Trung/Hàn; không ép toàn bộ glyph vào một font không hỗ trợ.
- Itim có face Regular; khai báo 400 đúng file. Không khai báo giả là variable hoặc có face 700 riêng. Đậm tổng hợp nếu dùng cần kiểm tra thực tế.
- Một file skill không chứa binary font. Khi triển khai, dùng font Itim đã có trong dự án hoặc lấy từ nguồn Google Fonts chính thức, giữ giấy phép khi self-host. Nếu không lấy được, dùng fallback hệ thống, nêu rõ sai khác; không để font cản việc hoàn thành giao diện.
- Mặc định body 16 px/1.5–1.65; nav/chú thích 14 px; tiêu đề nhóm 24 px mobile và 30 px desktop; hero 30–40 px theo nội dung, không phóng quá lớn làm vỡ bố cục.
- Với văn bản dài, bảng, số tiền hoặc dữ liệu dày, có thể dùng sans-serif dễ đọc cho phần dữ liệu, giữ Itim ở tiêu đề/nhãn để bảo toàn nhận diện. Đây là lựa chọn bổ sung, cần nhất quán.
- Giá và dữ liệu dùng `font-variant-numeric: tabular-nums`; dấu tiếng Việt không bị cắt. Không dùng cỡ chữ dưới 14 px cho điều kiện mua hoặc thông tin thiết yếu.

### Spacing và responsive

Nhịp 4 px; padding panel 16–24 px; khoảng cách phần 24–32 px (nguồn có khoảng 22 px ở trang chủ). Main tối đa khoảng 1215 px, mobile padding ngang 16 px, desktop 32 px. Đo và điều chỉnh theo nội dung, không ép mọi trang đúng một chiều rộng.

| Loại trang/thành phần | Mobile | Tablet | Desktop |
|---|---|---|---|
| App nhiều tác vụ | Header khoảng 56 px + drawer | Header + drawer nếu thiếu chỗ | Sidebar khoảng 224 px từ 1024 px khi đủ rộng |
| Landing/bảng giá độc lập | Nav gọn + menu khi cần | Nav thích ứng | Nav ngang; không bắt buộc sidebar |
| Bìa ngắn chỉ nhãn + số | 3 cột nếu còn đọc được | 4 cột từ 640 px | 6 cột từ 1024 px |
| Card mô tả dài | 1 cột | 2 cột nếu đủ rộng | 2–3 cột |
| Pricing card | 1 cột | 2 cột nếu không chật | 3 cột khi số gói phù hợp; nhiều gói hơn cần nhóm |

Với dashboard được yêu cầu mở rộng hết vùng nội dung desktop, để app shell và wrapper của trang cùng dùng chiều rộng khả dụng (`w-full`, `min-w-0`); bỏ cả max-width/container lồng gây hai dải trống. Giữ padding mép vừa phải và kiểm cả trạng thái sidebar mở/thu. Giới hạn chiều dài dòng cho riêng đoạn văn dài khi cần, không bó cả dashboard theo cột bài đọc.

Breakpoint mặc định 640/1024 px là điểm bắt đầu, chuyển bố cục khi nội dung thực tế cần. Header cố định phải có phần bù chiều cao; anchor có scroll-margin. Mobile không có tràn ngang toàn trang, kể cả shadow. Bảng rộng chỉ cuộn bên trong vùng có nhãn và chỉ báo phù hợp.

## 4. Công thức thành phần

### 4.1. Component dùng chung là nguồn chuẩn

Token → primitive/component dùng chung → component nghiệp vụ → trang. Khi triển khai skill, thiết lập hoặc sửa lớp component UI của dự án trước khi thay giao diện từng trang. Có màu/token chung chưa có nghĩa là đã có hệ component chung.

- Tái dùng lớp UI hiện có của dự án; tên bên dưới là vai trò, không bắt buộc đổi tên mọi component thành Nhai. Không tạo một bộ Button mới song song nếu Button hiện có có thể đáp ứng bằng variant.
- Mỗi họ component có **một** định nghĩa style/state. Button điều hướng dạng link/router-link dùng cùng recipe với Button thao tác; khác element semantics, không khác ngôn ngữ hiệu ứng.
- Trang chỉ chọn variant, size, dữ liệu, icon và callback/đích điều hướng. Không tự viết lại shadow, border, radius, transform, hover, active hoặc duration của primitive trong từng trang.
- `className`/class mở rộng ở điểm dùng dành cho bố cục như margin, width, grid placement. Nếu cần đổi hành vi/hình thức primitive, thêm variant có tên và dùng lại từ định nghĩa chung; không chữa bằng override cục bộ hay `!important`.
- Không đổi nghĩa toàn cục utility của framework như `.border`, `.rounded-xl`, `.shadow-sm` để ép phong cách. Dùng token/component hoặc utility có tên riêng; tránh utility hover cũ vô tình ghi đè bóng cứng bằng bóng mờ.
- IconButton dùng Button với size icon và accessible name. Nút phát âm, bookmark, refresh, submit bọc Button chung; chỉ thêm logic nghiệp vụ, không sở hữu một bộ hiệu ứng riêng.
- StatCard, ChartPanel, CourseCard, PricingCard dùng Card/Panel chung cho surface/viền/bóng/padding. Khác bố cục bên trong không phải lý do để nhân bản style vỏ.
- Skeleton/EmptyState/ErrorState dùng cùng quy tắc vỏ và kích thước bố cục; không tự đưa lại góc tròn hoặc shadow từ theme cũ. Chỉ tạo các component cần dùng, không dựng thư viện lớn ngoài phạm vi.

### 4.2. Hợp đồng variant và trạng thái

| Primitive/vai trò | Variant/thuộc tính khởi đầu | Trạng thái dùng chung và hiệu ứng |
|---|---|---|
| Button / ActionLink | primary, secondary/outline, danger; size sm/md/lg/icon | Các variant nổi dùng cùng press recipe theo theme; chỉ khác màu/diện mạo có chủ đích |
| Button nhẹ | ghost, text/link | Không có shadow/translate; hover đổi nền hoặc underline, focus rõ; không lấy class raised từ primary |
| Card/Panel tĩnh | static mặc định; padding sm/md/lg khi cần | Có viền/bóng theo theme nhưng **không** hover/active/cursor pointer/tabIndex |
| Card toàn phần có hành động | interactive + href hoặc onActivate | Cùng press recipe với nút nổi; focus-visible; ngữ nghĩa link hoặc button đúng hành vi |
| Card chứa nút/link con | static wrapper + control con | Chỉ control con phản hồi; không dịch cả card khi hover/focus vào con |
| Field/Input | label, hint, error, disabled, required | Focus/error/disabled nhất quán; không dịch chuyển khi nhập |
| Select/Dropdown dạng nút | label, value, expanded, disabled | Trigger dùng cùng recipe secondary/outline Button: viền, radius, bóng cứng và press; menu là panel tĩnh có bóng cứng, không dịch cả wrapper |
| Tabs/ChoiceChip/Toggle | selected/pressed, disabled | Selected đổi fill/viền + semantics; không giữ transform như trạng thái “đang bị nhấn” |
| NavItem | current, expanded, disabled nếu có | Current/hover/focus thống nhất; không tự thêm hiệu ứng card nhún vào sidebar |

**Quy tắc nhất quán:** cùng họ + cùng variant + cùng trạng thái phải có cùng hiệu ứng trên mọi trang. Khác hiệu ứng chỉ khi vai trò hoặc variant khác nhau theo bảng, không do người xây từng trang tùy chọn. Một StatCard không click được không cần nhún theo CourseCard có link.

Press recipe chuẩn: rest có shadow theo token; hover (chỉ thiết bị hỗ trợ hover) và active dịch đúng `--nh-press` theo cả hai trục, shadow none; focus-visible có outline riêng và không tự dịch; 150 ms easing chung. Disabled, aria-disabled và loading không dịch, không chạy hover/active, không kích hoạt. Reduced-motion bỏ dịch chuyển và transition, vẫn có phản hồi màu/outline/shadow. Không trộn `scale`, nâng card lên, shadow mờ hoặc số pixel hardcode vào các điểm dùng.

Với dropdown dạng nút, tái dùng recipe Button cho trigger thay vì chép class riêng; size giữ khớp các nút cạnh bên. Chỉ trigger dịch khi hover/active, menu neo vào wrapper ổn định để vị trí không nhảy theo trigger. Menu dùng border/radius/shadow token của theme, item phản hồi bằng màu; không thêm press cho toàn menu. Mũi tên phản ánh expanded, transition theo motion token và tắt với reduced-motion. Giữ nguyên bàn phím, Escape trả focus, chọn bằng Enter, đóng khi click ngoài và disabled. Không giữ trạng thái nhấn chỉ vì menu đang mở.

Với Tailwind 3 và biến chứa toàn bộ giá trị bóng, dùng `[box-shadow:var(--nh-shadow)]` hoặc CSS property trực tiếp. `shadow-[var(--nh-shadow)]` có thể bị suy luận thành shadow-color; kiểm computed `box-shadow` của menu thay vì chỉ kiểm tên class.

Với tab chuyển form, có thể dùng gạch dưới rõ và nền tint nhẹ thay cho khối màu đặc. Giữ cả hai form mounted để bảo toàn dữ liệu, nhưng panel ẩn phải inert, aria-hidden và chặn pointer/focus. Mặc định khung vừa chiều cao tự nhiên của form đang mở: không lấy chiều cao form dài nhất rồi đẩy nút của form ngắn xuống tạo khoảng trống lớn. Nếu cần chuyển kích thước mượt, đo panel active bằng ResizeObserver và chuyển chiều cao wrapper khoảng 220 ms; các ô nhập giữ nguyên kích thước, không scale/stretch cả form. Cập nhật khi validation, font hoặc chiều rộng đổi; dọn observer và bỏ callback cũ khi đổi tab nhanh. Chỉ dùng khung cao cố định khi khoảng trống được chấp nhận rõ ràng. Panel có thể crossfade/trượt nhẹ 4–8 px; reduced-motion tắt cả chuyển động panel và wrapper. Trạng thái selected cập nhật ngay, không phụ thuộc timer/animationend. Đồng bộ ARIA tabs và bàn phím, giữ riêng dữ liệu, chặn đổi mode khi đang gửi. Kiểm trực tiếp khoảng cách ô cuối–nút gửi, các khung hình giữa chuyển cảnh và nội dung lỗi, không chỉ xác nhận có transition trong CSS.

Với focus của ô nhập, có thể dùng viền theo muted-foreground và halo mỏng theo primary ở opacity khoảng 0.16 để hợp màu giấy, thay vì chồng thêm outline xanh đậm lên viền đen. Giữ viền focus đọc được bằng bàn phím; halo nhạt chỉ bổ trợ, không là dấu nhận biết duy nhất. Đặt recipe ở Input chung và kiểm computed style để tránh hai vòng focus từ CSS nền và utility.

API component phải tự gán motion mặc định từ variant: raised Button/interactive Card → `data-nh-motion="press"`; ghost/text/static → không có press. Đây là **chi tiết nội bộ primitive**, không phải thuộc tính mà từng trang được tự bật/tắt. Chỉ cho phép ngoại lệ được đặt tên rõ trong component chung khi có yêu cầu thực tế.

Button mặc định `type="button"`, chỉ dùng submit khi chủ đích. Loading giữ kích thước/label truy cập được, `aria-busy`, chặn thao tác lặp. `aria-disabled` trên link không tự chặn điều hướng: primitive phải xử lý cả pointer lẫn bàn phím; dùng native disabled cho button khi phù hợp. Card có link con không được bọc bằng link/button khác hoặc nested control. Không làm div clickable chỉ để dễ thêm animation.

Trong React có thể dùng Button + một hàm variants dùng chung cho ActionLink; trong Vue/Svelte dùng props/recipe tương đương; HTML thuần dùng class/data attribute chuẩn từ cùng CSS. Không phụ thuộc một thư viện component cụ thể.

### 4.3. Công thức nội dung

**Panel:** nền surface của theme, viền 2 px, radius 5 px, shadow theo token theme (Cool 2 px, Warm 4 px). Tiêu đề và nội dung có khoảng cách rõ. Không biến mọi panel thành click target nếu không có hành động.

**Section heading:** marker trái 4 px + padding trái 12 px, tiêu đề ngắn, một dòng mô tả. Không thêm sticker cạnh mọi dòng.

**Button:** tối thiểu 44 px chiều cao, label rõ hành động và đối tượng; primary theo theme (Cool xanh, Warm vàng), surface cho secondary, link cho tertiary. Hover/active dịch bằng offset shadow của theme và bỏ shadow, khoảng 150 ms. Có focus rõ và trạng thái đang xử lý/disabled. Bảo đảm thao tác không bị gửi lặp. Link không có thuộc tính disabled: nếu cần vô hiệu hóa, dùng trạng thái semantics phù hợp và chặn kích hoạt thật, không chỉ giảm opacity.

**Book card:** title plate trắng có viền đen trên bìa màu; số lớn góc dưới chỉ cho level/trình tự. Tỉ lệ 3:4 là gợi ý, không phải yêu cầu. Toàn thẻ là link nếu dẫn trang; tránh nested button/link. Có mô tả dài thì dùng content card thay vì thu nhỏ chữ.

**Thẻ catalog ngang kiểu HSK:** mảng đỏ đất lớn với tên cấp độ/chủ đề màu vàng; dải vàng phía dưới có số lượng/metadata màu mực. Lưới desktop 3 cột như quan sát HSK; mobile đề xuất 1–2 cột tùy độ dài và cỡ chữ. Tỉ lệ khoảng 5:3 là điểm khởi đầu đề xuất, không phải số đo nguồn. Texture chỉ trang trí, không làm biến mất nét chữ; metadata dạng text thật và accessible name có cả tiêu đề + thông tin hữu ích.

**Content card:** tiêu đề → metadata hữu ích → mô tả ngắn → hành động. Ảnh nếu có cần đúng crop; đặt trong khung viền, không bắt buộc cùng tỉ lệ với bìa sách. Chiều cao nội dung linh hoạt; canh CTA trong cùng hàng bằng layout, không chèn dòng giả.

**Navigation:** icon đi kèm nhãn; active có màu/viền và `aria-current`, không chỉ dựa vào màu. Nhóm theo tác vụ, mục ít dùng giảm ưu tiên. Mobile drawer đóng bằng Escape, trap focus khi modal, trả focus về trigger, phần ẩn không nằm trong tab order. Hỗ trợ thay đổi viewport khi drawer đang mở.

**Form:** label nhìn thấy, placeholder chỉ là ví dụ; input >=16 px để tránh zoom bất ngờ trên mobile. Lỗi cạnh trường, giữ giá trị nhập, hướng dẫn sửa, không dùng alert chung làm phản hồi duy nhất. Dùng input type/autocomplete phù hợp; không tự tạo hàng loạt trường trước khi cần.

**Search/filter:** giữ trạng thái lọc khi quay lại nếu phù hợp; có số kết quả, xóa lọc và empty state hữu ích. Với nội dung tiếng Việt có thể hỗ trợ tìm không dấu. Không dùng kết quả giả hoặc nút không hoạt động mà không nói rõ là prototype.

**Notice:** tối đa một thông báo nổi bật trong vùng đầu trang; pastel + label nhỏ, hành động có liên quan. Không bê mã giảm giá, thời hạn hoặc lời chứng thực từ nguồn sang dự án mới.

## 5. UX trang gói dịch vụ — học từ HSK và cải tiến

Mục tiêu: người dùng hiểu gói nào phù hợp, tổng tiền phải trả, thời gian/quyền sử dụng và bước tiếp theo. Chỉ áp dụng phần này khi sản phẩm có gói dịch vụ.

Với **một gói**, ưu tiên cấu trúc HSK đã xem: desktop hai cột (card mua chính + giải thích/FAQ); mobile đề xuất một cột, tóm tắt khác biệt miễn phí/trả phí trước quyết định, FAQ chi tiết phía dưới. Không tự tạo nhiều gói để lấp bố cục. Với nhiều gói thật, mới dùng lưới so sánh dưới đây. Phần mobile là đề xuất chưa kiểm thử trên nguồn.

### Thứ tự nội dung

1. Tiêu đề nêu giá trị + một câu đối tượng phù hợp.
2. Chọn chu kỳ nếu thực sự có nhiều chu kỳ; không thêm toggle chỉ để trang có vẻ đầy đủ.
3. Các gói có cấu trúc so sánh nhất quán.
4. Bảng quyền lợi chi tiết nếu cần; FAQ và điều kiện thanh toán/hủy/gia hạn có thể tìm được gần quyết định.
5. Hỗ trợ liên quan và CTA kết thúc khi trang dài, không lặp CTA cạnh tranh liên tục.

### Một pricing card phải trả lời

- Tên gói và “phù hợp với ai”.
- Giá, đơn vị tiền, chu kỳ, tổng tiền thu ngay; nếu hiển thị quy đổi theo tháng thì giải thích rõ bên cạnh tổng tiền.
- Quyền lợi khác biệt, giới hạn số lượng/tính năng/thời hạn; không dùng “không giới hạn” khi không có dữ liệu xác nhận.
- Điều kiện gia hạn tự động, thuế/phí nếu liên quan và đã biết. Nếu chưa có giá/chính sách thì đánh dấu dữ liệu cần bổ sung, không bịa.
- CTA cụ thể như “Chọn gói Cơ bản”; trạng thái hiện tại của hội viên nếu có dữ liệu thật.

Giữ cùng thứ tự thuộc tính giữa các gói. Các gói so sánh dùng cùng font, format giá và vị trí đơn vị. Badge “Phù hợp nhất”/“Tiết kiệm” chỉ khi có cơ sở; tối đa một gói được nhấn mạnh. Không tự chọn gói đắt nhất, thêm phí ẩn hoặc dùng đếm ngược/khuyến mãi giả.

“Trọn đời” phải định nghĩa phạm vi, tính năng và giới hạn theo chính sách thật; không tự hứa quyền truy cập vĩnh viễn. Công thức tiết kiệm phải dùng cùng kỳ và giá gốc có thật. Toggle chu kỳ cập nhật giá, tổng tiền, quyền lợi và nhãn CTA một cách nhất quán; không đổi gói âm thầm. Công bố thay đổi động với aria-live phù hợp, tránh đọc lại toàn trang.

### Tương tác và mobile

Không giấu giá/giới hạn thiết yếu trong tooltip chỉ có hover. Quyền lợi dài có thể thu gọn nhưng khác biệt quyết định giữa gói phải hiện sẵn. Mobile xếp các gói theo thứ tự rõ, không dùng carousel buộc người dùng nhớ giá qua nhiều swipe. Bảng so sánh có heading/row header, nhãn “Có/Không” truy cập được; đừng chỉ dùng tick/cross màu.

Nếu CTA cần đăng nhập, giải thích trước và giữ gói/chu kỳ đã chọn sau xác thực. Chỉ tiếp tục checkout khi người dùng chủ động yêu cầu; bản prototype không tạo thanh toán thật. Trước xác nhận mua, hiển thị tóm tắt lựa chọn và tổng tiền. Có trạng thái đang xử lý, thất bại và cách thử lại; không ghi nhận thành công nếu backend chưa xác nhận.

Sticky CTA mobile chỉ khi có ích; chứa lựa chọn/tổng tiền, không che FAQ/nút khác, có phần bù chiều cao và safe-area. FAQ ưu tiên câu hỏi ảnh hưởng quyết định: khác nhau giữa gói, thời hạn, gia hạn, hủy/hoàn tiền, hỗ trợ. Trả lời bằng chính sách thật, không viết bảo đảm theo suy đoán.

## 6. CSS nền tảng tự chứa

Chuyển các token/class này sang hệ styling của dự án khi cần. Không yêu cầu framework cụ thể. Chưa có font Itim thì dùng fallback và ghi giới hạn; sau khi có font hãy khai báo `@font-face` 400 đúng đường dẫn công khai trong dự án. Không sao chép đường dẫn máy của người viết hoặc giả định gói này chứa font. Không thêm dependency chỉ để tái tạo texture đơn giản.

```css
.nhai-ui {
  --nh-motion-duration: 150ms;
  --nh-motion-ease: cubic-bezier(.4,0,.2,1);
  --nh-focus: #243d99;
  --nh-press: 2px;
  --nh-primary: oklch(67.47% .1726 259.49);
  --nh-ink: oklch(14.5% 0 0);
  --nh-border: oklch(13.979% .00002 271.152);
  --nh-surface: oklch(98% 0 0);
  --nh-paper: color-mix(in oklch, var(--nh-primary) 15%, white);
  --nh-shadow: 2px 2px 0 var(--nh-border);
  margin: 0; color: var(--nh-ink); background: oklch(97% 0 0);
  font: 400 16px/1.6 Itim, system-ui, sans-serif;
}
.nhai-ui *, .nhai-ui *::before, .nhai-ui *::after { box-sizing: border-box; }
.nhai-ui h1, .nhai-ui h2, .nhai-ui h3, .nhai-ui p { margin: 0; }
.nhai-ui h1, .nhai-ui h2, .nhai-ui h3 { line-height: 1.25; }
.nhai-ui h1 { font-size: clamp(30px,3vw,40px); }
.nhai-ui h2 { font-size: clamp(24px,2.3vw,30px); }
.nhai-ui h3 { font-size: 20px; }
.nhai-ui .stack { display: grid; gap: 16px; }
.nhai-ui .muted { font-size: 14px; color: #57534e; }
.nhai-ui .panel a:not(.action) { color: inherit; text-underline-offset: 3px; }
.nhai-ui .paper {
  background-color: var(--nh-paper);
  background-image: linear-gradient(90deg,#8080804d 1px,transparent 1px),
                    linear-gradient(#8080808f 1px,transparent 1px);
  background-size: 20px 20px;
}
.nhai-ui .content { width: 100%; max-width: 1215px; margin-inline: auto; padding: 24px 16px; }
.nhai-ui .panel {
  min-width: 0; padding: 20px; background: var(--nh-surface);
  border: 2px solid var(--nh-border); border-radius: 5px;
  box-shadow: var(--nh-shadow); overflow-wrap: anywhere;
}
.nhai-ui .action {
  font: inherit; color: var(--nh-ink); text-decoration: none;
  display: inline-flex; align-items: center; justify-content: center;
  gap: 8px; min-height: 44px; padding: 8px 16px; cursor: pointer;
  border: 2px solid var(--nh-border); border-radius: 5px;
  background: var(--nh-primary); box-shadow: var(--nh-shadow);
}
.nhai-ui .action.secondary { background: var(--nh-surface); }
.nhai-ui .action.ghost, .nhai-ui .action.text {
  background: transparent; border-color: transparent; box-shadow: none;
}
.nhai-ui .action.text { text-decoration: underline; text-underline-offset: 3px; }
.nhai-ui .card-link { display: block; color: inherit; text-decoration: none; }
.nhai-ui [data-nh-motion="press"] {
  transition: transform var(--nh-motion-duration) var(--nh-motion-ease),
              box-shadow var(--nh-motion-duration) var(--nh-motion-ease);
}
.nhai-ui :is(.action,.card-link):is(:disabled,[aria-disabled="true"],[aria-busy="true"]) {
  opacity: .6; cursor: not-allowed;
}
.nhai-ui :focus-visible { outline: 3px solid var(--nh-focus); outline-offset: 4px; }
.nhai-ui .field { display: grid; gap: 6px; }
/* Dùng class này cho input văn bản/số, select hoặc textarea; không dùng cho checkbox/radio. */
.nhai-ui .text-control {
  font: inherit; font-size: 16px; color: inherit; width: 100%; min-width: 0;
  min-height: 44px; padding: 10px 12px; border: 2px solid var(--nh-border);
  border-radius: 5px; background: white;
}
.nhai-ui input:is([type="checkbox"],[type="radio"]) { accent-color: var(--nh-ink); }
.nhai-ui .card-grid { display: grid; gap: 16px; grid-template-columns: minmax(0,1fr); }
.nhai-ui .price { font-variant-numeric: tabular-nums; font-size: 32px; line-height: 1.25; }
.nhai-ui section[id] { scroll-margin-top: 80px; }
.nhai-ui [hidden] { display: none !important; }
@media (min-width: 640px) {
  .nhai-ui .card-grid { grid-template-columns: repeat(2,minmax(0,1fr)); }
}
@media (min-width: 1024px) {
  .nhai-ui .content { padding: 32px; }
  .nhai-ui .card-grid { grid-template-columns: repeat(3,minmax(0,1fr)); }
}
@media (hover: hover) {
  .nhai-ui [data-nh-motion="press"]:not(:disabled):not([aria-disabled="true"]):not([aria-busy="true"]):hover {
    transform: translate(var(--nh-press),var(--nh-press)); box-shadow: none;
  }
  .nhai-ui .action:is(.ghost,.text):not(:disabled):not([aria-disabled="true"]):not([aria-busy="true"]):hover {
    background: color-mix(in srgb,var(--nh-primary) 15%,var(--nh-surface));
  }
}
.nhai-ui [data-nh-motion="press"]:not(:disabled):not([aria-disabled="true"]):not([aria-busy="true"]):active {
  transform: translate(var(--nh-press),var(--nh-press)); box-shadow: none;
}
.nhai-ui .action:is(.ghost,.text):not(:disabled):not([aria-disabled="true"]):not([aria-busy="true"]):active {
  background: color-mix(in srgb,var(--nh-primary) 25%,var(--nh-surface));
}
@media (prefers-reduced-motion: reduce) {
  .nhai-ui [data-nh-motion="press"] { transition: none; }
  .nhai-ui [data-nh-motion="press"]:not(:disabled):not([aria-disabled="true"]):not([aria-busy="true"]):is(:hover,:active) { transform: none; }
}
```

CSS chỉ là nền tảng, không phải layout app hay checkout hoàn chỉnh. Dùng `.field` cho nhóm label/trường nhập và `.text-control` cho input văn bản/số, select, textarea; checkbox/radio giữ kích thước riêng và có label đủ vùng chạm. Scope các selector mới tương tự; dùng component semantics phù hợp (`button`, `a`, label, heading, dialog), không dùng div click thay cho control.

Để dùng biến thể ấm, đặt `data-theme="warm"` trên cùng phần tử `.nhai-ui` và thêm override này sau CSS trên. Không cần thư viện texture hoặc ảnh từ nguồn:

```css
.nhai-ui[data-theme="warm"] {
  --nh-primary: #e3bd68;
  --nh-ink: #302d27;
  --nh-border: #39342d;
  --nh-surface: #fffaf2;
  --nh-paper: #faf5ec;
  --nh-press: 4px;
  --nh-shadow: 4px 4px 0 #b8ac96;
  --nh-red: #b84d43;
  background: #f2e8d9;
}
.nhai-ui[data-theme="warm"] .paper {
  background-image: linear-gradient(90deg,rgba(97,81,54,.13) 1px,transparent 1px),
                    linear-gradient(rgba(97,81,54,.13) 1px,transparent 1px);
}
```

### Chế độ Tối (Dark Mode) cho Sổ tay Nhai (Đã đo kiểm WCAG 2.2)

Khi ứng dụng hỗ trợ cả chế độ Sáng và Tối (Light / Dark mode):
- **Nguyên tắc:** Giữ phong cách sổ tay: ô lưới nhẹ, viền rõ, bóng cứng và offset chuyển động. Dark mode dùng nền giấy tối ấm (`--nh-paper`), thẻ card sẫm màu (`--nh-surface`), chữ mực sáng ấm (`--nh-ink`), và bóng cứng đen sẫm (`--nh-shadow-color`).
- **Tương phản chữ (WCAG AA/AAA):** Không dùng màu vàng thương hiệu fill (`--nh-primary`: `#e3bd68`) cho chữ nhỏ hoặc link trên nền sáng (chỉ đạt 1.72:1). Bổ sung role `--nh-primary-text`:
  - Light mode: `#8c5b08` (đạt 5.42:1 trên giấy kem `#faf5ec`).
  - Dark mode: `#f3cf7e` (đạt 10.54:1 trên card sẫm `#26221d`).
- **Focus ring:** Trên nền tối dùng `--nh-focus: #e3bd68` (vàng amber, đạt 8.86:1 trên card), thay vì dùng viền xanh tối chìm trên nền đen.
- **Ô lưới (Paper grid):** Dùng biến `--nh-grid-color`. Light: `rgba(97, 81, 54, 0.13)`; Dark: `rgba(230, 215, 190, 0.08)` (đường kẻ sáng mảnh mờ trên nền giấy tối).
- **Hợp đồng token Warm Dark:**
```css
.dark,
[data-theme="warm"].dark {
  --nh-paper: #1b1814;
  --nh-surface: #26221d;
  --nh-secondary: #2f2a24;
  --nh-ink: #fbf8f2;
  --nh-border: #524a3e;
  --nh-shadow-color: #0e0d0b;
  --nh-shadow: 4px 4px 0 #0e0d0b;
  --nh-focus: #e3bd68;
  --nh-primary: #e3bd68;
  --nh-primary-text: #f3cf7e;
  --nh-grid-color: rgba(230, 215, 190, 0.08);
}
```

Warm chỉ đổi token, không viết thêm bộ hiệu ứng theo theme. Khi bổ sung danger/selected/error, định nghĩa semantic token và variant chung tương ứng; kiểm tra tương phản. CSS minh họa có primary/secondary/ghost/text và card tương tác; không coi mọi variant trong hợp đồng là đã được triển khai sẵn. Các lớp đều thuộc cùng recipe, không sao chép thành CSS riêng cho mỗi feature.

### Ví dụ ghép cấu trúc tối thiểu

Ví dụ này chỉ minh họa cách ghép class, không phải nội dung mặc định phải đưa vào mọi dự án. Chèn CSS ở trên vào `<style>` trong `<head>` khi cần một HTML độc lập; trong app, chuyển thành component/style theo quy ước dự án. Form/checkout phải có xử lý riêng nếu nhiệm vụ yêu cầu; snippet này không giả lập các luồng đó.

```html
<!doctype html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Thư viện dự án</title>
  <!-- Đặt CSS nền tảng và override Warm của skill tại đây -->
</head>
<body class="nhai-ui" data-theme="warm">
  <div class="paper">
    <main class="content stack">
      <header class="panel stack">
        <h1>Thư viện dự án</h1>
        <p>Những dự án nhỏ, những ý tưởng đáng thử.</p>
        <a class="action" data-nh-motion="press" href="#du-an">Khám phá dự án</a>
      </header>
      <section class="stack" id="du-an" aria-labelledby="project-title">
        <h2 id="project-title">Dự án nổi bật</h2>
        <div class="card-grid">
          <article class="panel stack">
            <h3>Sổ tay màu sắc</h3>
            <p>Một nghiên cứu nhỏ về màu sắc trong cuộc sống thường ngày.</p>
            <span class="muted">Nội dung minh họa — thay bằng dự án thật.</span>
          </article>
        </div>
      </section>
    </main>
  </div>
</body>
</html>
```

Không thêm nút “Xem chi tiết” khi chưa có route/nội dung đích. Với component trong React/Vue/Svelte, dùng class hoặc className đúng framework; không đưa cả `html/head/body` vào component con. CSS snippets không bao gồm app shell, texture sờn hay logic tương tác: triển khai phần thật sự cần theo mục 4, không coi sự tồn tại của class là bằng chứng chức năng đã hoàn thành.

## 7. Quy trình thực thi

1. **Hiểu nhiệm vụ:** xác định đối tượng, tác vụ chính, loại trang và dữ liệu thật; chỉ hỏi phần thiếu thực sự chặn triển khai. Không tự tạo bảng giá/chính sách giả để làm trang trông hoàn chỉnh.
2. **Chọn cấu trúc:** chọn app shell hoặc nav đơn giản; map nội dung vào thành phần theo độ dài, không theo hình thức nguồn. Ghi ngắn các trang/file cần sửa với dự án nhiều trang.
3. **Hiệu chỉnh:** làm font + nền + panel + nút + một thẻ dùng nội dung thật trước. Kiểm tra nhãn dài và dấu tiếng Việt; sửa font trước khi chữa wrap bằng cách đổi width.
4. **Triển khai:** chuẩn hóa primitive/variant/state theo mục 4.1–4.2 trước, rồi thay các điểm dùng trong phạm vi bằng component chung. Không chỉ thêm token rồi để từng trang giữ style riêng. Dùng state thật, CSS cho motion đơn giản. Giữ mapping tài nguyên dùng thật, đúng loại file/weight/crop và giấy phép phù hợp; không lấy logo/mascot nguồn làm thương hiệu mới.
5. **Kiểm tra render:** mobile khoảng 390 px, desktop khoảng 1440 px; thêm 320 px nếu nhãn dài, rộng hơn max-width nếu layout cần. Với app, thử luồng chính và các trang dùng chung bị ảnh hưởng.
6. **Sửa có ưu tiên:** khung/chiều rộng → chữ/wrap → ảnh/crop → khoảng cách → motion. Ảnh đối chiếu ghi route, viewport, scroll và state; so sánh cùng điều kiện. Khi sản phẩm khác nội dung, đánh giá ngôn ngữ hình ảnh thay vì độ giống screenshot toàn trang.
7. **Bàn giao:** chạy check/build mà stack có; báo riêng kết quả render, hành vi, giới hạn chưa xác minh và cách chạy. Không chuyển stack chỉ để có build, không hứa pixel-perfect hoặc UX tối ưu đã được kiểm chứng.

Khi bàn giao, trả lời ngắn gọn theo các ý: theme và loại trang đã chọn; file/trang đã tạo hoặc sửa; cách mở/chạy; kiểm tra thực sự đã làm; phần còn thiếu dữ liệu hoặc chưa xác minh. Nếu AI không có quyền sửa/chạy file, cung cấp code hoặc hướng dẫn cụ thể và nói rõ giới hạn đó. Không yêu cầu người dùng cài repo cloner để hoàn thành.

## 8. Tiêu chí nghiệm thu

### Kiểm tra tính nhất quán component

Với dự án có UI lặp lại, dùng gallery/dev view/test harness hiện có để xem primitive cùng nhau; nếu chưa có, tạo fixture nhỏ khi giúp kiểm tra, không bắt cài Storybook. Bao gồm các variant Button/ActionLink, IconButton, Card static/interactive, disabled/loading và focus; dùng nhãn đủ dài. Không cần đưa gallery lên sản phẩm công khai.

- [ ] Các điểm dùng được ánh xạ vào component/variant chung; StatCard, ChartPanel và Card có nút con được phân loại đúng.
- [ ] Cùng variant trên hai trang có cùng radius, border, shadow, hover/active/focus, disabled và duration. Size thay kích thước, không tự thay motion.
- [ ] Button và link trông như button có cùng recipe; ghost/text khác raised có chủ đích và nhất quán.
- [ ] Card tĩnh không tạo affordance click giả; card tương tác dùng cùng press recipe; không có hover dịch cả wrapper khi chỉ nút con click được.
- [ ] Rà source các `hover:shadow-*`, `active:scale-*`, `translate-*`, transition, shadow/radius hardcode và override utility toàn cục trong phạm vi thay đổi. Xử lý tại primitive; không xóa motion nghiệp vụ hợp lệ của progress/chart chỉ vì cùng có transition.
- [ ] Thử rest → hover → active → focus-visible → disabled/loading và reduced-motion. Dùng thao tác thật khi có browser; đo computed style nếu cần phân biệt lỗi cascade. Không dùng chỉ so tên class làm bằng chứng hiệu ứng đúng.
- [ ] Component cũ trong phạm vi đã chuyển sang recipe mới; báo rõ phần chưa chuyển, không khẳng định toàn app nhất quán nếu chỉ sửa một route.

### Kiểm tra sản phẩm

- [ ] Phong cách nhận ra qua font, ô lưới, viền, bóng và màu; không cần sao chép nội dung hay bố cục nguồn.
- [ ] Có một hành động ưu tiên theo ngữ cảnh; không để banner/mascot/sticky che hoặc cạnh tranh với tác vụ chính.
- [ ] Đọc được dấu tiếng Việt; text thường ít nhất 16 px; zoom/reflow không làm mất nội dung thiết yếu.
- [ ] Tương phản mục tiêu tối thiểu 4.5:1 cho chữ thường, 3:1 cho chữ lớn và dấu hiệu điều khiển; đo các tổ hợp màu thực dùng. Không dựa riêng vào màu để truyền đạt trạng thái.
- [ ] Điều khiển chính có vùng chạm khoảng 44 × 44 px, focus rõ, dùng được bằng bàn phím; modal xử lý Escape/focus và không bỏ lại overlay.
- [ ] Không tràn ngang toàn trang, không cắt shadow/label; vị trí anchor không nằm dưới header.
- [ ] Loading/empty/error/success được xử lý ở phần có liên quan; nội dung nhập và lựa chọn không bị mất khi lỗi.
- [ ] Nếu có pricing: giá/chu kỳ/tổng tiền/quyền lợi/giới hạn đồng nhất; không dữ liệu giả, khuyến mãi giả hoặc phí ẩn; test đổi chu kỳ và lựa chọn gói.
- [ ] Asset/font tải được; không nhầm MIME/weight; reduced-motion không làm mất khả năng dùng.
- [ ] Check/build hiện có và kiểm tra giao diện được báo riêng; nêu rõ nếu không có công cụ render.

## 9. Xuất xứ

Biên soạn và bổ sung ngày 29/10/2026. Nguồn hình thức đã quan sát: https://nhaitopik.com/, https://nhaihsk.com/ và https://nhaihsk.com/membership. HSK được xem qua UI trình duyệt Helium, không đọc computed styles; kết luận về màu/layout là quan sát trực quan, palette Warm là đề xuất triển khai. Đã thử đổi thời hạn 1 → 12 tháng; chưa thử đăng nhập, thanh toán, mobile hoặc mọi trạng thái. Quy trình quan sát/so sánh được đúc kết từ lần đọc repo cloner trước đó; file này không cần repo và không gọi công cụ/skill của repo. Font có thể lấy từ kho Google Fonts chính thức tại https://github.com/google/fonts/tree/main/ofl/itim; file này không kèm hoặc nhúng font.

Đây là đặc tả thiết kế tái sử dụng tổng hợp từ cả hai nguồn với giới hạn bằng chứng rõ ràng, không phải tuyên bố đã kiểm thử toàn bộ UX hoặc xác định được theme mặc định của mọi người truy cập.
