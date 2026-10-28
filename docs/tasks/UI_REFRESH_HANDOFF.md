# Làm mới UI/UX — kế hoạch giao công nhân

Ngày 29/10/2026. Baseline `c887fca` và các thay đổi FE chưa commit hiện có. Mục tiêu: thay đổi rõ trải nghiệm học trên desktop, giữ luồng FE–BE–DB và dữ liệu đã dùng được.

## 1. Đọc trước khi làm

1. `AGENTS.md`, `FE_CODE_CONVENTION.md`; BE convention khi thực sự chạm backend.
2. `.agents/skills/ui-ux-pro-max/SKILL.md` và `design-system/english-study/MASTER.md`.
3. Tài liệu này; xem `docs/design/ui-refresh-preview.html` bằng trình duyệt.
4. `docs/DANH_GIA_SAN_SANG_HOC.md` và `docs/LO_TRINH_HOC_DE_XUAT.md` để biết giới hạn giáo trình. Đợt UI không đồng nghĩa viết lại giáo trình.

Codex đã chốt hướng thiết kế trong MASTER. Công nhân không cần hỏi lại màu/font/layout thông thường hay sinh thêm 3–5 phương án. Khi dùng skill, bỏ gợi ý trẻ em, landing bán hàng và cú pháp Tailwind 4 không phù hợp dự án này.

## 2. Kết quả rà soát cập nhật

Đã mở web đang chạy, đăng nhập tài khoản audit riêng, xem Hôm nay ở trạng thái REST_DAY, mở buổi 2 và thử IPA. Không ghi tiến độ hoặc sửa note trong lượt rà soát này. Quan sát ở 1280×720 ngày 29/10/2026.

| Hiện trạng | Quyết định trong thiết kế mới |
|---|---|
| REST_DAY hiện có ba card lớn nhắc lại lộ trình/ngày nghỉ/học trước | Một card quyết định gọn theo state; tiến độ chuyển rail phụ |
| Header + dropdown + card lặp tên lộ trình dài | Tên chỉ một lần trong context; switcher gọn, label tiếng Việt |
| Sidebar 256px, nhiều chữ nhỏ và badge RP lặp | Sidebar 216–232px, topbar nhẹ, thứ bậc chữ lớn hơn |
| Workspace lặp hai h1, đọc dài rồi mới đến nhiệm vụ | Một h1 và navigator nhiệm vụ; hiển thị nội dung có trọng tâm |
| Ghi chú mặc định mở làm hẹp bài | Drawer mở khi cần, editor giữ mounted/state |
| Nút Bài tiếp nổi ngang thao tác học khi chưa xong | Hành động nộp/hoàn thành nổi bật; điều hướng bài tiếp là secondary |
| Source R1 đã thêm Escape/focus cho IPA | Đã thử Escape đóng và trả focus về nút mở; giữ phần sửa này, chưa coi đã nghiệm thu đủ Tab/layer |
| Source R1 đã chặn loa với text không giống tiếng Anh, đổi nhãn TTS và thêm “Đã nộp” | Tái sử dụng, kiểm tra lại trong luồng mới; không quay về claim “giọng RP đã chứng nhận” |
| Source R1 mới có calculateEstimatedPlan(..., **130**) trong EnrollmentDialog | **Cần sửa trước khi nghiệm thu:** không hardcode 130 cho mọi curriculum; khóa pilot/khóa khác sẽ báo sai |

Chưa có báo cáo nghiệm thu R1 trong repo ở thời điểm rà soát. Các thay đổi chưa commit là công việc đang có; không checkout/reset/stash hoặc ghi đè chúng. Đọc diff và sửa tiếp phần liên quan. Nếu file thay đổi trong lúc làm, đọc lại trước khi ghi. Không giao hai công nhân đồng thời sửa cùng các file learning/layout.

## 3. Phạm vi và thứ tự giao

