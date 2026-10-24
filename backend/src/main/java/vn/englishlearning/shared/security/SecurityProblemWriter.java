package vn.englishlearning.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.ProblemFactory;

/**
 * Standardized Problem Details writer for Spring Security filter-level exceptions. Handles 401
 * Unauthorized and 403 Forbidden / CSRF Invalid errors adhering to RFC 7807.
 */
@Component
public class SecurityProblemWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public SecurityProblemWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }

    ErrorCode errorCode =
        (authException instanceof BadCredentialsException)
            ? ErrorCode.INVALID_CREDENTIALS
            : ErrorCode.AUTHENTICATION_REQUIRED;

    writeProblem(request, response, errorCode, errorCode.getDefaultDetail());
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }

    ErrorCode errorCode =
        (accessDeniedException instanceof CsrfException)
            ? ErrorCode.CSRF_INVALID
            : ErrorCode.FORBIDDEN;

    writeProblem(request, response, errorCode, errorCode.getDefaultDetail());
  }

  private void writeProblem(
      HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String detail)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ProblemDetail problem = ProblemFactory.create(errorCode, detail, request.getRequestURI());

    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
