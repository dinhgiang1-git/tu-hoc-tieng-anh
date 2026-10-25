package vn.englishlearning.learning.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "activity_progress")
public class ActivityProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "activity_id", nullable = false)
  private UUID activityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ActivityStatus status = ActivityStatus.NOT_STARTED;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "user_response", columnDefinition = "TEXT")
  private String userResponse;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ActivityProgress() {}

  public ActivityProgress(
      UUID enrollmentId, UUID activityId, ActivityStatus status, String userResponse, Instant now) {
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.activityId = Objects.requireNonNull(activityId, "activityId must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.userResponse = userResponse;
    if (status == ActivityStatus.COMPLETED) {
      this.completedAt = now;
    }
    this.createdAt = Objects.requireNonNull(now, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public ActivityProgress(UUID enrollmentId, UUID activityId, ActivityStatus status, Instant now) {
    this(enrollmentId, activityId, status, null, now);
  }

  public UUID getId() {
    return id;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public ActivityStatus getStatus() {
    return status;
  }

  public void setStatus(ActivityStatus status, Instant now) {
    this.status = status;
    if (status == ActivityStatus.COMPLETED) {
      this.completedAt = now;
    } else {
      this.completedAt = null;
    }
    this.updatedAt = now;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getUserResponse() {
    return userResponse;
  }

  public void setUserResponse(String userResponse, Instant now) {
    this.userResponse = userResponse;
    this.updatedAt = now;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
