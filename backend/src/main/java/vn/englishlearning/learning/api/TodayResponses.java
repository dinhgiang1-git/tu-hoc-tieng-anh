package vn.englishlearning.learning.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TodayResponses {
  private TodayResponses() {}

  public record TodayResponse(
      String state,
      UUID enrollmentId,
      UUID revisionId,
      String curriculumTitle,
      LocalDate todayDate,
      boolean isRestDay,
      Long enrollmentVersion,
      PrimaryActionDto primaryAction,
      List<OverdueLessonDto> overdueLessons,
      ProgressSummaryResponse progress) {}

  public record PrimaryActionDto(
      String type,
      String message,
      UUID lessonId,
      String lessonTitle,
      Integer estimatedMinutes,
      Integer completedActivities,
      Integer totalActivities) {}

  public record OverdueLessonDto(
      UUID lessonId, String lessonTitle, LocalDate plannedDate, int overdueDays) {}

  public record ProgressSummaryResponse(
      int completedRequiredActivities,
      int totalRequiredActivities,
      BigDecimal progressPercent,
      Long enrollmentVersion) {}
}
