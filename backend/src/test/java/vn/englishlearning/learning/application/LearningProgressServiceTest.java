package vn.englishlearning.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.Curriculum;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityProjectionDto;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.LessonSummaryDto;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.learning.api.LearningProgressRequests.SubmitResponseRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.UpdateProgressRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonActivitiesResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressSummaryResponse;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.LearningEvent;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.learning.infrastructure.LearningEventRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

class LearningProgressServiceTest {

  private Clock clock;
  private EnrollmentRepository enrollmentRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private ActivityProgressRepository activityProgressRepository;
  private LearningEventRepository learningEventRepository;
  private AppUserRepository userRepository;
  private CurrentUser currentUser;
  private jakarta.persistence.EntityManager entityManager;

  private LearningProgressService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final UUID lessonId = UUID.randomUUID();
  private final UUID activity1Id = UUID.randomUUID();
  private final UUID activity2Id = UUID.randomUUID();
  private final UUID optionalActivityId = UUID.randomUUID();

  private final Instant now = Instant.parse("2026-10-25T10:00:00Z");

  private Enrollment enrollment;
  private Lesson lesson;
  private Activity activity1;
  private Activity activity2;
  private Activity optionalActivity;
  private LessonSchedule lessonSchedule;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(now, ZoneId.of("Asia/Ho_Chi_Minh"));
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    activityProgressRepository = mock(ActivityProgressRepository.class);
    learningEventRepository = mock(LearningEventRepository.class);
    userRepository = mock(AppUserRepository.class);
    currentUser = mock(CurrentUser.class);
    entityManager = mock(jakarta.persistence.EntityManager.class);

    service =
        new LearningProgressService(
            clock,
            enrollmentRepository,
            lessonScheduleRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            learningEventRepository,
            userRepository,
            currentUser,
            entityManager);

