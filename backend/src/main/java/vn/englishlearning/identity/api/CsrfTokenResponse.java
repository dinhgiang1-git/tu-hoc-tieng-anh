package vn.englishlearning.identity.api;

/** Standard CSRF token response adhering to openapi.yaml CsrfTokenResponse. */
public record CsrfTokenResponse(String headerName, String token) {}
