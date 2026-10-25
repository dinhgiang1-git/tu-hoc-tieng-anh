package vn.englishlearning.enrollment.domain;

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

@Entity
@Table(name = "lesson_schedule")
public class LessonSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "planned_date", nullable = false)
  private LocalDate plannedDate;

  @Column(name = "original_date", nullable = false)
  private LocalDate originalDate;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LessonSchedule() {}

  public LessonSchedule(
      UUID enrollmentId,
      UUID lessonId,
      LocalDate plannedDate,
      LocalDate originalDate,
      Instant now) {
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.lessonId = Objects.requireNonNull(lessonId, "lessonId must not be null");
    this.plannedDate = Objects.requireNonNull(plannedDate, "plannedDate must not be null");
    this.originalDate = Objects.requireNonNull(originalDate, "originalDate must not be null");
    this.createdAt = Objects.requireNonNull(now, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getLessonId() {
    return lessonId;
  }

  public LocalDate getPlannedDate() {
    return plannedDate;
  }

  public void setPlannedDate(LocalDate plannedDate, Instant now) {
    this.plannedDate = plannedDate;
    this.updatedAt = now;
  }

  public LocalDate getOriginalDate() {
    return originalDate;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt, Instant now) {
    this.completedAt = completedAt;
    this.updatedAt = now;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
