# English Study — hệ thống thiết kế web desktop

Chốt thiết kế: 29/10/2026. Chủ trì: Codex. Áp dụng cho đợt UI-01 đến UI-04 trong `docs/tasks/UI_REFRESH_HANDOFF.md`.

## Hướng thiết kế đã chọn

**Sổ học hiện đại cho người lớn:** bố cục rõ, màu xanh trầm, nền sáng dịu, chữ dễ đọc, mỗi màn có một hành động học chính. Cảm giác như một bàn học được sắp xếp gọn. Sidebar trái; nội dung và câu hỏi được ưu tiên hơn thống kê, thương hiệu và hiệu ứng.

Giữ React 19, TypeScript, Tailwind **3.4**, TanStack Query, primitives hiện có và Lucide. Không đổi stack hoặc nâng Tailwind để chạy ví dụ của skill. Không cần GSAP, chart library hay một bộ UI mới trong UI-01/UI-02.

### Cách đã sử dụng UI UX Pro Max

- Đã đọc SKILL.md, quick-reference mục 1–3 và pro-rules. Tra cứu design-system, minimalism-and-swiss-style, focus và React.
- Hai lượt sinh design-system tự động trả về Educational App theo hướng trẻ em và AI/Chatbot Platform; không phù hợp app tự học người lớn. **Không lưu hoặc áp dụng hai output này như thiết kế được chốt.**
- Query riêng `Minimalism Swiss Style` khớp style `minimalism-and-swiss-style`: thứ bậc chữ, grid, ít hiệu ứng, ưu tiên khả năng đọc. Query React focus management phù hợp dialog hiện tại.
- Query progressive disclosure không trả kết quả đúng ý; nguyên tắc hiển thị từng phần ở đây là quyết định thiết kế của Codex dựa trên luồng học, không phải kết quả tìm kiếm đã được xác minh của skill.
- Query html-tailwind trả cả hướng dẫn phiên bản 4.3. Loại bỏ cú pháp/plugin chỉ dành cho v4; chỉ dùng nguyên tắc bố cục tương thích v3.4.
- Quy tắc dự án ưu tiên desktop; các checklist native/mobile trong skill không mở rộng phạm vi công việc.

## 1. Màu và trạng thái

Map các giá trị sau vào semantic tokens hiện có trong `frontend/src/styles/tokens.css`. HEX là đặc tả thiết kế; chuyển sang HSL cho cấu hình hiện tại. Không rải HEX trực tiếp trong TSX.

| Token | Sáng | Tối | Vai trò |
|---|---|---|---|
| background | #F7F8FA | #101816 | Nền ứng dụng |
| foreground | #182321 | #EEF4F0 | Chữ chính |
| card / popover | #FFFFFF | #18231F | Mặt phẳng nội dung |
| card-foreground / popover-foreground | #182321 | #EEF4F0 | Chữ trên mặt phẳng |
| primary | #246B55 | #8CC9A8 | Nút học chính, focus |
| primary-foreground | #FFFFFF | #102019 | Chữ nút chính |
| primary hover | #1C5544 | #A1D6BA | Hover, không đổi kích thước |
| secondary / muted | #EAF0EC | #22332B | Nền phụ |
| secondary-foreground | #244A3A | #C4E2D0 | Chữ nhấn nhẹ |
| muted-foreground | #56635F | #A4B5AB | Chữ phụ có ý nghĩa |
| border | #D9E1DD | #34463D | Phân vùng trang trí |
| input | #7A8D82 | #71897C | Ranh giới điều khiển cần nhận biết |
| ring | #246B55 | #8CC9A8 | Focus 2px, offset 2px |
| destructive | #B42318 | #FDA29B | Lỗi / thao tác nguy hiểm |
| warning foreground | #805500 | #F4D38A | Trễ lịch / cần chú ý |

Success dùng icon check + chữ; warning dùng chữ và icon, không chỉ màu. Chữ trên nút destructive phải có token riêng phù hợp từng theme. Trạng thái disabled không được giống pending; pending có nhãn “Đang lưu…” và chặn gửi trùng.

