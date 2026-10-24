package vn.englishlearning.curriculum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
                        a.getLesson().isRequired(), a.isRequired()))
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
  }
}
