package vn.englishlearning.scheduling.domain;

import java.util.Objects;
import java.util.UUID;

public record SchedulableLesson(UUID lessonId, boolean required) {
  public SchedulableLesson {
    Objects.requireNonNull(lessonId, "lessonId must not be null");
  }
}