Đã tính contrast sRGB của một số cặp token đề xuất: chữ chính sáng/trắng 16.14:1; chữ phụ sáng/nền trang 5.91:1; chữ trắng/primary sáng 6.35:1; chữ chính tối/card tối 14.50:1; chữ phụ tối/card tối 7.53:1; chữ primary tối/nền nút 8.88:1. Đây là kiểm tra màu đặc, **chưa phải chứng nhận contrast của toàn bộ UI sau khi phối opacity, overlay, hover**. Đo lại kết quả thật lúc triển khai: chữ thông thường ≥4.5:1; ranh giới control/focus quan trọng ≥3:1.

## 2. Typography và khoảng cách

- Một font UI: `"Segoe UI", ui-sans-serif, system-ui, sans-serif`. Giảm font trang trí; không cần tải font mới để đạt thiết kế này. IPA: `"Segoe UI", "Noto Sans", "DejaVu Sans", sans-serif`, kiểm tra ký tự thật trên Windows. Không gọi mọi đoạn IPA là monospace.
- H1: 30–32px / 40px, weight 650–700; H2: 22–24px / 32px, weight 600; H3: 18–20px / 28px.
- Thân bài: 17px / 28px; UI/form: 14–16px / 22–24px; metadata: 13–14px / 20px. Không dùng chữ 10–11px cho thông tin phải đọc để học hoặc quyết định.
- Bài đọc rộng khoảng 65–78ch, không kéo thành một dòng dài toàn màn hình. IPA ví dụ 20–24px, câu mẫu 20px. Không in nghiêng cả bài/IPA.
- Spacing: 4, 8, 12, 16, 24, 32, 48px. Card padding 24–32px; khoảng giữa section 24–32px. Control height 40–44px; icon button tối thiểu 36×36px cho desktop, focus rõ.
- Radius: input/button 10px, card 16px, pill 999px chỉ dùng trạng thái ngắn. Border 1px, shadow nhẹ ở modal; tránh shadow/gradient/blur trên mọi card.

## 3. Khung màn hình

- Sidebar mở 216–232px; thu gọn 64–72px. Có tên/tooltip khi thu gọn, `aria-current="page"`; điều hướng vẫn giống hiện tại. Không thêm menu ngoài phạm vi.
- Topbar 56px, chỉ context ngắn + theme + tài khoản. Không lặp avatar, tên người và lời chào ở nhiều vùng trên cùng màn.
- Main padding 24px ở 1280, 32px ở 1440 trở lên; max-width 1120px. Dùng `minmax(0,1fr)` và `min-w-0` để tên bài dài xuống dòng.
- Hôm nay: vùng chính + rail phụ rộng 260–280px khi đủ chỗ. Ở chiều rộng còn lại nhỏ, rail xuống dưới; nút học chính xuất hiện trước rail trong DOM và thị giác.
- Workspace: sidebar app thu gọn theo lựa chọn người dùng; rail các nhiệm vụ 180–200px + mặt phẳng bài 65–78ch. Ở 1280, nhiệm vụ có thể thành hàng chuyển bước gọn thay vì ép ba cột.
- Ghi chú mặc định đóng, mở bằng nút có nhãn. 1280/1440 dùng drawer bên phải tối đa 360px; không bóp bài thành cột hẹp. Editor vẫn giữ state khi đóng; dùng modal semantics đúng nếu drawer che phần nền.
- Focus mode ẩn chrome khỏi cả tab order, không chỉ che bằng CSS. Thoát focus bằng nút hoặc Escape. Escape đóng dialog trên cùng trước, không đồng thời thoát focus.
- Không tạo nhiều vùng scroll lồng nhau cho bài học. Sticky thanh điều hướng gọn, có khoảng đệm tránh che input/keyboard focus ở cuối trang.

## 4. Ngôn ngữ và hành động

- Một nút primary chính trong một vùng quyết định: Hôm nay là “Tiếp tục học”/“Bắt đầu học”; activity là thao tác hợp lệ theo completionPolicy.
- “Bài tiếp theo” là điều hướng, không phải hoàn thành. Trước khi buổi xong, dùng secondary; không tự tăng tiến độ khi chuyển bước.
- Dịch nhãn UI: ACTIVE → Đang học; READING → Đọc bài; PRACTICE → Thực hành; QUIZ → Kiểm tra; REFLECTION → Ghi lại điều đã học. Không đổi enum gửi API.
- Ghi chú: “Tự động lưu”, “Đang lưu…”, “Đã lưu”, “Chưa lưu được — thử lại”; không hiện 700ms, version, query cache trong luồng người học.
- Phân biệt “Đã nộp” và “Đã lưu nháp”. Lỗi giữ văn bản người học. Thông báo 409 có nút đồng bộ và giữ draft; không retry ghi tự động.
- “Anh-Anh” là định hướng. Nút mẫu đọc ghi “Nghe từ ví dụ”/“Nghe câu mẫu”; trạng thái thiếu en-GB trung thực. Không tự đặt tên giáo viên, chứng nhận, điểm năng lực hoặc dữ liệu thành tích.

