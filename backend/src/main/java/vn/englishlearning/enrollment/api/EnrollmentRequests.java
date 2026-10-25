package vn.englishlearning.enrollment.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class EnrollmentRequests {
  private EnrollmentRequests() {}

  public record CreateEnrollmentRequest(
      @NotNull UUID curriculumId,
      @NotNull UUID revisionId,
      @NotNull LocalDate startDate,
      @NotEmpty List<DayOfWeek> studyDays,
      @Min(10) @Max(180) int dailyTargetMinutes,
      @NotNull String timeZone) {}
}
