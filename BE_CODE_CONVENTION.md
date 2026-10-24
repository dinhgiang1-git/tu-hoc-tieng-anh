# Backend code convention

Ngày cập nhật: 24/10/2026. Áp dụng cho Java Spring Boot + PostgreSQL và phần tích hợp speech worker nội bộ.

Đọc cùng [kế hoạch dự án](./KE_HOACH_DU_AN.md) và [FE convention](./FE_CODE_CONVENTION.md). Đây là **quy chuẩn và mẫu code để triển khai**, các class/hàm trong tài liệu chưa tồn tại trong ứng dụng. Chỉ tạo thành phần khi task cần dùng; không sinh trước mọi abstraction.

## 1. Quy tắc nền tảng

- Java 21, Maven Wrapper, Spring Web MVC, Spring Security, Bean Validation, JPA, Flyway; pin dependency ở G1.
- Modular monolith theo nghiệp vụ; một backend chính. Python xử lý model âm thanh qua adapter riêng.
- Controller xử lý HTTP; application service điều phối use case/transaction; domain policy xử lý quy tắc; repository truy cập DB.
- Mọi dữ liệu cá nhân phải kiểm tra owner từ authenticated principal.
- Nội dung lộ trình và tiến độ cá nhân tách biệt. Không hardcode Lộ trình A, user ID hoặc 26 tuần.
- Service không trả JPA entity ra API. Không sử dụng frontend làm nơi kiểm soát quyền/tiến độ chính.
- Ưu tiên code dễ hiểu. Không tạo generic CRUD framework, `BaseService`, `BaseController`, `CommonUtils` hoặc interface chỉ để có hậu tố `Impl`.
- Business rule nằm trong module sở hữu nó; `shared` chỉ chứa nền tảng thực sự dùng chung.
- Code/identifier tiếng Anh; mô tả nghiệp vụ và thông báo hiển thị có thể tiếng Việt. Comment giải thích lý do, không diễn lại code.

## 2. Cấu trúc package và hướng phụ thuộc

Chọn root package `vn.englishlearning` tại G1, dùng thống nhất. Nếu đổi cần sửa template/ADR một lần.

```text
backend/src/main/java/vn/englishlearning/
  identity/
  curriculum/
  contentops/
  enrollment/
  scheduling/
  learning/
    api/                     # controller, request/response DTO
    application/             # use case, command/query, mapper
    domain/                  # entity, enums, policy, repository interface nếu cần
    infrastructure/          # JPA/query/adapters đặc thù
  assessment/
  notes/
  review/
  reporting/
  pronunciation/
    application/port/        # PronunciationProvider
    infrastructure/         # LocalSpeechClient
  shared/
    api/                     # PageResponse, FieldViolation, error handling
    security/                # CurrentUser, principal
    time/                    # Clock configuration
    observability/           # trace/correlation
    config/
```

- Module nhỏ có thể dùng ít thư mục hơn, nhưng giữ vai trò và đường phụ thuộc rõ ràng.
- `api → application → domain`; infrastructure cung cấp persistence/adapters. Domain policy không gọi HTTP, servlet, SecurityContext hoặc speech worker.
- Được dùng JPA annotations trong entity domain để giảm mapping ở giai đoạn đầu; không cần sao chép entity thành domain object giống hệt chỉ để “clean architecture”.
- JPA repository đơn giản có thể đặt trong infrastructure; application dùng repository đó trong module. Khi cần thay provider hoặc có nhiều persistence implementation, mới thêm port riêng.
- Module khác chỉ gọi application facade/query service đã công bố; không truy cập repository/entity nội bộ để sửa dữ liệu chéo module.
- `shared` không import module nghiệp vụ. Không tạo vòng phụ thuộc. Reporting có query projection riêng cho đọc, không sửa bảng của module khác.

