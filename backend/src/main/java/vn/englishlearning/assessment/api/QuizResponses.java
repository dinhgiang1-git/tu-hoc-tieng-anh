package vn.englishlearning.assessment.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QuizResponses {

  private QuizResponses() {}

  public record QuizDetailResponse(
      UUID activityId,
      String title,
      String completionPolicy,
      int passingScorePercentage,
      List<QuizQuestionDto> questions,
      QuizAttemptSummaryDto latestAttempt) {}

  public record QuizQuestionDto(
      UUID id, int orderIndex, String prompt, List<QuizOptionDto> options) {}

  public record QuizOptionDto(UUID id, int orderIndex, String content) {}

  public record QuizAttemptSummaryDto(
      UUID id,
      int attemptNumber,
      int score,
      int totalQuestions,
      int scorePercentage,
      boolean passed,
      Instant submittedAt) {}

  public record QuizResultResponse(
      UUID attemptId,
      int attemptNumber,
      int score,
      int totalQuestions,
      int scorePercentage,
      boolean passed,
      boolean activityCompleted,
      Long enrollmentVersion,
      List<QuestionResultDto> questionResults) {}

  public record QuestionResultDto(
      UUID questionId,
      UUID selectedOptionId,
      UUID correctOptionId,
      boolean isCorrect,
      String explanation) {}
}
