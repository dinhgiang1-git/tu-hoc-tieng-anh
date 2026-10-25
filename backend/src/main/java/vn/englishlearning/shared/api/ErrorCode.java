package vn.englishlearning.shared.api;

import org.springframework.http.HttpStatus;

/**
 * Standard business error codes adhering to BE_CODE_CONVENTION.md (Section 4.2). Clients branch on
 * code rather than localized message strings.
 */
public enum ErrorCode {
  RESOURCE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "Tài nguyên không tồn tại hoặc không thuộc quyền sở hữu."),
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ hoặc sai định dạng payload."),
  RESPONSE_REQUIRED(
      HttpStatus.BAD_REQUEST, "Nhiệm vụ yêu cầu nhập câu trả lời trước khi hoàn thành."),
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thực hiện thao tác."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này."),
  CSRF_INVALID(HttpStatus.FORBIDDEN, "CSRF token không hợp lệ hoặc đã hết hạn."),
  VERSION_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu."),
  STATE_CONFLICT(HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thực hiện thao tác này."),
  IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Yêu cầu trùng lặp nhưng nội dung payload khác nhau."),
  RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."),
  METHOD_NOT_ALLOWED(
      HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ cho tài nguyên này."),
  UNSUPPORTED_MEDIA_TYPE(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng Media-Type không được hỗ trợ."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");

  private final HttpStatus status;
  private final String defaultDetail;

  ErrorCode(HttpStatus status, String defaultDetail) {
    this.status = status;
    this.defaultDetail = defaultDetail;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getDefaultDetail() {
    return defaultDetail;
  }
}
