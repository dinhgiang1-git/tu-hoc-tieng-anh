package vn.englishlearning.contentops.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.contentops.domain.ContentConflictException;
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
import vn.englishlearning.curriculum.domain.RevisionStatus;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumPhaseRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.ResourceRepository;

class ImportAndPublishServiceTest {

  private CurriculumRepository curriculumRepository;
  private CurriculumRevisionRepository revisionRepository;
  private CurriculumPhaseRepository phaseRepository;
  private CurriculumWeekRepository weekRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private ResourceRepository resourceRepository;
  private ObjectMapper objectMapper;
  private Clock fixedClock;

  private ImportCurriculumPackageService importService;
  private PublishCurriculumRevisionService publishService;

  @BeforeEach
  void setUp() {
    curriculumRepository = mock(CurriculumRepository.class);
    revisionRepository = mock(CurriculumRevisionRepository.class);
    phaseRepository = mock(CurriculumPhaseRepository.class);
    weekRepository = mock(CurriculumWeekRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    resourceRepository = mock(ResourceRepository.class);
    objectMapper = new ObjectMapper();
    fixedClock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

    ContentPackageValidator validator = new ContentPackageValidator(objectMapper);
    PackageChecksumCalculator checksumCalc = new PackageChecksumCalculator(objectMapper);

    importService =
        new ImportCurriculumPackageService(
            validator,
            checksumCalc,
            curriculumRepository,
            revisionRepository,
            phaseRepository,
            weekRepository,
            lessonRepository,
            activityRepository,
            resourceRepository,
            objectMapper,
            fixedClock);

    publishService =
        new PublishCurriculumRevisionService(
            revisionRepository,
            phaseRepository,
            lessonRepository,
            activityRepository,
            resourceRepository,
            objectMapper,
            fixedClock);
  }

  @Test
  @DisplayName("Imports package successfully when curriculum and revision do not exist")
  void importPackage_newCurriculumAndRevision_success() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    when(curriculumRepository.findByCurriculumKey("pilot-curriculum")).thenReturn(Optional.empty());
    when(curriculumRepository.save(any(Curriculum.class))).thenReturn(curriculum);
    when(revisionRepository.findByCurriculumIdAndVersion(any(), any()))
        .thenReturn(Optional.empty());
    when(revisionRepository.save(any(CurriculumRevision.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(phaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(weekRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lessonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ImportSummary summary = importService.importPackage(packageDir);

    assertThat(summary.curriculumKey()).isEqualTo("pilot-curriculum");
    assertThat(summary.version()).isEqualTo("1.0.0");
    assertThat(summary.status()).isEqualTo("DRAFT");
    assertThat(summary.newlyImported()).isTrue();
    assertThat(summary.phaseCount()).isEqualTo(1);
    assertThat(summary.weekCount()).isEqualTo(1);
    assertThat(summary.lessonCount()).isEqualTo(6);
    assertThat(summary.activityCount()).isEqualTo(21);
    assertThat(summary.resourceCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("Re-importing exact same package is idempotent and creates no duplicates")
  void importPackage_identicalPackage_idempotent() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    when(curriculumRepository.findByCurriculumKey("pilot-curriculum"))
        .thenReturn(Optional.of(curriculum));

    PackageChecksumCalculator calc = new PackageChecksumCalculator(objectMapper);
    String checksum;
    try {
      checksum = calc.calculate(packageDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    CurriculumRevision existingRevision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Title",
            "Desc",
            "[\"Author\"]",
            "{}",
            checksum,
            Instant.now(fixedClock));

    when(revisionRepository.findByCurriculumIdAndVersion(any(), any()))
        .thenReturn(Optional.of(existingRevision));

    ImportSummary summary = importService.importPackage(packageDir);

    assertThat(summary.newlyImported()).isFalse();
    assertThat(summary.checksum()).isEqualTo(checksum);
    verify(phaseRepository, never()).save(any());
  }

  @Test
  @DisplayName("Importing package with different content on same draft version reports conflict")
  void importPackage_differentChecksum_throwsContentConflictException() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    when(curriculumRepository.findByCurriculumKey("pilot-curriculum"))
        .thenReturn(Optional.of(curriculum));

    CurriculumRevision existingRevision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Title",
            "Desc",
            "[\"Author\"]",
            "{}",
            "different-old-checksum-00000000000000000000000000000000000000000000000",
            Instant.now(fixedClock));

    when(revisionRepository.findByCurriculumIdAndVersion(any(), any()))
        .thenReturn(Optional.of(existingRevision));

    assertThatThrownBy(() -> importService.importPackage(packageDir))
        .isInstanceOf(ContentConflictException.class)
        .hasMessageContaining("already exists in DRAFT with a different checksum");
  }

  @Test
  @DisplayName("Importing package on an already PUBLISHED revision reports conflict")
  void importPackage_publishedRevision_throwsContentConflictException() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    when(curriculumRepository.findByCurriculumKey("pilot-curriculum"))
        .thenReturn(Optional.of(curriculum));

    CurriculumRevision publishedRevision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Title",
            "Desc",
            "[\"Author\"]",
            "{}",
            "different-old-checksum",
            Instant.now(fixedClock));
    publishedRevision.publish(Instant.now(fixedClock));

    when(revisionRepository.findByCurriculumIdAndVersion(any(), any()))
        .thenReturn(Optional.of(publishedRevision));

    assertThatThrownBy(() -> importService.importPackage(packageDir))
        .isInstanceOf(ContentConflictException.class)
        .hasMessageContaining("Published revisions are strictly immutable");
  }

  @Test
  @DisplayName("Publishes valid DRAFT revision successfully making it PUBLISHED")
  void publish_validDraftRevision_success() {
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Title",
            "Desc",
            "[\"Author\"]",
            "{\"sourceName\":\"Google Sheet\",\"verificationStatus\":\"UNVERIFIED_DRAFT\"}",
            "dummy-checksum",
            Instant.now(fixedClock));

    CurriculumPhase phase = new CurriculumPhase(revision, "pilot.phase01", "Phase 1", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "pilot.w01", 1, 1, "Week 1", "Desc");
    Lesson lesson =
        new Lesson(week, "pilot.w01.l01", 1, "Lesson 1", 25, true, "# Content Markdown");
    Activity activity =
        new Activity(
            lesson,
            "pilot.w01.l01.a01",
            1,
            "Activity 1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Instructions",
            null);

    when(revisionRepository.findByCurriculumCurriculumKeyAndVersion("pilot-curriculum", "1.0.0"))
        .thenReturn(Optional.of(revision));
    when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(phaseRepository.findByRevisionIdOrderByOrderIndexAsc(revision.getId()))
        .thenReturn(List.of(phase));
    when(lessonRepository.findByWeekPhaseRevisionId(revision.getId())).thenReturn(List.of(lesson));
    when(activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getId()))
        .thenReturn(List.of(activity));

    PublishResult result = publishService.publish("pilot-curriculum", "1.0.0");

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(result.publishedAt()).isEqualTo(Instant.now(fixedClock));
    assertThat(revision.getStatus()).isEqualTo(RevisionStatus.PUBLISHED);
    assertThat(result.warnings())
        .anyMatch(w -> w.contains("Provenance status is UNVERIFIED_DRAFT"));
  }

  @Test
  @DisplayName("Publishing an already PUBLISHED revision throws IllegalStateException")
  void publish_alreadyPublished_throwsIllegalStateException() {
    Curriculum curriculum = new Curriculum("pilot-curriculum", "en-GB", Instant.now(fixedClock));
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Title",
            "Desc",
            "[\"Author\"]",
            "{}",
            "dummy-checksum",
            Instant.now(fixedClock));
    revision.publish(Instant.now(fixedClock));

    when(revisionRepository.findByCurriculumCurriculumKeyAndVersion("pilot-curriculum", "1.0.0"))
        .thenReturn(Optional.of(revision));

    assertThatThrownBy(() -> publishService.publish("pilot-curriculum", "1.0.0"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already PUBLISHED");
  }

  @Test
  @DisplayName(
      "Pilot curriculum activities without orderIndex are normalized sequentially (1, 2, 3, 4...)")
  void pilotActivitiesHaveSequentialOrder() {
    importPackage_newCurriculumAndRevision_success();
    org.mockito.ArgumentCaptor<Activity> activities =
        org.mockito.ArgumentCaptor.forClass(Activity.class);
    verify(activityRepository, org.mockito.Mockito.times(21)).save(activities.capture());
    assertThat(activities.getAllValues().subList(0, 4))
        .extracting(Activity::getOrderIndex)
        .containsExactly(1, 2, 3, 4);
  }
}
