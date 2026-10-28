package vn.englishlearning.assessment.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class QuizRequests {

  private QuizRequests() {}

  public record QuizSubmitRequest(
      @NotNull(message = "expectedVersion không được để trống") Long expectedVersion,
      @NotEmpty(message = "Danh sách câu trả lời không được để trống")
          List<@Valid QuizAnswerItemRequest> answers) {}

  public record QuizAnswerItemRequest(
      @NotNull(message = "questionId không được để trống") UUID questionId,
      @NotNull(message = "selectedOptionId không được để trống") UUID selectedOptionId) {}
}