| Đợt | Kết quả nhìn thấy | Phụ thuộc | Nghiệm thu xong mới chuyển |
|---|---|---|---|
| **UI-01** | Hệ thống màu/chữ/component, AppShell và **Hôm nay mới**; sửa dự báo hardcode 130 | R1 đang có được giữ và đối chiếu | Hai theme, các state Today, một luồng mở bài |
| **UI-02** | **Không gian học mới**: navigator, nội dung trọng tâm, ghi chú drawer, action rõ | UI-01 | Draft/note/quiz không mất; version và blocker đúng |
| **UI-03** | Lộ trình, chi tiết, đăng ký và lịch đồng bộ thiết kế | UI-02 | Đăng ký/pilot/A và preview/hủy/commit lịch không hồi quy |
| **UI-04** | Ôn tập, Tiến độ, Tùy chọn, Login; kiểm tra hoàn thiện | UI-03 | Một vòng học end-to-end, full FE suite và build/lint |

Mỗi đợt một báo cáo ngắn `docs/UI-0X_IMPLEMENTATION.md`, nêu file thay đổi, check đã/chưa chạy, lỗi còn lại và ảnh hai viewport khi có UI. Không tạo thêm báo cáo review/vòng lặp nếu không cần. Công nhân không commit/push; Codex nghiệm thu và commit từng phần đạt. Cùng một finding tối đa hai lần giao sửa, sau đó Codex xử lý.

## 4. UI-01 — nền tảng và Hôm nay

### File và component dự kiến

- `frontend/src/styles/tokens.css`, `globals.css`, `tailwind.config.js`, `index.html`: map token, typography, focus, reduced motion. Tái sử dụng key semantic hiện có; thay font display, không thêm CSS framework.
- `app/layouts/AppShell.tsx`, `AppSidebar.tsx`: chiều rộng, topbar, main, active state, điều hướng bàn phím; giữ auth/logout/error/loading sẵn có.
- `components/ui/button*.tsx`, `input.tsx`, `skeleton.tsx`, `progress.tsx`: chỉ điều chỉnh khi cần đồng bộ; không thay public props làm gãy consumer.
- `features/learning/pages/TodayPage.tsx`; tách `TodayActionCard`, `TodayProgressSummary` và helper view model trong feature nếu giảm copy thật.
- `features/enrollment/components/EnrollmentDialog.tsx`: sửa dự báo 130 ở dưới; không gộp redesign toàn form trong UI-01.

### Bố cục Hôm nay

```text
sidebar 224 | Hôm nay                         [đổi lộ trình nếu cần]
            | Thứ Năm, 29 tháng 10
            |
            | ┌──── việc học chính ──────────┐ ┌── Lộ trình của bạn ─┐
            | │ trạng thái ngắn             │ │ tên một lần        │
            | │ tên bài                     │ │ tiến độ chuẩn BE   │
            | │ ~25 phút · 2/4 hoạt động     │ │ [Xem lộ trình]     │
            | │ [Tiếp tục học →]            │ └────────────────────┘
            | └─────────────────────────────┘
            | tài nguyên phụ gọn / bài trễ còn lại khi thực sự có
```

Không thêm “mục tiêu tuần”, streak, giờ thực học, mục ôn đến hạn hoặc tổng bài đã xong nếu endpoint hiện tại không trả dữ liệu đó. UI đẹp không đòi thêm query hàng loạt.

### Map state vào hành động

| Today.state | Vùng chính và CTA |
|---|---|
| NO_ENROLLMENT | “Bắt đầu lộ trình của bạn” + Khám phá lộ trình; không khuyên A là đã thẩm định |
| IN_PROGRESS | Tiếp tục học; metadata completedActivities/totalActivities nếu có |
| DUE_TODAY | Bắt đầu học; estimatedMinutes là thời lượng bài, không phải thời gian còn lại |
| OVERDUE | Tiếp tục bài còn dang dở/học bù với ngôn từ nhẹ; danh sách trễ khác thu gọn; Đổi lịch là phụ |
| REST_DAY | “Hôm nay là ngày nghỉ”; nếu có UPCOMING lessonId thì Học trước là lựa chọn phụ, không giả có bài bắt buộc |
| UPCOMING | Bài sắp tới + Học trước; hiển thị ngày chỉ khi API có ngày, không đoán |
| PAUSED | Đã tạm dừng; quản lý/tiếp tục qua luồng lifecycle hiện có, không gọi mutation bằng UI mới tự chế |
| CURRICULUM_COMPLETED | Hoàn thành các nhiệm vụ bắt buộc; Xem lại / Ôn tập. Không tuyên bố thành thạo ngoại ngữ |

