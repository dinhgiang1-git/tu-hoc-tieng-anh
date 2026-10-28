package vn.englishlearning.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import vn.englishlearning.assessment.domain.QuizAttempt;
import vn.englishlearning.assessment.domain.QuizAttemptAnswer;
import vn.englishlearning.assessment.domain.QuizOption;
import vn.englishlearning.assessment.domain.QuizQuestion;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.assessment.infrastructure.QuizQuestionRepository;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.review.api.ReviewRequests.CheckQuestionRequest;
import vn.englishlearning.review.api.ReviewRequests.CreateBookmarkRequest;
import vn.englishlearning.review.api.ReviewResponses.BookmarkResponse;
import vn.englishlearning.review.api.ReviewResponses.QuestionCheckResponse;
import vn.englishlearning.review.api.ReviewResponses.ReviewSummaryResponse;
import vn.englishlearning.review.domain.ReviewBookmark;
import vn.englishlearning.review.domain.ReviewResolution;
import vn.englishlearning.review.infrastructure.ReviewBookmarkRepository;
import vn.englishlearning.review.infrastructure.ReviewResolutionRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

class ReviewServiceTest {

  private ReviewBookmarkRepository reviewBookmarkRepository;
  private ReviewResolutionRepository reviewResolutionRepository;
  private EnrollmentRepository enrollmentRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private QuizAttemptRepository quizAttemptRepository;
  private QuizQuestionRepository quizQuestionRepository;
  private Clock clock;

  private ReviewService reviewService;

  private final UUID userId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID lessonId = UUID.randomUUID();
  private final UUID activityId = UUID.randomUUID();
  private final Instant fixedNow = Instant.parse("2026-10-28T10:00:00Z");

  @BeforeEach
  void setUp() {
    reviewBookmarkRepository = mock(ReviewBookmarkRepository.class);
    reviewResolutionRepository = mock(ReviewResolutionRepository.class);
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    quizAttemptRepository = mock(QuizAttemptRepository.class);
    quizQuestionRepository = mock(QuizQuestionRepository.class);
    clock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

    reviewService =
        new ReviewService(
            reviewBookmarkRepository,
            reviewResolutionRepository,
            enrollmentRepository,
            lessonRepository,
            activityRepository,
            quizAttemptRepository,
            quizQuestionRepository,
            clock);
  }

