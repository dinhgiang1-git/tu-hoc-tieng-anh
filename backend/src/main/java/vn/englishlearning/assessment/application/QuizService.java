package vn.englishlearning.assessment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.assessment.api.QuizRequests.QuizAnswerItemRequest;
import vn.englishlearning.assessment.api.QuizRequests.QuizSubmitRequest;
import vn.englishlearning.assessment.api.QuizResponses.QuestionResultDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizAttemptSummaryDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizDetailResponse;
import vn.englishlearning.assessment.api.QuizResponses.QuizOptionDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizQuestionDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizResultResponse;
import vn.englishlearning.assessment.domain.QuizAttempt;
import vn.englishlearning.assessment.domain.QuizAttemptAnswer;
import vn.englishlearning.assessment.domain.QuizOption;
import vn.englishlearning.assessment.domain.QuizQuestion;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.assessment.infrastructure.QuizQuestionRepository;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
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

@Service
public class QuizService {

  public static final int DEFAULT_PASSING_PERCENTAGE = 80;
  private static final String OP_SUBMIT_QUIZ = "SUBMIT_QUIZ";

  private final QuizQuestionRepository quizQuestionRepository;
  private final QuizAttemptRepository quizAttemptRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final ActivityRepository activityRepository;
  private final ActivityProgressRepository activityProgressRepository;
  private final LearningProgressService learningProgressService;
  private final IdempotencyService idempotencyService;
  private final CurrentUser currentUser;
  private final Clock clock;

  public QuizService(
      QuizQuestionRepository quizQuestionRepository,
      QuizAttemptRepository quizAttemptRepository,
      EnrollmentRepository enrollmentRepository,
      ActivityRepository activityRepository,
      ActivityProgressRepository activityProgressRepository,
      LearningProgressService learningProgressService,
      IdempotencyService idempotencyService,
      CurrentUser currentUser,
      Clock clock) {
    this.quizQuestionRepository = quizQuestionRepository;
    this.quizAttemptRepository = quizAttemptRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.activityRepository = activityRepository;
    this.activityProgressRepository = activityProgressRepository;
    this.learningProgressService = learningProgressService;
    this.idempotencyService = idempotencyService;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public QuizDetailResponse getQuizDetail(UUID enrollmentId, UUID activityId) {
    UUID currentUserId = currentUser.requireUserId();
    Enrollment enrollment = findAndValidateEnrollment(enrollmentId, currentUserId);
    Activity activity = findAndValidateActivity(activityId, enrollment);

    List<QuizQuestion> questions = quizQuestionRepository.findByActivityIdWithOptions(activityId);

    List<QuizQuestionDto> questionDtos =
        questions.stream()
            .map(
                q ->
                    new QuizQuestionDto(
                        q.getId(),
                        q.getOrderIndex(),
                        q.getPrompt(),
                        q.getOptions().stream()
                            .map(
                                o ->
                                    new QuizOptionDto(o.getId(), o.getOrderIndex(), o.getContent()))
                            .toList()))
            .toList();

    Optional<QuizAttempt> latestAttemptOpt =
        quizAttemptRepository.findTopByEnrollmentIdAndActivityIdOrderBySubmittedAtDesc(
            enrollmentId, activityId);

    QuizAttemptSummaryDto latestAttemptDto =
        latestAttemptOpt
            .map(
                a ->
                    new QuizAttemptSummaryDto(
                        a.getId(),
                        a.getAttemptNumber(),
                        a.getScore(),
                        a.getTotalQuestions(),
                        a.getScorePercentage(),
                        a.isPassed(),
                        a.getSubmittedAt()))
            .orElse(null);

    int passingScorePercentage =
        activity.getCompletionPolicy() == CompletionPolicy.ON_SUBMIT
            ? 0
            : DEFAULT_PASSING_PERCENTAGE;

    return new QuizDetailResponse(
        activity.getId(),
        activity.getTitle(),
        activity.getCompletionPolicy().name(),
        passingScorePercentage,
        questionDtos,
        latestAttemptDto);
  }

  @Transactional
  public QuizResultResponse submitQuiz(
      String rawIdempotencyKey, UUID enrollmentId, UUID activityId, QuizSubmitRequest request) {
    UUID currentUserId = currentUser.requireUserId();

    if (request == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.answers() == null || request.answers().isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Danh sách câu trả lời không được để trống.");
    }

    String idempotencyKey =
        rawIdempotencyKey != null && !rawIdempotencyKey.isBlank()
            ? idempotencyService.validateAndNormalizeKey(rawIdempotencyKey)
            : null;

    if (idempotencyKey != null) {
      String answersSignature =
          request.answers().stream()
              .sorted(Comparator.comparing(QuizAnswerItemRequest::questionId))
              .map(a -> a.questionId() + ":" + a.selectedOptionId())
              .collect(Collectors.joining(","));
      String requestHash =
          idempotencyService.hashPayload(
              enrollmentId
                  + "|"
                  + activityId
                  + "|"
                  + request.expectedVersion()
                  + "|"
                  + answersSignature);

      Optional<IdempotencyRecord> existingOpt =
          idempotencyService.findExisting(currentUserId, OP_SUBMIT_QUIZ, idempotencyKey);
      if (existingOpt.isPresent()) {
        return idempotencyService.replay(existingOpt.get(), requestHash, QuizResultResponse.class);
      }
    }

    Enrollment enrollment = findAndValidateEnrollment(enrollmentId, currentUserId);

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED
        || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Lộ trình đang ở trạng thái " + enrollment.getStatus() + ", không thể thao tác học tập.");
    }

    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    Activity activity = findAndValidateActivity(activityId, enrollment);

    List<QuizQuestion> questions = quizQuestionRepository.findByActivityIdWithOptions(activityId);

    if (questions.isEmpty()) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Bài kiểm tra chưa có câu hỏi nào.");
    }

