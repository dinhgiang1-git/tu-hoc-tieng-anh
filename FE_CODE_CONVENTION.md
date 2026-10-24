# Frontend code convention

Ngày cập nhật: 24/10/2026. Áp dụng cho React + TypeScript TSX + Vite + Tailwind, tích hợp Spring Boot session API.

Đọc cùng [kế hoạch dự án](./KE_HOACH_DU_AN.md) và [BE convention](./BE_CODE_CONVENTION.md). Đây là **quy chuẩn, danh mục thành phần dùng chung và mẫu code để triển khai**, chưa phải các component/hàm đã được tạo trong app.

## 1. Quy tắc nền tảng

- React function component, TypeScript strict, TanStack Query cho server state; React Hook Form + Zod cho form.
- Một bộ UI primitives: shadcn/ui theo cấu hình đã chọn. Không trộn nhiều bộ Button/Dialog/Form khác nhau.
- Tailwind và semantic design tokens; Lucide cho icon. Recharts/Motion chỉ thêm khi feature cần.
- Ưu tiên vòng học: Hôm nay → tiếp tục bài → thực hành → lưu → bước tiếp theo. Sidebar trái, mobile drawer, chế độ tập trung.
- Identifier tiếng Anh; text UI tiếng Việt đặt ở nơi thống nhất theo feature hoặc label map dùng chung.
- Không hardcode curriculum A, số tuần hoặc user ID. Không tự tính headline progress từ cache rời rạc.
- Backend là nguồn chuẩn về quyền, completion, điểm quiz, lịch và version. FE validation giúp người dùng nhập đúng, không thay thế BE.
- Không tạo thư viện generic bao trùm mọi loại màn hình. Ưu tiên component nhỏ với props rõ, mở rộng khi có nhu cầu thật.

## 2. Cấu trúc thư mục

```text
frontend/src/
  app/
    router.tsx
    providers.tsx
    query-client.ts
    layouts/
      AppShell.tsx
      AppSidebar.tsx
      FocusLayout.tsx
  features/
    auth/
    curricula/
    enrollment/
    learning/
      api/                   # learning.api.ts, learning.keys.ts
      components/            # ActivityRenderer, LessonFooter...
      hooks/                 # useLesson, useUpdateProgress...
      pages/                 # LessonPage
      schemas/               # Zod forms/domain boundaries
      types/                 # UI view model, không sao chép DTO generated
      utils/                 # helper chỉ dùng trong feature
      index.ts               # public API nhỏ khi cần cross-feature
    scheduling/
    assessment/
    notes/
    review/
    progress/
    pronunciation/
  components/
    ui/                      # primitives: button, dialog, input...
    shared/                  # PageHeader, EmptyState, SubmitButton...
  hooks/                     # hook UI thực sự dùng chung
  lib/
    api/                     # api-client, ApiError, CSRF/session adapter
    format/                  # date-only, instant, percent, duration
    validation/
    utils.ts                 # chỉ cn; không biến thành kho nghiệp vụ
  generated/
    api-types.ts             # sinh từ OpenAPI, không sửa tay
  styles/
    globals.css
    tokens.css
  test/
    setup.ts
    fixtures/
```

- `app` lắp ghép providers/router/layout; không chứa business logic.
- Feature chỉ import feature khác qua public API đã chọn, tránh import xuyên vào file nội bộ và dependency vòng.
- `components/ui`, `components/shared`, `lib` không import feature. Component nghiệp vụ dù dùng ở nhiều trang vẫn thuộc feature sở hữu.
- `learning` có thể composition panel notes/pronunciation qua public component; notes/pronunciation không import ngược learning page.
- Không tạo folder rỗng để đủ cấu trúc. Không gom toàn bộ vào `utils`, `services` hoặc một file `types.ts` toàn dự án.

