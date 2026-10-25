package vn.englishlearning.learning.infrastructure;

import java.util.UUID;
import vn.englishlearning.learning.domain.ActivityStatus;

public record ActivityProgressDto(UUID enrollmentId, UUID activityId, ActivityStatus status) {

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public ActivityStatus getStatus() {
    return status;
  }
}