Null primaryAction hoặc null lessonId: không render link sai. Null progress: bỏ khối hoặc hiện chưa có dữ liệu, không tự đặt 0%. `todayDate` theo server/enrollment timezone; format date-only, không parse gây lùi ngày.

Đổi enrollment giữ query keys, cache invalidation và pending hiện tại; có lỗi thì giữ lựa chọn đã xác nhận và thông báo retry. Lỗi preferences/enrollments không được rơi thành NO_ENROLLMENT; không tạo spinner vĩnh viễn khi preferences đã lỗi.

### Sửa dự báo đăng ký

Loại bỏ `calculateEstimatedPlan(...,130)`. Lấy số **lesson required** từ outline đúng currentRevision đang đăng ký, hoặc thêm một field/read preview tối thiểu nếu cần nguồn chuẩn. Không dùng tổng activity hoặc đếm cả optional. Không dùng slug để phân nhánh pilot/A. Khi chưa tải được nguồn, hiển thị “Chưa tính được ngày dự kiến” và retry, không quay về 130.

Loại bỏ luôn phần copy cố định “26 chặng • 130 buổi” và lời giải thích đóng đinh 26 tuần trong form chung. Hiển thị số chặng/buổi đúng curriculum hoặc bỏ phần chưa có nguồn dữ liệu. Helper mới có thể đã được tách sang `features/enrollment/lib/schedule-estimator.ts` trong khi R1 tiếp tục chỉnh; đọc lại file trước khi sửa, không dựa vào số dòng cũ.

Nếu vẫn tính local estimate để hiển thị: gắn nhãn dự kiến; đúng startDate/date-only/studyDays, giữ rule một required lesson/ngày; kiểm tra DST/timezone và lịch học thực tế do BE tạo mới là chuẩn. Không dùng preview reschedule cần enrollmentId để dự báo enrollment chưa tồn tại. Không thay thuật toán xếp lịch BE trong task giao diện.

### Chấp nhận UI-01

- Thấy heading, trạng thái, tên bài và CTA chính ngay trong viewport 1280×720 ở state có bài; không cần cuộn qua banner quảng bá.
- Hai theme đúng MASTER, không tràn ngang với tên dài; sidebar thu gọn dùng được bằng keyboard.
- Đủ 8 state được kiểm bằng test view model/component; không bắt tạo 8 tài khoản/DB fixture để chụp mọi state.
- Luồng thật: tài khoản test đăng nhập → Hôm nay → vào đúng bài; không mutate dữ liệu cá nhân.
- Dự báo lịch ít nhất hai curriculum có số required lessons khác nhau, trong đó có optional lesson; không còn hằng số 130. Nếu chỉ sửa helper/frontend, kiểm tra helper + một đối chiếu API/lịch; nếu thêm query SQL thì giữ DB integration liên quan.
- Build + lint + TodayPage/AppSidebar/EnrollmentDialog tests liên quan một lần, chỉ thêm regression đáng giá.

## 5. UI-02 — không gian học

**Cập nhật theo phản hồi người dùng:** ưu tiên tinh gọn hơn bản mẫu đầu. Header chỉ tên bài, thời lượng và công cụ cần thiết; mỗi bước có đề/nội dung hiện tại và một action chính. Không thêm card động viên, giải thích cách dùng app dưới mỗi nút, badge type/required/status lặp hoặc thuật ngữ kỹ thuật. Ghi chú mặc định đóng, trạng thái lưu gọn. Vẫn giữ đầy đủ đề bài, hướng dẫn thực hành cụ thể, lỗi, cảnh báo mất dữ liệu và quyền thao tác. Đọc phần cập nhật mục 4 trong MASTER; không phục hồi những khối phụ vừa bị bỏ khỏi Hôm nay.

