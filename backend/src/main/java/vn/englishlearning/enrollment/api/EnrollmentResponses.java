package vn.englishlearning.enrollment.api;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;

public final class EnrollmentResponses {
  private EnrollmentResponses() {}

  public record EnrollmentResponse(
      UUID id,
      UUID userId,
      UUID curriculumId,
      UUID revisionId,
      EnrollmentStatus status,
      LocalDate startDate,
      String timeZone,
      List<DayOfWeek> studyDays,
      Long version) {}

  public record EnrollmentDetailResponse(
      UUID id,
      UUID userId,
      UUID curriculumId,
      UUID revisionId,
      EnrollmentStatus status,
      LocalDate startDate,
      String timeZone,
      List<DayOfWeek> studyDays,
      int dailyTargetMinutes,
      Long version,
      LocalDate estimatedEndDate) {}

  public record EnrollmentSummaryResponse(
      UUID id,
      String curriculumTitle,
      String revisionVersion,
      EnrollmentStatus status,
      int completedRequiredActivities,
      int totalRequiredActivities,
      BigDecimal progressPercent,
      Long version) {}

  public record LessonSummaryResponse(
      UUID enrollmentId,
      UUID lessonId,
      String curriculumTitle,
      String lessonTitle,
      int estimatedMinutes,
      LocalDate plannedDate,
      boolean isRequired) {}

  public record LessonScheduleResponse(
      UUID lessonId,
      String lessonTitle,
      LocalDate plannedDate,
      LocalDate originalDate,
      boolean isCompleted,
      boolean isOverdue) {}
}
