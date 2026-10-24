package vn.englishlearning.curriculum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "activity")
public class Activity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(name = "activity_key", nullable = false, length = 50)
  private String activityKey;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false, length = 30)
  private ActivityType activityType;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Enumerated(EnumType.STRING)
  @Column(name = "completion_policy", nullable = false, length = 30)
  private CompletionPolicy completionPolicy;

  @Column(name = "instructions", nullable = false, columnDefinition = "TEXT")
  private String instructions;

  @Column(name = "skill_tags", columnDefinition = "TEXT")
  private String skillTags;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "activity_resource",
      joinColumns = @JoinColumn(name = "activity_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  private Set<Resource> resources = new HashSet<>();

  protected Activity() {}

  public Activity(
      Lesson lesson,
      String activityKey,
      int orderIndex,
      String title,
      ActivityType activityType,
      boolean isRequired,
      CompletionPolicy completionPolicy,
      String instructions,
      String skillTags) {
    this.lesson = Objects.requireNonNull(lesson, "lesson must not be null");
    this.activityKey = Objects.requireNonNull(activityKey, "activityKey must not be null");
    this.orderIndex = orderIndex;
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.activityType = Objects.requireNonNull(activityType, "activityType must not be null");
    this.isRequired = isRequired;
    this.completionPolicy =
        Objects.requireNonNull(completionPolicy, "completionPolicy must not be null");
    this.instructions = Objects.requireNonNull(instructions, "instructions must not be null");
    this.skillTags = skillTags;
  }

  public UUID getId() {
    return id;
  }

  public Lesson getLesson() {
    return lesson;
  }

  public String getActivityKey() {
    return activityKey;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getTitle() {
    return title;
  }

  public ActivityType getActivityType() {
    return activityType;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public CompletionPolicy getCompletionPolicy() {
    return completionPolicy;
  }

  public String getInstructions() {
    return instructions;
  }

  public String getSkillTags() {
    return skillTags;
  }

  public Set<Resource> getResources() {
    return Collections.unmodifiableSet(resources);
  }

  public void addResource(Resource resource) {
    this.resources.add(Objects.requireNonNull(resource, "resource must not be null"));
  }
}
