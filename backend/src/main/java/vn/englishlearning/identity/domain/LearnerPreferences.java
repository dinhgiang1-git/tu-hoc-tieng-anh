package vn.englishlearning.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Learner preferences entity storing study schedule preferences and timezone. Maps 1-to-1 with
 * app_user via userId.
 */
@Entity
@Table(name = "learner_preferences")
public class LearnerPreferences {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "daily_goal_minutes", nullable = false)
  private int dailyGoalMinutes;

  @Column(name = "study_days", nullable = false, length = 50)
  private String studyDays;

  @Column(name = "timezone", nullable = false, length = 50)
  private String timezone;

  @Column(name = "focused_enrollment_id")
  private UUID focusedEnrollmentId;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LearnerPreferences() {
    // JPA required constructor
  }

  public LearnerPreferences(
      UUID userId, int dailyGoalMinutes, String studyDays, String timezone, Instant now) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    setDailyGoalMinutes(dailyGoalMinutes);
    this.studyDays = Objects.requireNonNull(studyDays, "studyDays must not be null");
    this.timezone = Objects.requireNonNull(timezone, "timezone must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public UUID getUserId() {
    return userId;
  }

  public int getDailyGoalMinutes() {
    return dailyGoalMinutes;
  }

  public void setDailyGoalMinutes(int dailyGoalMinutes) {
    if (dailyGoalMinutes < 10 || dailyGoalMinutes > 180) {
      throw new IllegalArgumentException("dailyGoalMinutes must be between 10 and 180");
    }
    this.dailyGoalMinutes = dailyGoalMinutes;
  }

  public String getStudyDays() {
    return studyDays;
  }

  public void setStudyDays(String studyDays, Instant now) {
    this.studyDays = Objects.requireNonNull(studyDays, "studyDays must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone, Instant now) {
    this.timezone = Objects.requireNonNull(timezone, "timezone must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public UUID getFocusedEnrollmentId() {
    return focusedEnrollmentId;
  }

  public void setFocusedEnrollmentId(UUID focusedEnrollmentId, Instant now) {
    this.focusedEnrollmentId = focusedEnrollmentId;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
