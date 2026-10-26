package vn.englishlearning.enrollment.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LessonScheduleItemDto(
    UUID lessonId,
    String lessonTitle,
    LocalDate plannedDate,
    LocalDate originalDate,
    Instant completedAt) {}