## 3. Định dạng và đặt tên

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Component/file TSX tự viết | PascalCase | `LessonCard.tsx` |
| Hook/file | camelCase bắt đầu use | `useLesson.ts` |
| API/query keys/schema file | kebab-case hoặc tên feature có hậu tố cố định | `learning.api.ts`, `learning.keys.ts`, `schedule.schema.ts` |
| Hàm/biến | camelCase | `formatStudyDate`, `isSaving` |
| Type/interface | PascalCase | `LessonCardProps` |
| Constant | UPPER_SNAKE_CASE cho constant module | `MAX_RECORDING_SECONDS` |
| Enum API | Giữ string từ BE | `"IN_PROGRESS"` |
| Handler nội bộ | `handle...` | `handleComplete` |
| Callback prop | `on...` | `onComplete` |

- Primitives shadcn giữ tên file do generator tạo như `button.tsx`, không rename chỉ để đồng bộ PascalCase.
- Prettier: 2 spaces, semicolon, double quotes, LF, trailing comma theo formatter. ESLint có TypeScript + React Hooks; cấu hình cùng CI.
- Alias `@/` đến `src/`. Named exports cho code tự viết; dùng `import type` khi chỉ import type.
- Không dùng `any`, non-null assertion `!` hoặc `as SomeType` để che dữ liệu API chưa được kiểm tra. Dữ liệu ngoài bắt đầu là `unknown`.
- Props immutable về cách dùng; không sửa object/array nhận từ props/query cache.
- Không gọi hooks theo điều kiện/loop hoặc sau early return. Derived state tính từ dữ liệu; không dùng effect để liên tục đồng bộ bản sao state.
- Không mặc định thêm useMemo/useCallback/React.memo cho mọi thứ. Chỉ dùng khi có lý do đo được hoặc yêu cầu identity.
- Dùng `type` cho union/alias; `interface` cho props/object có ý nghĩa mở rộng. Không prefix I/T vào tên type.

## 4. Contract bắt buộc đồng bộ với BE

Nguồn chuẩn `docs/api/openapi.yaml`; DTO sinh vào `src/generated`. View model riêng chỉ thêm khi UI thực sự cần chuyển đổi; không sao chép tất cả field của DTO sang type thủ công.

### 4.1. Dữ liệu và response

- Base URL `/api/v1`, cùng origin. Success DTO trực tiếp; không tự giả định có `response.data.data`.
- UUID string; JSON camelCase; enum UPPER_SNAKE_CASE.
- Date-only `YYYY-MM-DD`; timestamp UTC ISO-8601 có Z; timezone IANA riêng.
- `null` có nghĩa không có dữ liệu/không đánh giá được, phải hiển thị đúng; optional/absent trong PATCH khác explicit null.
- `progressPercent` là giá trị 0–100 do BE trả, không phải tỷ lệ 0–1. Score phát âm nullable, không mặc định 0.
- Pagination: `{ items, page, size, totalItems, totalPages }`; page bắt đầu 0, default size 20/max 100. Khi đổi filter đưa page về 0.
- `totalPages=0` khi không có bản ghi; UI không tạo nút “trang 1/0”.

### 4.2. Lỗi

```ts
interface FieldViolation {
  field: string;
  code: string;
  message: string;
}

interface ApiProblem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  code: string;
  traceId: string;
  fieldErrors: FieldViolation[];
}
```

Các interface trên minh họa hình dạng schema; trong code thật import type đã sinh từ OpenAPI. Parser tại HTTP boundary kiểm tra runtime, có fallback cho proxy trả HTML/body không hợp lệ. API schema types không tự là runtime validation.

