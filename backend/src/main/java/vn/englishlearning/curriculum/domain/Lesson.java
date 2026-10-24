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
@Table(name = "lesson")
public class Lesson {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "week_id", nullable = false)
  private CurriculumWeek week;

  @Column(name = "lesson_key", nullable = false, length = 50)
  private String lessonKey;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "estimated_minutes", nullable = false)
  private int estimatedMinutes;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
  private String contentMarkdown;

  protected Lesson() {}

  public Lesson(
      CurriculumWeek week,
      String lessonKey,
      int orderIndex,
      String title,
      int estimatedMinutes,
      boolean isRequired,
      String contentMarkdown) {
    this.week = Objects.requireNonNull(week, "week must not be null");
    this.lessonKey = Objects.requireNonNull(lessonKey, "lessonKey must not be null");
    this.orderIndex = orderIndex;
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.estimatedMinutes = estimatedMinutes;
    this.isRequired = isRequired;
    this.contentMarkdown =
        Objects.requireNonNull(contentMarkdown, "contentMarkdown must not be null");
  }

  public UUID getId() {
    return id;
  }

  public CurriculumWeek getWeek() {
    return week;
  }

  public String getLessonKey() {
    return lessonKey;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getTitle() {
    return title;
  }

  public int getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public String getContentMarkdown() {
    return contentMarkdown;
  }
}
