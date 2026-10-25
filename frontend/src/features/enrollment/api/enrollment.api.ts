import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type EnrollmentResponse = components["schemas"]["EnrollmentResponse"];
export type EnrollmentDetailResponse = components["schemas"]["EnrollmentDetailResponse"];
export type EnrollmentSummaryResponse = components["schemas"]["EnrollmentSummaryResponse"];
export type LessonSummaryResponse = components["schemas"]["LessonSummaryResponse"];
export type CreateEnrollmentRequest = components["schemas"]["CreateEnrollmentRequest"];
export type DayOfWeek = components["schemas"]["DayOfWeek"];

export const enrollmentKeys = {
  all: ["enrollments"] as const,
  detail: (id: string) => ["enrollments", "detail", id] as const,
  my: () => ["enrollments", "me"] as const,
  lessonSummary: (enrollmentId: string, lessonId: string) =>
    ["enrollments", enrollmentId, "lessons", lessonId, "summary"] as const,
};

export const enrollmentApi = {
  async create(
    payload: CreateEnrollmentRequest,
    idempotencyKey: string,
  ): Promise<EnrollmentResponse> {
    return apiRequest<EnrollmentResponse>("/enrollments", {
      method: "POST",
      json: payload,
      idempotencyKey,
    });
  },

  async getDetail(
    enrollmentId: string,
    signal?: AbortSignal,
  ): Promise<EnrollmentDetailResponse> {
    return apiRequest<EnrollmentDetailResponse>(`/enrollments/${enrollmentId}`, {
      method: "GET",
      signal,
    });
  },

  async myEnrollments(signal?: AbortSignal): Promise<EnrollmentSummaryResponse[]> {
    return apiRequest<EnrollmentSummaryResponse[]>("/me/enrollments", {
      method: "GET",
      signal,
    });
  },

  async getLessonSummary(
    enrollmentId: string,
    lessonId: string,
    signal?: AbortSignal,
  ): Promise<LessonSummaryResponse> {
    return apiRequest<LessonSummaryResponse>(
      `/enrollments/${enrollmentId}/lessons/${lessonId}/summary`,
      {
        method: "GET",
        signal,
      },
    );
  },
};
