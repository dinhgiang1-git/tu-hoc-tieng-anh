import { ApiError } from "./api-error";
import { csrfSession } from "./csrf-session";
import { normalizeProblem } from "./normalize-problem";

export interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  json?: unknown;
  formData?: FormData;
  signal?: AbortSignal;
  idempotencyKey?: string;
  timeoutMs?: number;
}

export type Decoder<T> = (value: unknown) => T;

export function identityDecoder<T>(value: unknown): T {
  return value as T;
}

export function voidDecoder(_value: unknown): void {
  return undefined;
}

const BASE_PREFIX = "/api/v1";

/**
 * Universal application HTTP client adhering strictly to FE_CODE_CONVENTION.md Section 5.3.
 */
export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
  decode: Decoder<T> = identityDecoder,
): Promise<T> {
  // 1. Strict internal path validation
  if (!path.startsWith("/") || path.startsWith("//")) {
    throw new Error(
      `Invalid API path: "${path}". Must start with a single "/" and never be an absolute URL.`,
    );
  }

  // 2. Body exclusivity validation
  if (options.json !== undefined && options.formData !== undefined) {
    throw new Error("Cannot provide both 'json' and 'formData' in the same API request.");
  }

  const method = options.method || "GET";
  const url = `${BASE_PREFIX}${path}`;

  const headers = new Headers();
  headers.set("Accept", "application/json, application/problem+json");

  // 3. Attach CSRF token for unsafe methods (POST, PUT, PATCH, DELETE)
  const isUnsafeMethod = ["POST", "PUT", "PATCH", "DELETE"].includes(
    method.toUpperCase(),
  );
  if (isUnsafeMethod) {
    const token = await csrfSession.getToken();
    headers.set(csrfSession.getHeaderName(), token);
  }

  // 4. Attach Idempotency-Key if provided
  if (options.idempotencyKey) {
    headers.set("Idempotency-Key", options.idempotencyKey);
  }

  let body: BodyInit | undefined = undefined;
  if (options.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.json);
  } else if (options.formData !== undefined) {
    body = options.formData;
  }

  // 5. Setup abort signal & timeout
  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  let signal = options.signal;

  if (options.timeoutMs && options.timeoutMs > 0) {
    const controller = new AbortController();
    if (signal) {
      signal.addEventListener("abort", () => controller.abort());
    }
    timeoutId = setTimeout(() => controller.abort(), options.timeoutMs);
    signal = controller.signal;
  }

  try {
    const response = await fetch(url, {
      method,
      headers,
      body,
      credentials: "include",
      signal,
    });

    // 6. Handle HTTP errors
    if (!response.ok) {
      let errorBody: unknown = null;
      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("json")) {
        try {
          errorBody = await response.json();
        } catch {
          errorBody = null;
        }
      }

      const problem = normalizeProblem(errorBody, response.status);
      if (response.status === 403 && problem.code === "CSRF_INVALID") {
        csrfSession.clear();
      }
      throw new ApiError(problem);
    }

    // 7. Handle 204 No Content / 205 Reset Content
    if (response.status === 204 || response.status === 205) {
      return decode(undefined);
    }

    // 8. Parse JSON success response
    let jsonResult: unknown;
    try {
      jsonResult = await response.json();
    } catch {
      throw new ApiError({
        type: "about:blank",
        title: "Lỗi giao thức dữ liệu",
        status: 500,
        detail: "Máy chủ trả về phản hồi không đúng định dạng JSON.",
        code: "PROTOCOL_ERROR",
        traceId: "trace-client-parse-error",
        fieldErrors: [],
      });
    }

    // 9. Run runtime decoder
    try {
      return decode(jsonResult);
    } catch {
      throw new ApiError({
        type: "about:blank",
        title: "Lỗi xác thực dữ liệu API",
        status: 500,
        detail: "Dữ liệu trả về từ máy chủ không khớp với schema mong đợi.",
        code: "PROTOCOL_ERROR",
        traceId: "trace-client-decode-error",
        fieldErrors: [],
      });
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }

    // Network error / connection failure
    throw new ApiError({
      type: "about:blank",
      title: "Lỗi kết nối mạng",
      status: 0,
      detail: "Không thể kết nối tới máy chủ. Vui lòng kiểm tra đường truyền mạng.",
      code: "NETWORK_ERROR",
      traceId: "trace-network-error",
      fieldErrors: [],
    });
  } finally {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  }
}