    when(currentUser.requireUserId()).thenReturn(userId);
    when(enrollmentRepository.updateVersionAndState(any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // Curriculum hierarchy
    Curriculum curriculum = new Curriculum("code", "en-GB", now);
    ReflectionTestUtils.setField(curriculum, "id", curriculumId);

    CurriculumRevision revision =
        new CurriculumRevision(curriculum, "1.0", "title", "desc", "auth", "prov", "hash", now);
    ReflectionTestUtils.setField(revision, "id", revisionId);

    CurriculumPhase phase = new CurriculumPhase(revision, "p1", "Phase 1", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "w1", 1, 1, "Week 1", "Goal");
    UUID weekId = UUID.randomUUID();
    ReflectionTestUtils.setField(week, "id", weekId);

    lesson = new Lesson(week, "lesson-1", 1, "Buổi 1", 15, true, "Content");
    ReflectionTestUtils.setField(lesson, "id", lessonId);

    activity1 =
        new Activity(
            lesson,
            "act-1",
            1,
            "Activity 1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Instructions 1",
            null);
    ReflectionTestUtils.setField(activity1, "id", activity1Id);

    activity2 =
        new Activity(
            lesson,
            "act-2",
            2,
            "Activity 2",
            ActivityType.REFLECTION,
            true,
            CompletionPolicy.RESPONSE_SUBMIT,
            "Instructions 2",
            null);
    ReflectionTestUtils.setField(activity2, "id", activity2Id);

    optionalActivity =
        new Activity(
            lesson,
            "act-opt",
            3,
            "Optional Activity",
            ActivityType.READING,
            false,
            CompletionPolicy.MANUAL_CHECK,
            "Instructions Opt",
            null);
    ReflectionTestUtils.setField(optionalActivity, "id", optionalActivityId);

    // Enrollment
    enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            now);
    ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    enrollment.setVersion(1L);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .thenAnswer(i -> i.getArgument(0));

    // LessonSchedule
    lessonSchedule =
        new LessonSchedule(
            enrollmentId, lessonId, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26), now);
    when(lessonScheduleRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId))
        .thenReturn(Optional.of(lessonSchedule));

    // Repositories for activity and stats
    when(activityRepository.findById(activity1Id)).thenReturn(Optional.of(activity1));
    when(activityRepository.findById(activity2Id)).thenReturn(Optional.of(activity2));
    when(activityRepository.findById(optionalActivityId)).thenReturn(Optional.of(optionalActivity));

    // Projections for lesson
    when(activityRepository.findActivityProjectionsByLessonId(lessonId))
        .thenReturn(
            List.of(
                new ActivityProjectionDto(
                    activity1Id,
                    lessonId,
                    1,
                    "Activity 1",
                    ActivityType.READING,
                    true,
                    CompletionPolicy.MANUAL_CHECK),
                new ActivityProjectionDto(
                    activity2Id,
                    lessonId,
                    2,
                    "Activity 2",
                    ActivityType.REFLECTION,
                    true,
                    CompletionPolicy.RESPONSE_SUBMIT),
                new ActivityProjectionDto(
                    optionalActivityId,
                    lessonId,
                    3,
                    "Optional Activity",
                    ActivityType.READING,
                    false,
                    CompletionPolicy.MANUAL_CHECK)));

    // Summaries for stats
    LessonSummaryDto lessonSummary =
        new LessonSummaryDto(lessonId, revisionId, weekId, 1, "Buổi 1", 15, true);
    when(lessonRepository.findLessonSummaryById(lessonId)).thenReturn(Optional.of(lessonSummary));
    when(lessonRepository.findLessonSummariesByRevisionId(revisionId))
        .thenReturn(List.of(lessonSummary));

    when(activityRepository.findActivitySummariesByRevisionId(revisionId))
        .thenReturn(
            List.of(
                new ActivitySummaryDto(activity1Id, lessonId, 1, true),
                new ActivitySummaryDto(activity2Id, lessonId, 2, true),
                new ActivitySummaryDto(optionalActivityId, lessonId, 3, false)));

    when(activityProgressRepository.save(any(ActivityProgress.class)))
        .thenAnswer(i -> i.getArgument(0));
  }

  @Test
  @DisplayName("updateProgress: Manual check completed sets COMPLETED and creates event")
  void updateProgress_manualCheck_completed_success() {
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    // Initial progress empty
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.empty());

    // Stats mock after completion: 1 of 2 completed
    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(new ActivityProgressDto(enrollmentId, activity1Id, ActivityStatus.COMPLETED)));

    ProgressResponse response = service.updateProgress(enrollmentId, activity1Id, request);

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(response.completedRequiredActivities()).isEqualTo(1);
    assertThat(response.totalRequiredActivities()).isEqualTo(2);

    verify(activityProgressRepository).save(any(ActivityProgress.class));
    verify(learningEventRepository).save(any(LearningEvent.class));
    verify(enrollmentRepository)
        .updateVersionAndState(eq(enrollmentId), eq(1L), eq(2L), any(), any(), any());
    assertThat(response.enrollmentVersion()).isEqualTo(2L);
  }

  @Test
  @DisplayName("updateProgress: Undo completed sets NOT_STARTED when no response text exists")
  void updateProgress_undo_completedToNotStarted_whenNoResponse() {
    ActivityProgress existingProgress =
        new ActivityProgress(enrollmentId, activity1Id, ActivityStatus.COMPLETED, now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.of(existingProgress));

    lessonSchedule.setCompletedAt(now, now);

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 1L);

    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(
                new ActivityProgressDto(enrollmentId, activity1Id, ActivityStatus.NOT_STARTED)));

    ProgressResponse response = service.updateProgress(enrollmentId, activity1Id, request);

    assertThat(response.status()).isEqualTo(ActivityStatus.NOT_STARTED);
    assertThat(existingProgress.getCompletedAt()).isNull();
    assertThat(lessonSchedule.getCompletedAt()).isNull();

    verify(learningEventRepository).save(any(LearningEvent.class));
  }

  @Test
  @DisplayName("updateProgress: Undo completed sets IN_PROGRESS when response text exists")
  void updateProgress_undo_completedToInProgress_whenResponseExists() {
    ActivityProgress existingProgress =
        new ActivityProgress(
            enrollmentId, activity1Id, ActivityStatus.COMPLETED, "Some response", now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.of(existingProgress));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 1L);

    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(
                new ActivityProgressDto(enrollmentId, activity1Id, ActivityStatus.IN_PROGRESS)));

    ProgressResponse response = service.updateProgress(enrollmentId, activity1Id, request);

    assertThat(response.status()).isEqualTo(ActivityStatus.IN_PROGRESS);
    assertThat(existingProgress.getCompletedAt()).isNull();
  }

  @Test
  @DisplayName(
      "updateProgress: No-op returns current state without saving event or updating enrollment")
  void updateProgress_noOp_doesNotIncrementVersionOrSaveEvent() {
    ActivityProgress existingProgress =
        new ActivityProgress(enrollmentId, activity1Id, ActivityStatus.COMPLETED, now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.of(existingProgress));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(new ActivityProgressDto(enrollmentId, activity1Id, ActivityStatus.COMPLETED)));

    ProgressResponse response = service.updateProgress(enrollmentId, activity1Id, request);

    assertThat(response.status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(response.enrollmentVersion()).isEqualTo(1L);

    verify(activityProgressRepository, never()).save(any());
    verify(learningEventRepository, never()).save(any());
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("updateProgress: Stale expectedVersion throws VERSION_CONFLICT")
  void updateProgress_staleVersion_throwsVersionConflict() {
    enrollment.setVersion(2L);
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("updateProgress: Manual check on QUIZ activity throws STATE_CONFLICT")
  void updateProgress_quizActivity_throwsStateConflict() {
    Activity quizActivity =
        new Activity(
            lesson,
            "quiz-1",
            4,
            "Quiz Act",
            ActivityType.QUIZ,
            true,
            CompletionPolicy.PASS_SCORE,
            "Quiz",
            null);
    UUID quizId = UUID.randomUUID();
    ReflectionTestUtils.setField(quizActivity, "id", quizId);
    when(activityRepository.findById(quizId)).thenReturn(Optional.of(quizActivity));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, quizId, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("updateProgress: Requires response without response text throws RESPONSE_REQUIRED")
  void updateProgress_requiresResponse_throwsResponseRequired() {
    Activity respActivity =
        new Activity(
            lesson,
            "resp-1",
            4,
            "Resp Act",
            ActivityType.PRACTICE,
            true,
            CompletionPolicy.MANUAL_CHECK_WITH_RESPONSE,
            "Practice",
            null);
    UUID respId = UUID.randomUUID();
    ReflectionTestUtils.setField(respActivity, "id", respId);
    when(activityRepository.findById(respId)).thenReturn(Optional.of(respActivity));
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, respId))
        .thenReturn(Optional.empty());

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, respId, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESPONSE_REQUIRED);
  }

  @Test
  @DisplayName(
      "updateProgress: Reflection with RESPONSE_SUBMIT policy throws RESPONSE_REQUIRED when no text")
  void updateProgress_reflectionPolicy_throwsResponseRequired() {
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity2Id))
        .thenReturn(Optional.empty());

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity2Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESPONSE_REQUIRED);
  }

  @Test
  @DisplayName("updateProgress: Unauthorized user throws RESOURCE_NOT_FOUND")
  void updateProgress_wrongOwner_throwsResourceNotFound() {
    when(currentUser.requireUserId()).thenReturn(otherUserId);
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("updateProgress: Activity in another revision throws RESOURCE_NOT_FOUND")
  void updateProgress_activityInAnotherRevision_throwsResourceNotFound() {
    Curriculum curriculum = new Curriculum("code2", "en-GB", now);
    CurriculumRevision otherRevision =
        new CurriculumRevision(curriculum, "1.0", "other", "desc", "auth", "prov", "hash", now);
    ReflectionTestUtils.setField(otherRevision, "id", UUID.randomUUID());
    CurriculumPhase phase = new CurriculumPhase(otherRevision, "p", "P", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "w", 1, 1, "W", "G");
    Lesson otherLesson = new Lesson(week, "other-l", 1, "Other L", 10, true, "C");
    Activity otherAct =
        new Activity(
            otherLesson,
            "oact",
            1,
            "Other Act",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "I",
            null);
    UUID otherActId = UUID.randomUUID();
    ReflectionTestUtils.setField(otherAct, "id", otherActId);
    when(activityRepository.findById(otherActId)).thenReturn(Optional.of(otherAct));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, otherActId, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("updateProgress: PAUSED or ABANDONED enrollment throws STATE_CONFLICT")
  void updateProgress_pausedOrAbandoned_throwsStateConflict() {
    enrollment.setStatus(EnrollmentStatus.PAUSED);
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);

    enrollment.setStatus(EnrollmentStatus.ABANDONED);
    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName(
      "updateProgress: Undoing required activity on COMPLETED enrollment reopens to ACTIVE when no conflict")
  void updateProgress_completedEnrollment_reopensActive_whenNoConflict() {
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    enrollment.setCompletedAt(now);

    ActivityProgress existingProgress =
        new ActivityProgress(enrollmentId, activity1Id, ActivityStatus.COMPLETED, now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.of(existingProgress));

    when(enrollmentRepository.findByUserIdAndCurriculumIdAndStatusInAndIdNot(
            eq(userId), eq(curriculumId), any(), eq(enrollmentId)))
        .thenReturn(List.of());

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 1L);

    ProgressResponse response = service.updateProgress(enrollmentId, activity1Id, request);

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(enrollment.getCompletedAt()).isNull();
    verify(userRepository).findByIdForUpdate(userId);
  }

  @Test
  @DisplayName(
      "updateProgress: Undoing required activity on COMPLETED enrollment throws STATE_CONFLICT if another active exists")
  void updateProgress_completedEnrollment_conflictExists_throwsStateConflict() {
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    enrollment.setCompletedAt(now);

    ActivityProgress existingProgress =
        new ActivityProgress(enrollmentId, activity1Id, ActivityStatus.COMPLETED, now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity1Id))
        .thenReturn(Optional.of(existingProgress));

    Enrollment conflictingEnrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findByUserIdAndCurriculumIdAndStatusInAndIdNot(
            eq(userId), eq(curriculumId), any(), eq(enrollmentId)))
        .thenReturn(List.of(conflictingEnrollment));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 1L);

    assertThatThrownBy(() -> service.updateProgress(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName(
      "updateProgress: Undoing OPTIONAL activity on COMPLETED enrollment does not reopen enrollment")
  void updateProgress_completedEnrollment_undoOptional_doesNotReopen() {
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    enrollment.setCompletedAt(now);

    ActivityProgress existingProgress =
        new ActivityProgress(enrollmentId, optionalActivityId, ActivityStatus.COMPLETED, now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(
            enrollmentId, optionalActivityId))
        .thenReturn(Optional.of(existingProgress));

    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.NOT_STARTED, 1L);

    service.updateProgress(enrollmentId, optionalActivityId, request);

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(enrollment.getCompletedAt()).isEqualTo(now);
    verify(userRepository, never()).findByIdForUpdate(any());
  }

  @Test
  @DisplayName("submitResponse: Valid text completes activity and records SUBMITTED event")
  void submitResponse_success() {
    SubmitResponseRequest request = new SubmitResponseRequest("My reflective answer", 1L);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity2Id))
        .thenReturn(Optional.empty());

    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(new ActivityProgressDto(enrollmentId, activity2Id, ActivityStatus.COMPLETED)));

    ProgressResponse response = service.submitResponse(enrollmentId, activity2Id, request);

    assertThat(response.status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(response.enrollmentVersion()).isEqualTo(2L);
    verify(activityProgressRepository).save(any(ActivityProgress.class));
    verify(learningEventRepository).save(any(LearningEvent.class));
    verify(enrollmentRepository)
        .updateVersionAndState(eq(enrollmentId), eq(1L), eq(2L), any(), any(), any());
  }

  @Test
  @DisplayName("submitResponse: Blank or whitespace response throws RESPONSE_REQUIRED")
  void submitResponse_blankText_throwsResponseRequired() {
    SubmitResponseRequest request = new SubmitResponseRequest("   ", 1L);

    assertThatThrownBy(() -> service.submitResponse(enrollmentId, activity2Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESPONSE_REQUIRED);
  }

  @Test
  @DisplayName("submitResponse: Response text exceeding 5000 characters throws VALIDATION_FAILED")
  void submitResponse_tooLong_throwsValidationFailed() {
    String longText = "a".repeat(5001);
    SubmitResponseRequest request = new SubmitResponseRequest(longText, 1L);

    assertThatThrownBy(() -> service.submitResponse(enrollmentId, activity2Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("submitResponse: Stale expectedVersion throws VERSION_CONFLICT")
  void submitResponse_staleVersion_throwsVersionConflict() {
    enrollment.setVersion(2L);
    SubmitResponseRequest request = new SubmitResponseRequest("Text", 1L);

    assertThatThrownBy(() -> service.submitResponse(enrollmentId, activity2Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("submitResponse: Wrong activity type (READING) throws STATE_CONFLICT")
  void submitResponse_wrongActivityType_throwsStateConflict() {
    SubmitResponseRequest request = new SubmitResponseRequest("Text", 1L);

    assertThatThrownBy(() -> service.submitResponse(enrollmentId, activity1Id, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("submitResponse: No-op returns existing state when already COMPLETED with same text")
  void submitResponse_noOp_returnsExisting() {
    ActivityProgress existingProgress =
        new ActivityProgress(
            enrollmentId, activity2Id, ActivityStatus.COMPLETED, "Existing text", now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activity2Id))
        .thenReturn(Optional.of(existingProgress));

    SubmitResponseRequest request = new SubmitResponseRequest("Existing text", 1L);

    ProgressResponse response = service.submitResponse(enrollmentId, activity2Id, request);

    assertThat(response.status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(response.enrollmentVersion()).isEqualTo(1L);

    verify(activityProgressRepository, never()).save(any());
    verify(learningEventRepository, never()).save(any());
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("getSummary: Returns progress stats and enrollment version for owner")
  void getSummary_success() {
    when(activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId))
        .thenReturn(
            List.of(new ActivityProgressDto(enrollmentId, activity1Id, ActivityStatus.COMPLETED)));

    ProgressSummaryResponse response = service.getSummary(enrollmentId);

    assertThat(response.completedRequiredActivities()).isEqualTo(1);
    assertThat(response.totalRequiredActivities()).isEqualTo(2);
    assertThat(response.progressPercent()).isEqualTo(new BigDecimal("50.0"));
    assertThat(response.enrollmentVersion()).isEqualTo(1L);
  }

  @Test
  @DisplayName("getSummary: Non-owner throws RESOURCE_NOT_FOUND")
  void getSummary_wrongOwner_throwsResourceNotFound() {
    when(currentUser.requireUserId()).thenReturn(otherUserId);

    assertThatThrownBy(() -> service.getSummary(enrollmentId))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("getLessonActivities: Returns lightweight activity list with statuses")
  void getLessonActivities_success() {
    ActivityProgress progress =
        new ActivityProgress(enrollmentId, activity2Id, ActivityStatus.COMPLETED, "My text", now);
    when(activityProgressRepository.findByEnrollmentIdAndActivityIdIn(eq(enrollmentId), any()))
        .thenReturn(List.of(progress));

    LessonActivitiesResponse response = service.getLessonActivities(enrollmentId, lessonId);

    assertThat(response.lessonId()).isEqualTo(lessonId);
    assertThat(response.enrollmentVersion()).isEqualTo(1L);
    assertThat(response.activities()).hasSize(3);

    // Activity 1: not started
    assertThat(response.activities().get(0).id()).isEqualTo(activity1Id);
    assertThat(response.activities().get(0).status()).isEqualTo(ActivityStatus.NOT_STARTED);
    assertThat(response.activities().get(0).responseText()).isNull();

    // Activity 2: completed with response
    assertThat(response.activities().get(1).id()).isEqualTo(activity2Id);
    assertThat(response.activities().get(1).status()).isEqualTo(ActivityStatus.COMPLETED);
    assertThat(response.activities().get(1).responseText()).isEqualTo("My text");
  }

  @Test
  @DisplayName("getLessonActivities: Non-owner throws RESOURCE_NOT_FOUND")
  void getLessonActivities_wrongOwner_throwsResourceNotFound() {
    when(currentUser.requireUserId()).thenReturn(otherUserId);

    assertThatThrownBy(() -> service.getLessonActivities(enrollmentId, lessonId))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }
}
