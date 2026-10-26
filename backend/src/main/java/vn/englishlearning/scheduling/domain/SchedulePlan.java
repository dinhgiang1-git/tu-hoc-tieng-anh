package vn.englishlearning.scheduling.domain;

import java.time.LocalDate;
import java.util.List;

public record SchedulePlan(List<PlannedLesson> plannedLessons, LocalDate estimatedEndDate) {
  public SchedulePlan {
    plannedLessons = plannedLessons != null ? List.copyOf(plannedLessons) : List.of();
  }
}