| Trường hợp | Hành vi FE |
|---|---|
| 400 + fieldErrors | Map field đã biết sang RHF; lỗi field không biết hiển thị ở form summary |
| 401 | Giữ draft cần thiết, dừng request cá nhân, clear cache theo auth lifecycle, yêu cầu đăng nhập |
| 403 `CSRF_INVALID` | Thông báo cần làm mới session/token; không tự phát lại mutation chưa rõ kết quả |
| 403 khác | Hiển thị thiếu quyền, không retry |
| 404 | NotFound trong ngữ cảnh tài nguyên; không suy đoán dữ liệu thuộc user khác |
| 409 `VERSION_CONFLICT` | Refetch để người dùng xem bản mới; giữ local draft, không tự ghi đè |
| 413/415 | Hướng dẫn thu lại/đổi audio phù hợp |
| 429 | Thông báo giới hạn, tôn trọng Retry-After |
| 503/timeout/network | Có trạng thái thử lại; chưa kết luận thao tác trước thất bại hay thành công |
| Abort chủ động | Không toast lỗi như mất mạng |

Branch theo code/status, không so chuỗi `detail`. UI không hiển thị raw HTML lỗi hoặc stack trace.

### 4.3. Session, CSRF và version

- Request dùng cookie session qua `credentials: "include"`; không lưu token đăng nhập trong localStorage.
- `GET /auth/csrf` trả `{ headerName: "X-CSRF-TOKEN", token }`; giữ token trong memory. Adapter CSRF dùng fetch riêng cho bootstrap, tránh gọi apiRequest tạo vòng lặp.
- Login/logout và mutation khác có CSRF token. Sau login/logout làm mới token; không gửi token cho URL bên ngoài.
- Sau logout hoặc 401: cancel request, clear query cache cá nhân, xóa CSRF memory và drafts theo policy. Response của session cũ không được ghi lại cache session mới.
- `expectedVersion` trong mutation body phải là version đã đọc. Với progress/lịch dùng enrollment version, không dùng nhầm version của note/attempt.
- Nhận version mới từ server. Version conflict không giải quyết bằng cách lấy version mới rồi tự gửi lại dữ liệu cũ.
- `Idempotency-Key` tạo một lần cho một thao tác logic, giữ nguyên khi retry cùng payload. Payload đổi phải dùng key mới; không sinh key mới ở mỗi lần HTTP retry.
- Create/submit quiz, enrollment và commit lịch tuân contract idempotency BE. Progress PUT retry gặp 409 phải đọc trạng thái hiện hành để xác nhận, không báo hoàn thành giả.

## 5. Hàm dùng chung và HTTP boundary

### 5.1. Danh mục cần triển khai

| File / thành phần | Signature định hướng | Trách nhiệm |
|---|---|---|
| `api-client.ts` | `apiRequest<T>(path, options, decode): Promise<T>` | Fetch, headers, CSRF, abort, decode, lỗi chuẩn |
| `api-error.ts` | `class ApiError extends Error` | Giữ ApiProblem an toàn; phân biệt network/protocol/http |
| `normalize-problem.ts` | `normalizeProblem(body: unknown, status: number): ApiProblem` | Runtime validation/fallback không chứa raw body |
| `csrf-session.ts` | `getToken()`, `refresh()`, `clear()` | Bootstrap một token, xử lý request đồng thời |
| `form-errors.ts` | `applyFieldErrors(problem, allowedFields, setError)` | Chỉ map field hợp lệ, trả lỗi chưa map |
| `utils.ts` | `cn(...inputs: ClassValue[]): string` | clsx + tailwind-merge cho className |
| `date-only.ts` | `formatStudyDate(value: string): string` | Format ngày lịch không lệch timezone |
| `instant.ts` | `formatInstant(value: string, timeZone: string): string` | Format timestamp theo timezone đích |
| `percent.ts` | `formatProgressPercent(value: number | null): string` | Giá trị 0–100, null có nhãn riêng |
| `duration.ts` | `formatDurationSeconds(seconds: number): string` | Unit rõ, không nhầm seconds/ms |
| `external-url.ts` | `toSafeExternalUrl(value: string): URL | null` | Chỉ HTTP(S); caller vẫn render link an toàn |

Không tạo wrapper cho mọi hàm JS; không đưa luật tính điểm/hoàn thành vào format helper. Helpers nhận dữ liệu rõ và có thể test độc lập.

