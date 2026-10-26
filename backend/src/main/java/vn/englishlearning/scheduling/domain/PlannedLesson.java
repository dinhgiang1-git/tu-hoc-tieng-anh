package vn.englishlearning.scheduling.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PlannedLesson(UUID lessonId, LocalDate plannedDate) {
  public PlannedLesson {
    Objects.requireNonNull(lessonId, "lessonId must not be null");
    Objects.requireNonNull(plannedDate, "plannedDate must not be null");
  }
}