## 5. Component và hành vi dùng chung

| Thành phần | Hành vi bắt buộc |
|---|---|
| Button/Input/Label/Progress/Skeleton | Tái sử dụng primitives hiện có; có focus, disabled, pending, error |
| PageHeader | H1 duy nhất, mô tả tối đa 1 dòng khi đủ; action phụ không lấn tiêu đề |
| EmptyState/ErrorState | Giữ rõ thiếu dữ liệu vs tải lỗi; retry đúng query, không biến lỗi thành danh sách rỗng |
| Dialog/Drawer | Một implementation dùng chung; title, close, focus initial/trap/restore, Escape, scroll lock và layer đúng |
| LearningStatus | Map enum sang nhãn + icon; không đổi dữ liệu |
| TodayActionCard (feature learning) | Một render theo view model và state; không copy 5 khối gần giống nhau |
| ActivityNavigator (feature learning) | Danh sách theo activityId thật; chọn bước không gọi mutation |
| ActivityRenderer | Giữ chính sách/transaction hiện có, đổi composition; không xây engine chấm điểm mới |
| NoteEditor | Một instance có draft/autosave riêng; không reset khi mở drawer/chuyển bước |

Chỉ tạo component khi được dùng hoặc giảm trùng thật. Không xây BasePage/generic wizard hoặc design framework riêng. Không đẩy component nghiệp vụ vào shared rồi import ngược feature.

## 6. Theme, chuyển động, accessibility

- Giữ preference theme đã lưu; nếu chưa có, theo hệ điều hành. Cả sáng/tối phải có thiết kế đầy đủ; không tự đổi theme mỗi route.
- Transition 120–180ms cho màu/opacity, tối đa 200ms mở drawer; không dịch layout khi hover. Reduced motion tắt confetti/bounce/di chuyển.
- Confetti nếu giữ chỉ khi có lần chuyển thực sự sang hoàn thành, không khi mở lại một bài đã xong. Không bật âm thanh tự động.
- Route mới đưa focus tới main/H1 hợp lý. Trong worksheet, đổi bước đưa focus tới tiêu đề bước, không đổi focus khi autosave.
- Error/success quan trọng có status/live region phù hợp; không đọc lại toàn trang mỗi keystroke. Dialog nằm trên focus-mode; Escape chỉ xử lý layer trên cùng.
- Desktop bắt buộc: 1280×720 và 1440×900; xem thêm zoom 125% ở một viewport. Không phát sinh sprint mobile; tránh khóa width khiến tràn khi zoom.

## 7. Nguồn chuẩn và giới hạn bản mẫu

`docs/design/ui-refresh-preview.html` minh họa bố cục và cảm giác hai màn. Dữ liệu, nội dung ví dụ và trạng thái trong file là mẫu, không được copy thành logic production. API/OpenAPI là nguồn dữ liệu chuẩn; contract và quy tắc bảo toàn ghi chú/version/quyền cao hơn bản mẫu.

Mục “Không gian học” trên sidebar bản mẫu là bộ chuyển hai màn để xem thiết kế, không phải yêu cầu thêm menu production. App thật giữ các mục Hôm nay, Lộ trình học, Lịch học, Ôn tập, Tiến độ, Tùy chọn; workspace mở từ bài cụ thể. Tên English Study trong mẫu cũng không yêu cầu đổi thương hiệu ứng dụng.

Khi có điểm chưa khớp: giữ hành vi nghiệp vụ, điều chỉnh composition theo tài liệu giao việc. Không sửa nội dung published hoặc giả lập học liệu/điểm số để giao diện giống hình. Không tạo page override âm thầm đổi hướng thiết kế đã chốt.
