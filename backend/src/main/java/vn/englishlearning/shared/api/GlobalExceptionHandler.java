package vn.englishlearning.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.englishlearning.shared.domain.DomainException;

/**
 * Central HTTP exception handler translating exceptions into RFC 7807 problem details. Ensures
 * consistent error representation across MVC endpoints and security boundaries.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final MediaType PROBLEM_JSON_MEDIA_TYPE =
      MediaType.parseMediaType("application/problem+json");

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String path = extractPath(request);
    List<FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                (FieldError error) ->
                    new FieldViolation(
                        error.getField(),
                        error.getCode() != null ? error.getCode() : "INVALID_FIELD",
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Trường dữ liệu không hợp lệ."))
            .toList();

    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.VALIDATION_FAILED,
            "Dữ liệu gửi lên không vượt qua kiểm tra tính hợp lệ.",
            path,
            violations);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String path = extractPath(request);
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.INVALID_REQUEST, "Cú pháp JSON hoặc định dạng dữ liệu không hợp lệ.", path);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String path = extractPath(request);
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.RESOURCE_NOT_FOUND, "Đường dẫn tài nguyên không tồn tại.", path);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String path = extractPath(request);
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.METHOD_NOT_ALLOWED,
            "Phương thức " + ex.getMethod() + " không được hỗ trợ cho đường dẫn này.",
            path);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String path = extractPath(request);
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Định dạng Media-Type không được hỗ trợ.", path);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.INVALID_CREDENTIALS,
            ErrorCode.INVALID_CREDENTIALS.getDefaultDetail(),
            request.getRequestURI());
    return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.AUTHENTICATION_REQUIRED.getDefaultDetail(),
            request.getRequestURI());
    return ResponseEntity.status(ErrorCode.AUTHENTICATION_REQUIRED.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ErrorCode errorCode =
        (ex instanceof CsrfException) ? ErrorCode.CSRF_INVALID : ErrorCode.FORBIDDEN;
    ProblemDetail problem =
        ProblemFactory.create(errorCode, errorCode.getDefaultDetail(), request.getRequestURI());
    return ResponseEntity.status(errorCode.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(
      org.springframework.dao.OptimisticLockingFailureException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.VERSION_CONFLICT,
            ErrorCode.VERSION_CONFLICT.getDefaultDetail(),
            request.getRequestURI());
    return ResponseEntity.status(ErrorCode.VERSION_CONFLICT.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
  public ResponseEntity<ProblemDetail> handleTransactionSystemException(
      org.springframework.transaction.TransactionSystemException ex, HttpServletRequest request) {
    Throwable rootCause = ex.getRootCause();
    if (rootCause instanceof org.hibernate.StaleObjectStateException
        || rootCause instanceof org.springframework.dao.OptimisticLockingFailureException
        || (rootCause != null
            && rootCause.getMessage() != null
            && rootCause
                .getMessage()
                .contains("Row was updated or deleted by another transaction"))) {
      ProblemDetail problem =
          ProblemFactory.create(
              ErrorCode.VERSION_CONFLICT,
              ErrorCode.VERSION_CONFLICT.getDefaultDetail(),
              request.getRequestURI());
      return ResponseEntity.status(ErrorCode.VERSION_CONFLICT.getStatus())
          .contentType(PROBLEM_JSON_MEDIA_TYPE)
          .body(problem);
    }
    return handleGenericException(ex, request);
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
      org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
    ErrorCode errorCode = ErrorCode.STATE_CONFLICT;
    String detail = errorCode.getDefaultDetail();
    if (ex.getMessage() != null
        && (ex.getMessage().contains("uq_enrollment_activity_progress_idx")
            || ex.getMessage().contains("activity_progress"))) {
      errorCode = ErrorCode.VERSION_CONFLICT;
      detail = "Dữ liệu đã thay đổi do thao tác đồng thời. Hãy tải lại trước khi lưu.";
    }
    ProblemDetail problem = ProblemFactory.create(errorCode, detail, request.getRequestURI());
    return ResponseEntity.status(errorCode.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(
      DomainException ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemFactory.create(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    return ResponseEntity.status(ex.getErrorCode().getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.INTERNAL_ERROR,
            ErrorCode.INTERNAL_ERROR.getDefaultDetail(),
            request.getRequestURI());

    String traceId = String.valueOf(problem.getProperties().get("traceId"));
    log.error(
        "Unhandled server exception [traceId: {}, path: {}]: {}",
        traceId,
        request.getRequestURI(),
        ex.getMessage(),
        ex);

    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
        .contentType(PROBLEM_JSON_MEDIA_TYPE)
        .body(problem);
  }

  private String extractPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return request.getContextPath();
  }
}