## 3. Định dạng và đặt tên

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Class/interface/record | PascalCase | `UpdateActivityProgressService` |
| Method/variable | camelCase, thể hiện ý nghĩa | `completedRequiredActivities` |
| Constant/enum | UPPER_SNAKE_CASE | `IN_PROGRESS` |
| Package | lowercase | `learning.application` |
| Controller | Danh từ + Controller | `ActivityProgressController` |
| Use case service | Động từ cụ thể + Service | `PublishCurriculumService` |
| Query service | Tên nghiệp vụ + QueryService | `LearningSummaryQueryService` |
| HTTP DTO | `...Request`, `...Response` | `UpdateProgressRequest` |
| Nội bộ use case | `...Command`, `...Query` | `UpdateProgressCommand` |
| Domain policy | Tên quy tắc + Policy | `ProgressPolicy` |
| Adapter | Công nghệ/chức năng + Client | `LocalSpeechClient` |
| SQL | snake_case | `activity_progress`, `completed_at` |

- Formatter: Spotless + Google Java Format, indentation 2 spaces theo formatter, UTF-8 và LF. Formatter quyết định line wrapping/import order.
- Dùng constructor injection, `private final` cho dependencies. Không field injection.
- Mặc định không dùng Lombok. DTO dùng record; JPA entity là class với constructor bảo vệ và method nghiệp vụ, không `@Data`/setter cho mọi field.
- Hạn chế public API. Method chỉ làm một việc rõ ràng; tách theo trách nhiệm thay vì áp giới hạn số dòng máy móc.
- Boolean dùng `is...`, `has...`, `can...`; số lượng dùng `...Count` hoặc tên đầy đủ, thời lượng kèm unit như `durationMs`.
- Không `Optional.get()` khi chưa xử lý rỗng; Optional dùng cho kết quả có thể không tồn tại, không dùng làm field entity/DTO.
- Enum dùng `EnumType.STRING`, không ordinal. Không dùng double cho tiền hoặc phép tính cần chính xác thập phân.
- `catch (Exception)` chỉ ở boundary cần chuyển lỗi có kiểm soát; không catch rồi trả null hoặc dữ liệu giả.

## 4. Contract bắt buộc đồng bộ với FE

**Nguồn chuẩn:** `docs/api/openapi.yaml`, cập nhật trước hoặc cùng thay đổi API. Hai file convention dùng cùng các quyết định dưới đây. Thay contract phải cập nhật cả consumer và migration nếu liên quan.

### 4.1. HTTP và JSON

- Prefix `/api/v1`; resource path dùng danh từ số nhiều/kebab-case. Action nghiệp vụ như `/publish`, `/pause`, `/schedule/preview` được dùng khi đã đặc tả.
- JSON camelCase; ID là UUID string; enum string UPPER_SNAKE_CASE.
- Ngày lịch `YYYY-MM-DD`; timestamp ISO-8601 UTC có `Z`; timezone IANA riêng, ví dụ `Asia/Ho_Chi_Minh`.
- Success trả DTO trực tiếp, không bọc thêm `{ success, data, message }`.
- Tạo mới: 201 + Location nếu có resource URL; đọc/cập nhật có body: 200; xóa/logout không body: 204.
- Field không có giá trị có ý nghĩa phải trả `null` và khai báo nullable trong schema; không dùng `""`, `0`, `-1` thay dữ liệu thiếu. PATCH phân biệt absent và explicit null; DTO patch cần biểu diễn được hai trạng thái.
- Danh sách nhỏ không phân trang có thể trả array nếu OpenAPI khai báo như vậy. Danh sách phân trang dùng duy nhất contract sau:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Page bắt đầu từ 0, mặc định size 20, tối đa 100. Sort chỉ cho field whitelist, có thứ tự phụ ổn định bằng ID. Không trả nguyên Spring `Page` vì đó là cấu trúc nội bộ.

### 4.2. Error contract

