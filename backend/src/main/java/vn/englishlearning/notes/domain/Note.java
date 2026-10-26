package vn.englishlearning.notes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "note")
public class Note {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "enrollment_id")
  private UUID enrollmentId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "activity_id")
  private UUID activityId;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Note() {}

  public Note(
      UUID userId, UUID enrollmentId, UUID lessonId, UUID activityId, String content, Instant now) {
    this.userId = userId;
    this.enrollmentId = enrollmentId;
    this.lessonId = lessonId;
    this.activityId = activityId;
    this.content = content != null ? content : "";
    this.version = 0L;
    this.createdAt = now;
    this.updatedAt = now;
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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content != null ? content : "";
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