### 5.2. Mẫu `cn` và formatter

```ts
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

const percentFormatter = new Intl.NumberFormat("vi-VN", {
  style: "percent",
  maximumFractionDigits: 1,
});

export function formatProgressPercent(value: number | null): string {
  if (value === null) return "Chưa có nội dung bắt buộc";
  if (!Number.isFinite(value) || value < 0 || value > 100) {
    throw new RangeError("Invalid progress percentage");
  }
  return percentFormatter.format(value / 100);
}
```

Đặt mỗi helper trong file tương ứng; snippet gom chỉ để minh họa. Không tính lại `value` từ số buổi học, thời gian mở tab hoặc điểm quiz.

### 5.3. HTTP client — hành vi bắt buộc

API client duy nhất dùng fetch trực tiếp, ngoại trừ CSRF bootstrap/auth teardown boundary đã thiết kế. Feature không tự gọi axios/fetch rải rác.

```ts
interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  json?: unknown;
  formData?: FormData;
  signal?: AbortSignal;
  idempotencyKey?: string;
}

// Decoder bắt buộc: dữ liệu HTTP là unknown trước khi validate.
type Decoder<T> = (value: unknown) => T;

// Contract của hàm cần triển khai; không phải implementation hoàn chỉnh.
declare function apiRequest<T>(
  path: string,
  options: ApiRequestOptions,
  decode: Decoder<T>,
): Promise<T>;
```

Implementation phải:

1. Chỉ nhận path nội bộ bắt đầu `/`, ghép base `/api/v1`; từ chối URL tuyệt đối và `//...` để không gửi cookie/token ra ngoài.
2. Không cho cùng lúc json và formData; set Content-Type JSON khi json có mặt. Với FormData để trình duyệt tự đặt multipart boundary.
3. Thêm CSRF cho unsafe methods, credentials, Accept phù hợp và Idempotency-Key nếu có.
4. Hỗ trợ AbortSignal và timeout theo endpoint; cleanup signal/timer. Timeout mutation là kết quả chưa rõ, không tự POST lại.
5. Xử lý 204/205 không body; decode `undefined` bằng decoder riêng cho void. Không gọi `response.json()` trên body rỗng.
6. Với lỗi HTTP, thử đọc Problem Details an toàn; fallback `HTTP_ERROR` cùng status khi proxy/body sai schema. Với success sai JSON/schema, throw lỗi `PROTOCOL_ERROR` thay vì trả object giả.
7. Không catch rồi trả undefined/default success. Không toast hoặc redirect trực tiếp từ utility; auth/error presentation xử lý ở tầng ứng dụng/feature.
8. Không tự retry mutation. QueryClient quyết định retry query; logical operation quyết định retry mutation với đúng key/payload.

Ví dụ feature API dùng client, lược import/type/schema do G1/contract generation cần tạo:

```ts
export function getLesson(
  enrollmentId: string,
  lessonId: string,
  signal?: AbortSignal,
): Promise<LessonResponse> {
  return apiRequest(
    `/enrollments/${encodeURIComponent(enrollmentId)}` +
      `/lessons/${encodeURIComponent(lessonId)}`,
    { signal },
    lessonResponseSchema.parse,
  );
}
```

## 6. Server state, query keys và mutation

- Query dùng TanStack Query; không useEffect fetch rồi copy kết quả vào useState.
- Mỗi feature có API functions, key factory, hooks riêng. Component không tự dựng query key bằng nhiều format khác nhau.
- Key chứa user scope và mọi filter ảnh hưởng kết quả; include enrollment/revision/lesson khi liên quan. Chưa có user ID thì query cá nhân disabled.
- Query function nhận AbortSignal từ TanStack Query. Clear/cancel cache khi đổi user; không persist cache cá nhân vào localStorage trong bản đầu.
- Static outline theo revision có thể stale lâu; Hôm nay/progress cần invalidate sau thay đổi. `staleTime` đặt theo feature, không để cùng một giá trị ngầm cho tất cả.
- Query retry tối đa 1 lần cho lỗi network/5xx phù hợp; không retry 400/401/403/404/409/429 tự động. Mutation mặc định retry 0.