Content-Type `application/problem+json`, body theo Problem Details với extension được chuẩn hóa:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.",
  "instance": "/api/v1/enrollments/example/schedule/commit",
  "code": "VERSION_CONFLICT",
  "traceId": "opaque-trace-id",
  "fieldErrors": []
}
```

Field violation: `{ "field": "startDate", "code": "INVALID_DATE", "message": "Ngày không hợp lệ." }`. Field theo tên JSON/dot path, không tên cột DB. `fieldErrors` luôn là array, kể cả rỗng. FE branch theo `code`, không theo `detail/title`.

| HTTP | Code tiêu biểu | Quy tắc |
|---|---|---|
| 400 | `VALIDATION_FAILED`, `INVALID_REQUEST` | Sai input/JSON |
| 401 | `AUTHENTICATION_REQUIRED`, `INVALID_CREDENTIALS` | Chưa đăng nhập/xác thực thất bại |
| 403 | `FORBIDDEN`, `CSRF_INVALID` | Thiếu quyền/CSRF không hợp lệ |
| 404 | `RESOURCE_NOT_FOUND` | Không có hoặc không thuộc người học |
| 409 | `VERSION_CONFLICT`, `STATE_CONFLICT`, `IDEMPOTENCY_CONFLICT` | Xung đột dữ liệu/nghiệp vụ |
| 413 | `AUDIO_TOO_LARGE` | Vượt giới hạn body |
| 415 | `UNSUPPORTED_AUDIO_FORMAT` | Định dạng không hỗ trợ |
| 429 | `RATE_LIMITED` | Quá giới hạn, Retry-After khi xác định được |
| 503 | `PRONUNCIATION_UNAVAILABLE` | Worker tạm không xử lý được |
| 500 | `INTERNAL_ERROR` | Thông báo an toàn, log lỗi với trace ID |

Không trả stack trace, SQL, path máy hoặc thông tin tài khoản khác. Trace ID do server tạo; request ID từ client nếu nhận vào phải được kiểm tra, không tin tùy ý.

### 4.3. Auth, version và idempotency

- Cookie session HttpOnly; Secure trên HTTPS. Same origin qua proxy; giữ CSRF.
- `GET /auth/csrf` trả `{ "headerName": "X-CSRF-TOKEN", "token": "opaque-token" }`, `Cache-Control: no-store`. BE cấu hình handler/token tương ứng cho SPA; tên header đúng contract, không dựa vào mặc định ngầm.
- FE gửi token cho POST/PUT/PATCH/DELETE. Login cũng dùng CSRF; sau login/logout làm mới token vì session/token có thể thay đổi.
- `expectedVersion` trong body cho update có optimistic locking; version hiện hành trong response. Request này không dùng thêm If-Match song song.
- Với progress, response và mutation dùng `enrollmentVersion` (body gửi dưới tên `expectedVersion`); enrollment là điểm đồng bộ với reschedule. Nếu bảng progress cũng có `@Version`, đó là cơ chế nội bộ bổ sung, không để FE nhầm hai version.
- Create enrollment, create/submit attempt và commit lịch có `Idempotency-Key` theo contract. Scope key = user + operation/target; lưu request hash, trạng thái và kết quả. Cùng key/payload trả kết quả cũ, khác payload trả 409.
- Tra cứu replay idempotency trước khi coi version cũ là xung đột. Cùng request đã thành công nhưng client timeout phải nhận lại kết quả thành công cũ.
- Với progress PUT: cùng trạng thái và version mới nhất không phát sinh event mới; version cũ do hai tab trả 409. FE refetch để xác nhận, không tự đổi version rồi gửi lại mutation.
- Đặt TTL idempotency cụ thể trong cấu hình/ADR; hết TTL không được bỏ unique constraint của dữ liệu nghiệp vụ.

## 5. Bộ thành phần dùng chung cần triển khai

### 5.1. Shared nền tảng

| Thành phần | Contract dự kiến | Quy tắc dùng |
|---|---|---|
| `CurrentUser` | `UUID requireUserId()` | Chỉ adapter security đọc principal; không lấy userId từ body |
| `ApplicationClock` | Bean `java.time.Clock` | Service inject Clock, không tự gọi đồng hồ hệ thống khắp nơi |
| `PageResponse<T>` | `items/page/size/totalItems/totalPages` | Mapper pagination tại boundary |
| `FieldViolation` | Record `field/code/message` | Validation lỗi đúng field contract |
| `ErrorCode` | Enum code/status/default safe detail | FE nhận string code ổn định |
| `DomainException` | Code + safe context cần thiết | Trường hợp nghiệp vụ dự kiến; không HTTP object trong entity |
| `GlobalExceptionHandler` | Exception → ProblemDetail | Validation, malformed input, optimistic lock, domain và lỗi ngoài dự kiến |
| `SecurityProblemWriter` | Ghi cùng Problem Details | Dùng cho auth entry point/access denied, vì lỗi security có thể ngoài MVC advice |
| `ProblemFactory` | Tạo ProblemDetail và trace metadata | Handler và security writer dùng cùng format |

Không tạo `DateUtils`, `JsonUtils`, `StringUtils` nếu JDK/thư viện hiện có đáp ứng. Validator đặc thù đặt cạnh feature, không để shared biết hết các domain.

### 5.2. Chính sách dùng lại theo nghiệp vụ

| Module sở hữu | Thành phần / hàm | Trách nhiệm |
|---|---|---|
| enrollment | `EnrollmentAccess.requireOwned(userId, enrollmentId)` | Query scoped owner, trả 404 nếu không thấy |
| learning | `ProgressPolicy.isEffectivelyRequired(lessonRequired, activityRequired)` | Thống nhất mẫu số tiến độ |
| learning | `ProgressPolicy.percentOrNull(completed, total)` | BigDecimal 0–100, làm tròn 1 chữ số; total=0 trả null |
| scheduling | `SchedulePlanner.plan(lessons, startDate, studyDays, exclusions)` | Hàm thuần; thứ tự bài ổn định, có giới hạn đầu vào |
| learning | `NextLessonPolicy.select(candidates, today)` | Đang dở → quá hạn → đến hạn → tiếp theo |
| contentops | `ContentPackageValidator.validate(package)` | Trả danh sách violation có vị trí; chưa ghi DB |
| assessment | `QuizGrader.grade(questions, answers)` | Chấm từ snapshot server, không nhận answer key client |
| pronunciation | `PronunciationProvider.assess(input)` | Port cho worker; dùng nội bộ và phiên bản hóa kết quả |

Module khác dùng các policy qua facade phù hợp. Không sao chép công thức phần trăm giữa reporting và learning; reporting query đếm dữ liệu rồi gọi cùng policy.

## 6. Mẫu code tham chiếu

Snippet minh họa convention, lược import. Khi triển khai phải bổ sung dependency/constructor/test và không sao chép phần còn thiếu như code production.

### 6.1. DTO và policy thuần

```java
public record UpdateProgressRequest(
    @NotNull ActivityStatus status,
    @NotNull @PositiveOrZero Long expectedVersion) {}

