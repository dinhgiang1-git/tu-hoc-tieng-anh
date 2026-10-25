package vn.englishlearning.learning.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.learning.domain.ActivityStatus;

public final class LearningProgressResponses {
  private LearningProgressResponses() {}

  public record ProgressResponse(
      UUID activityId,
      ActivityStatus status,
      Long enrollmentVersion,
      int completedRequiredActivities,
      int totalRequiredActivities,
      BigDecimal progressPercent) {}

  public record ProgressSummaryResponse(
      int completedRequiredActivities,
      int totalRequiredActivities,
      BigDecimal progressPercent,
      Long enrollmentVersion) {}

  public record LessonActivitiesResponse(
      UUID lessonId, Long enrollmentVersion, List<LessonActivityItemDto> activities) {}

  public record LessonActivityItemDto(
      UUID id,
      String title,
      ActivityType type,
      boolean required,
      CompletionPolicy completionPolicy,
      ActivityStatus status,
      String responseText) {}
}
