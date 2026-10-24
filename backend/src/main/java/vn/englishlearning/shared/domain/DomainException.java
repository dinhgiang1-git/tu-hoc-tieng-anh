package vn.englishlearning.shared.domain;

import vn.englishlearning.shared.api.ErrorCode;

/**
 * Standard business domain exception. Captures an ErrorCode and optional contextual details without
 * leaking technical internals.
 */
public class DomainException extends RuntimeException {

  private final ErrorCode errorCode;

  public DomainException(ErrorCode errorCode) {
    super(errorCode.getDefaultDetail());
    this.errorCode = errorCode;
  }

  public DomainException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
