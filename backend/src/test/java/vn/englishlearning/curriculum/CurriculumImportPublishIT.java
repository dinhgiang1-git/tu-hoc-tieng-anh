package vn.englishlearning.curriculum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import vn.englishlearning.contentops.domain.PublishResult;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.domain.RevisionStatus;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumPhaseRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.ResourceRepository;
import vn.englishlearning.learning.domain.ProgressPolicy;

/**
 * Integration Test for Curriculum Package Import & Publish pipeline. Runs strictly on an isolated,
 * disposable PostgreSQL Testcontainers instance. Never connects to the development database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class CurriculumImportPublishIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.15-alpine")
          .withDatabaseName("test_curriculum_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @Autowired private ImportCurriculumPackageService importService;

  @Autowired private PublishCurriculumRevisionService publishService;

  @Autowired private CurriculumRevisionRepository revisionRepository;

  @Autowired private CurriculumPhaseRepository phaseRepository;

  @Autowired private CurriculumWeekRepository weekRepository;

  @Autowired private LessonRepository lessonRepository;

  @Autowired private ActivityRepository activityRepository;

  @Autowired private ResourceRepository resourceRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetIsolatedContentDatabase() {
    // Each method starts from an empty Testcontainers content database. TRUNCATE
    // also clears published fixtures without invoking immutable DELETE guards.
    jdbcTemplate.execute("TRUNCATE TABLE curriculum CASCADE");
  }

  @Test
  @DisplayName(
      "Full pipeline: import pilot package, verify metrics, idempotent re-import, publish revision")
  void fullPipeline_importAndPublish() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();

    // 1. First import: imports into DRAFT status
    ImportSummary firstImport = importService.importPackage(packageDir);
    assertThat(firstImport.newlyImported()).isTrue();
    assertThat(firstImport.curriculumKey()).isEqualTo("pilot-curriculum");
    assertThat(firstImport.version()).isEqualTo("1.0.0");
    assertThat(firstImport.status()).isEqualTo("DRAFT");
    assertThat(firstImport.phaseCount()).isEqualTo(1);
    assertThat(firstImport.weekCount()).isEqualTo(1);
    assertThat(firstImport.lessonCount()).isEqualTo(6);
    assertThat(firstImport.activityCount()).isEqualTo(21);
    assertThat(firstImport.resourceCount()).isEqualTo(5);

    // 2. Verify relational DB counts and effective required activities
    CurriculumRevision revision =
        revisionRepository.findById(firstImport.revisionId()).orElseThrow();
    assertThat(revision.getStatus()).isEqualTo(RevisionStatus.DRAFT);

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revision.getId());
    assertThat(lessons).hasSize(6);

    long requiredLessons = lessons.stream().filter(Lesson::isRequired).count();
    long optionalLessons = lessons.stream().filter(l -> !l.isRequired()).count();
    assertThat(requiredLessons).isEqualTo(5);
    assertThat(optionalLessons).isEqualTo(1);

    List<Activity> activities =
        activityRepository.findByLessonWeekPhaseRevisionId(revision.getId());
    assertThat(activities).hasSize(21);

    long effectivelyRequiredActivities =
        activities.stream()
            .filter(
                a ->
                    ProgressPolicy.isEffectivelyRequired(
                        lessons.stream()
                            .filter(lesson -> lesson.getId().equals(a.getLesson().getId()))
                            .findFirst()
                            .orElseThrow()
                            .isRequired(),
                        a.isRequired()))
            .count();
    assertThat(effectivelyRequiredActivities).isEqualTo(19);

    // 3. Idempotent re-import: returns identical revision without duplicates
    ImportSummary secondImport = importService.importPackage(packageDir);
    assertThat(secondImport.newlyImported()).isFalse();
    assertThat(secondImport.revisionId()).isEqualTo(firstImport.revisionId());
    assertThat(revisionRepository.count()).isEqualTo(1);
    assertThat(lessonRepository.count()).isEqualTo(6);
    assertThat(activityRepository.count()).isEqualTo(21);

    // 4. Publish revision: transitions to PUBLISHED
    PublishResult publishResult = publishService.publish("pilot-curriculum", "1.0.0");
    assertThat(publishResult.status()).isEqualTo("PUBLISHED");
    assertThat(publishResult.publishedAt()).isNotNull();

    CurriculumRevision publishedRev =
        revisionRepository.findById(firstImport.revisionId()).orElseThrow();
    assertThat(publishedRev.getStatus()).isEqualTo(RevisionStatus.PUBLISHED);

    // 5. Publishing again throws IllegalStateException
    assertThatThrownBy(() -> publishService.publish("pilot-curriculum", "1.0.0"))
        .isInstanceOf(IllegalStateException.class);

    // 6. R3: DB Trigger immutability verification on PUBLISHED content
    // 6a. Attempting to update lesson content of published revision must fail via DB trigger
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE lesson SET content_markdown = 'Modified content' WHERE id = '"
                        + lessons.get(0).getId()
                        + "'"))
        .hasMessageContaining("Cannot modify or delete lesson of a PUBLISHED curriculum revision");

    // 6b. Attempting to delete activity of published revision must fail via DB trigger
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "DELETE FROM activity WHERE id = '" + activities.get(0).getId() + "'"))
        .hasMessageContaining(
            "Cannot modify or delete activity of a PUBLISHED curriculum revision");

    // 6c. Attempting to delete activity_resource link of published revision must fail
    UUID linkedActivityId =
        jdbcTemplate.queryForObject(
            "SELECT ar.activity_id FROM activity_resource ar "
                + "JOIN activity a ON a.id = ar.activity_id "
                + "JOIN lesson l ON l.id = a.lesson_id "
                + "JOIN curriculum_week w ON w.id = l.week_id "
                + "JOIN curriculum_phase p ON p.id = w.phase_id "
                + "WHERE p.revision_id = ? LIMIT 1",
            UUID.class,
            revision.getId());
    assertThat(linkedActivityId).isNotNull();
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "DELETE FROM activity_resource WHERE activity_id = '" + linkedActivityId + "'"))
        .hasMessageContaining(
            "Cannot modify or delete activity-resource link of a PUBLISHED curriculum revision");

    // 6d. Attempting to revert published revision to DRAFT must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE curriculum_revision SET status = 'DRAFT' WHERE id = '"
                        + publishedRev.getId()
                        + "'"))
        .hasMessageContaining("Cannot revert PUBLISHED curriculum revision to DRAFT");

    // 6e. Attempting to modify published_at timestamp must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE curriculum_revision SET published_at = NOW() - INTERVAL '1 day' WHERE id = '"
                        + publishedRev.getId()
                        + "'"))
        .hasMessageContaining(
            "curriculum revision is immutable and its metadata/timestamps cannot be modified");

    // 6f. Attempting to modify revision metadata (title) must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE curriculum_revision SET title = 'Altered Title' WHERE id = '"
                        + publishedRev.getId()
                        + "'"))
        .hasMessageContaining(
            "curriculum revision is immutable and its metadata/timestamps cannot be modified");

    // 6g. Attempting to delete published revision must fail
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "DELETE FROM curriculum_revision WHERE id = '" + publishedRev.getId() + "'"))
        .hasMessageContaining("Cannot delete a PUBLISHED curriculum revision");

    // 7. R3: Re-assigning parent between published and draft must fail
    UUID draftRevId = UUID.randomUUID();
    jdbcTemplate.execute(
        "INSERT INTO curriculum_revision (id, curriculum_id, version, status, title, description, authors, provenance, checksum, created_at) "
            + "VALUES ('"
            + draftRevId
            + "', '"
            + revision.getCurriculum().getId()
            + "', '2.0.0-draft', 'DRAFT', 'Draft Rev', 'Desc', '[]', '{}', 'chk2', NOW())");
    UUID draftPhaseId = UUID.randomUUID();
    jdbcTemplate.execute(
        "INSERT INTO curriculum_phase (id, revision_id, phase_key, title, order_index) "
            + "VALUES ('"
            + draftPhaseId
            + "', '"
            + draftRevId
            + "', 'draft.phase', 'Draft Phase', 1)");
    UUID draftWeekId = UUID.randomUUID();
    jdbcTemplate.execute(
        "INSERT INTO curriculum_week (id, phase_id, week_key, week_number, order_index, title, description) "
            + "VALUES ('"
            + draftWeekId
            + "', '"
            + draftPhaseId
            + "', 'draft.week', 1, 1, 'Draft Week', 'Desc')");

    // 7a. Attempting to steal a published phase into the draft revision must fail (OLD is
    // published)
    UUID publishedPhaseId =
        phaseRepository.findByRevisionIdOrderByOrderIndexAsc(publishedRev.getId()).get(0).getId();
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE curriculum_phase SET revision_id = '"
                        + draftRevId
                        + "' WHERE id = '"
                        + publishedPhaseId
                        + "'"))
        .hasMessageContaining("Cannot modify or delete phase of a PUBLISHED curriculum revision");

    // 7b. Attempting to steal a published lesson into a draft week must fail (OLD is published)
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE lesson SET week_id = '"
                        + draftWeekId
                        + "' WHERE id = '"
                        + lessons.get(0).getId()
                        + "'"))
        .hasMessageContaining("Cannot modify or delete lesson of a PUBLISHED curriculum revision");

    // 7c. Attempting to move a draft week into a published phase must fail (NEW is published)
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    "UPDATE curriculum_week SET phase_id = '"
                        + publishedPhaseId
                        + "' WHERE id = '"
                        + draftWeekId
                        + "'"))
        .hasMessageContaining("Cannot insert or reassign week to a PUBLISHED curriculum revision");

    // 8. R3: Valid DRAFT revisions can still be mutated and deleted freely
    UUID draftLessonId = UUID.randomUUID();
    jdbcTemplate.execute(
        "INSERT INTO lesson (id, week_id, lesson_key, order_index, title, estimated_minutes, is_required, content_markdown) "
            + "VALUES ('"
            + draftLessonId
            + "', '"
            + draftWeekId
            + "', 'draft.l01', 1, 'Draft Lesson', 15, true, '# Draft Markdown')");

    // Draft lesson markdown can be updated
    int rowsUpdated =
        jdbcTemplate.update(
            "UPDATE lesson SET content_markdown = '# Updated Draft Markdown' WHERE id = '"
                + draftLessonId
                + "'");
    assertThat(rowsUpdated).isEqualTo(1);

    // Draft lesson can be deleted
    int rowsDeleted = jdbcTemplate.update("DELETE FROM lesson WHERE id = '" + draftLessonId + "'");
    assertThat(rowsDeleted).isEqualTo(1);

    // Clean up temporary draft revision
    jdbcTemplate.execute("DELETE FROM curriculum_week WHERE id = '" + draftWeekId + "'");
    jdbcTemplate.execute("DELETE FROM curriculum_phase WHERE id = '" + draftPhaseId + "'");
    jdbcTemplate.execute("DELETE FROM curriculum_revision WHERE id = '" + draftRevId + "'");
  }

  @Test
  @DisplayName(
      "Rollback verification: mid-import failure after partial writes rolls back entire transaction")
  void rollbackOnFailure_midImport_rollsBackEntirely(@org.junit.jupiter.api.io.TempDir Path tempDir)
      throws Exception {
    Path source = Paths.get("../content/pilot-curriculum/v1").toRealPath();
    try (var stream = java.nio.file.Files.walk(source)) {
      stream.forEach(
          src -> {
            try {
              Path dest = tempDir.resolve(source.relativize(src));
              if (java.nio.file.Files.isDirectory(src)) {
                if (!java.nio.file.Files.exists(dest)) java.nio.file.Files.createDirectories(dest);
              } else {
                java.nio.file.Files.copy(
                    src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    // Change version to 3.0.0 so it is a completely valid package that passes validation and
    // checksum
    Path manifestPath = tempDir.resolve("manifest.json");
    java.nio.file.Files.writeString(
        manifestPath,
        java.nio.file.Files.readString(manifestPath)
            .replace("\"version\": \"1.0.0\"", "\"version\": \"3.0.0\""));
    Path resPath = tempDir.resolve("resources.json");
    java.nio.file.Files.writeString(
        resPath,
        java.nio.file.Files.readString(resPath)
            .replace("\"version\": \"1.0.0\"", "\"version\": \"3.0.0\""));

    // Add temporary DB trigger to simulate mid-transaction failure during activity insert
    // (after revision, resources, phase, week, and initial lessons are already inserted)
    jdbcTemplate.execute(
        "CREATE OR REPLACE FUNCTION trg_simulated_mid_import_error() RETURNS TRIGGER AS $$ "
            + "BEGIN "
            + "  IF NEW.activity_key = 'pilot.w01.l02.a01' THEN "
            + "    RAISE EXCEPTION 'Simulated mid-transaction DB failure on activity %', NEW.activity_key; "
            + "  END IF; "
            + "  RETURN NEW; "
            + "END; "
            + "$$ LANGUAGE plpgsql;");
    jdbcTemplate.execute(
        "DROP TRIGGER IF EXISTS trg_test_mid_import_failure ON activity; "
            + "CREATE TRIGGER trg_test_mid_import_failure "
            + "BEFORE INSERT ON activity "
            + "FOR EACH ROW "
            + "EXECUTE FUNCTION trg_simulated_mid_import_error();");

    try {
      // Import must fail mid-transaction
      assertThatThrownBy(() -> importService.importPackage(tempDir))
          .hasMessageContaining("Simulated mid-transaction DB failure on activity");

      // Verify 100% rollback: revision 3.0.0 must NOT exist
      assertThat(
              revisionRepository.findByCurriculumCurriculumKeyAndVersion(
                  "pilot-curriculum", "3.0.0"))
          .isEmpty();

      // Verify no dangling resources or lessons for 3.0.0
      Integer danglingResources =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM resource WHERE revision_id IN "
                  + "(SELECT id FROM curriculum_revision WHERE version = '3.0.0')",
              Integer.class);
      assertThat(danglingResources).isEqualTo(0);

      Integer danglingPhases =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM curriculum_phase WHERE revision_id IN "
                  + "(SELECT id FROM curriculum_revision WHERE version = '3.0.0')",
              Integer.class);
      assertThat(danglingPhases).isEqualTo(0);
    } finally {
      // Clean up simulated failure trigger
      jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_test_mid_import_failure ON activity;");
      jdbcTemplate.execute("DROP FUNCTION IF EXISTS trg_simulated_mid_import_error();");
    }
  }

  @Test
  @DisplayName("CLI smoke test: runs import command successfully against isolated test DB")
  void cli_import_smokeTestWithIsolatedDb() {
    int exitCode =
        vn.englishlearning.contentops.cli.CurriculumCli.execute(
            new String[] {
              "content",
              "import",
              "../content/pilot-curriculum/v1",
              "--db-url",
              postgres.getJdbcUrl(),
              "--db-user",
              postgres.getUsername(),
              "--db-password",
              postgres.getPassword()
            });
    assertThat(exitCode).isEqualTo(0);
  }
}
