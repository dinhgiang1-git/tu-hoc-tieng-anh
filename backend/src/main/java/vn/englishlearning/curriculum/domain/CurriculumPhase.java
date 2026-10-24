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
@Table(name = "curriculum_phase")
public class CurriculumPhase {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "revision_id", nullable = false)
  private CurriculumRevision revision;

  @Column(name = "phase_key", nullable = false, length = 50)
  private String phaseKey;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  protected CurriculumPhase() {}

  public CurriculumPhase(
      CurriculumRevision revision, String phaseKey, String title, int orderIndex) {
    this.revision = Objects.requireNonNull(revision, "revision must not be null");
    this.phaseKey = Objects.requireNonNull(phaseKey, "phaseKey must not be null");
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.orderIndex = orderIndex;
  }

  public UUID getId() {
    return id;
  }

  public CurriculumRevision getRevision() {
    return revision;
  }

  public String getPhaseKey() {
    return phaseKey;
  }

  public String getTitle() {
    return title;
  }

  public int getOrderIndex() {
    return orderIndex;
  }
}
