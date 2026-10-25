package vn.englishlearning.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "enrollment")
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "curriculum_id", nullable = false)
  private UUID curriculumId;

  @Column(name = "revision_id", nullable = false)
  private UUID revisionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "time_zone", nullable = false, length = 64)
  private String timeZone;

  @Column(name = "study_days", nullable = false, length = 128)
  private String studyDays;

  @Column(name = "daily_target_minutes", nullable = false)
  private int dailyTargetMinutes;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 1L;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Enrollment() {}

  public Enrollment(
      UUID userId,
      UUID curriculumId,
      UUID revisionId,
      LocalDate startDate,
      String timeZone,
      List<DayOfWeek> studyDays,
      int dailyTargetMinutes,
      Instant now) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.curriculumId = Objects.requireNonNull(curriculumId, "curriculumId must not be null");
    this.revisionId = Objects.requireNonNull(revisionId, "revisionId must not be null");
    this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
    this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
    setStudyDaysList(studyDays);
    this.dailyTargetMinutes = dailyTargetMinutes;
    this.status = EnrollmentStatus.ACTIVE;
    this.version = 1L;
    this.createdAt = Objects.requireNonNull(now, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getCurriculumId() {
    return curriculumId;
  }

  public UUID getRevisionId() {
    return revisionId;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public void setStatus(EnrollmentStatus status) {
    this.status = status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public String getStudyDays() {
    return studyDays;
  }

  public List<DayOfWeek> getStudyDaysList() {
    if (studyDays == null || studyDays.isBlank()) {
      return List.of();
    }
    return Arrays.stream(studyDays.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(DayOfWeek::valueOf)
        .collect(Collectors.toList());
  }

  public Set<DayOfWeek> getStudyDaysSet() {
    return Set.copyOf(getStudyDaysList());
  }

  public void setStudyDaysList(List<DayOfWeek> days) {
    Objects.requireNonNull(days, "studyDays must not be null");
    this.studyDays = days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
  }

  public int getDailyTargetMinutes() {
    return dailyTargetMinutes;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