Mẫu key factory:

```ts
export const learningKeys = {
  user: (userId: string) => ["learning", userId] as const,
  enrollment: (userId: string, enrollmentId: string) =>
    [...learningKeys.user(userId), "enrollment", enrollmentId] as const,
  lesson: (userId: string, enrollmentId: string, lessonId: string) =>
    [...learningKeys.enrollment(userId, enrollmentId), "lesson", lessonId] as const,
  today: (userId: string, enrollmentId: string, date: string, timeZone: string) =>
    [...learningKeys.enrollment(userId, enrollmentId), "today", date, timeZone] as const,
  summary: (userId: string, enrollmentId: string) =>
    [...learningKeys.enrollment(userId, enrollmentId), "summary"] as const,
};
```

Key userId chỉ chia cache; không phải tham số tự quyết định quyền server. Schedule preview phải chứa version/snapshot input; đổi form làm preview cũ hết hiệu lực.

Sau progress mutation thành công, cập nhật từ response hoặc invalidate lesson, today, summary, outline progress và schedule liên quan. Catalog chứa progress cũng cần invalidate; outline nội dung bất biến không cần refetch chỉ vì trạng thái cá nhân đổi.

- Giai đoạn đầu dùng server-confirmed UI: pending → success/error. Không optimistic completion trước khi có snapshot/rollback được test.
- Mutation trả xong không được sửa draft đang gõ thành bản cũ. Cache server và local draft là hai lớp khác nhau.
- Không toast trùng ở API client, mutation hook và page. Inline form error cho input; toast chỉ cho thông báo tổng quát cần thiết.

## 7. Component dùng chung

### 7.1. UI primitives (`components/ui`)

Dùng một implementation cho `Button`, `Input`, `Textarea`, `Label`, `Checkbox`, `RadioGroup`, `Select`, `Dialog`, `AlertDialog`, `Sheet`, `Tabs`, `Accordion`, `Tooltip`, `Skeleton`, `Progress`. Thay đổi primitive cần rà các feature đang dùng, không sửa cục bộ để phù hợp riêng một màn.

- Variants Button chuẩn: `default`, `secondary`, `outline`, `ghost`, `destructive`; size theo primitives đã chốt.
- Dialog xác nhận dùng AlertDialog; không dùng window.confirm/prompt cho flow nghiệp vụ.
- Dùng label thật, `aria-describedby` cho lỗi/helper; icon-only button có accessible name.

### 7.2. Shared presentation (`components/shared`)

| Component | Props chính | Trách nhiệm |
|---|---|---|
| `PageHeader` | `title`, `description?`, `actions?`, `breadcrumbs?` | H1, mô tả, CTA theo layout chung |
| `EmptyState` | `title`, `description?`, `action?`, `icon?` | Chưa có nội dung; không dùng thay loading/error |
| `ErrorState` | `message`, `traceId?`, `onRetry?` | Thông báo an toàn, thao tác thử lại |
| `PageSkeleton` | `variant: "list" | "detail" | "learning"` | Skeleton phù hợp bố cục |
| `SubmitButton` | Button props + `pending`, `children` | Disable, spinner, aria-busy nhất quán |
| `ConfirmDialog` | `open`, `onOpenChange`, `title`, `description`, `onConfirm`, `pending`, `destructive?` | Xác nhận hành động, pending không double-submit |
| `ExternalResourceLink` | `href`, `children`, `description?` | Validate URL, target/rel và nhãn mở tab mới |
| `SaveStatus` | discriminated union trạng thái | idle/dirty/saving/saved/error/conflict, có retry nếu phù hợp |
| `FormErrorSummary` | `errors` | Hiển thị lỗi không map được vào field |

