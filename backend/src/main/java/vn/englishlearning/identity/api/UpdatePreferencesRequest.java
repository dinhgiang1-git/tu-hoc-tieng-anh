package vn.englishlearning.identity.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * Request to update learner preferences adhering to openapi.yaml UpdatePreferencesRequest. Tracks
 * presence of fields to distinguish absent fields from explicit null in PATCH requests.
 */
public class UpdatePreferencesRequest {

  @Min(value = 10, message = "Mục tiêu tối thiểu là 10 phút.")
  @Max(value = 180, message = "Mục tiêu tối đa là 180 phút.")
  @JsonProperty("dailyTargetMinutes")
  private Integer dailyTargetMinutes;

  private boolean dailyTargetMinutesPresent = false;

  @JsonProperty("focusedEnrollmentId")
  private UUID focusedEnrollmentId;

  private boolean focusedEnrollmentIdPresent = false;

  public UpdatePreferencesRequest() {}

  public UpdatePreferencesRequest(Integer dailyTargetMinutes, UUID focusedEnrollmentId) {
    if (dailyTargetMinutes != null) {
      setDailyTargetMinutes(dailyTargetMinutes);
    }
    setFocusedEnrollmentId(focusedEnrollmentId);
  }

  public Integer dailyTargetMinutes() {
    return dailyTargetMinutes;
  }

  public boolean isDailyTargetMinutesPresent() {
    return dailyTargetMinutesPresent;
  }

  @JsonSetter(value = "dailyTargetMinutes", nulls = Nulls.SET)
  public void setDailyTargetMinutes(Integer dailyTargetMinutes) {
    this.dailyTargetMinutes = dailyTargetMinutes;
    this.dailyTargetMinutesPresent = true;
  }

  public UUID focusedEnrollmentId() {
    return focusedEnrollmentId;
  }

  public boolean isFocusedEnrollmentIdPresent() {
    return focusedEnrollmentIdPresent;
  }

  @JsonSetter(value = "focusedEnrollmentId", nulls = Nulls.SET)
  public void setFocusedEnrollmentId(UUID focusedEnrollmentId) {
    this.focusedEnrollmentId = focusedEnrollmentId;
    this.focusedEnrollmentIdPresent = true;
  }

  public static UpdatePreferencesRequest ofDailyTarget(Integer dailyTargetMinutes) {
    UpdatePreferencesRequest request = new UpdatePreferencesRequest();
    request.setDailyTargetMinutes(dailyTargetMinutes);
    return request;
  }

  public static UpdatePreferencesRequest ofFocusedEnrollment(UUID focusedEnrollmentId) {
    UpdatePreferencesRequest request = new UpdatePreferencesRequest();
    request.setFocusedEnrollmentId(focusedEnrollmentId);
    return request;
  }

  public static UpdatePreferencesRequest clearFocusedEnrollment() {
    UpdatePreferencesRequest request = new UpdatePreferencesRequest();
    request.setFocusedEnrollmentId(null);
    return request;
  }
}