  private Enrollment createEnrollment() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.of(2026, 10, 1),
            "Asia/Ho_Chi_Minh",
            List.of(java.time.DayOfWeek.MONDAY),
            30,
            fixedNow);
    ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    ReflectionTestUtils.setField(enrollment, "status", EnrollmentStatus.ACTIVE);
    return enrollment;
  }

  private Lesson mockLesson() {
    Lesson lesson = mock(Lesson.class);
    when(lesson.getId()).thenReturn(lessonId);
    when(lesson.getTitle()).thenReturn("Bài 1: Tổng kết & Kiểm tra Trắc nghiệm");
    return lesson;
  }

  private Activity mockActivity(Lesson lesson) {
    Activity activity = mock(Activity.class);
    when(activity.getId()).thenReturn(activityId);
    when(activity.getTitle()).thenReturn("Bài trắc nghiệm cuối bài");
    when(activity.getActivityType()).thenReturn(ActivityType.QUIZ);
    when(activity.getLesson()).thenReturn(lesson);
    return activity;
  }

  private QuizQuestion createQuestion(
      UUID questionId, String prompt, String expl, boolean aIsCorrect) {
    QuizQuestion question = new QuizQuestion(activityId, 1, prompt, expl, fixedNow);
    ReflectionTestUtils.setField(question, "id", questionId);

    QuizOption optA = new QuizOption(1, "Option A", aIsCorrect, fixedNow);
    ReflectionTestUtils.setField(optA, "id", UUID.randomUUID());
    QuizOption optB = new QuizOption(2, "Option B", !aIsCorrect, fixedNow);
    ReflectionTestUtils.setField(optB, "id", UUID.randomUUID());

    question.addOption(optA);
    question.addOption(optB);
    return question;
  }

  @Test
  @DisplayName("getReviewSummary: returns bookmarks and mistakes with options hiding isCorrect")
  void getReviewSummary_success() {
    Enrollment enrollment = createEnrollment();
    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));

    // Mock Bookmark
    Lesson lesson = mockLesson();
    Activity activity = mockActivity(lesson);
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

    ReviewBookmark bookmark =
        new ReviewBookmark(userId, enrollmentId, lessonId, activityId, "Review note", fixedNow);
    ReflectionTestUtils.setField(bookmark, "id", UUID.randomUUID());
    when(reviewBookmarkRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollmentId))
        .thenReturn(List.of(bookmark));

    // Mock Quiz Attempt with a wrong answer
    UUID questionId = UUID.randomUUID();
    QuizQuestion question =
        createQuestion(questionId, "What is Schwa?", "Schwa is unstressed", true);

    QuizAttempt attempt =
        new QuizAttempt(
            enrollmentId, activityId, userId, 1, 0, 1, 0, false, fixedNow.minusSeconds(3600));
    QuizAttemptAnswer answer =
        new QuizAttemptAnswer(questionId, question.getOptions().get(1).getId(), false);
    attempt.addAnswer(answer);

    when(quizAttemptRepository.findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(enrollmentId))
        .thenReturn(List.of(attempt));
    when(quizQuestionRepository.findByIdWithOptions(questionId)).thenReturn(Optional.of(question));
    when(reviewResolutionRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of());

    ReviewSummaryResponse response = reviewService.getReviewSummary(userId, enrollmentId);

    assertThat(response.enrollmentId()).isEqualTo(enrollmentId);
    assertThat(response.bookmarkedItems()).hasSize(1);
    assertThat(response.bookmarkedItems().get(0).lessonTitle())
        .isEqualTo("Bài 1: Tổng kết & Kiểm tra Trắc nghiệm");
    assertThat(response.bookmarkedItems().get(0).activityTitle())
        .isEqualTo("Bài trắc nghiệm cuối bài");

    assertThat(response.mistakes()).hasSize(1);
    assertThat(response.mistakes().get(0).questionId()).isEqualTo(questionId);
    assertThat(response.mistakes().get(0).prompt()).isEqualTo("What is Schwa?");
    assertThat(response.mistakes().get(0).resolved()).isFalse();
    assertThat(response.mistakes().get(0).options()).hasSize(2);
  }

  @Test
  @DisplayName("getReviewSummary: resolved mistake is properly recognized")
  void getReviewSummary_withResolution() {
    Enrollment enrollment = createEnrollment();
    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));
    when(reviewBookmarkRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollmentId))
        .thenReturn(List.of());

    UUID questionId = UUID.randomUUID();
    QuizQuestion question =
        createQuestion(questionId, "What is Schwa?", "Schwa is unstressed", true);

    Instant attemptTime = fixedNow.minusSeconds(3600);
    QuizAttempt attempt =
        new QuizAttempt(enrollmentId, activityId, userId, 1, 0, 1, 0, false, attemptTime);
    QuizAttemptAnswer answer =
        new QuizAttemptAnswer(questionId, question.getOptions().get(1).getId(), false);
    attempt.addAnswer(answer);

    when(quizAttemptRepository.findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(enrollmentId))
        .thenReturn(List.of(attempt));
    when(quizQuestionRepository.findByIdWithOptions(questionId)).thenReturn(Optional.of(question));

    // Resolution recorded AFTER attempt
    ReviewResolution resolution =
        new ReviewResolution(enrollmentId, questionId, attemptTime.plusSeconds(100));
    when(reviewResolutionRepository.findByEnrollmentId(enrollmentId))
        .thenReturn(List.of(resolution));

    ReviewSummaryResponse response = reviewService.getReviewSummary(userId, enrollmentId);

    assertThat(response.mistakes()).hasSize(1);
    assertThat(response.mistakes().get(0).resolved()).isTrue();
  }

  @Test
  @DisplayName("getReviewSummary: throws RESOURCE_NOT_FOUND when enrollment not owned")
  void getReviewSummary_notOwned() {
    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReviewSummary(userId, enrollmentId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("createBookmark: saves and returns bookmark")
  void createBookmark_success() {
    Enrollment enrollment = createEnrollment();
    Lesson lesson = mockLesson();
    Activity activity = mockActivity(lesson);

    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
    when(reviewBookmarkRepository.findBookmark(enrollmentId, lessonId, activityId))
        .thenReturn(Optional.empty());
    when(reviewBookmarkRepository.save(any(ReviewBookmark.class)))
        .thenAnswer(
            inv -> {
              ReviewBookmark b = inv.getArgument(0);
              ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
              return b;
            });

    CreateBookmarkRequest request =
        new CreateBookmarkRequest(enrollmentId, lessonId, activityId, "Remember to review");

    BookmarkResponse response = reviewService.createBookmark(userId, request);

    assertThat(response.enrollmentId()).isEqualTo(enrollmentId);
    assertThat(response.lessonId()).isEqualTo(lessonId);
    assertThat(response.activityId()).isEqualTo(activityId);
    assertThat(response.lessonTitle()).isEqualTo("Bài 1: Tổng kết & Kiểm tra Trắc nghiệm");
    assertThat(response.activityTitle()).isEqualTo("Bài trắc nghiệm cuối bài");
    assertThat(response.note()).isEqualTo("Remember to review");
  }

  @Test
  @DisplayName("createBookmark: idempotent when already bookmarked")
  void createBookmark_idempotent() {
    Enrollment enrollment = createEnrollment();
    Lesson lesson = mockLesson();
    Activity activity = mockActivity(lesson);

    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

    ReviewBookmark existing =
        new ReviewBookmark(userId, enrollmentId, lessonId, activityId, "Existing", fixedNow);
    ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
    when(reviewBookmarkRepository.findBookmark(enrollmentId, lessonId, activityId))
        .thenReturn(Optional.of(existing));

    CreateBookmarkRequest request =
        new CreateBookmarkRequest(enrollmentId, lessonId, activityId, "New Note");

    BookmarkResponse response = reviewService.createBookmark(userId, request);

    assertThat(response.id()).isEqualTo(existing.getId());
    assertThat(response.note()).isEqualTo("Existing");
  }

  @Test
  @DisplayName("createBookmark: throws VALIDATION_FAILED when activity not in lesson")
  void createBookmark_activityNotInLesson() {
    Enrollment enrollment = createEnrollment();
    Lesson lesson = mockLesson();
    Lesson otherLesson = mock(Lesson.class);
    when(otherLesson.getId()).thenReturn(UUID.randomUUID());

    Activity activity = mockActivity(otherLesson);

    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

    CreateBookmarkRequest request =
        new CreateBookmarkRequest(enrollmentId, lessonId, activityId, null);

    assertThatThrownBy(() -> reviewService.createBookmark(userId, request))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("deleteBookmark: successfully deletes user bookmark")
  void deleteBookmark_success() {
    UUID bookmarkId = UUID.randomUUID();
    ReviewBookmark bookmark =
        new ReviewBookmark(userId, enrollmentId, lessonId, activityId, null, fixedNow);
    when(reviewBookmarkRepository.findByIdAndUserId(bookmarkId, userId))
        .thenReturn(Optional.of(bookmark));

    reviewService.deleteBookmark(userId, bookmarkId);

    verify(reviewBookmarkRepository).delete(bookmark);
  }

  @Test
  @DisplayName("deleteBookmark: throws RESOURCE_NOT_FOUND when bookmark does not exist")
  void deleteBookmark_notFound() {
    UUID bookmarkId = UUID.randomUUID();
    when(reviewBookmarkRepository.findByIdAndUserId(bookmarkId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.deleteBookmark(userId, bookmarkId))
        .isInstanceOf(DomainException.class)
        .matches(e -> ((DomainException) e).getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("checkMistakeQuestion: correct answer records resolution and returns true")
  void checkMistakeQuestion_correctAnswer() {
    Enrollment enrollment = createEnrollment();
    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));

    UUID questionId = UUID.randomUUID();
    QuizQuestion question =
        createQuestion(questionId, "What is Schwa?", "Schwa is unstressed", true);
    when(quizQuestionRepository.findByIdWithOptions(questionId)).thenReturn(Optional.of(question));
    when(reviewResolutionRepository.findByEnrollmentIdAndQuestionId(enrollmentId, questionId))
        .thenReturn(Optional.empty());

    UUID correctOptId = question.getOptions().get(0).getId();
    CheckQuestionRequest request = new CheckQuestionRequest(correctOptId);

    QuestionCheckResponse response =
        reviewService.checkMistakeQuestion(userId, enrollmentId, questionId, request);

    assertThat(response.isCorrect()).isTrue();
    assertThat(response.resolved()).isTrue();
    assertThat(response.explanation()).isEqualTo("Schwa is unstressed");
    verify(reviewResolutionRepository).save(any(ReviewResolution.class));
  }

  @Test
  @DisplayName("checkMistakeQuestion: wrong answer returns false without saving resolution")
  void checkMistakeQuestion_wrongAnswer() {
    Enrollment enrollment = createEnrollment();
    when(enrollmentRepository.findByIdAndUserId(enrollmentId, userId))
        .thenReturn(Optional.of(enrollment));

    UUID questionId = UUID.randomUUID();
    QuizQuestion question =
        createQuestion(questionId, "What is Schwa?", "Schwa is unstressed", true);
    when(quizQuestionRepository.findByIdWithOptions(questionId)).thenReturn(Optional.of(question));

    UUID wrongOptId = question.getOptions().get(1).getId();
    CheckQuestionRequest request = new CheckQuestionRequest(wrongOptId);

    QuestionCheckResponse response =
        reviewService.checkMistakeQuestion(userId, enrollmentId, questionId, request);

    assertThat(response.isCorrect()).isFalse();
    assertThat(response.resolved()).isFalse();
    assertThat(response.explanation()).isEqualTo("Schwa is unstressed");
  }
}
