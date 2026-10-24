package vn.englishlearning.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.identity.application.IdentityService;

/**
 * REST controller for session authentication and CSRF token distribution. Adheres to openapi.yaml
 * (Section 1: Authentication & Session).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final IdentityService identityService;

  public AuthController(IdentityService identityService) {
    this.identityService = identityService;
  }

  @GetMapping("/csrf")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    String headerName = csrfToken != null ? csrfToken.getHeaderName() : "X-CSRF-TOKEN";
    String token = csrfToken != null ? csrfToken.getToken() : "";

    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new CsrfTokenResponse(headerName, token));
  }

  @PostMapping("/login")
  public UserResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    return identityService.login(request, httpRequest, httpResponse);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    identityService.logout(httpRequest, httpResponse);
    return ResponseEntity.noContent().build();
  }
}