### Luồng và bố cục

```text
< Hôm nay     tên bài · thời lượng ước tính       [Ghi chú] [Tập trung]
Các nhiệm vụ    | Tiêu đề nhiệm vụ hiện tại
✓ Đọc bài      | nội dung/âm mẫu/câu hỏi có liên quan
• Thực hành    |
  Kiểm tra     | [action hợp lệ của nhiệm vụ]
  Tổng kết     | < nhiệm vụ trước          nhiệm vụ tiếp >
```

Hình chỉ minh họa: số/thứ tự/tên nhiệm vụ lấy từ `workspace.activities`. Không ép bài nào cũng có đủ “Nghe”, “Quiz”, “Phản tư”. Không tự tạo nhiệm vụ nghe hoặc dùng TTS thay cho loại activity chưa hỗ trợ.

- Navigator dùng activityId; thứ tự server; nhãn type tiếng Việt và title rõ. Khi mở bài lần đầu, ưu tiên required activity chưa xong; tất cả xong thì mở overview/xem lại. Giữ selection khi refetch cùng scope.
- Markdown render **một lần**, hạ/bỏ h1 đầu nếu nó trùng title bài; không rewrite source published. Bài có READING thì đặt Markdown trong khu vực đọc đầu tiên; bài không có READING vẫn có mục “Nội dung tham khảo” riêng, không tạo activity/điểm giả. Nhiều READING không lặp lại toàn Markdown ở mỗi thẻ.
- Resource và instructions của activity nằm ngay bước sở hữu; optional ghi rõ. Lesson-level Markdown không có cấu trúc chia nhỏ nên không tự cắt theo heading để gán cho các activity khác.
- Chọn bước là điều hướng UI, không gọi complete, submit hay clear draft. Khi bấm tiếp sang activity khác, không tự coi bước trước đã làm.
- Giữ ActivityRenderer/QuizActivityView và các mutation hiện có. Một action chính cho đúng completionPolicy; không đặt thêm nút hoàn thành chung cả lesson để bypass BE.
- Activity chưa hỗ trợ giữ thông báo đúng; không cho đánh dấu tay nếu policy không cho phép.

### Bảo toàn trạng thái khi đổi composition

- Không unmount activity có response draft hoặc quiz đang làm chỉ để chuyển tab. Phương án ít rủi ro: giữ instance theo `(enrollmentId,lessonId,activityId)`, ẩn bằng `hidden`/inert thích hợp. Không nhân đôi form/renderer giữa desktop và focus.
- NoteEditor một instance, vẫn giữ hook khi drawer đóng; không dùng `key={isOpen}`. Scope change có generation guard như hiện tại. Không lưu draft cá nhân vào localStorage mới.
- Nếu phải unmount vì hiệu năng, nâng state draft/quiz về workspace trước và có regression chứng minh không mất nội dung. Không refactor engine state ngoài nhu cầu.
- Unified blocker vẫn một nơi, theo dõi cả activity đang ẩn, note dirty/saving/conflict và mutation pending. Điều hướng thật ra ngoài bài vẫn cần bảo vệ; đổi bước nội bộ không được xóa dữ liệu.
- Giữ version nhận từ BE, reconciliation cache, 409 recovery, paused/abandoned guards. Không “làm UI mượt” bằng cách tự tăng version hoặc mở khóa sớm.
- Quiz action dùng dữ liệu server/endpoint cũ, không đưa đáp án vào source FE. Thao tác review bookmark không được bị mất vì thay wrapper.

### Modal/drawer/focus

