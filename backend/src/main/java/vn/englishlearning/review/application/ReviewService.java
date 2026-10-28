package vn.englishlearning.review.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.assessment.api.QuizResponses.QuizOptionDto;
import vn.englishlearning.assessment.domain.QuizAttempt;
import vn.englishlearning.assessment.domain.QuizAttemptAnswer;
import vn.englishlearning.assessment.domain.QuizOption;
import vn.englishlearning.assessment.domain.QuizQuestion;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.assessment.infrastructure.QuizQuestionRepository;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.review.api.ReviewRequests.CheckQuestionRequest;
import vn.englishlearning.review.api.ReviewRequests.CreateBookmarkRequest;
import vn.englishlearning.review.api.ReviewResponses.BookmarkResponse;
import vn.englishlearning.review.api.ReviewResponses.MistakeItemResponse;
import vn.englishlearning.review.api.ReviewResponses.QuestionCheckResponse;
import vn.englishlearning.review.api.ReviewResponses.ReviewSummaryResponse;
import vn.englishlearning.review.domain.ReviewBookmark;
import vn.englishlearning.review.domain.ReviewResolution;
import vn.englishlearning.review.infrastructure.ReviewBookmarkRepository;
import vn.englishlearning.review.infrastructure.ReviewResolutionRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

@Service
@Transactional
public class ReviewService {

  private final ReviewBookmarkRepository reviewBookmarkRepository;
  private final ReviewResolutionRepository reviewResolutionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final QuizAttemptRepository quizAttemptRepository;
  private final QuizQuestionRepository quizQuestionRepository;
  private final Clock clock;

  public ReviewService(
      ReviewBookmarkRepository reviewBookmarkRepository,
      ReviewResolutionRepository reviewResolutionRepository,
      EnrollmentRepository enrollmentRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      QuizAttemptRepository quizAttemptRepository,
      QuizQuestionRepository quizQuestionRepository,
      Clock clock) {
    this.reviewBookmarkRepository = reviewBookmarkRepository;
    this.reviewResolutionRepository = reviewResolutionRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.quizAttemptRepository = quizAttemptRepository;
    this.quizQuestionRepository = quizQuestionRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ReviewSummaryResponse getReviewSummary(UUID userId, UUID enrollmentId) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndUserId(enrollmentId, userId)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    // 1. Bookmarked Items
    List<ReviewBookmark> bookmarks =
        reviewBookmarkRepository.findByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId());
    List<BookmarkResponse> bookmarkedResponses = new ArrayList<>();
    for (ReviewBookmark b : bookmarks) {
      Lesson lesson = lessonRepository.findById(b.getLessonId()).orElse(null);
      String lessonTitle = lesson != null ? lesson.getTitle() : "Bài học";
      String activityTitle = null;
      String activityType = null;
      if (b.getActivityId() != null) {
        Activity act = activityRepository.findById(b.getActivityId()).orElse(null);
        if (act != null) {
          activityTitle = act.getTitle();
          activityType = act.getActivityType().name();
        }
      }
      bookmarkedResponses.add(
          new BookmarkResponse(
              b.getId(),
              b.getEnrollmentId(),
              b.getLessonId(),
              lessonTitle,
              b.getActivityId(),
              activityTitle,
              activityType,
              b.getNote(),
              b.getCreatedAt()));
    }

    // 2. Mistakes tracking from latest quiz attempts
    Map<UUID, ReviewResolution> resolutionMap =
        reviewResolutionRepository.findByEnrollmentId(enrollment.getId()).stream()
            .collect(Collectors.toMap(ReviewResolution::getQuestionId, r -> r, (r1, r2) -> r1));

    List<QuizAttempt> attempts =
        quizAttemptRepository.findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(
            enrollment.getId());

    // Deduplicate to keep latest attempt per activity
    Map<UUID, QuizAttempt> latestAttemptPerActivity = new LinkedHashMap<>();
    for (QuizAttempt att : attempts) {
      latestAttemptPerActivity.putIfAbsent(att.getActivityId(), att);
    }

