package vn.englishlearning.curriculum.domain;

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
@Table(name = "curriculum_week")
public class CurriculumWeek {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "phase_id", nullable = false)
  private CurriculumPhase phase;

  @Column(name = "week_key", nullable = false, length = 50)
  private String weekKey;

  @Column(name = "week_number", nullable = false)
  private int weekNumber;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  protected CurriculumWeek() {}

  public CurriculumWeek(
      CurriculumPhase phase,
      String weekKey,
      int weekNumber,
      int orderIndex,
      String title,
      String description) {
    this.phase = Objects.requireNonNull(phase, "phase must not be null");
    this.weekKey = Objects.requireNonNull(weekKey, "weekKey must not be null");
    this.weekNumber = weekNumber;
    this.orderIndex = orderIndex;
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.description = Objects.requireNonNull(description, "description must not be null");
  }

  public UUID getId() {
    return id;
  }

  public CurriculumPhase getPhase() {
    return phase;
  }

  public String getWeekKey() {
    return weekKey;
  }

  public int getWeekNumber() {
    return weekNumber;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }
}
