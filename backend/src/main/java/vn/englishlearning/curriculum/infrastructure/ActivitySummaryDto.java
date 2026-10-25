package vn.englishlearning.curriculum.infrastructure;

import java.util.UUID;

public record ActivitySummaryDto(UUID id, UUID lessonId, int orderIndex, boolean isRequired) {

  public UUID getId() {
    return id;
  }

  public UUID getLessonId() {
    return lessonId;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public boolean isRequired() {
    return isRequired;
  }
}
