package vn.englishlearning.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import vn.englishlearning.identity.api.CsrfTokenResponse;
import vn.englishlearning.identity.api.LearnerPreferencesResponse;
import vn.englishlearning.identity.api.LoginRequest;
import vn.englishlearning.identity.api.UpdatePreferencesRequest;
import vn.englishlearning.identity.api.UserResponse;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;

/**
 * End-to-end integration tests for authentication, session lifecycle, CSRF protection, and user
 * preferences on an isolated PostgreSQL Testcontainer. Contains regression tests for A1 (Session
 * fixation & CSRF rotation) and A2 (PATCH field preservation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthAndIdentityIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_auth_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private AppUserRepository userRepository;

  @Autowired private LearnerPreferencesRepository preferencesRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private Clock clock;

  private String baseUrl() {
    return "http://localhost:" + port + "/api/v1";
  }

  private record CsrfSession(String csrfToken, String sessionCookie) {}

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
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().headerName()).isEqualTo("X-CSRF-TOKEN");
    assertThat(response.getBody().token()).isNotBlank();

    String sessionCookie = extractCookie(response.getHeaders());
    if (sessionCookie == null) {
      sessionCookie = existingSessionCookie;
    }
    return new CsrfSession(response.getBody().token(), sessionCookie);
  }

  private CsrfSession obtainCsrfToken() {
    return obtainCsrfToken(null);
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

  private AppUser createTestUser(String username, String rawPassword, UserRole role) {
    Instant now = Instant.now(clock);
    AppUser user =
        new AppUser(
            username,
            passwordEncoder.encode(rawPassword),
            "Test " + username,
            username + "@example.com",
            role,
            now);
    return userRepository.save(user);
  }

  @Test
  @DisplayName("GET /auth/csrf should return CSRF token with Cache-Control no-store")
  void getCsrfToken_shouldReturnTokenAndNoStore() {
    CsrfSession session = obtainCsrfToken();
    assertThat(session.csrfToken()).isNotBlank();
  }

  @Test
  @DisplayName("POST /auth/login without CSRF token should return 403 CSRF_INVALID")
  void login_withoutCsrf_shouldReturn403CsrfInvalid() {
    LoginRequest request = new LoginRequest("user1", "Pass123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl() + "/auth/login", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            response
                .getHeaders()
                .getContentType()
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();
    assertThat(response.getBody()).contains("\"code\":\"CSRF_INVALID\"");
  }

  @Test
  @DisplayName("POST /auth/login with wrong password should return 401 INVALID_CREDENTIALS")
  void login_withWrongPassword_shouldReturn401InvalidCredentials() {
    String username = "wrong_pass_user_" + UUID.randomUUID().toString().substring(0, 8);
    createTestUser(username, "CorrectPassword123!", UserRole.LEARNER);

    CsrfSession csrf = obtainCsrfToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", csrf.csrfToken());
    if (csrf.sessionCookie() != null) {
      headers.set(HttpHeaders.COOKIE, csrf.sessionCookie());
    }

    LoginRequest request = new LoginRequest(username, "IncorrectPassword");
    HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl() + "/auth/login", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            response
                .getHeaders()
                .getContentType()
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();
    assertThat(response.getBody()).contains("\"code\":\"INVALID_CREDENTIALS\"");
  }

  @Test
  @DisplayName("POST /auth/login with valid credentials should return 200 and Set-Cookie SESSION")
  void login_withValidCredentials_shouldReturn200AndSessionCookie() {
    String username = "valid_user_" + UUID.randomUUID().toString().substring(0, 8);
    AppUser user = createTestUser(username, "ValidPassword123!", UserRole.LEARNER);

    CsrfSession csrf = obtainCsrfToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-CSRF-TOKEN", csrf.csrfToken());
    if (csrf.sessionCookie() != null) {
      headers.set(HttpHeaders.COOKIE, csrf.sessionCookie());
    }

    LoginRequest request = new LoginRequest(username, "ValidPassword123!");
    HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<UserResponse> response =
        restTemplate.postForEntity(baseUrl() + "/auth/login", entity, UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(user.getId());
    assertThat(response.getBody().username()).isEqualTo(username);
    assertThat(response.getBody().role()).isEqualTo(UserRole.LEARNER);

    String sessionCookie = extractCookie(response.getHeaders());
    assertThat(sessionCookie).isNotNull();
    assertThat(sessionCookie).isNotEqualTo(csrf.sessionCookie());
  }

  @Test
  @DisplayName("GET /me unauthenticated should return 401 AUTHENTICATION_REQUIRED")
  void getMe_unauthenticated_shouldReturn401() {
    ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/me", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            response
                .getHeaders()
                .getContentType()
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();
    assertThat(response.getBody()).contains("\"code\":\"AUTHENTICATION_REQUIRED\"");
  }

  @Test
  @DisplayName(
      "A1 Regression: Login migrates session ID, rejects old session, rotates CSRF, and handles logout")
  void login_migratesSessionId_and_rotatesCsrfToken() {
    String username = "session_migrated_user_" + UUID.randomUUID().toString().substring(0, 8);
    createTestUser(username, "Password123!", UserRole.LEARNER);

    // 1. Pre-login: obtain CSRF token and pre-login session cookie
    CsrfSession preLoginCsrf = obtainCsrfToken();
    String preLoginSessionCookie = preLoginCsrf.sessionCookie();
    assertThat(preLoginSessionCookie).isNotNull();

    // 2. Perform login with pre-login CSRF token and pre-login session cookie
    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.setContentType(MediaType.APPLICATION_JSON);
    loginHeaders.set("X-CSRF-TOKEN", preLoginCsrf.csrfToken());
    loginHeaders.set(HttpHeaders.COOKIE, preLoginSessionCookie);

    ResponseEntity<UserResponse> loginResponse =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login",
            new HttpEntity<>(new LoginRequest(username, "Password123!"), loginHeaders),
            UserResponse.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Assert that login emitted a new session cookie with changed ID (session fixation protection)
    String postLoginSessionCookie = extractCookie(loginResponse.getHeaders());
    assertThat(postLoginSessionCookie).isNotNull();
    assertThat(postLoginSessionCookie).isNotEqualTo(preLoginSessionCookie);

    // 3. Old pre-login session cookie cannot access /me (must return 401)
    HttpHeaders oldCookieHeaders = new HttpHeaders();
    oldCookieHeaders.set(HttpHeaders.COOKIE, preLoginSessionCookie);
    ResponseEntity<String> oldSessionMeResponse =
        restTemplate.exchange(
            baseUrl() + "/me", HttpMethod.GET, new HttpEntity<>(oldCookieHeaders), String.class);
    assertThat(oldSessionMeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // 4. New session cookie successfully accesses /me (returns 200)
    HttpHeaders newCookieHeaders = new HttpHeaders();
    newCookieHeaders.set(HttpHeaders.COOKIE, postLoginSessionCookie);
    ResponseEntity<UserResponse> newSessionMeResponse =
        restTemplate.exchange(
            baseUrl() + "/me",
            HttpMethod.GET,
            new HttpEntity<>(newCookieHeaders),
            UserResponse.class);
    assertThat(newSessionMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(newSessionMeResponse.getBody().username()).isEqualTo(username);

    // 5. Old pre-login CSRF token is rejected on mutation with 403 CSRF_INVALID
    HttpHeaders oldCsrfMutationHeaders = new HttpHeaders();
    oldCsrfMutationHeaders.setContentType(MediaType.APPLICATION_JSON);
    oldCsrfMutationHeaders.set(HttpHeaders.COOKIE, postLoginSessionCookie);
    oldCsrfMutationHeaders.set("X-CSRF-TOKEN", preLoginCsrf.csrfToken());

    ResponseEntity<String> rejectedMutationResponse =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(UpdatePreferencesRequest.ofDailyTarget(50), oldCsrfMutationHeaders),
            String.class);
    assertThat(rejectedMutationResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(rejectedMutationResponse.getBody()).contains("\"code\":\"CSRF_INVALID\"");

    // 6. Obtain new CSRF token with post-login session cookie and execute mutation successfully
    CsrfSession postLoginCsrf = obtainCsrfToken(postLoginSessionCookie);
    assertThat(postLoginCsrf.csrfToken()).isNotBlank();

    HttpHeaders validMutationHeaders = new HttpHeaders();
    validMutationHeaders.setContentType(MediaType.APPLICATION_JSON);
    validMutationHeaders.set(HttpHeaders.COOKIE, postLoginSessionCookie);
    validMutationHeaders.set("X-CSRF-TOKEN", postLoginCsrf.csrfToken());

    ResponseEntity<LearnerPreferencesResponse> validMutationResponse =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(UpdatePreferencesRequest.ofDailyTarget(50), validMutationHeaders),
            LearnerPreferencesResponse.class);
    assertThat(validMutationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(validMutationResponse.getBody().dailyTargetMinutes()).isEqualTo(50);

    // 7. Logout invalidates the logged-in session cookie
    ResponseEntity<Void> logoutResponse =
        restTemplate.exchange(
            baseUrl() + "/auth/logout",
            HttpMethod.POST,
            new HttpEntity<>(validMutationHeaders),
            Void.class);
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> meAfterLogoutResponse =
        restTemplate.exchange(
            baseUrl() + "/me", HttpMethod.GET, new HttpEntity<>(newCookieHeaders), String.class);
    assertThat(meAfterLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // 8. Subsequent login succeeds
    CsrfSession freshCsrf = obtainCsrfToken();
    HttpHeaders reloginHeaders = new HttpHeaders();
    reloginHeaders.setContentType(MediaType.APPLICATION_JSON);
    reloginHeaders.set("X-CSRF-TOKEN", freshCsrf.csrfToken());
    if (freshCsrf.sessionCookie() != null) {
      reloginHeaders.set(HttpHeaders.COOKIE, freshCsrf.sessionCookie());
    }
    ResponseEntity<UserResponse> reloginResponse =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login",
            new HttpEntity<>(new LoginRequest(username, "Password123!"), reloginHeaders),
            UserResponse.class);
    assertThat(reloginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName(
      "A2 Regression: PATCH preserves focusedEnrollmentId when absent, clears on explicit null, empty does nothing")
  void patchPreferences_preservesFocusedEnrollmentWhenAbsent_andClearsWhenExplicitNull() {
    String username = "patch_prefs_user_" + UUID.randomUUID().toString().substring(0, 8);
    AppUser user = createTestUser(username, "Password123!", UserRole.LEARNER);

    CsrfSession csrf = obtainCsrfToken();
    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.setContentType(MediaType.APPLICATION_JSON);
    loginHeaders.set("X-CSRF-TOKEN", csrf.csrfToken());
    if (csrf.sessionCookie() != null) {
      loginHeaders.set(HttpHeaders.COOKIE, csrf.sessionCookie());
    }

    ResponseEntity<UserResponse> loginResponse =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login",
            new HttpEntity<>(new LoginRequest(username, "Password123!"), loginHeaders),
            UserResponse.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    String sessionCookie = extractCookie(loginResponse.getHeaders());
    assertThat(sessionCookie).isNotNull();

    CsrfSession freshCsrf = obtainCsrfToken(sessionCookie);

    HttpHeaders mutationHeaders = new HttpHeaders();
    mutationHeaders.setContentType(MediaType.APPLICATION_JSON);
    mutationHeaders.set(HttpHeaders.COOKIE, sessionCookie);
    mutationHeaders.set("X-CSRF-TOKEN", freshCsrf.csrfToken());

    // 1. Initial state: set a valid focused enrollment ID belonging to user
    UUID initialEnrollmentId = UUID.randomUUID();
    UUID dummyCurriculumId = UUID.randomUUID();
    UUID dummyRevisionId = UUID.randomUUID();
    Instant now = Instant.now(clock);
    jdbcTemplate.update(
        "INSERT INTO curriculum (id, curriculum_key, target_locale) VALUES (?, ?, ?)",
        dummyCurriculumId,
        "dummy-" + UUID.randomUUID(),
        "en-US");
    jdbcTemplate.update(
        "INSERT INTO curriculum_revision (id, curriculum_id, version, status, title, description, authors, provenance, checksum, published_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
        dummyRevisionId,
        dummyCurriculumId,
        "1.0.0",
        "PUBLISHED",
        "Dummy Curriculum",
        "Description",
        "Authors",
        "Provenance",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    jdbcTemplate.update(
        "INSERT INTO enrollment (id, user_id, curriculum_id, revision_id, status, start_date, time_zone, study_days, daily_target_minutes, version, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_DATE, 'Asia/Ho_Chi_Minh', 'MON,WED,FRI', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        initialEnrollmentId,
        user.getId(),
        dummyCurriculumId,
        dummyRevisionId);

    UpdatePreferencesRequest setEnrollmentReq =
        UpdatePreferencesRequest.ofFocusedEnrollment(initialEnrollmentId);

    ResponseEntity<LearnerPreferencesResponse> resp1 =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(setEnrollmentReq, mutationHeaders),
            LearnerPreferencesResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp1.getBody().focusedEnrollmentId()).isEqualTo(initialEnrollmentId);

    // 2. PATCH ONLY dailyTargetMinutes -> focusedEnrollmentId MUST BE PRESERVED
    String patchOnlyMinutesJson = "{\"dailyTargetMinutes\": 45}";
    HttpHeaders jsonHeaders = new HttpHeaders();
    jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
    jsonHeaders.set(HttpHeaders.COOKIE, sessionCookie);
    jsonHeaders.set("X-CSRF-TOKEN", freshCsrf.csrfToken());

    ResponseEntity<LearnerPreferencesResponse> resp2 =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(patchOnlyMinutesJson, jsonHeaders),
            LearnerPreferencesResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp2.getBody().dailyTargetMinutes()).isEqualTo(45);
    assertThat(resp2.getBody().focusedEnrollmentId()).isEqualTo(initialEnrollmentId);

    // 3. PATCH explicit null focusedEnrollmentId -> focusedEnrollmentId MUST BE CLEARED
    String patchNullEnrollmentJson = "{\"focusedEnrollmentId\": null}";
    ResponseEntity<LearnerPreferencesResponse> resp3 =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(patchNullEnrollmentJson, jsonHeaders),
            LearnerPreferencesResponse.class);
    assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp3.getBody().dailyTargetMinutes()).isEqualTo(45);
    assertThat(resp3.getBody().focusedEnrollmentId()).isNull();

    // 4. Empty PATCH {} -> leaves data untouched
    String patchEmptyJson = "{}";
    ResponseEntity<LearnerPreferencesResponse> resp4 =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.PATCH,
            new HttpEntity<>(patchEmptyJson, jsonHeaders),
            LearnerPreferencesResponse.class);
    assertThat(resp4.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp4.getBody().dailyTargetMinutes()).isEqualTo(45);
    assertThat(resp4.getBody().focusedEnrollmentId()).isNull();
  }

  @Test
  @DisplayName("User A cannot access or mutate User B preferences (Isolation)")
  void userIsolationTest() {
    String userA = "user_a_" + UUID.randomUUID().toString().substring(0, 8);
    String userB = "user_b_" + UUID.randomUUID().toString().substring(0, 8);

    AppUser createdA = createTestUser(userA, "PassA123!", UserRole.LEARNER);
    AppUser createdB = createTestUser(userB, "PassB123!", UserRole.LEARNER);

    Instant now = Instant.now(clock);
    preferencesRepository.save(
        new LearnerPreferences(createdA.getId(), 30, "MON,WED,FRI", "Asia/Ho_Chi_Minh", now));
    preferencesRepository.save(
        new LearnerPreferences(createdB.getId(), 60, "TUE,THU", "Asia/Ho_Chi_Minh", now));

    // Login User A
    CsrfSession csrfA = obtainCsrfToken();
    HttpHeaders headersA = new HttpHeaders();
    headersA.setContentType(MediaType.APPLICATION_JSON);
    headersA.set("X-CSRF-TOKEN", csrfA.csrfToken());
    if (csrfA.sessionCookie() != null) {
      headersA.set(HttpHeaders.COOKIE, csrfA.sessionCookie());
    }

    ResponseEntity<UserResponse> loginA =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login",
            new HttpEntity<>(new LoginRequest(userA, "PassA123!"), headersA),
            UserResponse.class);
    String sessionA = extractCookie(loginA.getHeaders());
    assertThat(sessionA).isNotNull();

    // Login User B
    CsrfSession csrfB = obtainCsrfToken();
    HttpHeaders headersB = new HttpHeaders();
    headersB.setContentType(MediaType.APPLICATION_JSON);
    headersB.set("X-CSRF-TOKEN", csrfB.csrfToken());
    if (csrfB.sessionCookie() != null) {
      headersB.set(HttpHeaders.COOKIE, csrfB.sessionCookie());
    }

    ResponseEntity<UserResponse> loginB =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login",
            new HttpEntity<>(new LoginRequest(userB, "PassB123!"), headersB),
            UserResponse.class);
    String sessionB = extractCookie(loginB.getHeaders());
    assertThat(sessionB).isNotNull();

    // User A fetches preferences -> must see 30
    HttpHeaders reqHeadersA = new HttpHeaders();
    reqHeadersA.set(HttpHeaders.COOKIE, sessionA);
    ResponseEntity<LearnerPreferencesResponse> prefsA =
        restTemplate.exchange(
            baseUrl() + "/me/preferences",
            HttpMethod.GET,
            new HttpEntity<>(reqHeadersA),
            LearnerPreferencesResponse.class);
    assertThat(prefsA.getBody().dailyTargetMinutes()).isEqualTo(30);

    // User B updates own preferences to 90
    CsrfSession freshCsrfB = obtainCsrfToken(sessionB);
    HttpHeaders patchHeadersB = new HttpHeaders();
    patchHeadersB.setContentType(MediaType.APPLICATION_JSON);
    patchHeadersB.set(HttpHeaders.COOKIE, sessionB);
    patchHeadersB.set("X-CSRF-TOKEN", freshCsrfB.csrfToken());
    restTemplate.exchange(
        baseUrl() + "/me/preferences",
        HttpMethod.PATCH,
        new HttpEntity<>(UpdatePreferencesRequest.ofDailyTarget(90), patchHeadersB),
        LearnerPreferencesResponse.class);

    // Verify User A's preferences in DB remain untouched at 30
    LearnerPreferences freshA = preferencesRepository.findById(createdA.getId()).orElseThrow();
    assertThat(freshA.getDailyGoalMinutes()).isEqualTo(30);

    // Verify User B's preferences in DB updated to 90
    LearnerPreferences freshB = preferencesRepository.findById(createdB.getId()).orElseThrow();
    assertThat(freshB.getDailyGoalMinutes()).isEqualTo(90);
  }
}