- Nút note có aria-expanded/controls; mở đưa focus vào editor/heading, đóng trả về trigger; drawer không cho Tab lọt nền nếu đang modal.
- Khi note conflict hoặc đang lưu, đóng drawer chỉ ẩn view, không bỏ draft. Rời route vẫn chạy blocker; dialog xác nhận nằm trên drawer hoặc đóng view drawer để hiện blocker mà không unmount editor.
- Focus mode thực sự loại sidebar/topbar khỏi tab order bằng layout hoặc inert; một nguồn focus state, không render hai workspace.
- Escape ưu tiên dialog trên cùng → drawer → focus mode. Kiểm tra focus đang ở textarea, không nuốt các phím soạn văn bản thông thường.
- Completion banner chỉ sau dữ liệu authoritative; không phát confetti khi reload/mở lại bài. Không ép người học học tiếp: có Về Hôm nay và Xem lại.

### Chấp nhận UI-02

- Một h1, nhìn thấy bước hiện tại và nhiệm vụ; ghi chú mặc định gọn, bài đọc không bị ép cột.
- Nhập response → đổi nhiệm vụ → quay lại: nguyên text. Mở quiz, chọn đáp án → đổi nhiệm vụ → quay lại: lựa chọn còn nguyên.
- Note gõ/lưu → đóng mở drawer → chuyển focus → reload sau lưu: nội dung còn. Không cần reset DB toàn bộ.
- Dùng regression sẵn cho 409, autosave, blocker, read-only; không xóa tests để làm suite xanh. Nếu test cần cập nhật vì layout, giữ cùng invariant nghiệp vụ.
- Luồng thật tối thiểu: note + response + complete + reload + Today; thêm một quiz và review sai trên account test khi task chạm quiz mounting.
- Build + lint; nhóm LessonWorkspace/ActivityRenderer/notes/quiz tests liên quan một lần. Không mặc định full backend vì chỉ đổi presentation.

## 6. UI-03 — lộ trình và lịch

- Catalog giảm hero, bỏ claim chưa kiểm chứng còn sót; thẻ có đối tượng, số buổi thực, nhịp khuyến nghị và trạng thái kiểm chứng đúng metadata. Không đặt độ dài khóa thành cam kết ngày kết thúc cá nhân.
- Detail chỉ mở phase/chặng liên quan; còn lại thu gọn. Không lặp “Tuần 1: Tuần 1”. Search/filter đơn giản nếu API hiện có đủ dữ liệu, không xây search service.
- Đăng ký theo MASTER, dự báo đã sửa ở UI-01; form lỗi giữ input; key idempotency và source revision đúng.
- Lịch weekly vẫn dùng date-only/timezone BE, card completed/upcoming/overdue phân biệt bằng chữ + icon. “Kết thúc lộ trình” chuyển menu phụ với xác nhận hiện có.
- Preview đổi lịch: summary số bài/ngày cuối + 5–10 dòng đầu, “Xem toàn bộ” có thể expand vùng bảng; không đổ 129 dòng vào modal ngay. Commit vẫn đúng preview snapshot/expectedVersion/key; thay input làm preview cũ mất hiệu lực.
- Targeted Curricula/Schedule/Enrollment tests, một đăng ký test và preview/hủy/commit theo fixtures cho phép; không làm lại concurrency đã không đổi.

## 7. UI-04 — ôn tập, tiến độ và hoàn thiện

- Review: ưu tiên Cần ôn và Đã đánh dấu; card chỉ giữ câu hỏi/ngữ cảnh/action chính; hiển thị empty/error/pagination. Chưa có SRS không thêm “đến hạn hôm nay”.
- Progress: giữ tổng số chuẩn BE, tuần đang học mở trước, các tuần khác thu gọn; thời lượng ước tính không gọi là phút thực học. Đối soát bug badge lịch sử 1 vs 3 đã ghi trong đánh giá cũ; sửa semantic count từ nguồn, không đếm DOM.
- Preferences: form ngắn, trạng thái lưu rõ, giải thích mục tiêu phút không tự đổi lịch cũ. Login bỏ hướng dẫn bootstrap env cho learner, giữ đăng nhập/session/error/password manager/paste.
- Split route/heavy feature nếu cần xử lý bundle lớn, dùng dynamic import tương thích code hiện tại. Không đặt mục tiêu KB tùy tiện hoặc xóa tính năng để đạt size.
- Full FE suite + build/lint cuối đợt, một vòng học và một đổi lịch, kiểm tra cả 1280×720/1440×900, một lượt zoom 125%, keyboard và reduced motion. Backend verify chỉ cần nếu đã sửa auth/schema/shared infrastructure hoặc logic có nguy cơ lan rộng theo AGENTS.