    Map<UUID, QuizQuestion> questionMap =
        questions.stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

    Map<UUID, UUID> submittedAnswers =
        request.answers().stream()
            .collect(
                Collectors.toMap(
                    QuizAnswerItemRequest::questionId,
                    QuizAnswerItemRequest::selectedOptionId,
                    (o1, o2) -> o1));

    if (submittedAnswers.size() != questions.size()
        || !questionMap.keySet().containsAll(submittedAnswers.keySet())) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Bạn phải trả lời đầy đủ tất cả các câu hỏi của bài kiểm tra.");
    }

    Instant now = Instant.now(clock);
    int score = 0;
    int totalQuestions = questions.size();
    List<QuestionResultDto> questionResults = new ArrayList<>();
    List<QuizAttemptAnswer> attemptAnswers = new ArrayList<>();

    for (QuizQuestion question : questions) {
      UUID selectedOptionId = submittedAnswers.get(question.getId());
      Map<UUID, QuizOption> optionMap =
          question.getOptions().stream().collect(Collectors.toMap(QuizOption::getId, o -> o));

      QuizOption selectedOption = optionMap.get(selectedOptionId);
      if (selectedOption == null) {
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED,
            "Lựa chọn đáp án không hợp lệ cho câu hỏi: " + question.getPrompt());
      }

      QuizOption correctOption =
          question.getOptions().stream()
              .filter(QuizOption::isCorrect)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Question "
                              + question.getId()
                              + " does not have a correct option configured"));

      boolean isCorrect = selectedOption.isCorrect();
      if (isCorrect) {
        score++;
      }

      attemptAnswers.add(new QuizAttemptAnswer(question.getId(), selectedOptionId, isCorrect));
      questionResults.add(
          new QuestionResultDto(
              question.getId(),
              selectedOptionId,
              correctOption.getId(),
              isCorrect,
              question.getExplanation()));
    }

    int scorePercentage = Math.round(((float) score / totalQuestions) * 100);
    boolean passed;
    if (activity.getCompletionPolicy() == CompletionPolicy.ON_SUBMIT) {
      passed = true;
    } else {
      passed = (scorePercentage >= DEFAULT_PASSING_PERCENTAGE);
    }

    int attemptCount =
        quizAttemptRepository.countByEnrollmentIdAndActivityId(enrollmentId, activityId);
    int attemptNumber = attemptCount + 1;

    QuizAttempt attempt =
        new QuizAttempt(
            enrollmentId,
            activityId,
            currentUserId,
            attemptNumber,
            score,
            totalQuestions,
            scorePercentage,
            passed,
            now);

    for (QuizAttemptAnswer ans : attemptAnswers) {
      attempt.addAnswer(ans);
    }
    quizAttemptRepository.save(attempt);

    // Update learning progress & increment CAS version
    Long newVersion =
        learningProgressService.recordQuizAssessmentResult(
            enrollmentId, activityId, passed, score, totalQuestions, request.expectedVersion());

    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, activityId)
            .orElse(null);
    boolean activityCompleted =
        progress != null && progress.getStatus() == ActivityStatus.COMPLETED;

    QuizResultResponse response =
        new QuizResultResponse(
            attempt.getId(),
            attemptNumber,
            score,
            totalQuestions,
            scorePercentage,
            passed,
            activityCompleted,
            newVersion,
            questionResults);

    if (idempotencyKey != null) {
      String answersSignature =
          request.answers().stream()
              .sorted(Comparator.comparing(QuizAnswerItemRequest::questionId))
              .map(a -> a.questionId() + ":" + a.selectedOptionId())
              .collect(Collectors.joining(","));
      String requestHash =
          idempotencyService.hashPayload(
              enrollmentId
                  + "|"
                  + activityId
                  + "|"
                  + request.expectedVersion()
                  + "|"
                  + answersSignature);
      idempotencyService.record(
          currentUserId, OP_SUBMIT_QUIZ, idempotencyKey, requestHash, 200, response);
    }

    return response;
  }

  private Enrollment findAndValidateEnrollment(UUID enrollmentId, UUID userId) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId));

    if (!enrollment.getUserId().equals(userId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId);
    }
    return enrollment;
  }

  private Activity findAndValidateActivity(UUID activityId, Enrollment enrollment) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Activity not found: " + activityId));

    UUID activityRevisionId = activity.getLesson().getWeek().getPhase().getRevision().getId();
    if (!enrollment.getRevisionId().equals(activityRevisionId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Activity does not belong to enrollment revision: " + activityId);
    }

    if (activity.getActivityType() != ActivityType.QUIZ) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Nhiệm vụ này không phải là bài trắc nghiệm (QUIZ).");
    }

    return activity;
  }
}
