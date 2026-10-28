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
@Table(name = "review_resolution")
public class ReviewResolution {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "resolved_at", nullable = false)
  private Instant resolvedAt;

  protected ReviewResolution() {}

  public ReviewResolution(UUID enrollmentId, UUID questionId, Instant resolvedAt) {
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.questionId = Objects.requireNonNull(questionId, "questionId must not be null");
    this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getQuestionId() {
    return questionId;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
  }
}
