package vn.englishlearning.assessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
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
import vn.englishlearning.assessment.api.QuizRequests.QuizAnswerItemRequest;
import vn.englishlearning.assessment.api.QuizRequests.QuizSubmitRequest;
import vn.englishlearning.assessment.api.QuizResponses.QuizDetailResponse;
import vn.englishlearning.assessment.api.QuizResponses.QuizResultResponse;
import vn.englishlearning.assessment.domain.QuizOption;
import vn.englishlearning.assessment.domain.QuizQuestion;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.assessment.infrastructure.QuizQuestionRepository;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.learning.application.LearningProgressService;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.application.IdempotencyService;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.security.CurrentUser;

class QuizServiceTest {

  private QuizQuestionRepository quizQuestionRepository;
  private QuizAttemptRepository quizAttemptRepository;
  private EnrollmentRepository enrollmentRepository;
  private ActivityRepository activityRepository;
  private ActivityProgressRepository activityProgressRepository;
  private LearningProgressService learningProgressService;
  private IdempotencyService idempotencyService;
  private CurrentUser currentUser;
  private Clock clock;

  private QuizService quizService;

  private final UUID userId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final UUID activityId = UUID.randomUUID();
  private final Instant fixedNow = Instant.parse("2026-10-28T10:00:00Z");

  @BeforeEach
  void setUp() {
    quizQuestionRepository = mock(QuizQuestionRepository.class);
    quizAttemptRepository = mock(QuizAttemptRepository.class);
    enrollmentRepository = mock(EnrollmentRepository.class);
    activityRepository = mock(ActivityRepository.class);
    activityProgressRepository = mock(ActivityProgressRepository.class);
    learningProgressService = mock(LearningProgressService.class);
    idempotencyService = mock(IdempotencyService.class);
    currentUser = mock(CurrentUser.class);
    clock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

    when(currentUser.requireUserId()).thenReturn(userId);

    quizService =
        new QuizService(
            quizQuestionRepository,
            quizAttemptRepository,
            enrollmentRepository,
            activityRepository,
            activityProgressRepository,
            learningProgressService,
            idempotencyService,
            currentUser,
            clock);
  }

  private Enrollment createEnrollment(EnrollmentStatus status, Long version) {
    Enrollment enrollment =
        new Enrollment(
            userId,
            UUID.randomUUID(),
            revisionId,
            LocalDate.of(2026, 10, 1),
            "Asia/Ho_Chi_Minh",
            List.of(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.FRIDAY),
            30,
            fixedNow);
    ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    ReflectionTestUtils.setField(enrollment, "status", status);
    ReflectionTestUtils.setField(enrollment, "version", version);
    return enrollment;
  }

  private Activity mockActivity(CompletionPolicy completionPolicy, ActivityType activityType) {
    Activity activity = mock(Activity.class);
    Lesson lesson = mock(Lesson.class);
    CurriculumWeek week = mock(CurriculumWeek.class);
    CurriculumPhase phase = mock(CurriculumPhase.class);
    CurriculumRevision revision = mock(CurriculumRevision.class);

    when(activity.getId()).thenReturn(activityId);
    when(activity.getTitle()).thenReturn("Unit Quiz");
    when(activity.getActivityType()).thenReturn(activityType);
    when(activity.getCompletionPolicy()).thenReturn(completionPolicy);
    when(activity.getLesson()).thenReturn(lesson);
    when(lesson.getWeek()).thenReturn(week);
    when(week.getPhase()).thenReturn(phase);
    when(phase.getRevision()).thenReturn(revision);
    when(revision.getId()).thenReturn(revisionId);

    return activity;
  }

  private QuizQuestion createQuestionWithTwoOptions(
      int orderIndex, String prompt, String expl, String optA, String optB, boolean aIsCorrect) {
    QuizQuestion question = new QuizQuestion(activityId, orderIndex, prompt, expl, fixedNow);
    ReflectionTestUtils.setField(question, "id", UUID.randomUUID());

    QuizOption option1 = new QuizOption(1, optA, aIsCorrect, fixedNow);
    ReflectionTestUtils.setField(option1, "id", UUID.randomUUID());
    QuizOption option2 = new QuizOption(2, optB, !aIsCorrect, fixedNow);
    ReflectionTestUtils.setField(option2, "id", UUID.randomUUID());

    question.addOption(option1);
    question.addOption(option2);
    return question;
  }