public record ProgressResponse(
    UUID activityId,
    ActivityStatus status,
    long enrollmentVersion,
    long completedRequiredActivities,
    long totalRequiredActivities,
    BigDecimal progressPercent) {}

public final class ProgressPolicy {
  private ProgressPolicy() {}

  public static boolean isEffectivelyRequired(
      boolean lessonRequired, boolean activityRequired) {
    return lessonRequired && activityRequired;
  }

  public static BigDecimal percentOrNull(long completed, long total) {
    if (total < 0 || completed < 0 || completed > total) {
      throw new IllegalArgumentException("Invalid progress counts");
    }
    if (total == 0) {
      return null;
    }
    return BigDecimal.valueOf(completed)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }
}
```

`progressPercent` nullable trong OpenAPI; FE hiển thị “Chưa có nội dung bắt buộc”. Rule buổi optional và publish validation theo kế hoạch. Không coi null là 0% hoặc 100%.

### 6.2. Controller mỏng

```java
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/activities")
public class ActivityProgressController {
  private final CurrentUser currentUser;
  private final UpdateActivityProgressService service;

  public ActivityProgressController(
      CurrentUser currentUser, UpdateActivityProgressService service) {
    this.currentUser = currentUser;
    this.service = service;
  }

  @PutMapping("/{activityId}/progress")
  public ProgressResponse update(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID activityId,
      @Valid @RequestBody UpdateProgressRequest request) {
    return service.execute(new UpdateProgressCommand(
        currentUser.requireUserId(), enrollmentId, activityId,
        request.status(), request.expectedVersion()));
  }
}
```

Service phải xác minh owner, revision, loại activity, transition và version. DTO hợp lệ không đồng nghĩa nghiệp vụ hợp lệ. QUIZ không được đổi progress bằng endpoint checklist.

## 7. Transaction, persistence và đồng thời

- Đặt transaction ở public application service được gọi qua Spring bean proxy; không dựa vào self-invocation để kích hoạt annotation.
- Query dùng read-only transaction khi phù hợp; `readOnly` không thay thế kiểm soát quyền hoặc cấm ghi bằng DB.
- Progress, event và enrollment version phải cập nhật nguyên tử. Reschedule và completion khóa cùng enrollment theo cùng thứ tự trước khi khóa các bản ghi con.
- Ghi rõ chiến lược: `@Version` cho optimistic locking; operation đa bảng có thể khóa hàng enrollment. Nếu bảng con thay đổi thì chủ động thay enrollment aggregate để version được tăng; không giả định JPA tự tăng parent version.
- Concurrent unique conflict được chuyển thành lỗi/replay đúng nghiệp vụ; không map mọi `DataIntegrityViolationException` thành 409 chung chung mà che lỗi schema.
- JPA: `EnumType.STRING`, explicit fetch strategy; to-one ưu tiên LAZY. Tắt Open Session in View, map DTO/projection trong service transaction.
- Không dùng collection fetch join với pagination một cách làm sai số lượng; dùng query IDs/projections phù hợp.
- Có thứ tự rõ khi lấy danh sách; không dựa vào thứ tự mặc định của DB.
- Flyway filename `V001__create_identity_tables.sql`, các migration tiếp theo tăng duy nhất. Trong nhóm nhiều người phải kiểm tra trùng version trước merge; không sửa migration đã áp dụng ở môi trường dùng chung.
- FK/unique/check/index phải thể hiện các invariant quan trọng. Import nội dung đi qua importer riêng, không qua Flyway.
- Không cascade-delete lịch sử enrollment khi archive nội dung. Entity audit fields không thay thế learning event.
- PostgreSQL date cho lịch, timestamptz cho sự kiện. Inject Clock rồi dùng `Instant.now(clock)`; `LocalDate.now(clock.withZone(zoneId))` theo timezone của enrollment.

## 8. Tích hợp speech worker và tài nguyên ngoài

- `pronunciation` sở hữu `PronunciationProvider`, request/response nội bộ và adapter. Worker payload không lộ thẳng ra FE.
- Spring Boot lấy reference từ activity server; client gửi audio + activity/enrollment, không quyết định reference chuẩn.
- Kiểm tra ownership, byte size, thời lượng thực sau decode, media type/codec; không tin chỉ extension/header client.
- Giới hạn timeout, concurrent jobs, queue size; có cancel/expiry policy. Không giữ DB transaction/row lock trong lúc suy luận.
- Tạo attempt/job trong transaction ngắn → gọi worker ngoài transaction → ghi kết quả có điều kiện/version trong transaction ngắn.
- Worker nhận job ID; finalize chỉ từ trạng thái cho phép, không ghi đè attempt đã cancel/finish. Retry cần tránh nhân đôi kết quả/chi phí tài nguyên.
- `en-GB` là locale ứng dụng; ánh xạ sang British voice ID của công cụ tại một adapter/config, không rải chuỗi voice ID khắp code.
- Pin model/reference/calibration version; lưu kèm attempt. Nhận dạng âm chưa tương đương chứng nhận phát âm đúng.
- Audio quá nhỏ/nhiễu/không có tiếng nói trả kết quả không đánh giá được, score nullable. Không dùng lỗi provider thành điểm 0.
- File tạm ngoài webroot, tên ngẫu nhiên, không dùng tên file client làm path. Cleanup có TTL kể cả lỗi; audio dài hạn có chính sách riêng.
- Không fetch URL tùy ý từ input hoặc chạy command được ghép từ input. Nếu gọi bộ chuyển audio, dùng argument array, timeout và giới hạn tài nguyên.

## 9. Logging, cấu hình và secrets

- Log theo cấu trúc có event name, traceId, operation, thời gian xử lý; không ghi password, session/CSRF token, raw audio, toàn bộ note/answer.
- Một lỗi ngoài dự kiến log stack trace ở boundary chịu trách nhiệm; tránh cùng exception được log lặp ở mọi tầng.
- `@ConfigurationProperties` + validation cho cấu hình nhóm; profile dev/test/prod rõ ràng.
- Secret lấy từ môi trường/secret store, không hardcode hoặc commit. Không đổi container/volume DB hiện có ngoài phạm vi task.
- Public error message ngắn, log nội bộ đủ chẩn đoán. Không tự suppress lỗi rồi báo thành công.

## 10. Kiểm thử và công cụ thống nhất

- Unit policy: plain JUnit, không khởi động Spring cho hàm thuần. Tên test `returnsNullWhenNoRequiredActivities` hoặc `method_condition_expected` thống nhất trong class.
- Integration dùng PostgreSQL Testcontainers cho transaction, FK, enum/index, concurrency, timezone. Không lấy H2 thay bằng chứng SQL PostgreSQL.
- HTTP/security test kiểm tra status/body/schema, CSRF, owner A/B và security errors cùng format.
- Bắt buộc cover các invariant bị thay đổi: retry, version, wrong revision, optional task, archive/revision, quiz server grading.
- Dùng fixed Clock và fixture factory nhỏ; không phụ thuộc giờ thật, test order hoặc database cá nhân.
- Chọn formatter/checkstyle hoặc static analysis phù hợp và cấu hình một lần. Maven `verify` bao gồm unit/integration suite đã cấu hình; CI không bỏ qua test bằng mặc định.
- API contract được validate; FE type generation phải chạy lại khi schema thay đổi. Không chỉnh file generated bằng tay.

Các lệnh cần được cấu hình khi dựng project, chưa phải lệnh đã chạy trong workspace hiện tại:

```text
Windows: .\mvnw.cmd spotless:check verify
WSL/CI:  ./mvnw spotless:check verify
```

## 11. Checklist cho mỗi task và AI lập trình

1. Đọc kế hoạch, hai convention và contract trước khi tạo code; tìm hàm/policy tương ứng trước khi thêm mới.
2. Chọn module sở hữu; thêm shared chỉ khi có ít nhất hai consumer thực tế hoặc là cross-cutting boundary đã xác định.
3. DTO và error code mới phải cập nhật OpenAPI/FE type. Shared signature đổi phải rà mọi caller.
4. Không viết lại auth, pagination, error handler, progress policy hoặc speech HTTP client cho từng feature.
5. Kiểm tra transaction/owner/version/timezone và dữ liệu cũ; thêm migration khi cần.
6. Chạy check tương ứng, nêu rõ kết quả và những phần chưa kiểm chứng. Build thành công không chứng minh scoring phát âm chính xác.
7. Trước review, bỏ debug log, secret, TODO thay nghiệp vụ và catch rỗng. Mô tả cách demo thay đổi và rủi ro thực tế.

## 12. Nguồn kỹ thuật

- [Spring Problem Details](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html): dùng cơ chế chuẩn để thống nhất error body.
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html): lưu ý proxy và boundary transaction.
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html): cấu hình đúng với session/SPA.

Các lựa chọn về tên class, pagination, error code và convention trong tài liệu này là quyết định của dự án. Khi dependency đổi major, kiểm tra tài liệu phiên bản được pin và ghi thay đổi contract vào ADR.