    List<MistakeItemResponse> mistakes = new ArrayList<>();
    for (QuizAttempt latestAttempt : latestAttemptPerActivity.values()) {
      Activity activity = activityRepository.findById(latestAttempt.getActivityId()).orElse(null);
      String activityTitle = activity != null ? activity.getTitle() : "Bài trắc nghiệm";
      Lesson lesson = activity != null ? activity.getLesson() : null;
      UUID lessonId = lesson != null ? lesson.getId() : null;
      String lessonTitle = lesson != null ? lesson.getTitle() : "Bài học";

      for (QuizAttemptAnswer answer : latestAttempt.getAnswers()) {
        if (!answer.isCorrect()) {
          QuizQuestion question =
              quizQuestionRepository.findByIdWithOptions(answer.getQuestionId()).orElse(null);
          if (question == null) {
            continue;
          }

          ReviewResolution resolution = resolutionMap.get(question.getId());
          boolean resolved =
              resolution != null
                  && !resolution.getResolvedAt().isBefore(latestAttempt.getSubmittedAt());

          List<QuizOptionDto> optionDtos =
              question.getOptions().stream()
                  .sorted(Comparator.comparingInt(QuizOption::getOrderIndex))
                  .map(opt -> new QuizOptionDto(opt.getId(), opt.getOrderIndex(), opt.getContent()))
                  .toList();

          mistakes.add(
              new MistakeItemResponse(
                  question.getId(),
                  question.getPrompt(),
                  lessonId,
                  lessonTitle,
                  latestAttempt.getActivityId(),
                  activityTitle,
                  optionDtos,
                  resolved,
                  latestAttempt.getSubmittedAt()));
        }
      }
    }

    return new ReviewSummaryResponse(enrollment.getId(), bookmarkedResponses, mistakes);
  }

  public BookmarkResponse createBookmark(UUID userId, CreateBookmarkRequest request) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndUserId(request.enrollmentId(), userId)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    Lesson lesson =
        lessonRepository
            .findById(request.lessonId())
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found"));

    Activity activity = null;
    if (request.activityId() != null) {
      activity =
          activityRepository
              .findById(request.activityId())
              .orElseThrow(
                  () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found"));
      if (!activity.getLesson().getId().equals(lesson.getId())) {
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED, "Activity does not belong to specified lesson");
      }
    }

    Optional<ReviewBookmark> existing =
        reviewBookmarkRepository.findBookmark(
            request.enrollmentId(), request.lessonId(), request.activityId());
    if (existing.isPresent()) {
      ReviewBookmark b = existing.get();
      return new BookmarkResponse(
          b.getId(),
          b.getEnrollmentId(),
          b.getLessonId(),
          lesson.getTitle(),
          b.getActivityId(),
          activity != null ? activity.getTitle() : null,
          activity != null ? activity.getActivityType().name() : null,
          b.getNote(),
          b.getCreatedAt());
    }

    ReviewBookmark bookmark =
        new ReviewBookmark(
            userId,
            enrollment.getId(),
            lesson.getId(),
            activity != null ? activity.getId() : null,
            request.note(),
            clock.instant());
    ReviewBookmark saved = reviewBookmarkRepository.save(bookmark);

    return new BookmarkResponse(
        saved.getId(),
        saved.getEnrollmentId(),
        saved.getLessonId(),
        lesson.getTitle(),
        saved.getActivityId(),
        activity != null ? activity.getTitle() : null,
        activity != null ? activity.getActivityType().name() : null,
        saved.getNote(),
        saved.getCreatedAt());
  }

  public void deleteBookmark(UUID userId, UUID bookmarkId) {
    ReviewBookmark bookmark =
        reviewBookmarkRepository
            .findByIdAndUserId(bookmarkId, userId)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Bookmark not found"));
    reviewBookmarkRepository.delete(bookmark);
  }

  public QuestionCheckResponse checkMistakeQuestion(
      UUID userId, UUID enrollmentId, UUID questionId, CheckQuestionRequest request) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndUserId(enrollmentId, userId)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    QuizQuestion question =
        quizQuestionRepository
            .findByIdWithOptions(questionId)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Question not found"));

    QuizOption selectedOption =
        question.getOptions().stream()
            .filter(opt -> Objects.equals(opt.getId(), request.selectedOptionId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.VALIDATION_FAILED, "Option does not belong to question"));

    QuizOption correctOption =
        question.getOptions().stream()
            .filter(QuizOption::isCorrect)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Question has no correct option"));

    boolean isCorrect = selectedOption.isCorrect();
    boolean resolved = isCorrect;

    if (isCorrect) {
      ReviewResolution resolution =
          reviewResolutionRepository
              .findByEnrollmentIdAndQuestionId(enrollment.getId(), questionId)
              .orElseGet(
                  () -> new ReviewResolution(enrollment.getId(), questionId, clock.instant()));
      resolution.setResolvedAt(clock.instant());
      reviewResolutionRepository.save(resolution);
    }

    return new QuestionCheckResponse(
        questionId,
        request.selectedOptionId(),
        correctOption.getId(),
        isCorrect,
        question.getExplanation(),
        resolved);
  }
}
