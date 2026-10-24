package vn.englishlearning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;

/**
 * Foundation Integration Test. Runs strictly on an isolated, disposable PostgreSQL Testcontainers
 * instance. Never connects or leaks test data into the development database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EnglishLearningApplicationIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_learning_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private Clock clock;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private AppUserRepository userRepository;

  @Autowired private LearnerPreferencesRepository preferencesRepository;

  @Test
  @DisplayName("Should successfully load application context and inject required foundation beans")
  void shouldLoadContext() {
    assertThat(postgres.isRunning()).isTrue();
    assertThat(clock).isNotNull();
    assertThat(passwordEncoder).isNotNull();
    assertThat(userRepository).isNotNull();
    assertThat(preferencesRepository).isNotNull();
  }

  @Test
  @DisplayName("Actuator health endpoint should return UP status")
  void shouldReturnActuatorHealthUp() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }

  @Test
  @DisplayName(
      "Flyway V001 migration should allow persisting and querying user and preferences in isolated testcontainer")
  void shouldPersistAndRetrieveUserAndPreferences() {
    Instant now = Instant.now(clock);
    String username = "learner_" + UUID.randomUUID().toString().substring(0, 8);
    String encodedPassword = passwordEncoder.encode("SecureP@ss123");

    AppUser user =
        new AppUser(username, encodedPassword, "Nguyễn Văn A", username + "@example.com", now);
    AppUser savedUser = userRepository.save(user);

    assertThat(savedUser.getId()).isNotNull();

    LearnerPreferences preferences =
        new LearnerPreferences(savedUser.getId(), 45, "MON,WED,FRI", "Asia/Ho_Chi_Minh", now);
    LearnerPreferences savedPreferences = preferencesRepository.save(preferences);

    assertThat(savedPreferences.getUserId()).isEqualTo(savedUser.getId());
    assertThat(savedPreferences.getDailyGoalMinutes()).isEqualTo(45);

    Optional<AppUser> retrieved = userRepository.findByUsername(username);
    assertThat(retrieved).isPresent();
    assertThat(passwordEncoder.matches("SecureP@ss123", retrieved.get().getPasswordHash()))
        .isTrue();
  }

  @Test
  @DisplayName(
      "Calling non-existent path should return RFC 7807 problem+json with RESOURCE_NOT_FOUND")
  void shouldReturnProblemDetailsOnNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/unknown-resource-path", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(
            response
                .getHeaders()
                .getContentType()
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();

    String body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    assertThat(body).contains("\"traceId\":");
    assertThat(body).contains("\"fieldErrors\":[]");
  }
}
