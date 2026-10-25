package vn.englishlearning.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.englishlearning.contentops.application.ImportCurriculumPackageService;
import vn.englishlearning.contentops.application.PublishCurriculumRevisionService;
import vn.englishlearning.contentops.domain.ImportSummary;
import vn.englishlearning.curriculum.api.CatalogResponses.*;

/**
 * Integration Test for Curriculum Catalog & Outline endpoints. Runs strictly on an isolated,
 * disposable PostgreSQL Testcontainers instance. Never connects or leaks test data into the
 * development database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CurriculumCatalogIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_curriculum_catalog_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ImportCurriculumPackageService importService;

  @Autowired private PublishCurriculumRevisionService publishService;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  private final Path pilotPackageDir = Paths.get("../content/pilot-curriculum/v1").normalize();

  @BeforeEach
  void resetContentDatabase() {
    jdbcTemplate.execute("TRUNCATE TABLE curriculum CASCADE");
  }

  @Test
  @DisplayName(
      "Draft curriculum/revision does NOT appear in catalog, detail returns 404, outline returns 404")
  void draftCurriculum_doesNotAppearInCatalog_andReturns404() {
    // 1. Import pilot package without publishing (remains DRAFT)
    ImportSummary draftImport = importService.importPackage(pilotPackageDir);
    UUID draftRevisionId = draftImport.revisionId();

    // 2. GET /api/v1/curricula -> draft must not appear
    ResponseEntity<CurriculumSummaryResponse[]> listResponse =
        restTemplate.getForEntity("/api/v1/curricula", CurriculumSummaryResponse[].class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).isEmpty();

    // 3. GET /api/v1/curricula/pilot-curriculum -> draft slug must return 404 Problem Details
    ResponseEntity<String> detailResponse =
        restTemplate.getForEntity("/api/v1/curricula/pilot-curriculum", String.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(detailResponse.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType("application/problem+json"));
    assertThat(detailResponse.getBody()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    assertThat(detailResponse.getBody()).contains("\"status\":404");

    // 4. GET /api/v1/revisions/{draftRevisionId}/outline -> draft outline must return 404 Problem
    // Details
    ResponseEntity<String> outlineResponse =
        restTemplate.getForEntity(
            "/api/v1/revisions/" + draftRevisionId + "/outline", String.class);
    assertThat(outlineResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(outlineResponse.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType("application/problem+json"));
    assertThat(outlineResponse.getBody()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    assertThat(outlineResponse.getBody()).contains("\"status\":404");
  }

  @Test
  @DisplayName(
      "Published pilot curriculum has 1 phase, 1 week, 6 lessons, and accurate British English metadata")
  void publishedPilotCurriculum_appearsInCatalog_withCorrectMetadataAndOutline() {
    // 1. Import and publish pilot package
    ImportSummary summary = importService.importPackage(pilotPackageDir);
    publishService.publish(summary.curriculumKey(), summary.version());

    // 2. GET /api/v1/curricula -> returns published pilot summary
    ResponseEntity<CurriculumSummaryResponse[]> listResponse =
        restTemplate.getForEntity("/api/v1/curricula", CurriculumSummaryResponse[].class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).isNotNull().hasSize(1);

    CurriculumSummaryResponse catalogItem = listResponse.getBody()[0];
    assertThat(catalogItem.slug()).isEqualTo("pilot-curriculum");
    assertThat(catalogItem.currentRevisionVersion()).isEqualTo("1.0.0");
    assertThat(catalogItem.targetLocale()).isEqualTo("en-GB");
    assertThat(catalogItem.currentRevisionId()).isEqualTo(summary.revisionId());

    // 3. GET /api/v1/curricula/pilot-curriculum -> returns detail with metadata and outline
    ResponseEntity<CurriculumDetailResponse> detailResponse =
        restTemplate.getForEntity(
            "/api/v1/curricula/pilot-curriculum", CurriculumDetailResponse.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detailResponse.getBody()).isNotNull();

    CurriculumDetailResponse detail = detailResponse.getBody();
    assertThat(detail.slug()).isEqualTo("pilot-curriculum");
    assertThat(detail.targetLocale()).isEqualTo("en-GB");
    assertThat(detail.authors()).isNotEmpty();
    assertThat(detail.provenance().verificationStatus()).isEqualTo("UNVERIFIED_DRAFT");
    assertThat(detail.provenance().sourceName())
        .isEqualTo("Khung mục tiêu Lộ trình tiếng Anh tham khảo");

    RevisionOutlineResponse outline = detail.revision();
    assertThat(outline.id()).isEqualTo(summary.revisionId());
    assertThat(outline.version()).isEqualTo("1.0.0");
    assertThat(outline.status()).isEqualTo("PUBLISHED");
    assertThat(outline.phases()).hasSize(1);

    PhaseOutlineDto phase = outline.phases().get(0);
    assertThat(phase.key()).isEqualTo("pilot.phase01");
    assertThat(phase.weeks()).hasSize(1);

    WeekOutlineDto week = phase.weeks().get(0);
    assertThat(week.key()).isEqualTo("pilot.w01");
    assertThat(week.weekNumber()).isEqualTo(1);
    assertThat(week.lessons()).hasSize(6);

    // 6 lessons: exactly 5 required and 1 optional (lesson 6)
    List<LessonOutlineDto> lessons = week.lessons();
    assertThat(lessons.stream().filter(LessonOutlineDto::required).count()).isEqualTo(5);
    assertThat(lessons.stream().filter(l -> !l.required()).count()).isEqualTo(1);
    assertThat(lessons.get(5).key()).isEqualTo("pilot.w01.l06");
    assertThat(lessons.get(5).required()).isFalse();

    int totalMinutes = lessons.stream().mapToInt(LessonOutlineDto::estimatedMinutes).sum();
    assertThat(totalMinutes).isEqualTo(135);
  }

  @Test
  @DisplayName("Outline response does NOT leak content_markdown, activity payloads, or answers")
  void outline_doesNotContainMarkdownOrActivityPayloadsOrAnswers() throws Exception {
    ImportSummary summary = importService.importPackage(pilotPackageDir);
    publishService.publish(summary.curriculumKey(), summary.version());

    ResponseEntity<String> rawOutline =
        restTemplate.getForEntity(
            "/api/v1/revisions/" + summary.revisionId() + "/outline", String.class);
    assertThat(rawOutline.getStatusCode()).isEqualTo(HttpStatus.OK);

    String json = rawOutline.getBody();
    assertThat(json).isNotNull();

    // Verify raw JSON does NOT contain content_markdown or quiz answers/payloads
    assertThat(json).doesNotContain("content_markdown");
    assertThat(json).doesNotContain("contentMarkdown");
    assertThat(json).doesNotContain("payload");
    assertThat(json).doesNotContain("correct_option");
    assertThat(json).doesNotContain("correctOption");
    assertThat(json).doesNotContain("explanation");
    assertThat(json).doesNotContain("pilot.w01.l01.a01");

    // Verify it only contains outline fields
    JsonNode root = objectMapper.readTree(json);
    JsonNode firstLesson = root.get("phases").get(0).get("weeks").get(0).get("lessons").get(0);
    assertThat(firstLesson.has("id")).isTrue();
    assertThat(firstLesson.has("key")).isTrue();
    assertThat(firstLesson.has("title")).isTrue();
    assertThat(firstLesson.has("orderIndex")).isTrue();
    assertThat(firstLesson.has("estimatedMinutes")).isTrue();
    assertThat(firstLesson.has("required")).isTrue();
    assertThat(firstLesson.has("contentMarkdown")).isFalse();
    assertThat(firstLesson.has("activities")).isFalse();
  }

  @Test
  @DisplayName(
      "Non-existent slug or revisionId returns 404 Problem Details with RESOURCE_NOT_FOUND")
  void nonExistentResources_return404ProblemDetails() {
    // 1. Unknown slug
    ResponseEntity<String> unknownSlugResponse =
        restTemplate.getForEntity("/api/v1/curricula/unknown-curriculum-slug", String.class);
    assertThat(unknownSlugResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(unknownSlugResponse.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType("application/problem+json"));
    assertThat(unknownSlugResponse.getBody()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    assertThat(unknownSlugResponse.getBody()).contains("\"status\":404");

    // 2. Unknown revisionId
    UUID randomRevisionId = UUID.randomUUID();
    ResponseEntity<String> unknownRevisionResponse =
        restTemplate.getForEntity(
            "/api/v1/revisions/" + randomRevisionId + "/outline", String.class);
    assertThat(unknownRevisionResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(unknownRevisionResponse.getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType("application/problem+json"));
    assertThat(unknownRevisionResponse.getBody()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    assertThat(unknownRevisionResponse.getBody()).contains("\"status\":404");
  }

  @Test
  @DisplayName(
      "Outline structure maintains stable ordering by order_index for phases, weeks, and lessons")
  void outline_hasStableOrdering() {
    ImportSummary summary = importService.importPackage(pilotPackageDir);
    publishService.publish(summary.curriculumKey(), summary.version());

    ResponseEntity<RevisionOutlineResponse> response =
        restTemplate.getForEntity(
            "/api/v1/revisions/" + summary.revisionId() + "/outline",
            RevisionOutlineResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    RevisionOutlineResponse outline = response.getBody();
    assertThat(outline.phases()).isNotEmpty();

    int lastPhaseOrder = 0;
    for (PhaseOutlineDto phase : outline.phases()) {
      assertThat(phase.orderIndex()).isGreaterThan(lastPhaseOrder);
      lastPhaseOrder = phase.orderIndex();

      int lastWeekOrder = 0;
      for (WeekOutlineDto week : phase.weeks()) {
        assertThat(week.orderIndex()).isGreaterThan(lastWeekOrder);
        lastWeekOrder = week.orderIndex();

        int lastLessonOrder = 0;
        for (LessonOutlineDto lesson : week.lessons()) {
          assertThat(lesson.orderIndex()).isGreaterThan(lastLessonOrder);
          lastLessonOrder = lesson.orderIndex();
        }
      }
    }
  }

  @Test
  @DisplayName("Outline executes one metadata and three flat queries without N+1")
  void outlineQuery_executesFlatQueriesWithoutNPlusOne() {
    ImportSummary summary = importService.importPackage(pilotPackageDir);
    publishService.publish(summary.curriculumKey(), summary.version());

    org.mockito.Mockito.clearInvocations(jdbcTemplate);
    ResponseEntity<RevisionOutlineResponse> response =
        restTemplate.getForEntity(
            "/api/v1/revisions/" + summary.revisionId() + "/outline",
            RevisionOutlineResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().phases().get(0).weeks().get(0).lessons()).hasSize(6);
    // Count the parameterized RowMapper entry point, excluding JdbcTemplate's
    // internal overloads, so a slow machine cannot mask or falsely flag N+1.
    long queryCount =
        org.mockito.Mockito.mockingDetails(jdbcTemplate).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("query"))
            .filter(invocation -> invocation.getMethod().getParameterCount() == 3)
            .filter(
                invocation ->
                    invocation.getMethod().getParameterTypes()[1]
                        == org.springframework.jdbc.core.RowMapper.class)
            .count();
    assertThat(queryCount).as("one metadata query plus phases, weeks and lessons").isEqualTo(4);
  }
}
