package vn.englishlearning.identity.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.identity.application.IdentityService;

/**
 * REST controller for authenticated learner profile and preferences. Adheres to openapi.yaml (/me
 * and /me/preferences).
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final IdentityService identityService;

  public MeController(IdentityService identityService) {
    this.identityService = identityService;
  }

  @GetMapping
  public UserResponse getCurrentUser() {
    return identityService.getCurrentUser();
  }

  @GetMapping("/preferences")
  public LearnerPreferencesResponse getPreferences() {
    return identityService.getPreferences();
  }

  @PatchMapping("/preferences")
  public LearnerPreferencesResponse updatePreferences(
      @Valid @RequestBody UpdatePreferencesRequest request) {
    return identityService.updatePreferences(request);
  }
}