  @Test
  @DisplayName("getQuizDetail: returns questions and options hiding isCorrect and explanation")
  void getQuizDetail_success() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    Activity activity = mockActivity(CompletionPolicy.PASS_SCORE, ActivityType.QUIZ);
    QuizQuestion q1 =
        createQuestionWithTwoOptions(
            1, "What is Spring?", "Spring is a framework", "Framework", "Fruit", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId)).thenReturn(List.of(q1));
    when(quizAttemptRepository.findTopByEnrollmentIdAndActivityIdOrderBySubmittedAtDesc(
            enrollmentId, activityId))
        .thenReturn(Optional.empty());

    QuizDetailResponse response = quizService.getQuizDetail(enrollmentId, activityId);

    assertThat(response.activityId()).isEqualTo(activityId);
    assertThat(response.completionPolicy()).isEqualTo("PASS_SCORE");
    assertThat(response.passingScorePercentage()).isEqualTo(80);
    assertThat(response.questions()).hasSize(1);
    assertThat(response.questions().get(0).prompt()).isEqualTo("What is Spring?");
    assertThat(response.questions().get(0).options()).hasSize(2);
    assertThat(response.latestAttempt()).isNull();
  }

  @Test
  @DisplayName("getQuizDetail: throws RESOURCE_NOT_FOUND when enrollment not owned by current user")
  void getQuizDetail_nonOwner_throwsNotFound() {
    Enrollment otherUserEnrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    ReflectionTestUtils.setField(otherUserEnrollment, "userId", UUID.randomUUID());

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(otherUserEnrollment));

    assertThatThrownBy(() -> quizService.getQuizDetail(enrollmentId, activityId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("getQuizDetail: throws VALIDATION_FAILED when activity is not QUIZ")
  void getQuizDetail_nonQuizActivity_throwsValidationFailed() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    Activity activity = mockActivity(CompletionPolicy.ON_SUBMIT, ActivityType.READING);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

    assertThatThrownBy(() -> quizService.getQuizDetail(enrollmentId, activityId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("submitQuiz: ON_SUBMIT marks activity COMPLETED regardless of score")
  void submitQuiz_onSubmit_marksCompleted() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    Activity activity = mockActivity(CompletionPolicy.ON_SUBMIT, ActivityType.QUIZ);

    QuizQuestion q1 = createQuestionWithTwoOptions(1, "Q1", "Exp1", "Correct", "Wrong", true);
    QuizQuestion q2 = createQuestionWithTwoOptions(2, "Q2", "Exp2", "Correct", "Wrong", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId))
        .thenReturn(List.of(q1, q2));
    when(quizAttemptRepository.countByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(0);

    ActivityProgress mockProgress = mock(ActivityProgress.class);
    when(mockProgress.getStatus()).thenReturn(ActivityStatus.COMPLETED);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(Optional.of(mockProgress));

    when(learningProgressService.recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(true), eq(1), eq(2), eq(1L)))
        .thenReturn(2L);

    // User chooses Correct for Q1, Wrong for Q2 (Score = 1/2 = 50%)
    UUID q1CorrectOpt = q1.getOptions().get(0).getId();
    UUID q2WrongOpt = q2.getOptions().get(1).getId();

    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L,
            List.of(
                new QuizAnswerItemRequest(q1.getId(), q1CorrectOpt),
                new QuizAnswerItemRequest(q2.getId(), q2WrongOpt)));

    QuizResultResponse response = quizService.submitQuiz(null, enrollmentId, activityId, request);

    assertThat(response.score()).isEqualTo(1);
    assertThat(response.totalQuestions()).isEqualTo(2);
    assertThat(response.scorePercentage()).isEqualTo(50);
    assertThat(response.passed()).isTrue(); // ON_SUBMIT policy always passes on submit
    assertThat(response.activityCompleted()).isTrue();
    assertThat(response.enrollmentVersion()).isEqualTo(2L);
    assertThat(response.attemptNumber()).isEqualTo(1);

    verify(learningProgressService)
        .recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(true), eq(1), eq(2), eq(1L));
  }

  @Test
  @DisplayName("submitQuiz: PASS_SCORE with score >= 80% marks activity COMPLETED")
  void submitQuiz_passScore_whenScoreAboveThreshold_marksCompleted() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 2L);
    Activity activity = mockActivity(CompletionPolicy.PASS_SCORE, ActivityType.QUIZ);

    QuizQuestion q1 = createQuestionWithTwoOptions(1, "Q1", "Exp1", "Correct", "Wrong", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId)).thenReturn(List.of(q1));
    when(quizAttemptRepository.countByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(0);

    ActivityProgress mockProgress = mock(ActivityProgress.class);
    when(mockProgress.getStatus()).thenReturn(ActivityStatus.COMPLETED);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(Optional.of(mockProgress));

    when(learningProgressService.recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(true), eq(1), eq(1), eq(2L)))
        .thenReturn(3L);

    QuizSubmitRequest request =
        new QuizSubmitRequest(
            2L, List.of(new QuizAnswerItemRequest(q1.getId(), q1.getOptions().get(0).getId())));

    QuizResultResponse response = quizService.submitQuiz(null, enrollmentId, activityId, request);

    assertThat(response.score()).isEqualTo(1);
    assertThat(response.scorePercentage()).isEqualTo(100);
    assertThat(response.passed()).isTrue();
    assertThat(response.activityCompleted()).isTrue();
    assertThat(response.enrollmentVersion()).isEqualTo(3L);

    verify(learningProgressService)
        .recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(true), eq(1), eq(1), eq(2L));
  }

  @Test
  @DisplayName("submitQuiz: PASS_SCORE with score < 80% marks activity IN_PROGRESS")
  void submitQuiz_passScore_whenScoreBelowThreshold_marksInProgress() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    Activity activity = mockActivity(CompletionPolicy.PASS_SCORE, ActivityType.QUIZ);

    QuizQuestion q1 = createQuestionWithTwoOptions(1, "Q1", "Exp1", "Correct", "Wrong", true);
    QuizQuestion q2 = createQuestionWithTwoOptions(2, "Q2", "Exp2", "Correct", "Wrong", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId))
        .thenReturn(List.of(q1, q2));
    when(quizAttemptRepository.countByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(0);

    ActivityProgress mockProgress = mock(ActivityProgress.class);
    when(mockProgress.getStatus()).thenReturn(ActivityStatus.IN_PROGRESS);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(Optional.of(mockProgress));

    when(learningProgressService.recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(false), eq(0), eq(2), eq(1L)))
        .thenReturn(2L);

    // Score = 0/2 = 0%
    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L,
            List.of(
                new QuizAnswerItemRequest(q1.getId(), q1.getOptions().get(1).getId()),
                new QuizAnswerItemRequest(q2.getId(), q2.getOptions().get(1).getId())));

    QuizResultResponse response = quizService.submitQuiz(null, enrollmentId, activityId, request);

    assertThat(response.score()).isEqualTo(0);
    assertThat(response.scorePercentage()).isEqualTo(0);
    assertThat(response.passed()).isFalse();
    assertThat(response.activityCompleted()).isFalse();

    verify(learningProgressService)
        .recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(false), eq(0), eq(2), eq(1L));
  }

  @Test
  @DisplayName("submitQuiz: lower re-attempt does not revoke already COMPLETED activity")
  void submitQuiz_lowerReattempt_preservesPreviousCompletedStatus() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 3L);
    Activity activity = mockActivity(CompletionPolicy.PASS_SCORE, ActivityType.QUIZ);

    QuizQuestion q1 = createQuestionWithTwoOptions(1, "Q1", "Exp1", "Correct", "Wrong", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId)).thenReturn(List.of(q1));
    when(quizAttemptRepository.countByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(1);

    ActivityProgress existingProgress = mock(ActivityProgress.class);
    when(existingProgress.getStatus()).thenReturn(ActivityStatus.COMPLETED);
    when(activityProgressRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId))
        .thenReturn(Optional.of(existingProgress));

    when(learningProgressService.recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(false), eq(0), eq(1), eq(3L)))
        .thenReturn(4L);

    // Re-attempt answers wrong option: score = 0%
    QuizSubmitRequest request =
        new QuizSubmitRequest(
            3L, List.of(new QuizAnswerItemRequest(q1.getId(), q1.getOptions().get(1).getId())));

    QuizResultResponse response = quizService.submitQuiz(null, enrollmentId, activityId, request);

    assertThat(response.passed()).isFalse();
    assertThat(response.activityCompleted()).isTrue(); // Preserved previous achievement!
    assertThat(response.attemptNumber()).isEqualTo(2);

    verify(learningProgressService)
        .recordQuizAssessmentResult(
            eq(enrollmentId), eq(activityId), eq(false), eq(0), eq(1), eq(3L));
  }

  @Test
  @DisplayName("submitQuiz: throws VALIDATION_FAILED when answers list is incomplete")
  void submitQuiz_missingAnswers_throwsValidationFailed() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    Activity activity = mockActivity(CompletionPolicy.PASS_SCORE, ActivityType.QUIZ);

    QuizQuestion q1 = createQuestionWithTwoOptions(1, "Q1", "Exp1", "Correct", "Wrong", true);
    QuizQuestion q2 = createQuestionWithTwoOptions(2, "Q2", "Exp2", "Correct", "Wrong", true);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(quizQuestionRepository.findByActivityIdWithOptions(activityId))
        .thenReturn(List.of(q1, q2));

    // Only answer 1 question
    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L, List.of(new QuizAnswerItemRequest(q1.getId(), q1.getOptions().get(0).getId())));

    assertThatThrownBy(() -> quizService.submitQuiz(null, enrollmentId, activityId, request))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("submitQuiz: throws STATE_CONFLICT when enrollment is PAUSED")
  void submitQuiz_pausedEnrollment_throwsStateConflict() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.PAUSED, 1L);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L, List.of(new QuizAnswerItemRequest(UUID.randomUUID(), UUID.randomUUID())));

    assertThatThrownBy(() -> quizService.submitQuiz(null, enrollmentId, activityId, request))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("submitQuiz: throws VERSION_CONFLICT when expectedVersion does not match")
  void submitQuiz_versionMismatch_throwsVersionConflict() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 5L);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    QuizSubmitRequest request =
        new QuizSubmitRequest(
            4L, List.of(new QuizAnswerItemRequest(UUID.randomUUID(), UUID.randomUUID())));

    assertThatThrownBy(() -> quizService.submitQuiz(null, enrollmentId, activityId, request))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("submitQuiz: replays idempotent response when key already processed")
  void submitQuiz_idempotentReplay() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(any())).thenReturn("hash-123");

    IdempotencyRecord mockRecord = mock(IdempotencyRecord.class);
    when(idempotencyService.findExisting(userId, "SUBMIT_QUIZ", rawKey))
        .thenReturn(Optional.of(mockRecord));

    QuizResultResponse cachedResponse =
        new QuizResultResponse(UUID.randomUUID(), 1, 1, 1, 100, true, true, 2L, List.of());

    when(idempotencyService.replay(mockRecord, "hash-123", QuizResultResponse.class))
        .thenReturn(cachedResponse);

    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L, List.of(new QuizAnswerItemRequest(UUID.randomUUID(), UUID.randomUUID())));

    QuizResultResponse response = quizService.submitQuiz(rawKey, enrollmentId, activityId, request);

    assertThat(response).isSameAs(cachedResponse);
    verify(enrollmentRepository, never()).findById(any());
  }
}
