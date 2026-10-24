import type { ApiProblem, FieldViolation } from "./api-error";

/**
 * Validates unknown body and normalizes it to RFC 7807 ApiProblem structure.
 * Never outputs raw HTML or unparsed stack traces to UI.
 * Adheres to FE_CODE_CONVENTION.md Section 4.2 & 5.1.
 */
export function normalizeProblem(body: unknown, status: number): ApiProblem {
  if (typeof body === "object" && body !== null) {
    const candidate = body as Record<string, unknown>;

    const type = typeof candidate.type === "string" ? candidate.type : "about:blank";
    const title =
      typeof candidate.title === "string"
        ? candidate.title
        : defaultTitleForStatus(status);
    const detail =
      typeof candidate.detail === "string"
        ? candidate.detail
        : defaultDetailForStatus(status);
    const code =
      typeof candidate.code === "string"
        ? candidate.code
        : defaultCodeForStatus(status);
    const traceId =
      typeof candidate.traceId === "string"
        ? candidate.traceId
        : "trace-unknown";

    const fieldErrors: FieldViolation[] = [];
    if (Array.isArray(candidate.fieldErrors)) {
      for (const item of candidate.fieldErrors) {
        if (
          typeof item === "object" &&
          item !== null &&
          typeof item.field === "string" &&
          typeof item.code === "string" &&
          typeof item.message === "string"
        ) {
          fieldErrors.push({
            field: item.field,
            code: item.code,
            message: item.message,
          });
        }
      }
    }

    return {
      type,
      title,
      status: typeof candidate.status === "number" ? candidate.status : status,
      detail,
      instance: typeof candidate.instance === "string" ? candidate.instance : undefined,
      code,
      traceId,
      fieldErrors,
    };
  }

  return {
    type: "about:blank",
    title: defaultTitleForStatus(status),
    status,
    detail: defaultDetailForStatus(status),
    code: defaultCodeForStatus(status),
    traceId: "trace-fallback",
    fieldErrors: [],
  };
}

function defaultTitleForStatus(status: number): string {
  switch (status) {
    case 400:
      return "Yêu cầu không hợp lệ";
    case 401:
      return "Yêu cầu xác thực";
    case 403:
      return "Truy cập bị từ chối";
    case 404:
      return "Không tìm thấy tài nguyên";
    case 409:
      return "Xung đột phiên bản";
    case 500:
      return "Lỗi máy chủ nội bộ";
    default:
      return `Lỗi hệ thống (${status})`;
  }
}

function defaultDetailForStatus(status: number): string {
  switch (status) {
    case 401:
      return "Phiên làm việc đã hết hạn hoặc bạn chưa đăng nhập.";
    case 403:
      return "Bạn không có quyền thực hiện thao tác này.";
    case 404:
      return "Tài nguyên bạn yêu cầu không tồn tại.";
    case 500:
      return "Đã xảy ra lỗi trên hệ thống máy chủ. Vui lòng thử lại sau.";
    default:
      return "Đã xảy ra lỗi trong quá trình xử lý yêu cầu.";
  }
}

function defaultCodeForStatus(status: number): string {
  switch (status) {
    case 400:
      return "INVALID_REQUEST";
    case 401:
      return "AUTHENTICATION_REQUIRED";
    case 403:
      return "FORBIDDEN";
    case 404:
      return "RESOURCE_NOT_FOUND";
    case 409:
      return "VERSION_CONFLICT";
    default:
      return "HTTP_ERROR";
  }
}
