package vn.englishlearning.learning.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "learning_event")
public class LearningEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "activity_id")
  private UUID activityId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private String payload;

  @Column(name = "local_date")
  private LocalDate localDate;

  @Column(name = "time_zone", length = 64)
  private String timeZone;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected LearningEvent() {}

  public LearningEvent(
      UUID userId,
      UUID enrollmentId,
      UUID activityId,
      String eventType,
      String payload,
      LocalDate localDate,
      String timeZone,
      Instant occurredAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.activityId = activityId;
    this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
    this.payload = payload;
    this.localDate = localDate;
    this.timeZone = timeZone;
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  public LearningEvent(
      UUID userId,
      UUID enrollmentId,
      UUID activityId,
      String eventType,
      String payload,
      Instant occurredAt) {
    this(userId, enrollmentId, activityId, eventType, payload, null, null, occurredAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
