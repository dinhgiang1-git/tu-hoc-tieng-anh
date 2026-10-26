package vn.englishlearning.learning.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.englishlearning.learning.domain.ActivityStatus;

public final class LearningProgressRequests {
  private LearningProgressRequests() {}

  public record UpdateProgressRequest(
      @NotNull(message = "Trạng thái không được để trống") ActivityStatus status,
      @NotNull(message = "expectedVersion không được để trống") Long expectedVersion) {}

  public record SubmitResponseRequest(
      @NotNull(message = "Nội dung phản hồi không được để trống")
          @Size(min = 1, max = 5000, message = "Nội dung phản hồi phải từ 1 đến 5000 ký tự")
          String responseText,
      @NotNull(message = "expectedVersion không được để trống") Long expectedVersion) {}

  public record SaveResponseDraftRequest(
      @Size(max = 5000, message = "Nội dung phản hồi tối đa 5000 ký tự") String responseText,
      @NotNull(message = "expectedVersion không được để trống") Long expectedVersion) {}
}
