package vn.englishlearning.review.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.englishlearning.assessment.api.QuizResponses.QuizOptionDto;

public final class ReviewResponses {

  private ReviewResponses() {}

  public record BookmarkResponse(
      UUID id,
      UUID enrollmentId,
      UUID lessonId,
      String lessonTitle,
      UUID activityId,
      String activityTitle,
      String activityType,
      String note,
      Instant createdAt) {}

  public record MistakeItemResponse(
      UUID questionId,
      String prompt,
      UUID lessonId,
      String lessonTitle,
      UUID activityId,
      String activityTitle,
      List<QuizOptionDto> options,
      boolean resolved,
      Instant lastAttemptedAt) {}

  public record ReviewSummaryResponse(
      UUID enrollmentId,
      List<BookmarkResponse> bookmarkedItems,
      List<MistakeItemResponse> mistakes) {}

  public record QuestionCheckResponse(
      UUID questionId,
      UUID selectedOptionId,
      UUID correctOptionId,
      boolean isCorrect,
      String explanation,
      boolean resolved) {}
}
