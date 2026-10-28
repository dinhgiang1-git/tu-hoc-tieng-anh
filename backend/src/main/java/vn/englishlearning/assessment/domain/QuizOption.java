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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quiz_option")
public class QuizOption {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private QuizQuestion question;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_correct", nullable = false)
  private boolean isCorrect;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected QuizOption() {}

  public QuizOption(int orderIndex, String content, boolean isCorrect, Instant now) {
    this.orderIndex = orderIndex;
    this.content = Objects.requireNonNull(content, "content must not be null");
    this.isCorrect = isCorrect;
    this.createdAt = Objects.requireNonNull(now, "now must not be null");
  }

  public UUID getId() {
    return id;
  }

  public QuizQuestion getQuestion() {
    return question;
  }

  void setQuestion(QuizQuestion question) {
    this.question = question;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getContent() {
    return content;
  }

  public boolean isCorrect() {
    return isCorrect;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
