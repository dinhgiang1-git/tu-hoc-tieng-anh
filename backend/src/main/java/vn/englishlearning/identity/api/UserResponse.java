package vn.englishlearning.identity.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import vn.englishlearning.identity.domain.UserRole;

/** Standard User profile response adhering to openapi.yaml UserResponse. */
public record UserResponse(
    UUID id, String username, String fullName, String timeZone, UserRole role) {

  @JsonProperty("displayName")
  public String displayName() {
    return fullName;
  }

  @JsonProperty("timezone")
  public String timezone() {
    return timeZone;
  }
}
