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

  public record ActivityResponseDto(
      UUID id,
      UUID activityId,
      String responseText,
      java.time.Instant updatedAt,
      Long enrollmentVersion) {}

  public record ResourceDto(
      UUID id, String title, String url, String mediaType, String attribution) {}

  public record ActivityWorkspaceDto(
      UUID activityId,
      String title,
      ActivityType type,
      boolean required,
      CompletionPolicy completionPolicy,
      String instructions,
      ActivityStatus status,
      String userResponse,
      List<ResourceDto> resources) {}

  public record LessonDetailResponse(
      UUID lessonId,
      String title,
      int estimatedMinutes,
      boolean required,
      String markdownContent,
      Long enrollmentVersion,
      UUID nextLessonId,
      List<ActivityWorkspaceDto> activities) {}
}
