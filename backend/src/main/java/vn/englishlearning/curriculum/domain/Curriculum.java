package vn.englishlearning.curriculum.domain;

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
@Table(name = "curriculum")
public class Curriculum {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "curriculum_key", nullable = false, unique = true, length = 50)
  private String curriculumKey;

  @Column(name = "target_locale", nullable = false, length = 10)
  private String targetLocale;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Curriculum() {}

  public Curriculum(String curriculumKey, String targetLocale, Instant createdAt) {
    this.curriculumKey = Objects.requireNonNull(curriculumKey, "curriculumKey must not be null");
    this.targetLocale = Objects.requireNonNull(targetLocale, "targetLocale must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getCurriculumKey() {
    return curriculumKey;
  }

  public String getTargetLocale() {
    return targetLocale;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateTimestamp(Instant updatedAt) {
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
