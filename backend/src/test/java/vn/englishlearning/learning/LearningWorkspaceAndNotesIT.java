package vn.englishlearning.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.api.EnrollmentRequests;
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.identity.api.CsrfTokenResponse;
import vn.englishlearning.identity.api.LoginRequest;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.learning.api.LearningProgressRequests.SaveResponseDraftRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.SubmitResponseRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.ActivityResponseDto;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonDetailResponse;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.notes.api.CreateNoteRequest;
import vn.englishlearning.notes.api.NotePageResponse;
import vn.englishlearning.notes.api.NoteResponse;
import vn.englishlearning.notes.api.UpdateNoteRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class LearningWorkspaceAndNotesIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_workspace_db")
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
  @Autowired private CurriculumRevisionRepository revisionRepository;
  @Autowired private LessonRepository lessonRepository;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ActivityProgressRepository activityProgressRepository;
  @Autowired private Clock clock;

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
    jdbcTemplate.execute("TRUNCATE TABLE note CASCADE");

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
    assertThat(resp.getBody()).isNotNull();
    return resp.getBody().id();
  }

  @Test
  @DisplayName("GET /enrollments/{enrollmentId}/lessons/{lessonId}: returns complete workspace")
  void getLessonWorkspace_success() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    assertThat(lessons).isNotEmpty();
    Lesson firstLesson = lessons.get(0);

    ResponseEntity<LessonDetailResponse> resp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            LessonDetailResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    LessonDetailResponse body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.lessonId()).isEqualTo(firstLesson.getId());
    assertThat(body.title()).isEqualTo(firstLesson.getTitle());
    assertThat(body.markdownContent()).isNotEmpty();
    assertThat(body.enrollmentVersion()).isEqualTo(1L);
    if (lessons.size() > 1) {
      assertThat(body.nextLessonId()).isEqualTo(lessons.get(1).getId());
    }
    assertThat(body.activities()).isNotEmpty();
    body.activities()
        .forEach(
            act -> {
              assertThat(act.activityId()).isNotNull();
              assertThat(act.status()).isEqualTo(ActivityStatus.NOT_STARTED);
              assertThat(act.resources()).isNotNull();
            });

    // Verify User B cannot access User A's workspace
    CsrfSession sessionB = login("userb", "Password123!");
    ResponseEntity<String> forbiddenResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionB)),
            String.class);
    assertThat(forbiddenResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName(
      "PUT /enrollments/{id}/activities/{id}/response: draft save, reload, no-op, clear, and conflict")
  void activityResponseDraft_lifecycleAndLocking() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    Lesson firstLesson = lessons.get(0);

    List<Activity> activities =
        activityRepository.findActivitiesWithResourcesByLessonId(firstLesson.getId());
    Activity reflectionAct =
        activities.stream()
            .filter(
                a ->
                    a.getActivityType() == ActivityType.REFLECTION
                        || a.getActivityType() == ActivityType.PRACTICE)
            .findFirst()
            .orElse(activities.get(0));

    // 1. Initial draft save with expectedVersion = 1 (initial enrollment version)
    SaveResponseDraftRequest draft1 = new SaveResponseDraftRequest("My first draft text", 1L);
    ResponseEntity<ActivityResponseDto> saveResp1 =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response",
            HttpMethod.PUT,
            new HttpEntity<>(draft1, authHeaders(sessionA)),
            ActivityResponseDto.class);

    assertThat(saveResp1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(saveResp1.getBody()).isNotNull();
    assertThat(saveResp1.getBody().responseText()).isEqualTo("My first draft text");
    assertThat(saveResp1.getBody().enrollmentVersion()).isEqualTo(2L);

    // 2. Reload workspace to verify persistence & IN_PROGRESS status
    ResponseEntity<LessonDetailResponse> workspaceResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            LessonDetailResponse.class);

    assertThat(workspaceResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(workspaceResp.getBody()).isNotNull();
    assertThat(workspaceResp.getBody().enrollmentVersion()).isEqualTo(2L);

    var actDto =
        workspaceResp.getBody().activities().stream()
            .filter(a -> a.activityId().equals(reflectionAct.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(actDto.status()).isEqualTo(ActivityStatus.IN_PROGRESS);
    assertThat(actDto.userResponse()).isEqualTo("My first draft text");

    // 3. No-op: sending same draft text with expectedVersion = 2 returns 200 without bumping
    // version
    SaveResponseDraftRequest noOpDraft = new SaveResponseDraftRequest("My first draft text", 2L);
    ResponseEntity<ActivityResponseDto> noOpResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response",
            HttpMethod.PUT,
            new HttpEntity<>(noOpDraft, authHeaders(sessionA)),
            ActivityResponseDto.class);
    assertThat(noOpResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(noOpResp.getBody().enrollmentVersion()).isEqualTo(2L);

    // 4. Stale version: sending expectedVersion = 1 when version is 2 returns 409 Conflict
    SaveResponseDraftRequest conflictDraft = new SaveResponseDraftRequest("Conflicting draft", 1L);
    ResponseEntity<String> conflictResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response",
            HttpMethod.PUT,
            new HttpEntity<>(conflictDraft, authHeaders(sessionA)),
            String.class);
    assertThat(conflictResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    // 5. Clear draft: sending empty string "" clears the draft and increments version to 3
    SaveResponseDraftRequest clearDraft = new SaveResponseDraftRequest("", 2L);
    ResponseEntity<ActivityResponseDto> clearResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response",
            HttpMethod.PUT,
            new HttpEntity<>(clearDraft, authHeaders(sessionA)),
            ActivityResponseDto.class);
    assertThat(clearResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(clearResp.getBody().enrollmentVersion()).isEqualTo(3L);
    assertThat(clearResp.getBody().responseText()).isEmpty();

    // 6. User B cannot save draft on User A's enrollment
    CsrfSession sessionB = login("userb", "Password123!");
    ResponseEntity<String> forbiddenDraftResp =
        restTemplate.exchange(
            baseUrl()
                + "/enrollments/"
                + enrollmentId
                + "/activities/"
                + reflectionAct.getId()
                + "/response",
            HttpMethod.PUT,
            new HttpEntity<>(new SaveResponseDraftRequest("Hacked", 3L), authHeaders(sessionB)),
            String.class);
    assertThat(forbiddenDraftResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName(
      "Notes CRUD, optimistic locking, idempotent creation, and PostgreSQL unique constraint")
  void notes_crudAndConcurrency() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    Lesson firstLesson = lessons.get(0);

    // 1. Create lesson-level note
    CreateNoteRequest createReq =
        new CreateNoteRequest(enrollmentId, firstLesson.getId(), null, "Lesson 1 initial note");
    ResponseEntity<NoteResponse> createResp =
        restTemplate.postForEntity(
            baseUrl() + "/notes",
            new HttpEntity<>(createReq, authHeaders(sessionA)),
            NoteResponse.class);

    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    NoteResponse createdNote = createResp.getBody();
    assertThat(createdNote).isNotNull();
    assertThat(createdNote.content()).isEqualTo("Lesson 1 initial note");
    assertThat(createdNote.version()).isEqualTo(0L);
    UUID noteId = createdNote.id();

    // 2. Idempotent re-post with exact same content returns 201 with existing note
    ResponseEntity<NoteResponse> idempotentResp =
        restTemplate.postForEntity(
            baseUrl() + "/notes",
            new HttpEntity<>(createReq, authHeaders(sessionA)),
            NoteResponse.class);
    assertThat(idempotentResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(idempotentResp.getBody().id()).isEqualTo(noteId);

    // 3. Conflicting post with different content returns 409 Conflict
    CreateNoteRequest conflictingCreate =
        new CreateNoteRequest(enrollmentId, firstLesson.getId(), null, "Different note content");
    ResponseEntity<String> conflictCreateResp =
        restTemplate.postForEntity(
            baseUrl() + "/notes",
            new HttpEntity<>(conflictingCreate, authHeaders(sessionA)),
            String.class);
    assertThat(conflictCreateResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    // 4. GET /me/notes with filter
    ResponseEntity<NotePageResponse> listResp =
        restTemplate.exchange(
            baseUrl()
                + "/me/notes?enrollmentId="
                + enrollmentId
                + "&lessonId="
                + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            NotePageResponse.class);
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody().items()).hasSize(1);
    assertThat(listResp.getBody().items().get(0).id()).isEqualTo(noteId);

    // 5. PATCH /notes/{id}: stale version returns 409 Conflict
    UpdateNoteRequest stalePatch = new UpdateNoteRequest("Updated text", 99L);
    ResponseEntity<String> stalePatchResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId,
            HttpMethod.PATCH,
            new HttpEntity<>(stalePatch, authHeaders(sessionA)),
            String.class);
    assertThat(stalePatchResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    // 6. PATCH /notes/{id}: valid update increments version to 1
    UpdateNoteRequest validPatch = new UpdateNoteRequest("Updated note text", 0L);
    ResponseEntity<NoteResponse> patchResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId,
            HttpMethod.PATCH,
            new HttpEntity<>(validPatch, authHeaders(sessionA)),
            NoteResponse.class);
    assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(patchResp.getBody().content()).isEqualTo("Updated note text");
    assertThat(patchResp.getBody().version()).isEqualTo(1L);

    // 7. PATCH /notes/{id}: no-op identical content returns 200 without bump
    UpdateNoteRequest noOpPatch = new UpdateNoteRequest("Updated note text", 1L);
    ResponseEntity<NoteResponse> noOpPatchResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId,
            HttpMethod.PATCH,
            new HttpEntity<>(noOpPatch, authHeaders(sessionA)),
            NoteResponse.class);
    assertThat(noOpPatchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(noOpPatchResp.getBody().version()).isEqualTo(1L);

    // 8. DELETE /notes/{id}: stale version returns 409 Conflict
    ResponseEntity<String> staleDeleteResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId + "?expectedVersion=0",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(sessionA)),
            String.class);
    assertThat(staleDeleteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    // 9. DELETE /notes/{id}: valid expectedVersion returns 204 No Content
    ResponseEntity<Void> validDeleteResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId + "?expectedVersion=1",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(sessionA)),
            Void.class);
    assertThat(validDeleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // 10. Verify note is deleted from GET /me/notes
    ResponseEntity<NotePageResponse> listAfterDelete =
        restTemplate.exchange(
            baseUrl()
                + "/me/notes?enrollmentId="
                + enrollmentId
                + "&lessonId="
                + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            NotePageResponse.class);
    assertThat(listAfterDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listAfterDelete.getBody().items()).isEmpty();
  }

  @Test
  @DisplayName(
      "Concurrent draft save vs submit on same expectedVersion: one succeeds, one conflicts with 409")
  void concurrentDraftVsSubmit_oneSucceedsOneConflicts() throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    Lesson firstLesson = lessons.get(0);
    List<Activity> activities =
        activityRepository.findActivitiesWithResourcesByLessonId(firstLesson.getId());
    Activity reflectionAct =
        activities.stream()
            .filter(
                a ->
                    a.getActivityType() == ActivityType.REFLECTION
                        || a.getActivityType() == ActivityType.PRACTICE)
            .findFirst()
            .orElse(activities.get(0));

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    Callable<ResponseEntity<ActivityResponseDto>> draftTask =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          SaveResponseDraftRequest req = new SaveResponseDraftRequest("Concurrent draft text", 1L);
          return restTemplate.exchange(
              baseUrl()
                  + "/enrollments/"
                  + enrollmentId
                  + "/activities/"
                  + reflectionAct.getId()
                  + "/response",
              HttpMethod.PUT,
              new HttpEntity<>(req, authHeaders(sessionA)),
              ActivityResponseDto.class);
        };

    Callable<ResponseEntity<String>> submitTask =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          SubmitResponseRequest req = new SubmitResponseRequest("Concurrent submit text", 1L);
          return restTemplate.exchange(
              baseUrl()
                  + "/enrollments/"
                  + enrollmentId
                  + "/activities/"
                  + reflectionAct.getId()
                  + "/response/submit",
              HttpMethod.POST,
              new HttpEntity<>(req, authHeaders(sessionA)),
              String.class);
        };

    Future<ResponseEntity<ActivityResponseDto>> draftFuture = executor.submit(draftTask);
    Future<ResponseEntity<String>> submitFuture = executor.submit(submitTask);

    readyLatch.await(5, TimeUnit.SECONDS);
    startLatch.countDown();

    ResponseEntity<ActivityResponseDto> draftResp = draftFuture.get(10, TimeUnit.SECONDS);
    ResponseEntity<String> submitResp = submitFuture.get(10, TimeUnit.SECONDS);
    executor.shutdown();

    List<Integer> statuses =
        List.of(draftResp.getStatusCode().value(), submitResp.getStatusCode().value());
    assertThat(statuses).containsExactlyInAnyOrder(200, 409);

    // Verify enrollment version is now 2
    ResponseEntity<LessonDetailResponse> workspaceResp =
        restTemplate.exchange(
            baseUrl() + "/enrollments/" + enrollmentId + "/lessons/" + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            LessonDetailResponse.class);
    assertThat(workspaceResp.getBody()).isNotNull();
    assertThat(workspaceResp.getBody().enrollmentVersion()).isEqualTo(2L);
  }

  @Test
  @DisplayName(
      "Concurrent note creation on same scope: one succeeds with 201, one conflicts with 409")
  void concurrentNoteCreationOnSameScope_oneSucceedsOneConflicts() throws Exception {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    Lesson firstLesson = lessons.get(0);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    Callable<ResponseEntity<String>> taskA =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          CreateNoteRequest req =
              new CreateNoteRequest(
                  enrollmentId, firstLesson.getId(), null, "First concurrent content");
          return restTemplate.exchange(
              baseUrl() + "/notes",
              HttpMethod.POST,
              new HttpEntity<>(req, authHeaders(sessionA)),
              String.class);
        };

    Callable<ResponseEntity<String>> taskB =
        () -> {
          readyLatch.countDown();
          startLatch.await();
          CreateNoteRequest req =
              new CreateNoteRequest(
                  enrollmentId, firstLesson.getId(), null, "Second concurrent content");
          return restTemplate.exchange(
              baseUrl() + "/notes",
              HttpMethod.POST,
              new HttpEntity<>(req, authHeaders(sessionA)),
              String.class);
        };

    Future<ResponseEntity<String>> futureA = executor.submit(taskA);
    Future<ResponseEntity<String>> futureB = executor.submit(taskB);

    readyLatch.await(5, TimeUnit.SECONDS);
    startLatch.countDown();

    ResponseEntity<String> respA = futureA.get(10, TimeUnit.SECONDS);
    ResponseEntity<String> respB = futureB.get(10, TimeUnit.SECONDS);
    executor.shutdown();

    List<Integer> statuses = List.of(respA.getStatusCode().value(), respB.getStatusCode().value());
    assertThat(statuses).containsExactlyInAnyOrder(201, 409);

    // Verify exactly one note exists
    ResponseEntity<NotePageResponse> listResp =
        restTemplate.exchange(
            baseUrl()
                + "/me/notes?enrollmentId="
                + enrollmentId
                + "&lessonId="
                + firstLesson.getId(),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(sessionA)),
            NotePageResponse.class);
    assertThat(listResp.getBody()).isNotNull();
    assertThat(listResp.getBody().items()).hasSize(1);
  }

  @Test
  @DisplayName("PATCH /notes/{id} with identical content maintains version and updatedAt")
  void fixedClockNoOpVersionStability() {
    CsrfSession sessionA = login("usera", "Password123!");
    UUID enrollmentId = createEnrollmentForUser(sessionA);

    List<Lesson> lessons = lessonRepository.findLessonsByRevisionIdOrdered(revisionId);
    Lesson firstLesson = lessons.get(0);

    // Create note
    CreateNoteRequest createReq =
        new CreateNoteRequest(enrollmentId, firstLesson.getId(), null, "Initial static note text");
    ResponseEntity<NoteResponse> createResp =
        restTemplate.exchange(
            baseUrl() + "/notes",
            HttpMethod.POST,
            new HttpEntity<>(createReq, authHeaders(sessionA)),
            NoteResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    NoteResponse initial = createResp.getBody();
    assertThat(initial).isNotNull();
    UUID noteId = initial.id();
    Long initialVersion = initial.version();
    Instant initialUpdatedAt = initial.updatedAt();

    // Patch with exact same content and matching expectedVersion
    UpdateNoteRequest patchReq = new UpdateNoteRequest("Initial static note text", initialVersion);
    ResponseEntity<NoteResponse> patchResp =
        restTemplate.exchange(
            baseUrl() + "/notes/" + noteId,
            HttpMethod.PATCH,
            new HttpEntity<>(patchReq, authHeaders(sessionA)),
            NoteResponse.class);

    assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    NoteResponse updated = patchResp.getBody();
    assertThat(updated).isNotNull();
    assertThat(updated.version()).isEqualTo(initialVersion);
    assertThat(updated.updatedAt().truncatedTo(ChronoUnit.MILLIS))
        .isEqualTo(initialUpdatedAt.truncatedTo(ChronoUnit.MILLIS));
  }
}
