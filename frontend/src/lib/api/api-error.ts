export interface FieldViolation {
  field: string;
  code: string;
  message: string;
}

export interface ApiProblem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  code: string;
  traceId: string;
  fieldErrors: FieldViolation[];
}

/**
 * Standard API error class carrying RFC 7807 problem details.
 * Adheres to FE_CODE_CONVENTION.md Section 4.2 & 5.1.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem) {
    super(problem.detail || problem.title || `Lỗi API (${problem.status})`);
    this.name = "ApiError";
    this.status = problem.status;
    this.code = problem.code;
    this.problem = problem;
  }

  isUnauthorized(): boolean {
    return this.status === 401;
  }

  isForbidden(): boolean {
    return this.status === 403;
  }

  isCsrfInvalid(): boolean {
    return this.status === 403 && this.code === "CSRF_INVALID";
  }

  isNotFound(): boolean {
    return this.status === 404;
  }

  isValidationFailed(): boolean {
    return this.status === 400 && this.code === "VALIDATION_FAILED";
  }
}