`ErrorState` không log hoặc gửi request; action nhận callback từ owner. `ConfirmDialog` không biết endpoint cần gọi.

### 7.3. Component nghiệp vụ tái sử dụng trong feature

| Feature | Component | Quy tắc |
|---|---|---|
| learning | `ProgressSummary` | Counts + percent từ BE; không tự đoán % |
| learning | `LearningStatusBadge` | Map enum → nhãn/icon/semantic variant tập trung |
| learning | `ActivityRenderer` | Discriminated union theo activity type; unsupported có state rõ |
| learning | `LessonNavigation` | Previous/next đúng dữ liệu server, không tính ID theo số thứ tự |
| notes | `NoteEditor` | Draft, save status, version conflict, khả năng rời trang |
| scheduling | `SchedulePreview` | Hiển thị tác động, version và confirm; không dùng preview đã stale |
| pronunciation | `AudioRecorder` | State machine microphone/recording, không gọi trực tiếp model |
| pronunciation | `AudioPlayback` | Phát audio, cleanup URL, label rõ |
| pronunciation | `PronunciationFeedback` | Result thử nghiệm, null/unavailable rõ, nghe lại/luyện lại |

Chỉ nâng component lên shared khi thật sự không chứa nghiệp vụ và có nhiều consumer. `ProgressSummary` không cần generic hóa để phục vụ mọi loại chart.

### 7.4. Mẫu `SubmitButton`

```tsx
import type { ComponentProps, ReactNode } from "react";
import { LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

type SubmitButtonProps = Omit<ComponentProps<typeof Button>, "children" | "asChild"> & {
  pending: boolean;
  children: ReactNode;
};

export function SubmitButton({
  pending,
  disabled,
  children,
  type = "submit",
  ...props
}: SubmitButtonProps) {
  return (
    <Button {...props} type={type} disabled={disabled || pending} aria-busy={pending}>
      {pending && <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />}
      {children}
    </Button>
  );
}
```

Không dùng SubmitButton như link/asChild. Nếu primitives đã dùng cơ chế composition khác, điều chỉnh wrapper một lần theo version được pin.

## 8. Form, ghi chú và hook tái sử dụng

- RHF + Zod cho form có validation; schema trong feature, message phù hợp ngôn ngữ UI. Number/date input phải được chuyển kiểu có chủ đích, tránh empty string biến thành 0.
- Dùng field primitives có label/error/id nhất quán. Không tạo mega-component với hàng chục boolean props để biểu diễn mọi loại field.
- Default values đến từ server; chỉ reset khi người dùng chọn hoặc khi chưa có draft dirty. Refetch nền không được xóa dữ liệu đang nhập.
- Xử lý Promise của event bằng mutation state/try-catch; không để unhandled rejection hoặc success toast trước khi lưu xong.

Hook cần dùng lại:

| Hook | Phạm vi | Hành vi bắt buộc |
|---|---|---|
| `useDebouncedValue` | shared UI | Cleanup timer, nhận delay rõ; không thay cho hàng đợi lưu |
| `useUnsavedChangesGuard` | shared integration | Route guard theo router đã pin, beforeunload khi dirty |
| `useAutosaveNote` | notes | Draft/version, một request đang lưu mỗi note, serialize lượt tiếp theo |
| `useUpdateProgress` | learning | Quyền/type/version/pending từ server, invalidate đúng dữ liệu |
| `useAudioRecorder` | pronunciation | Quyền microphone, start/stop/reset, duration cap, cleanup |

Quy tắc autosave: debounce khoảng 700 ms là mặc định đề xuất, cấu hình một nơi. Nếu request A đang chạy mà người dùng gõ B, giữ B local và chỉ gửi sau A bằng version A trả về. Response A không được ghi đè B. Conflict giữ draft, tải bản server và cho người dùng quyết định; không tự overwrite hoặc retry vô hạn. Rời/unmount giữa request không được hiện “Đã lưu” cho draft chưa gửi.

