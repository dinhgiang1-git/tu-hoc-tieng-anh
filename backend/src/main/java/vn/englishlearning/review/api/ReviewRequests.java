package vn.englishlearning.review.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ReviewRequests {

  private ReviewRequests() {}

  public record CreateBookmarkRequest(
      @NotNull(message = "enrollmentId is required") UUID enrollmentId,
      @NotNull(message = "lessonId is required") UUID lessonId,
      UUID activityId,
      @Size(max = 500, message = "note must not exceed 500 characters") String note) {}

  public record CheckQuestionRequest(
      @NotNull(message = "selectedOptionId is required") UUID selectedOptionId) {}
}
