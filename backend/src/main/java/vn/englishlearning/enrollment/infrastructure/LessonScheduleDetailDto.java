package vn.englishlearning.enrollment.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LessonScheduleDetailDto(
    UUID scheduleId,
    UUID lessonId,
    String lessonTitle,
    LocalDate plannedDate,
    LocalDate originalDate,
    Instant completedAt,
    boolean isRequired) {}
