package vn.englishlearning.assessment.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "activity_id", nullable = false)
  private UUID activityId;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
  private String prompt;

  @Column(name = "explanation", columnDefinition = "TEXT")
  private String explanation;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(
      mappedBy = "question",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("orderIndex ASC")
  private List<QuizOption> options = new ArrayList<>();

  protected QuizQuestion() {}

  public QuizQuestion(
      UUID activityId, int orderIndex, String prompt, String explanation, Instant now) {
    this.activityId = Objects.requireNonNull(activityId, "activityId must not be null");
    this.orderIndex = orderIndex;
    this.prompt = Objects.requireNonNull(prompt, "prompt must not be null");
    this.explanation = explanation;
    this.createdAt = Objects.requireNonNull(now, "now must not be null");
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getExplanation() {
    return explanation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<QuizOption> getOptions() {
    return Collections.unmodifiableList(options);
  }

  public void addOption(QuizOption option) {
    options.add(option);
    option.setQuestion(this);
  }

  public void setExplanation(String explanation, Instant now) {
    this.explanation = explanation;
    this.updatedAt = now;
  }
}