## 8. Nguyên tắc tránh vòng sửa vô hạn

- MASTER là hướng đã chọn. Không chặn nghiệm thu vì thích màu/bo góc khác sau khi đã chốt.
- Chặn chuyển đợt khi mất dữ liệu, sai quyền/version/ngày, hỏng thao tác học, keyboard trap, không đọc được hoặc vi phạm đáng kể bố cục chính.
- Tinh chỉnh icon/spacing nhỏ Codex tự sửa; gợi ý thêm tính năng ghi backlog, không kéo vào scope.
- Bản mẫu là minh họa: không dùng fake progress, counters, audio hay nội dung trong đó làm dữ liệu production. Không cần pixel-perfect mọi chi tiết; cần đúng hierarchy, token, hành vi và bố cục desktop.
- Giữ `docs/DAY2_REVIEW.md`, `backend/src/main/resources/META-INF/spring/`, secrets, tài liệu riêng trong tailieu và thay đổi ngoài task không được commit kèm.

## 9. Prompt giao công nhân ngay

```text
Triển khai CHỈ UI-01 theo docs/tasks/UI_REFRESH_HANDOFF.md.
Đọc AGENTS.md, FE_CODE_CONVENTION.md, .agents/skills/ui-ux-pro-max/SKILL.md,
design-system/english-study/MASTER.md và xem docs/design/ui-refresh-preview.html.

Giữ các sửa R1 đang có; kiểm tra git diff trước khi chỉnh. Không reset/ghi đè
công việc khác. Hoàn thiện token, AppShell/sidebar và trang Hôm nay theo thiết kế;
sửa dự báo lịch EnrollmentDialog đang hardcode 130 bằng nguồn đúng revision.
Giữ API, quyền, timezone, version, cache và dữ liệu cá nhân; không làm UI-02,
không viết lại giáo trình, không thêm hạ tầng hoặc thư viện đồ họa không cần thiết.

Tự quyết chi tiết thông thường trong phạm vi MASTER. Build + lint và nhóm test
liên quan một lần; thử luồng thật Hôm nay -> bài học trên tài khoản test riêng.
Kiểm tra sáng/tối ở 1280x720 và 1440x900. Báo chính xác check đã/chưa chạy.
Viết docs/UI-01_IMPLEMENTATION.md ngắn, kèm ảnh trước/sau nếu có.
Không commit/push; hoàn thành UI-01 thì dừng để Codex nghiệm thu.
```

## 10. Kiểm tra tài liệu và bản mẫu trong lượt lập kế hoạch

- Đã xem trực tiếp app hiện tại và bản mẫu ở viewport trình duyệt khoảng 1280×720; không tuyên bố đã kiểm bản triển khai ở 1440×900 hoặc mọi state.
- Bản mẫu đã thử chuyển Hôm nay → Không gian học, sáng/tối, mở/đóng note bằng Escape và trả focus, giữ note khi mở lại, giữ câu trả lời khi chuyển nhiệm vụ rồi quay lại.
- JavaScript trong HTML qua `node --check`; một số cặp token đã tính contrast trong MASTER. Bản mẫu chưa có audio/API/backend; dữ liệu chỉ minh họa.
- Chưa chạy lại build/test ứng dụng vì lượt này chỉ thêm tài liệu và bản mẫu độc lập. Những test nghiệm thu ở các mục trên là công việc bắt buộc của đợt triển khai tương ứng, chưa được coi là đã đạt.
