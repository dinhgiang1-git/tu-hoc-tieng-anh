package vn.englishlearning.assessment.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "activity_id", nullable = false)
  private UUID activityId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "attempt_number", nullable = false)
  private int attemptNumber;

  @Column(name = "score", nullable = false)
  private int score;

  @Column(name = "total_questions", nullable = false)
  private int totalQuestions;

  @Column(name = "score_percentage", nullable = false)
  private int scorePercentage;

  @Column(name = "passed", nullable = false)
  private boolean passed;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @OneToMany(
      mappedBy = "attempt",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<QuizAttemptAnswer> answers = new ArrayList<>();

  protected QuizAttempt() {}

  public QuizAttempt(
      UUID enrollmentId,
      UUID activityId,
      UUID userId,
      int attemptNumber,
      int score,
      int totalQuestions,
      int scorePercentage,
      boolean passed,
      Instant submittedAt) {
    this.enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    this.activityId = Objects.requireNonNull(activityId, "activityId must not be null");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.attemptNumber = attemptNumber;
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.scorePercentage = scorePercentage;
    this.passed = passed;
    this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
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

  public UUID getUserId() {
    return userId;
  }

  public int getAttemptNumber() {
    return attemptNumber;
  }

  public int getScore() {
    return score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public int getScorePercentage() {
    return scorePercentage;
  }

  public boolean isPassed() {
    return passed;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public List<QuizAttemptAnswer> getAnswers() {
    return Collections.unmodifiableList(answers);
  }

  public void addAnswer(QuizAttemptAnswer answer) {
    answers.add(answer);
    answer.setAttempt(this);
  }
}
