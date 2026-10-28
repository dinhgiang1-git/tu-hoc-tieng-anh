package vn.englishlearning.learning.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsResponses {
  private AnalyticsResponses() {}

  public record EnrollmentAnalyticsResponse(
      UUID enrollmentId,
      String curriculumTitle,
      String status,
      LocalDate startDate,
      String timeZone,
      Long enrollmentVersion,
      int completedRequiredActivities,
      int totalRequiredActivities,
      BigDecimal progressPercent,
      int completedLessons,
      int totalLessons,
      int estimatedCompletedMinutes,
      int estimatedTotalMinutes,
      StreakStatsDto streak,
      QuizSummaryDto quizSummary,
      List<WeeklyProgressItemDto> weeklyProgress,
      List<DailyActivityHistoryDto> dailyHistory) {}

  public record StreakStatsDto(
      int currentStreakDays,
      int longestStreakDays,
      int totalActiveDays,
      LocalDate lastActiveDate) {}

  public record QuizSummaryDto(
      int totalQuizzesTaken, int passedQuizzes, BigDecimal averageScorePercent) {}

  public record WeeklyProgressItemDto(
      UUID weekId,
      int weekNumber,
      String weekTitle,
      int totalRequiredActivities,
      int completedRequiredActivities,
      BigDecimal progressPercent,
      int totalLessons,
      int completedLessons,
      int estimatedMinutes,
      String status) {}

  public record DailyActivityHistoryDto(
      LocalDate date,
      int completedActivitiesCount,
      int quizAttemptsCount,
      List<DailyEventDto> events) {}

  public record DailyEventDto(
      UUID id, String eventType, Instant occurredAt, String activityTitle, String lessonTitle) {}
}
