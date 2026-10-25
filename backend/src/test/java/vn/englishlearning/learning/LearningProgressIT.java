package vn.englishlearning.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.englishlearning.contentops.application.ImportCurriculumPackageService;
import vn.englishlearning.contentops.application.PublishCurriculumRevisionService;
import vn.englishlearning.contentops.domain.ImportSummary;
import vn.englishlearning.contentops.domain.PublishResult;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.api.EnrollmentRequests;
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.api.CsrfTokenResponse;
import vn.englishlearning.identity.api.LoginRequest;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.learning.api.LearningProgressRequests.SubmitResponseRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.UpdateProgressRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonActivitiesResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressSummaryResponse;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.learning.infrastructure.LearningEventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class LearningProgressIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_progress_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
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
  @Autowired private LessonRepository lessonRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private LessonScheduleRepository lessonScheduleRepository;
  @Autowired private ActivityProgressRepository activityProgressRepository;
  @MockitoSpyBean private LearningEventRepository learningEventRepository;
  @MockitoSpyBean private Clock clock;

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

  private HttpHeaders authHeaders(CsrfSession session) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", session.csrfToken());
    headers.set(HttpHeaders.COOKIE, session.sessionCookie());
    return headers;
  }

  private UUID createEnrollmentForUser(CsrfSession session) {
    EnrollmentRequests.CreateEnrollmentRequest createReq =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    HttpHeaders headers = authHeaders(session);
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    ResponseEntity<EnrollmentResponses.EnrollmentResponse> resp =
        restTemplate.postForEntity(
            baseUrl() + "/enrollments",
            new HttpEntity<>(createReq, headers),
            EnrollmentResponses.EnrollmentResponse.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return resp.getBody().id();
  }

  @Test
  @DisplayName(
      "Full progress workflow: check, undo, submit reflection, stale version, no-op, summary")
  void testFullProgressWorkflow() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    // Find lesson 1 and its activities
    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Lesson lesson1 = lessons.stream().filter(l -> l.getOrderIndex() == 1).findFirst().orElseThrow();
    List<Activity> activities =
        activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson1.getId());

    Activity manualAct =
        activities.stream()
            .filter(a -> a.getActivityType() == ActivityType.READING)
            .findFirst()
            .orElseThrow();
    Activity reflectionAct =
        activities.stream()
            .filter(
                a ->
                    a.getActivityType() == ActivityType.REFLECTION
                        || a.getActivityType() == ActivityType.PRACTICE)
            .findFirst()
            .orElseThrow();

    HttpHeaders headers = authHeaders(sessionA);

    // 1. GET activities
    ResponseEntity<LessonActivitiesResponse> getActsResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/lessons/"
                + lesson1.getId()
                + "/activities",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            LessonActivitiesResponse.class);
    assertThat(getActsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getActsResp.getBody()).isNotNull();
    assertThat(getActsResp.getBody().enrollmentVersion()).isEqualTo(1L);
    assertThat(getActsResp.getBody().activities()).isNotEmpty();
    assertThat(getActsResp.getBody().activities().get(0).status())
        .isEqualTo(ActivityStatus.NOT_STARTED);

    // 2. PUT manual check complete
    UpdateProgressRequest completeReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);
    ResponseEntity<ProgressResponse> completeResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + manualAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(completeReq, headers),
            ProgressResponse.class);
    assertThat(completeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(completeResp.getBody().status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(completeResp.getBody().enrollmentVersion()).isEqualTo(2L);

    // 3. Stale version returns 409 VERSION_CONFLICT
    UpdateProgressRequest staleReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);
    ResponseEntity<String> staleResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + manualAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(staleReq, headers),
            String.class);
    assertThat(staleResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(staleResp.getBody()).contains("VERSION_CONFLICT");

    // 4. No-op returns 200 without bumping version or duplicate event
    int eventCountBefore =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);

    UpdateProgressRequest noopReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 2L);
    ResponseEntity<ProgressResponse> noopResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + manualAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(noopReq, headers),
            ProgressResponse.class);
    assertThat(noopResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(noopResp.getBody().status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(noopResp.getBody().enrollmentVersion()).isEqualTo(2L);

    int eventCountAfter =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);
    assertThat(eventCountAfter).isEqualTo(eventCountBefore);

    // 5. Submit response for reflection activity
    // Blank response -> 400 RESPONSE_REQUIRED
    SubmitResponseRequest blankReq = new SubmitResponseRequest("   ", 2L);
    ResponseEntity<String> blankResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response/submit",
            HttpMethod.POST,
            new HttpEntity<>(blankReq, headers),
            String.class);
    assertThat(blankResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Valid response -> 200 OK
    SubmitResponseRequest validSubmitReq =
        new SubmitResponseRequest("Phản tư: Tôi đã hiểu rõ các âm hữu thanh.", 2L);
    ResponseEntity<ProgressResponse> submitResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response/submit",
            HttpMethod.POST,
            new HttpEntity<>(validSubmitReq, headers),
            ProgressResponse.class);
    assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(submitResp.getBody().status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(submitResp.getBody().enrollmentVersion()).isEqualTo(3L);

    // Verify response text stored in DB
    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, reflectionAct.getId())
            .orElseThrow();
    assertThat(progress.getUserResponse()).isEqualTo("Phản tư: Tôi đã hiểu rõ các âm hữu thanh.");

    // 6. Summary endpoint
    ResponseEntity<ProgressSummaryResponse> summaryResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/summary",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ProgressSummaryResponse.class);
    assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(summaryResp.getBody().completedRequiredActivities()).isGreaterThanOrEqualTo(2);
    assertThat(summaryResp.getBody().enrollmentVersion()).isEqualTo(3L);

    // 7. Undo manual activity
    UpdateProgressRequest undoReq = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 3L);
    ResponseEntity<ProgressResponse> undoResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + manualAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(undoReq, headers),
            ProgressResponse.class);
    assertThat(undoResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(undoResp.getBody().status()).isEqualTo(ActivityStatus.NOT_STARTED);
    assertThat(undoResp.getBody().enrollmentVersion()).isEqualTo(4L);

    ActivityProgress undoneProgress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, manualAct.getId())
            .orElseThrow();
    assertThat(undoneProgress.getCompletedAt()).isNull();
  }

  @Test
  @DisplayName(
      "Concurrency: concurrent updates with same expectedVersion results in 1 success, 1 conflict")
  void testConcurrentProgressUpdate() throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Lesson lesson1 = lessons.stream().filter(l -> l.getOrderIndex() == 1).findFirst().orElseThrow();
    Activity act1 = activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson1.getId()).get(0);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<ResponseEntity<String>> task =
          () -> {
            HttpHeaders h = authHeaders(sessionA);
            UpdateProgressRequest req = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);
            return restTemplate.exchange(
                baseUrl()
                    + "/enrollments/"
                    + enrollmentId
                    + "/activities/"
                    + act1.getId()
                    + "/progress",
                HttpMethod.PUT,
                new HttpEntity<>(req, h),
                String.class);
          };

      Future<ResponseEntity<String>> f1 = executor.submit(task);
      Future<ResponseEntity<String>> f2 = executor.submit(task);

      ResponseEntity<String> r1 = f1.get(10, TimeUnit.SECONDS);
      ResponseEntity<String> r2 = f2.get(10, TimeUnit.SECONDS);

      List<HttpStatus> statuses =
          List.of((HttpStatus) r1.getStatusCode(), (HttpStatus) r2.getStatusCode());
      assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);

      String conflictBody = r1.getStatusCode() == HttpStatus.CONFLICT ? r1.getBody() : r2.getBody();
      assertThat(conflictBody).contains("VERSION_CONFLICT");

      Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
      assertThat(enrollment.getVersion()).isEqualTo(2L);
    } finally {
      executor.shutdown();
    }
  }

  @Test
  @DisplayName("Rollback: failure during event logging rolls back progress and version")
  void testRollbackOnFailureDuringEventLogging() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Activity act1 =
        activityRepository.findByLessonIdOrderByOrderIndexAsc(lessons.get(0).getId()).get(0);

    HttpHeaders headers = authHeaders(sessionA);
    UpdateProgressRequest completeReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    doThrow(new RuntimeException("Simulated event save failure"))
        .when(learningEventRepository)
        .save(any());

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              baseUrl()
                  + "/enrollments/"
                  + enrollmentId
                  + "/activities/"
                  + act1.getId()
                  + "/progress",
              HttpMethod.PUT,
              new HttpEntity<>(completeReq, headers),
              String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

      // Verify rollback: activity_progress was NOT saved
      assertThat(
              activityProgressRepository.findByEnrollmentIdAndActivityId(
                  enrollmentId, act1.getId()))
          .isEmpty();

      // Enrollment version was NOT bumped
      Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
      assertThat(enrollment.getVersion()).isEqualTo(1L);
    } finally {
      Mockito.reset(learningEventRepository);
    }
  }

  @Test
  @DisplayName("Cross-user isolation: User B cannot access or update User A's progress")
  void testCrossUserIsolation() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID userAEnrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Lesson lesson1 = lessons.get(0);
    Activity act1 = activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson1.getId()).get(0);

    CsrfSession sessionB = login("userb", "Password123!");
    HttpHeaders headersB = authHeaders(sessionB);

    // GET summary
    ResponseEntity<String> summaryResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + userAEnrollmentId + "/summary",
            HttpMethod.GET,
            new HttpEntity<>(headersB),
            String.class);
    assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(summaryResp.getBody()).contains("RESOURCE_NOT_FOUND");

    // GET activities
    ResponseEntity<String> activitiesResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + userAEnrollmentId
                + "/lessons/"
                + lesson1.getId()
                + "/activities",
            HttpMethod.GET,
            new HttpEntity<>(headersB),
            String.class);
    assertThat(activitiesResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    // PUT progress
    UpdateProgressRequest updateReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);
    ResponseEntity<String> updateResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + userAEnrollmentId
                + "/activities/"
                + act1.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(updateReq, headersB),
            String.class);
    assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("PostgreSQL schema: learning_event stores local_date, time_zone, and JSONB payload")
  void testLearningEventSchemaAndPersistence() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Activity act1 =
        activityRepository.findByLessonIdOrderByOrderIndexAsc(lessons.get(0).getId()).get(0);

    HttpHeaders headers = authHeaders(sessionA);
    UpdateProgressRequest completeReq = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    ResponseEntity<ProgressResponse> resp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + act1.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(completeReq, headers),
            ProgressResponse.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Query learning_event in PostgreSQL directly
    Map<String, Object> eventRow =
        jdbcTemplate.queryForMap(
            "SELECT event_type, local_date, time_zone, payload FROM learning_event WHERE enrollment_id = ?",
            enrollmentId);

    assertThat(eventRow.get("event_type")).isEqualTo("COMPLETED");
    assertThat(eventRow.get("time_zone")).isEqualTo("Asia/Ho_Chi_Minh");
    assertThat(eventRow.get("local_date")).isNotNull();
    assertThat(eventRow.get("payload").toString()).contains("COMPLETED");
  }

  @Test
  @DisplayName(
      "Reopen completed enrollment: undoing required activity reopens COMPLETED enrollment to ACTIVE")
  void testReopenCompletedEnrollment() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<vn.englishlearning.curriculum.infrastructure.LessonSummaryDto> lessonSummaries =
        lessonRepository.findLessonSummariesByRevisionId(revisionId);
    java.util.Set<UUID> reqLessonIds =
        lessonSummaries.stream()
            .filter(vn.englishlearning.curriculum.infrastructure.LessonSummaryDto::isRequired)
            .map(vn.englishlearning.curriculum.infrastructure.LessonSummaryDto::getId)
            .collect(java.util.stream.Collectors.toSet());

    List<vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto> activitySummaries =
        activityRepository.findActivitySummariesByRevisionId(revisionId);
    List<UUID> reqActIds =
        activitySummaries.stream()
            .filter(a -> reqLessonIds.contains(a.getLessonId()) && a.isRequired())
            .map(vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto::getId)
            .toList();

    Instant now = Instant.now(clock);
    for (UUID actId : reqActIds) {
      activityProgressRepository.save(
          new ActivityProgress(enrollmentId, actId, ActivityStatus.COMPLETED, now));
    }

    jdbcTemplate.update(
        "UPDATE enrollment SET status = 'COMPLETED', completed_at = ?, version = 5 WHERE id = ?",
        java.sql.Timestamp.from(now),
        enrollmentId);

    // Undoing one required activity should reopen to ACTIVE
    HttpHeaders headers = authHeaders(sessionA);
    UUID actToUndo = reqActIds.get(0);
    UpdateProgressRequest undoReq = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 5L);

    ResponseEntity<ProgressResponse> undoResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/activities/" + actToUndo + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(undoReq, headers),
            ProgressResponse.class);

    assertThat(undoResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(undoResp.getBody().status()).isEqualTo(ActivityStatus.NOT_STARTED);

    Enrollment reopened = enrollmentRepository.findById(enrollmentId).orElseThrow();
    assertThat(reopened.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(reopened.getCompletedAt()).isNull();

    // Now re-complete it and test reopening conflict when another active enrollment exists
    jdbcTemplate.update(
        "UPDATE enrollment SET status = 'COMPLETED', completed_at = ?, version = 6 WHERE id = ?",
        java.sql.Timestamp.from(now),
        enrollmentId);

    // Create a 2nd enrollment for same user & curriculum
    createEnrollmentForUser(sessionA);

    // Now attempting to undo another required activity on enrollment 1 must fail with 409
    // STATE_CONFLICT
    UUID secondActToUndo = reqActIds.get(1);
    UpdateProgressRequest undoConflictReq =
        new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 6L);
    ResponseEntity<String> conflictResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + secondActToUndo
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(undoConflictReq, headers),
            String.class);
    assertThat(conflictResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(conflictResp.getBody()).contains("STATE_CONFLICT");
  }

  @Test
  @DisplayName(
      "Meaningful progress change advances version even when clock instant does not advance")
  void testMeaningfulChangeAdvancesVersionWhenClockDoesNotAdvance() {
    CsrfSession session = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(session);
    var before = enrollmentRepository.findById(enrollmentId).orElseThrow();
    Instant unchanged = before.getUpdatedAt();
    doReturn(unchanged).when(clock).instant();

    Activity readingAct =
        activityRepository.findByLessonWeekPhaseRevisionId(revisionId).stream()
            .filter(a -> a.getActivityType() == ActivityType.READING)
            .findFirst()
            .orElseThrow();
    HttpHeaders headers = authHeaders(session);

    ResponseEntity<ProgressResponse> response =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + readingAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateProgressRequest(ActivityStatus.COMPLETED, before.getVersion()), headers),
            ProgressResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().enrollmentVersion())
        .as("meaningful progress change must advance enrollment version even at same instant")
        .isEqualTo(before.getVersion() + 1);
    assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getVersion())
        .isEqualTo(before.getVersion() + 1);
  }

  @Test
  @DisplayName("No-op mutation with matching version does not advance version or create events")
  void testNoOpDoesNotAdvanceVersionWhenClockDoesNotAdvance() {
    CsrfSession session = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(session);
    var before = enrollmentRepository.findById(enrollmentId).orElseThrow();
    Instant unchanged = before.getUpdatedAt();
    doReturn(unchanged).when(clock).instant();

    Activity readingAct =
        activityRepository.findByLessonWeekPhaseRevisionId(revisionId).stream()
            .filter(a -> a.getActivityType() == ActivityType.READING)
            .findFirst()
            .orElseThrow();
    HttpHeaders headers = authHeaders(session);

    // Initial complete
    ResponseEntity<ProgressResponse> firstResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + readingAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateProgressRequest(ActivityStatus.COMPLETED, before.getVersion()), headers),
            ProgressResponse.class);
    assertThat(firstResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Long updatedVersion = firstResp.getBody().enrollmentVersion();
    assertThat(updatedVersion).isEqualTo(before.getVersion() + 1);

    int eventCountBefore =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);

    // No-op: complete again with updatedVersion
    ResponseEntity<ProgressResponse> noopResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + readingAct.getId()
                + "/progress",
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateProgressRequest(ActivityStatus.COMPLETED, updatedVersion), headers),
            ProgressResponse.class);
    assertThat(noopResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(noopResp.getBody().enrollmentVersion()).isEqualTo(updatedVersion);
    assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getVersion())
        .isEqualTo(updatedVersion);

    int eventCountAfter =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);
    assertThat(eventCountAfter).isEqualTo(eventCountBefore);
  }

  @Test
  @DisplayName(
      "Submitting response advances version even when clock does not advance; no-op with identical text does not advance")
  void testSubmitResponseAdvancesVersionWhenClockDoesNotAdvance() {
    CsrfSession session = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(session);
    var before = enrollmentRepository.findById(enrollmentId).orElseThrow();
    Instant unchanged = before.getUpdatedAt();
    doReturn(unchanged).when(clock).instant();

    Activity reflectionAct =
        activityRepository.findByLessonWeekPhaseRevisionId(revisionId).stream()
            .filter(
                a ->
                    a.getActivityType() == ActivityType.REFLECTION
                        || a.getActivityType() == ActivityType.PRACTICE)
            .findFirst()
            .orElseThrow();
    HttpHeaders headers = authHeaders(session);

    // Submit response text
    SubmitResponseRequest submitReq =
        new SubmitResponseRequest("Câu trả lời lần 1", before.getVersion());
    ResponseEntity<ProgressResponse> resp1 =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response/submit",
            HttpMethod.POST,
            new HttpEntity<>(submitReq, headers),
            ProgressResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp1.getBody().enrollmentVersion()).isEqualTo(before.getVersion() + 1);

    // Resubmit identical response text -> no-op: same version, no extra event
    int eventCountBefore =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);
    SubmitResponseRequest identicalReq =
        new SubmitResponseRequest("Câu trả lời lần 1", resp1.getBody().enrollmentVersion());
    ResponseEntity<ProgressResponse> noopResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response/submit",
            HttpMethod.POST,
            new HttpEntity<>(identicalReq, headers),
            ProgressResponse.class);
    assertThat(noopResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(noopResp.getBody().enrollmentVersion())
        .isEqualTo(resp1.getBody().enrollmentVersion());
    int eventCountAfter =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM learning_event WHERE enrollment_id = ?",
            Integer.class,
            enrollmentId);
    assertThat(eventCountAfter).isEqualTo(eventCountBefore);

    // Resubmit changed response text -> advances version
    SubmitResponseRequest changedReq =
        new SubmitResponseRequest(
            "Câu trả lời lần 2 đã sửa đổi", resp1.getBody().enrollmentVersion());
    ResponseEntity<ProgressResponse> resp2 =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response/submit",
            HttpMethod.POST,
            new HttpEntity<>(changedReq, headers),
            ProgressResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp2.getBody().enrollmentVersion())
        .isEqualTo(resp1.getBody().enrollmentVersion() + 1);
    assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getVersion())
        .isEqualTo(resp1.getBody().enrollmentVersion() + 1);
  }

  @Test
  @DisplayName(
      "Concurrent mutations on different activities with same expectedVersion allow at most one commit")
  void testConcurrentMutationsOnDifferentActivitiesWithSameExpectedVersion() throws Exception {
    CsrfSession session = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(session);
    var before = enrollmentRepository.findById(enrollmentId).orElseThrow();
    Long expectedVersion = before.getVersion();

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    Lesson lesson1 = lessons.stream().filter(l -> l.getOrderIndex() == 1).findFirst().orElseThrow();
    List<Activity> activities =
        activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson1.getId());
    List<Activity> manualActs =
        activityRepository.findByLessonWeekPhaseRevisionId(revisionId).stream()
            .filter(
                a ->
                    a.getActivityType() == ActivityType.READING
                        || a.getActivityType() == ActivityType.EXTERNAL_RESOURCE)
            .toList();
    assertThat(manualActs).hasSizeGreaterThanOrEqualTo(2);
    Activity act1 = manualActs.get(0);
    Activity act2 = manualActs.get(1);

    HttpHeaders headers = authHeaders(session);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);

    Callable<ResponseEntity<String>> task1 =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          return restTemplate.exchange(
              baseUrl()
                  + "/enrollments/"
                  + enrollmentId
                  + "/activities/"
                  + act1.getId()
                  + "/progress",
              HttpMethod.PUT,
              new HttpEntity<>(
                  new UpdateProgressRequest(ActivityStatus.COMPLETED, expectedVersion), headers),
              String.class);
        };

    Callable<ResponseEntity<String>> task2 =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          return restTemplate.exchange(
              baseUrl()
                  + "/enrollments/"
                  + enrollmentId
                  + "/activities/"
                  + act2.getId()
                  + "/progress",
              HttpMethod.PUT,
              new HttpEntity<>(
                  new UpdateProgressRequest(ActivityStatus.COMPLETED, expectedVersion), headers),
              String.class);
        };

    Future<ResponseEntity<String>> future1 = executor.submit(task1);
    Future<ResponseEntity<String>> future2 = executor.submit(task2);

    readyLatch.await(5, TimeUnit.SECONDS);
    startLatch.countDown();

    ResponseEntity<String> resp1 = future1.get(10, TimeUnit.SECONDS);
    ResponseEntity<String> resp2 = future2.get(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(resp1.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CONFLICT);
    assertThat(resp2.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CONFLICT);
    assertThat(resp1.getStatusCode()).isNotEqualTo(resp2.getStatusCode());

    ResponseEntity<String> conflictResp =
        resp1.getStatusCode() == HttpStatus.CONFLICT ? resp1 : resp2;
    assertThat(conflictResp.getBody()).contains("VERSION_CONFLICT");

    // Version must have advanced by exactly 1
    assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getVersion())
        .isEqualTo(expectedVersion + 1);
  }

  @Test
  @DisplayName(
      "Race between creating new enrollment and undoing completed enrollment serializes on user lock and returns 409 STATE_CONFLICT")
  void testConcurrentRaceBetweenCreateEnrollmentAndUndoCompletedEnrollment() throws Exception {
    CsrfSession session = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(session);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revisionId);
    List<UUID> reqLessonIds =
        lessons.stream().filter(Lesson::isRequired).map(Lesson::getId).toList();
    List<vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto> activitySummaries =
        activityRepository.findActivitySummariesByRevisionId(revisionId);
    List<UUID> reqActIds =
        activitySummaries.stream()
            .filter(a -> reqLessonIds.contains(a.getLessonId()) && a.isRequired())
            .map(vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto::getId)
            .toList();

    Instant now = Instant.now(clock);
    for (UUID actId : reqActIds) {
      activityProgressRepository.save(
          new ActivityProgress(enrollmentId, actId, ActivityStatus.COMPLETED, now));
    }

    jdbcTemplate.update(
        "UPDATE enrollment SET status = 'COMPLETED', completed_at = ?, version = 10 WHERE id = ?",
        java.sql.Timestamp.from(now),
        enrollmentId);

    HttpHeaders headers = authHeaders(session);
    UUID actToUndo = reqActIds.get(0);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);

    Callable<ResponseEntity<String>> createNewTask =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          EnrollmentRequests.CreateEnrollmentRequest createReq =
              new EnrollmentRequests.CreateEnrollmentRequest(
                  curriculumId,
                  revisionId,
                  LocalDate.of(2026, 10, 26),
                  List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                  30,
                  "Asia/Ho_Chi_Minh");
          HttpHeaders createHeaders = authHeaders(session);
          createHeaders.set("Idempotency-Key", UUID.randomUUID().toString());
          return restTemplate.postForEntity(
              baseUrl() + "/enrollments", new HttpEntity<>(createReq, createHeaders), String.class);
        };

    Callable<ResponseEntity<String>> undoTask =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          UpdateProgressRequest undoReq =
              new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 10L);
          return restTemplate.exchange(
              baseUrl() + "/enrollments/" + enrollmentId + "/activities/" + actToUndo + "/progress",
              HttpMethod.PUT,
              new HttpEntity<>(undoReq, headers),
              String.class);
        };

    Future<ResponseEntity<String>> futureCreate = executor.submit(createNewTask);
    Future<ResponseEntity<String>> futureUndo = executor.submit(undoTask);

    readyLatch.await(5, TimeUnit.SECONDS);
    startLatch.countDown();

    ResponseEntity<String> createResp = futureCreate.get(10, TimeUnit.SECONDS);
    ResponseEntity<String> undoResp = futureUndo.get(10, TimeUnit.SECONDS);
    executor.shutdown();

    // One succeeds (201 or 200), the other must return 409 STATE_CONFLICT. Neither returns 500!
    if (createResp.getStatusCode() == HttpStatus.CREATED) {
      assertThat(undoResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(undoResp.getBody()).contains("STATE_CONFLICT");
    } else {
      assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(createResp.getBody()).contains("STATE_CONFLICT");
      assertThat(undoResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Verify exactly one active enrollment exists in DB
    List<Enrollment> activeEnrollments =
        enrollmentRepository.findByUserIdAndCurriculumIdAndStatusInAndIdNot(
            userAId, curriculumId, List.of(EnrollmentStatus.ACTIVE), UUID.randomUUID());
    assertThat(activeEnrollments).hasSize(1);
  }
}
