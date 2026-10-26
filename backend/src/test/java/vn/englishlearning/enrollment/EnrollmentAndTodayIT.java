package vn.englishlearning.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.englishlearning.contentops.application.ImportCurriculumPackageService;
import vn.englishlearning.contentops.application.PublishCurriculumRevisionService;
import vn.englishlearning.contentops.domain.ImportSummary;
import vn.englishlearning.contentops.domain.PublishResult;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.Curriculum;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumPhaseRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.api.EnrollmentRequests;
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.api.CsrfTokenResponse;
import vn.englishlearning.identity.api.LoginRequest;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.learning.api.TodayResponses;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.test.SqlStatementInspector;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EnrollmentAndTodayIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_enrollment_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    registry.add(
        "spring.jpa.properties.hibernate.session_factory.statement_inspector",
        () -> "vn.englishlearning.shared.test.SqlStatementInspector");
  }

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AppUserRepository userRepository;
  @Autowired private LearnerPreferencesRepository preferencesRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ImportCurriculumPackageService importService;
  @Autowired private PublishCurriculumRevisionService publishService;
  @Autowired private CurriculumRepository curriculumRepository;
  @Autowired private CurriculumRevisionRepository revisionRepository;
  @Autowired private CurriculumPhaseRepository curriculumPhaseRepository;
  @Autowired private CurriculumWeekRepository curriculumWeekRepository;
  @Autowired private LessonRepository lessonRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @SpyBean private LessonScheduleRepository lessonScheduleRepository;
  @Autowired private ActivityProgressRepository activityProgressRepository;
  @Autowired private Clock clock;
  @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  private UUID userAId;
  private UUID userBId;
  private UUID curriculumId;
  private UUID revisionId;

  private record CsrfSession(String csrfToken, String sessionCookie) {}

  private String baseUrl() {
    return "http://localhost:" + port + "/api/v1";
  }

  @BeforeEach
  void setUpDatabase() {
    jdbcTemplate.execute("TRUNCATE TABLE app_user CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE curriculum CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE idempotency_record CASCADE");

    Instant now = Instant.now(clock);

    // Create User A
    AppUser userA =
        new AppUser(
            "usera",
            passwordEncoder.encode("Password123!"),
            "User A",
            "usera@example.com",
            UserRole.LEARNER,
            now);
    userA = userRepository.save(userA);
    userAId = userA.getId();
    preferencesRepository.save(
        new LearnerPreferences(userAId, 30, "MONDAY,WEDNESDAY,FRIDAY", "Asia/Ho_Chi_Minh", now));

    // Create User B
    AppUser userB =
        new AppUser(
            "userb",
            passwordEncoder.encode("Password123!"),
            "User B",
            "userb@example.com",
            UserRole.LEARNER,
            now);
    userB = userRepository.save(userB);
    userBId = userB.getId();
    preferencesRepository.save(
        new LearnerPreferences(userBId, 20, "TUESDAY,THURSDAY", "Asia/Ho_Chi_Minh", now));

    // Import and publish pilot curriculum
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    ImportSummary summary = importService.importPackage(packageDir);
    PublishResult publishResult = publishService.publishById(summary.revisionId());

    revisionId = publishResult.revisionId();
    curriculumId = revisionRepository.findById(revisionId).orElseThrow().getCurriculum().getId();
  }

  private String extractCookie(HttpHeaders headers) {
    List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
    if (cookies == null || cookies.isEmpty()) {
      cookies = headers.get("Set-Cookie");
    }
    if (cookies == null || cookies.isEmpty()) {
      return null;
    }
    for (String cookie : cookies) {
      if (cookie.contains("SESSION=") || cookie.contains("JSESSIONID=")) {
        return cookie.split(";")[0];
      }
    }
    return cookies.get(0).split(";")[0];
  }

  private CsrfSession obtainCsrfToken(String existingSessionCookie) {
    HttpHeaders headers = new HttpHeaders();
    if (existingSessionCookie != null) {
      headers.set(HttpHeaders.COOKIE, existingSessionCookie);
    }

    ResponseEntity<CsrfTokenResponse> response =
        restTemplate.exchange(
            baseUrl() + "/auth/csrf",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            CsrfTokenResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    String sessionCookie = extractCookie(response.getHeaders());
    if (sessionCookie == null) {
      sessionCookie = existingSessionCookie;
    }
    return new CsrfSession(response.getBody().token(), sessionCookie);
  }

  private CsrfSession login(String username, String password) {
    CsrfSession preLoginCsrf = obtainCsrfToken(null);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", preLoginCsrf.csrfToken());
    if (preLoginCsrf.sessionCookie() != null) {
      headers.set(HttpHeaders.COOKIE, preLoginCsrf.sessionCookie());
    }

    HttpEntity<LoginRequest> loginEntity =
        new HttpEntity<>(new LoginRequest(username, password), headers);
    ResponseEntity<String> loginResp =
        restTemplate.postForEntity(baseUrl() + "/auth/login", loginEntity, String.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String authCookie = extractCookie(loginResp.getHeaders());
    CsrfSession postLoginCsrf = obtainCsrfToken(authCookie);
    return new CsrfSession(postLoginCsrf.csrfToken(), authCookie);
  }

  @Test
  @DisplayName(
      "Full workflow: create enrollment, schedule 5 required lessons, replay, conflict, cross-user isolation")
  void testEnrollmentFullWorkflow() {
    CsrfSession sessionA = login("usera", "Password123!");
    String idempotencyKey = UUID.randomUUID().toString();

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", idempotencyKey);

    // 1. First POST: 201 Created
    HttpEntity<EnrollmentRequests.CreateEnrollmentRequest> requestEntity =
        new HttpEntity<>(createReq, headers);
    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp1 =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            requestEntity,
            EnrollmentResponses.EnrollmentResponse.class);

    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    EnrollmentResponses.EnrollmentResponse created = resp1.getBody();
    assertThat(created).isNotNull();
    assertThat(created.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(created.version()).isEqualTo(1L);

    UUID enrollmentId = created.id();

    // Verify 5 required lessons scheduled on Mon/Wed/Fri starting 2026-10-26
    List<LessonSchedule> schedules =
        lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId);
    assertThat(schedules).hasSize(5);
    assertThat(schedules.get(0).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 26)); // Mon
    assertThat(schedules.get(1).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(schedules.get(2).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 30)); // Fri
    assertThat(schedules.get(3).getPlannedDate()).isEqualTo(LocalDate.of(2026, 11, 2)); // Mon
    assertThat(schedules.get(4).getPlannedDate()).isEqualTo(LocalDate.of(2026, 11, 4)); // Wed

    // Verify user preferences has focusedEnrollmentId set
    LearnerPreferences prefsA = preferencesRepository.findById(userAId).orElseThrow();
    assertThat(prefsA.getFocusedEnrollmentId()).isEqualTo(enrollmentId);

    // 2. Replay with identical key + payload -> 201 with cached response
    ResponseEntity<EnrollmentResponses.EnrollmentResponse> respReplay =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            requestEntity,
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(respReplay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(respReplay.getBody()).isNotNull();
    assertThat(respReplay.getBody().id()).isEqualTo(enrollmentId);
    assertThat(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId))
        .hasSize(5);

    // 3. Same key with different payload -> 409 IDEMPOTENCY_CONFLICT
    EnrollmentRequests.CreateEnrollmentRequest diffReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            45,
            "Asia/Ho_Chi_Minh");
    HttpEntity<EnrollmentRequests.CreateEnrollmentRequest> diffEntity =
        new HttpEntity<>(diffReq, headers);
    ResponseEntity<String> respConflictKey =
        restTemplate.postForEntity(baseUrl() + "/enrollments", diffEntity, String.class);
    assertThat(respConflictKey.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(respConflictKey.getBody()).contains("IDEMPOTENCY_CONFLICT");

    // 4. Different key for same curriculum when already ACTIVE -> 409 STATE_CONFLICT
    HttpHeaders headersNewKey = new HttpHeaders();
    headersNewKey.setContentType(MediaType.APPLICATION_JSON);
    headersNewKey.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headersNewKey.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headersNewKey.set("Idempotency-Key", UUID.randomUUID().toString());
    HttpEntity<EnrollmentRequests.CreateEnrollmentRequest> newKeyEntity =
        new HttpEntity<>(createReq, headersNewKey);
    ResponseEntity<String> respActiveConflict =
        restTemplate.postForEntity(baseUrl() + "/enrollments", newKeyEntity, String.class);
    assertThat(respActiveConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(respActiveConflict.getBody()).contains("STATE_CONFLICT");

    // 5. Cross-user key isolation: User B can use the same key without conflict
    CsrfSession sessionB = login("userb", "Password123!");
    HttpHeaders headersUserB = new HttpHeaders();
    headersUserB.setContentType(MediaType.APPLICATION_JSON);
    headersUserB.set("X-CSRF-TOKEN", sessionB.csrfToken());
    headersUserB.set(HttpHeaders.COOKIE, sessionB.sessionCookie());
    headersUserB.set("Idempotency-Key", idempotencyKey); // Same key as User A!

    HttpEntity<EnrollmentRequests.CreateEnrollmentRequest> userBEntity =
        new HttpEntity<>(createReq, headersUserB);
    ResponseEntity<EnrollmentResponses.EnrollmentResponse> respUserB =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments", userBEntity, EnrollmentResponses.EnrollmentResponse.class);
    assertThat(respUserB.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(respUserB.getBody().id()).isNotEqualTo(enrollmentId);

    // 6. Owner isolation: User B cannot GET User A's enrollment
    HttpHeaders getHeadersB = new HttpHeaders();
    getHeadersB.set(HttpHeaders.COOKIE, sessionB.sessionCookie());
    ResponseEntity<String> respGetForbidden =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId,
            HttpMethod.GET,
            new HttpEntity<>(getHeadersB),
            String.class);
    assertThat(respGetForbidden.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    // 7. GET /api/v1/me/today returns valid dashboard with version
    HttpHeaders getHeadersA = new HttpHeaders();
    getHeadersA.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    ResponseEntity<TodayResponses.TodayResponse> todayResp =
        restTemplate.exchange(
            baseUrl() + "/me/today",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            TodayResponses.TodayResponse.class);
    assertThat(todayResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    TodayResponses.TodayResponse today = todayResp.getBody();
    assertThat(today).isNotNull();
    assertThat(today.enrollmentId()).isEqualTo(enrollmentId);
    assertThat(today.enrollmentVersion()).isEqualTo(1L);
    assertThat(today.progress()).isNotNull();
    assertThat(today.progress().totalRequiredActivities()).isEqualTo(19);
  }

  @Test
  @DisplayName("GET /api/v1/me/today returns NO_ENROLLMENT when user has no focused enrollment")
  void testTodayNoEnrollment() {
    CsrfSession session = login("usera", "Password123!");
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.COOKIE, session.sessionCookie());

    ResponseEntity<TodayResponses.TodayResponse> response =
        restTemplate.exchange(
            baseUrl() + "/me/today",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            TodayResponses.TodayResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TodayResponses.TodayResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.state()).isEqualTo("NO_ENROLLMENT");
    assertThat(body.enrollmentId()).isNull();
    assertThat(body.primaryAction()).isNull();
    assertThat(body.progress()).isNull();
  }

  @Test
  @DisplayName(
      "Concurrent enrollment: two requests with same key and same payload result in single enrollment")
  void testConcurrentEnrollment_sameKeySamePayload_bothSucceedWithSameEnrollment()
      throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");
    String idempotencyKey = UUID.randomUUID().toString();

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", idempotencyKey);

    HttpEntity<EnrollmentRequests.CreateEnrollmentRequest> requestEntity =
        new HttpEntity<>(createReq, headers);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<ResponseEntity<EnrollmentResponses.EnrollmentResponse>> task =
          () ->
              restTemplate.postForEntity(
                  baseUrl() + "/enrollments",
                  requestEntity,
                  EnrollmentResponses.EnrollmentResponse.class);

      Future<ResponseEntity<EnrollmentResponses.EnrollmentResponse>> f1 = executor.submit(task);
      Future<ResponseEntity<EnrollmentResponses.EnrollmentResponse>> f2 = executor.submit(task);

      ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp1 = f1.get(10, TimeUnit.SECONDS);
      ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp2 = f2.get(10, TimeUnit.SECONDS);

      assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(resp1.getBody()).isNotNull();
      assertThat(resp2.getBody()).isNotNull();
      assertThat(resp1.getBody().id()).isEqualTo(resp2.getBody().id());

      List<Enrollment> enrollments = enrollmentRepository.findAll();
      assertThat(enrollments).hasSize(1);
      List<LessonSchedule> schedules =
          lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(resp1.getBody().id());
      assertThat(schedules).hasSize(5);
    } finally {
      executor.shutdown();
    }
  }

  @Test
  @DisplayName(
      "Concurrent enrollment: two requests with different keys for same curriculum return 201 and 409 without 500")
  void testConcurrentEnrollment_differentKeysSameCurriculum_oneSucceedsOneConflict()
      throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    String key1 = UUID.randomUUID().toString();
    String key2 = UUID.randomUUID().toString();

    HttpHeaders headers1 = new HttpHeaders();
    headers1.setContentType(MediaType.APPLICATION_JSON);
    headers1.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers1.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers1.set("Idempotency-Key", key1);

    HttpHeaders headers2 = new HttpHeaders();
    headers2.setContentType(MediaType.APPLICATION_JSON);
    headers2.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers2.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers2.set("Idempotency-Key", key2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<ResponseEntity<String>> f1 =
          executor.submit(
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/enrollments",
                      new HttpEntity<>(createReq, headers1),
                      String.class));
      Future<ResponseEntity<String>> f2 =
          executor.submit(
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/enrollments",
                      new HttpEntity<>(createReq, headers2),
                      String.class));

      ResponseEntity<String> resp1 = f1.get(10, TimeUnit.SECONDS);
      ResponseEntity<String> resp2 = f2.get(10, TimeUnit.SECONDS);

      List<org.springframework.http.HttpStatusCode> statuses =
          List.of(resp1.getStatusCode(), resp2.getStatusCode());
      assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);

      String conflictBody =
          resp1.getStatusCode() == HttpStatus.CONFLICT ? resp1.getBody() : resp2.getBody();
      assertThat(conflictBody).contains("STATE_CONFLICT");

      List<Enrollment> enrollments = enrollmentRepository.findAll();
      assertThat(enrollments).hasSize(1);
    } finally {
      executor.shutdown();
    }
  }

  @Test
  @DisplayName(
      "Transaction rollback: failure mid-flight rolls back enrollment, schedules, focus, and idempotency")
  void testRollbackOnFailureDuringScheduleCreation() {
    CsrfSession sessionA = login("usera", "Password123!");
    String idempotencyKey = UUID.randomUUID().toString();

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", idempotencyKey);

    doThrow(new RuntimeException("Simulated schedule write failure"))
        .when(lessonScheduleRepository)
        .saveAll(any());

    try {
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              baseUrl() + "/enrollments", new HttpEntity<>(createReq, headers), String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

      assertThat(enrollmentRepository.count()).isEqualTo(0);
      assertThat(lessonScheduleRepository.count()).isEqualTo(0);
      LearnerPreferences prefs = preferencesRepository.findById(userAId).orElseThrow();
      assertThat(prefs.getFocusedEnrollmentId()).isNull();
      Integer idempotencyCount =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM idempotency_record WHERE user_id = ? AND idempotency_key = ?",
              Integer.class,
              userAId,
              idempotencyKey);
      assertThat(idempotencyCount).isEqualTo(0);
    } finally {
      Mockito.reset(lessonScheduleRepository);
    }
  }

  @Test
  @DisplayName(
      "Enrollment rejection: attempting to enroll in DRAFT or ARCHIVED revision returns 409 STATE_CONFLICT")
  void testEnrollmentInDraftOrArchivedRevisionRejected() {
    CsrfSession sessionA = login("usera", "Password123!");

    // 1. DRAFT revision
    CurriculumRevision draftRev = revisionRepository.findById(revisionId).orElseThrow();
    CurriculumRevision draft =
        new CurriculumRevision(
            draftRev.getCurriculum(),
            "2.0.0",
            "Draft Course",
            "Desc",
            "Auth",
            "Prov",
            "hash-draft",
            Instant.now(clock));
    draft = revisionRepository.save(draft);

    EnrollmentRequests.CreateEnrollmentRequest draftReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            draft.getId(),
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<String> draftResp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments", new HttpEntity<>(draftReq, headers), String.class);
    assertThat(draftResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(draftResp.getBody()).contains("STATE_CONFLICT");

    // 2. ARCHIVED revision
    CurriculumRevision archived =
        new CurriculumRevision(
            draftRev.getCurriculum(),
            "3.0.0",
            "Archived Course",
            "Desc",
            "Auth",
            "Prov",
            "hash-archived",
            Instant.now(clock));
    archived.archive(Instant.now(clock));
    archived = revisionRepository.save(archived);

    EnrollmentRequests.CreateEnrollmentRequest archivedReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            archived.getId(),
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headersArchived = new HttpHeaders();
    headersArchived.setContentType(MediaType.APPLICATION_JSON);
    headersArchived.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headersArchived.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headersArchived.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<String> archivedResp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(archivedReq, headersArchived),
            String.class);
    assertThat(archivedResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(archivedResp.getBody()).contains("STATE_CONFLICT");
  }

  @Test
  @DisplayName(
      "Existing enrollment on ARCHIVED revision can still be queried via getDetail, getToday, and getLessonSummary")
  void testExistingEnrollmentOnArchivedRevisionCanStillBeRead() {
    CsrfSession sessionA = login("usera", "Password123!");

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> createResp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(createReq, headers),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID enrollmentId = createResp.getBody().id();

    // Now archive the revision
    CurriculumRevision rev = revisionRepository.findById(revisionId).orElseThrow();
    rev.archive(Instant.now(clock));
    revisionRepository.save(rev);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set(HttpHeaders.COOKIE, sessionA.sessionCookie());

    // 1. GET /enrollments/{enrollmentId} still works
    ResponseEntity<EnrollmentResponses.EnrollmentDetailResponse> detailResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId,
            HttpMethod.GET,
            new HttpEntity<>(getHeaders),
            EnrollmentResponses.EnrollmentDetailResponse.class);
    assertThat(detailResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detailResp.getBody().id()).isEqualTo(enrollmentId);

    // 2. GET /me/today still works
    ResponseEntity<TodayResponses.TodayResponse> todayResp =
        restTemplate.exchange(
            baseUrl() + "/me/today",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders),
            TodayResponses.TodayResponse.class);
    assertThat(todayResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(todayResp.getBody().enrollmentId()).isEqualTo(enrollmentId);

    // 3. GET /enrollments/{enrollmentId}/lessons/{lessonId}/summary works even on ARCHIVED
    // revision!
    List<LessonSchedule> schedules =
        lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId);
    UUID lessonId = schedules.get(0).getLessonId();

    ResponseEntity<EnrollmentResponses.LessonSummaryResponse> summaryResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + lessonId + "/summary",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders),
            EnrollmentResponses.LessonSummaryResponse.class);
    assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(summaryResp.getBody().lessonId()).isEqualTo(lessonId);
    assertThat(summaryResp.getBody().plannedDate()).isEqualTo(LocalDate.of(2026, 10, 26));
  }

  @Test
  @DisplayName("Schedule calculation across London DST transition and month-end boundary")
  void testTimezoneLondonDstAndMonthEndScheduleCalculation() {
    CsrfSession sessionA = login("usera", "Password123!");

    // Start on Friday 2026-10-23 in Europe/London.
    // Sunday 2026-10-25 is DST fallback in London.
    // Saturday 2026-10-31 is month-end.
    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 23),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            45,
            "Europe/London");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(createReq, headers),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID enrollmentId = resp.getBody().id();

    List<LessonSchedule> schedules =
        lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId);
    assertThat(schedules).hasSize(5);
    assertThat(schedules.get(0).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 23)); // Fri
    assertThat(schedules.get(1).getPlannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 26)); // Mon (after DST change on 2026-10-25)
    assertThat(schedules.get(2).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(schedules.get(3).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 30)); // Fri
    assertThat(schedules.get(4).getPlannedDate())
        .isEqualTo(LocalDate.of(2026, 11, 2)); // Mon (after month-end transition)
  }

  @Test
  @DisplayName(
      "GET /enrollments/{enrollmentId}/lessons/{lessonId}/summary returns 404 when lesson not in enrollment revision")
  void testLessonSummary_lessonMismatch_returnsNotFound() {
    CsrfSession sessionA = login("usera", "Password123!");

    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(createReq, headers),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID enrollmentId = resp.getBody().id();

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set(HttpHeaders.COOKIE, sessionA.sessionCookie());

    UUID randomLessonId = UUID.randomUUID();
    ResponseEntity<String> notFoundResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + randomLessonId + "/summary",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders),
            String.class);
    assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName(
      "R4: SQL query count is constant across dataset sizes and heavy columns are not selected")
  void testQueryCountAndNoHeavyColumnsSelectedForTodayAndListEnrollments() {
    CsrfSession sessionA = login("usera", "Password123!");

    // 1. Create Enrollment 1 for User A
    EnrollmentRequests.CreateEnrollmentRequest req1 =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers1 = new HttpHeaders();
    headers1.setContentType(MediaType.APPLICATION_JSON);
    headers1.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers1.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers1.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp1 =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(req1, headers1),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    HttpHeaders getHeadersA = new HttpHeaders();
    getHeadersA.set(HttpHeaders.COOKIE, sessionA.sessionCookie());

    // --- TEST TODAY DASHBOARD ---
    SqlStatementInspector.startRecording();
    ResponseEntity<TodayResponses.TodayResponse> todayResp =
        restTemplate.exchange(
            baseUrl() + "/me/today",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            TodayResponses.TodayResponse.class);
    List<String> todayQueries = SqlStatementInspector.stopRecording();

    assertThat(todayResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(todayQueries).isNotEmpty();

    // Verify NO heavy columns in ANY today query
    for (String q : todayQueries) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("Today query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("Today query must not select instructions: " + q)
          .doesNotContain("instructions");
      assertThat(lower)
          .as("Today query must not select skill_tags: " + q)
          .doesNotContain("skill_tags");
      assertThat(lower)
          .as("Today query must not select user_response: " + q)
          .doesNotContain("user_response");
    }

    // --- TEST LIST ENROLLMENTS SCALING (1 ENROLLMENT VS MULTIPLE ENROLLMENTS) ---
    // Measure with 1 enrollment
    SqlStatementInspector.startRecording();
    ResponseEntity<List> listResp1 =
        restTemplate.exchange(
            baseUrl() + "/me/enrollments",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            List.class);
    List<String> listQueries1 = SqlStatementInspector.stopRecording();
    assertThat(listResp1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp1.getBody()).hasSize(1);
    int queryCountWith1Enrollment = listQueries1.size();

    // Verify NO heavy columns in list queries
    for (String q : listQueries1) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("List query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("List query must not select instructions: " + q)
          .doesNotContain("instructions");
      assertThat(lower)
          .as("List query must not select skill_tags: " + q)
          .doesNotContain("skill_tags");
      assertThat(lower)
          .as("List query must not select user_response: " + q)
          .doesNotContain("user_response");
    }

    // Now create a 2nd curriculum & enrollment for User A with a dataset larger than pilot
    // (10 lessons and 20 activities vs 5 lessons and 10 activities in pilot)
    Curriculum curr2 = new Curriculum("pilot-2", "en-GB", Instant.now(clock));
    curr2 = curriculumRepository.save(curr2);
    CurriculumRevision rev2 =
        new CurriculumRevision(
            curr2, "1.0.0", "Pilot 2 Course", "Desc", "Auth", "Prov", "hash-2", Instant.now(clock));
    rev2 = revisionRepository.save(rev2);

    CurriculumPhase p2 =
        curriculumPhaseRepository.save(new CurriculumPhase(rev2, "p2", "Phase 2", 1));
    CurriculumWeek w2 =
        curriculumWeekRepository.save(new CurriculumWeek(p2, "w2", 1, 1, "Week 2", "Goal"));

    for (int i = 1; i <= 10; i++) {
      Lesson l =
          lessonRepository.save(
              new Lesson(
                  w2, "l2-" + i, i, "L2 Lesson " + i, 20, true, "Heavy Markdown Content " + i));
      activityRepository.save(
          new Activity(
              l,
              "a2-" + i + "-1",
              1,
              "Act " + i + ".1",
              ActivityType.READING,
              true,
              CompletionPolicy.MANUAL_CHECK,
              "Heavy Instructions " + i + ".1",
              "tag1,tag2"));
      activityRepository.save(
          new Activity(
              l,
              "a2-" + i + "-2",
              2,
              "Act " + i + ".2",
              ActivityType.PRACTICE,
              true,
              CompletionPolicy.MANUAL_CHECK,
              "Heavy Instructions " + i + ".2",
              "tag3,tag4"));
    }

    rev2.publish(Instant.now(clock));
    rev2 = revisionRepository.save(rev2);

    // Enroll User A in 2nd curriculum
    EnrollmentRequests.CreateEnrollmentRequest req2 =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curr2.getId(),
            rev2.getId(),
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");
    HttpHeaders headers2 = new HttpHeaders();
    headers2.setContentType(MediaType.APPLICATION_JSON);
    headers2.set("X-CSRF-TOKEN", sessionA.csrfToken());
    headers2.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    headers2.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp2 =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(req2, headers2),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // --- TEST TODAY QUERY COUNT: PILOT (5 lessons) VS LARGE DATASET (10 lessons) ---
    SqlStatementInspector.startRecording();
    ResponseEntity<TodayResponses.TodayResponse> todayResp1 =
        restTemplate.exchange(
            baseUrl() + "/me/today?enrollmentId=" + resp1.getBody().id(),
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            TodayResponses.TodayResponse.class);
    List<String> todayQueries1 = SqlStatementInspector.stopRecording();
    assertThat(todayResp1.getStatusCode()).isEqualTo(HttpStatus.OK);

    SqlStatementInspector.startRecording();
    ResponseEntity<TodayResponses.TodayResponse> todayResp2 =
        restTemplate.exchange(
            baseUrl() + "/me/today?enrollmentId=" + resp2.getBody().id(),
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            TodayResponses.TodayResponse.class);
    List<String> todayQueries2 = SqlStatementInspector.stopRecording();
    assertThat(todayResp2.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Today query count does NOT increase when number of lessons doubles (constant O(1))
    assertThat(todayQueries2.size())
        .as(
            "Today query count must remain constant O(1) across dataset sizes (5 lessons vs 10 lessons)")
        .isEqualTo(todayQueries1.size());

    for (String q : todayQueries2) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("Today query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("Today query must not select instructions: " + q)
          .doesNotContain("instructions");
      assertThat(lower)
          .as("Today query must not select skill_tags: " + q)
          .doesNotContain("skill_tags");
      assertThat(lower)
          .as("Today query must not select user_response: " + q)
          .doesNotContain("user_response");
    }

    // --- TEST LIST ENROLLMENTS SCALING (1 ENROLLMENT VS 2 ENROLLMENTS ACROSS 15 LESSONS) ---
    // Measure with 2 enrollments across 2 different curricula and revisions
    SqlStatementInspector.startRecording();
    ResponseEntity<List> listResp2 =
        restTemplate.exchange(
            baseUrl() + "/me/enrollments",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            List.class);
    List<String> listQueries2 = SqlStatementInspector.stopRecording();
    assertThat(listResp2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp2.getBody()).hasSize(2);
    int queryCountWith2Enrollments = listQueries2.size();

    // Query count with 2 enrollments (15 total lessons) is IDENTICAL to query count with 1
    // enrollment (5 lessons)!
    assertThat(queryCountWith2Enrollments)
        .as(
            "Query count must remain constant O(1) regardless of enrollment count (no N+1 per enrollment or lesson)")
        .isEqualTo(queryCountWith1Enrollment);

    // Verify again that NO heavy columns are present in any query
    for (String q : listQueries2) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("List query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("List query must not select instructions: " + q)
          .doesNotContain("instructions");
      assertThat(lower)
          .as("List query must not select skill_tags: " + q)
          .doesNotContain("skill_tags");
      assertThat(lower)
          .as("List query must not select user_response: " + q)
          .doesNotContain("user_response");
    }

    // --- TEST LESSON SUMMARY QUERY: NO HEAVY COLUMNS SELECTED ---
    UUID firstLessonId =
        lessonRepository.findLessonSummariesByRevisionId(revisionId).get(0).getId();
    SqlStatementInspector.startRecording();
    ResponseEntity<EnrollmentResponses.LessonSummaryResponse> summaryResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + resp1.getBody().id()
                + "/lessons/"
                + firstLessonId
                + "/summary",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            EnrollmentResponses.LessonSummaryResponse.class);
    List<String> summaryQueries = SqlStatementInspector.stopRecording();
    assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    for (String q : summaryQueries) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("Lesson summary query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("Lesson summary query must not select instructions: " + q)
          .doesNotContain("instructions");
      assertThat(lower)
          .as("Lesson summary query must not select skill_tags: " + q)
          .doesNotContain("skill_tags");
      assertThat(lower)
          .as("Lesson summary query must not select user_response: " + q)
          .doesNotContain("user_response");
    }
  }

  @Test
  @DisplayName(
      "G4-01: GET /enrollments/{id}/schedule respects range defaults, validation, ownership, and no-mutation")
  void g4_01_scheduleEndpoint_comprehensiveIntegrationTest() throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");
    CsrfSession sessionB = login("userb", "Password123!");

    // 1. User A creates enrollment (Mon/Wed/Fri, starting 2026-10-26 which is Monday)
    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders postHeaders = new HttpHeaders();
    postHeaders.setContentType(MediaType.APPLICATION_JSON);
    postHeaders.set(HttpHeaders.COOKIE, sessionA.sessionCookie());
    postHeaders.set("X-CSRF-TOKEN", sessionA.csrfToken());
    postHeaders.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> createResp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(createReq, postHeaders),
            EnrollmentResponses.EnrollmentResponse.class);

    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID enrollmentId = createResp.getBody().id();

    // 2. Verify estimatedEndDate in GET /enrollments/{id}
    HttpHeaders getHeadersA = new HttpHeaders();
    getHeadersA.set(HttpHeaders.COOKIE, sessionA.sessionCookie());

    ResponseEntity<EnrollmentResponses.EnrollmentDetailResponse> detailResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId,
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            EnrollmentResponses.EnrollmentDetailResponse.class);

    assertThat(detailResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    LocalDate estimatedEndDate = detailResp.getBody().estimatedEndDate();
    assertThat(estimatedEndDate).isNotNull();

    // Verify it matches DB MAX(planned_date)
    LocalDate maxDbDate =
        lessonScheduleRepository.findMaxPlannedDateByEnrollmentId(enrollmentId).orElseThrow();
    assertThat(estimatedEndDate).isEqualTo(maxDbDate);

    // 3. GET /enrollments/{id}/schedule without from/to: defaults to current week
    ResponseEntity<EnrollmentResponses.LessonScheduleResponse[]> defaultScheduleResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/schedule",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            EnrollmentResponses.LessonScheduleResponse[].class);

    assertThat(defaultScheduleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(defaultScheduleResp.getBody()).isNotNull();

    // 4. GET /enrollments/{id}/schedule with explicit range covering full schedule
    ResponseEntity<EnrollmentResponses.LessonScheduleResponse[]> fullScheduleResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/schedule?from=2026-10-26&to="
                + estimatedEndDate,
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            EnrollmentResponses.LessonScheduleResponse[].class);

    assertThat(fullScheduleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    EnrollmentResponses.LessonScheduleResponse[] items = fullScheduleResp.getBody();
    assertThat(items).isNotEmpty();
    assertThat(items[items.length - 1].plannedDate()).isEqualTo(estimatedEndDate);

    // Pedagogical ordering check: plannedDate asc
    for (int i = 0; i < items.length - 1; i++) {
      assertThat(items[i].plannedDate()).isBeforeOrEqualTo(items[i + 1].plannedDate());
    }

    // 5. Ownership isolation: User B attempting to view User A's schedule -> 404
    HttpHeaders getHeadersB = new HttpHeaders();
    getHeadersB.set(HttpHeaders.COOKIE, sessionB.sessionCookie());

    ResponseEntity<String> userBResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/schedule",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersB),
            String.class);

    assertThat(userBResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    // 6. Validation: from > to -> 400 ProblemDetails
    ResponseEntity<String> invertedResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/schedule?from=2026-11-10&to=2026-11-01",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            String.class);
    assertThat(invertedResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 7. Validation: range > 366 days -> 400 ProblemDetails
    ResponseEntity<String> tooWideResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/schedule?from=2026-01-01&to=2027-01-05",
            HttpMethod.GET,
            new HttpEntity<>(getHeadersA),
            String.class);
    assertThat(tooWideResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 8. No-mutation assertion: version and updated_at on enrollment must remain unchanged
    Enrollment enrollmentBefore = enrollmentRepository.findById(enrollmentId).orElseThrow();
    long versionBefore = enrollmentBefore.getVersion();
    Instant updatedAtBefore = enrollmentBefore.getUpdatedAt();

    // Call schedule multiple times
    restTemplate.exchange(
        baseUrl() + "/enrollments/" + enrollmentId + "/schedule",
        HttpMethod.GET,
        new HttpEntity<>(getHeadersA),
        EnrollmentResponses.LessonScheduleResponse[].class);

    Enrollment enrollmentAfter = enrollmentRepository.findById(enrollmentId).orElseThrow();
    assertThat(enrollmentAfter.getVersion()).isEqualTo(versionBefore);
    assertThat(enrollmentAfter.getUpdatedAt()).isEqualTo(updatedAtBefore);

    // 9. Query efficiency & projection test: assert no heavy columns loaded
    SqlStatementInspector.startRecording();
    restTemplate.exchange(
        baseUrl() + "/enrollments/" + enrollmentId + "/schedule?from=2026-10-26&to=2026-11-01",
        HttpMethod.GET,
        new HttpEntity<>(getHeadersA),
        EnrollmentResponses.LessonScheduleResponse[].class);
    List<String> scheduleQueries = SqlStatementInspector.stopRecording();

    for (String q : scheduleQueries) {
      String lower = q.toLowerCase();
      assertThat(lower)
          .as("Schedule query must not select content_markdown: " + q)
          .doesNotContain("content_markdown");
      assertThat(lower)
          .as("Schedule query must not select user_response: " + q)
          .doesNotContain("user_response");
      assertThat(lower)
          .as("Schedule query must not select instructions: " + q)
          .doesNotContain("instructions");
    }
  }
}