Dirty text và bản ghi không persist ngoài memory mặc định. Nếu muốn draft offline/localStorage, cần policy riêng có giới hạn dung lượng, owner scope và cleanup logout; không tự thêm trong task UI.

## 9. Styling, design tokens và accessibility

- Semantic tokens: `background`, `foreground`, `card`, `primary`, `muted`, `border`, `destructive`, `success`, `warning`, `ring`; tên cụ thể ánh xạ một lần trong theme Tailwind.
- Dùng `bg-background`, `text-foreground`, `border-border` thay màu hex rải rác. Không trộn nhiều teal/green cho cùng CTA.
- Không ghép dynamic class như `bg-${color}-500`; dùng mapping class tĩnh hoặc CSS variables được kiểm soát.
- Spacing theo scale 4 px; chọn width/sidebar/content tokens thay magic number từng trang. Content bài học có line-height/bề rộng đọc thống nhất.
- IPA dùng font hỗ trợ; không upper-case/letter-spacing tùy tiện làm hỏng ký hiệu.
- CTA/loading/error có text; không chỉ đổi màu. Focus ring rõ; tab order tự nhiên; không div onClick thay button.
- Modal/drawer dùng primitives có focus management và Escape. Sau khi chuyển nhiệm vụ/route, focus đến heading phù hợp, thông báo lưu nhẹ qua live region.
- Respect reduced motion; không tự phát audio, animation nền hoặc confetti làm gián đoạn bài.
- Mobile thao tác không bị footer sticky che, kiểm tra keyboard/textarea. Sidebar drawer không thu hẹp vùng học thành hai cột trên màn nhỏ.
- Markdown dùng renderer an toàn, raw HTML tắt/sanitize; không dùng dangerouslySetInnerHTML với dữ liệu thô.

## 10. Ngày giờ và trạng thái học

- Ngày lịch là date-only; không `new Date("YYYY-MM-DD")` rồi format theo timezone máy vì có thể lệch ngày.
- Formatter date-only dùng thư viện/plain-date adapter hoặc parse thành UTC rồi format cố định UTC để giữ calendar date; validate cả ngày tồn tại. Không tự cộng 7 giờ để sửa lệch.
- Instant mới format bằng timezone user/enrollment được chỉ định. Giữ `Asia/Ho_Chi_Minh`, không rút thành offset cố định.
- Hôm nay và lịch lấy từ server; FE có thể hiển thị đồng hồ, không tự viết lại toàn bộ lịch khi mở trang.
- Cập nhật today query khi đổi timezone/ngày địa phương, tab quay lại sau nửa đêm; dùng chung scheduler/hook trong app thay timer mỗi component.
- Map enum trạng thái một nơi theo feature. API có giá trị chưa hỗ trợ thì hiện trạng thái an toàn và ghi lỗi kỹ thuật, không tự coi là COMPLETED.
- Quiz score, progress và gợi ý phát âm là các chỉ số khác nhau, có component/nhãn riêng. Lỗi audio không biến thành điểm 0 hay chưa hoàn thành cả lộ trình.

## 11. Ghi âm và tích hợp pronunciation

- `useAudioRecorder` quản lý union trạng thái: `idle`, `requestingPermission`, `recording`, `stopping`, `ready`, `error`. Trạng thái gửi/chấm thuộc hook attempt riêng.
- Xin microphone sau thao tác người dùng; feature detect API/codec, kiểm tra `MediaRecorder.isTypeSupported`. HTTPS hoặc môi trường local được trình duyệt cho phép.
- Cleanup: stop tracks, stop recorder phù hợp, clear timers, remove listeners, revoke object URLs khi thay recording/unmount. Không cleanup URL vẫn đang được audio player dùng.
- Preview audio trước submit; dùng FormData qua API client, không set multipart Content-Type bằng tay. Backend kiểm tra/chuẩn hóa audio lại.
- Model và thư viện Python không bundle vào FE. Không tải audio người học lên dịch vụ ngoài khi đang dùng chế độ local.
- Không gọi worker trực tiếp từ trình duyệt. BE chịu trách nhiệm reference, owner, attempt, engine version và result schema.
- Kết quả thử nghiệm hiện “Gợi ý luyện phát âm”; không trình bày edit distance như xác suất đúng. Trường không có dữ liệu hiển thị chưa đánh giá được.
- Poll attempt chỉ khi ở trạng thái chưa kết thúc, có interval/cap/cleanup; abort polling không đồng nghĩa đã cancel job server. Cancel phải có API/quy tắc riêng nếu được hỗ trợ.

