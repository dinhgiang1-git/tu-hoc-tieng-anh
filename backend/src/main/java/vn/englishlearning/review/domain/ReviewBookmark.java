package vn.englishlearning.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "review_bookmark")
public class ReviewBookmark {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "activity_id")
  private UUID activityId;

  @Column(name = "note", length = 500)
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ReviewBookmark() {}

  public ReviewBookmark(
      UUID userId,
      UUID enrollmentId,
      UUID lessonId,
      UUID activityId,
      String note,
      Instant createdAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.lessonId = Objects.requireNonNull(lessonId, "lessonId must not be null");
    this.activityId = activityId;
    this.note = note;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
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

  public UUID getLessonId() {
    return lessonId;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public String getNote() {
    return note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
