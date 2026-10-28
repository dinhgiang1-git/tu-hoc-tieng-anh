package vn.englishlearning.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt_answer")
public class QuizAttemptAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attempt_id", nullable = false)
  private QuizAttempt attempt;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "selected_option_id", nullable = false)
  private UUID selectedOptionId;

  @Column(name = "is_correct", nullable = false)
  private boolean isCorrect;

  protected QuizAttemptAnswer() {}

  public QuizAttemptAnswer(UUID questionId, UUID selectedOptionId, boolean isCorrect) {
    this.questionId = Objects.requireNonNull(questionId, "questionId must not be null");
    this.selectedOptionId =
        Objects.requireNonNull(selectedOptionId, "selectedOptionId must not be null");
    this.isCorrect = isCorrect;
  }

  public UUID getId() {
    return id;
  }

  public QuizAttempt getAttempt() {
    return attempt;
  }

  void setAttempt(QuizAttempt attempt) {
    this.attempt = attempt;
  }

  public UUID getQuestionId() {
    return questionId;
  }

  public UUID getSelectedOptionId() {
    return selectedOptionId;
  }

  public boolean isCorrect() {
    return isCorrect;
  }
}