## 12. Kiểm thử và kiểm tra tự động

- Formatter/ESLint/TypeScript strict chạy CI. Bật kiểm tra rules-of-hooks và exhaustive-deps; không disable hàng loạt để qua lint.
- Vitest cho helper có logic (ngày, nullable percent, error normalization), component behavior và hook concurrency quan trọng.
- Testing Library kiểm tra qua role/name/label thay CSS class. TestId chỉ khi không có selector ngữ nghĩa phù hợp.
- Mock HTTP ở boundary (ví dụ MSW khi được thêm); không mock sâu mọi hook khiến mất kiểm tra integration giữa API/cache/UI.
- Playwright: login → enroll → học → note → complete → reload; conflict hai tab; session hết hạn; quiz submit retry; mobile + bàn phím.
- Test media bằng recorder adapter/mock và một lượt kiểm tra thiết bị thật. Test codec/browser thực tế trước khi tuyên bố hỗ trợ.
- Unit test date-only ở timezone khác nhau, response204, lỗi HTML proxy, abort, CSRF refresh và cache không rò giữa hai user.
- Kiểm tra autosave bằng hai response về khác thời điểm và user còn gõ; không chỉ test debounce timer.
- Không coi screenshot hoặc build thành công là bằng chứng tính đúng của điểm phát âm/tiến độ server.

Script names cần được cấu hình ở G1 (ví dụ dưới dùng npm cùng package-lock; đổi package manager phải chốt một lần):

```text
npm run format:check
npm run lint
npm run typecheck
npm run test:run
npm run build
npm run test:e2e
```

Chỉ chạy E2E cần môi trường phù hợp; báo rõ nếu chưa chạy. Không dùng test tùy ý sửa DB cá nhân hoặc nhập fixture vào production.

## 13. Checklist cho mỗi task và AI lập trình

1. Đọc kế hoạch, BE/FE convention; tìm component/helper/hook hiện có trước khi tạo mới.
2. Chốt props và contract API; không tự đặt response shape khác BE.
3. Chọn đúng feature và owner của state. Không copy server state sang global store hoặc thêm shared abstraction vì dự đoán tương lai.
4. Có loading/empty/error/pending/success đúng ngữ cảnh, xử lý 401/409 và giữ draft.
5. Dùng primitives, tokens, label maps, API client và key factory chung.
6. Chạy check phù hợp và demo flow trên desktop/mobile. Thay API phải sinh types lại, không sửa generated file tay.
7. Báo những helper/component dùng lại, thành phần mới và lý do; tránh tự refactor feature ngoài phạm vi.

## 14. Nguồn kỹ thuật

- [React Rules of Hooks](https://react.dev/reference/rules/rules-of-hooks).
- [TypeScript strict](https://www.typescriptlang.org/tsconfig/strict.html).
- [TanStack Query keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys).
- [Tailwind Vite](https://tailwindcss.com/docs/installation/using-vite).
- [MediaRecorder](https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder).

Props, helper names, token names, pagination/error contract và cách tổ chức ở trên là convention của dự án. Khi implement, chọn API đúng phiên bản dependency đã pin; thay đổi shared contract phải cập nhật cả hai phía và ADR tương ứng.
