package vn.englishlearning.curriculum.infrastructure;

import java.util.UUID;

public record LessonSummaryDto(
    UUID id,
    UUID revisionId,
    UUID weekId,
    int orderIndex,
    String title,
    int estimatedMinutes,
    boolean isRequired) {

  public UUID getId() {
    return id;
  }

  public UUID getRevisionId() {
    return revisionId;
  }

  public UUID getWeekId() {
    return weekId;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getTitle() {
    return title;
  }

  public int getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public boolean isRequired() {
    return isRequired;
  }
}
