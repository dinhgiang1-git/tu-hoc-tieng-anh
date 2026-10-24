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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "curriculum_revision")
public class CurriculumRevision {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "curriculum_id", nullable = false)
  private Curriculum curriculum;

  @Column(name = "version", nullable = false, length = 20)
  private String version;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RevisionStatus status;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "authors", nullable = false, columnDefinition = "TEXT")
  private String authors;

  @Column(name = "provenance", nullable = false, columnDefinition = "TEXT")
  private String provenance;

  @Column(name = "checksum", nullable = false, length = 64)
  private String checksum;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CurriculumRevision() {}

  public CurriculumRevision(
      Curriculum curriculum,
      String version,
      String title,
      String description,
      String authors,
      String provenance,
      String checksum,
      Instant createdAt) {
    this.curriculum = Objects.requireNonNull(curriculum, "curriculum must not be null");
    this.version = Objects.requireNonNull(version, "version must not be null");
    this.status = RevisionStatus.DRAFT;
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.description = Objects.requireNonNull(description, "description must not be null");
    this.authors = Objects.requireNonNull(authors, "authors must not be null");
    this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
    this.checksum = Objects.requireNonNull(checksum, "checksum must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public Curriculum getCurriculum() {
    return curriculum;
  }

  public String getVersion() {
    return version;
  }

  public RevisionStatus getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getAuthors() {
    return authors;
  }

  public String getProvenance() {
    return provenance;
  }

  public String getChecksum() {
    return checksum;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void publish(Instant now) {
    if (this.status != RevisionStatus.DRAFT) {
      throw new IllegalStateException(
          "Cannot publish curriculum revision from status: " + this.status);
    }
    this.status = RevisionStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(now, "publishedAt must not be null");
    this.updatedAt = now;
  }

  public void archive(Instant now) {
    if (this.status == RevisionStatus.ARCHIVED) {
      return;
    }
    this.status = RevisionStatus.ARCHIVED;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }
}
