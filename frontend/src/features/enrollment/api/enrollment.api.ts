import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type EnrollmentResponse = components["schemas"]["EnrollmentResponse"];
export type EnrollmentDetailResponse = components["schemas"]["EnrollmentDetailResponse"];
export type EnrollmentSummaryResponse = components["schemas"]["EnrollmentSummaryResponse"];
export type LessonSummaryResponse = components["schemas"]["LessonSummaryResponse"];
export type LessonScheduleResponse = components["schemas"]["LessonScheduleResponse"];
export type CreateEnrollmentRequest = components["schemas"]["CreateEnrollmentRequest"];
export type DayOfWeek = components["schemas"]["DayOfWeek"];
export type SchedulePreviewRequest = components["schemas"]["SchedulePreviewRequest"];
export type SchedulePreviewResponse = components["schemas"]["SchedulePreviewResponse"];
export type RescheduledLessonItemResponse = components["schemas"]["RescheduledLessonItemResponse"];
export type ScheduleCommitRequest = components["schemas"]["ScheduleCommitRequest"];
export interface LifecycleTransitionRequest {
  expectedVersion: number;
}

export const enrollmentKeys = {
  all: ["enrollments"] as const,
  detail: (id: string) => ["enrollments", "detail", id] as const,
  my: () => ["enrollments", "me"] as const,
  lessonSummary: (enrollmentId: string, lessonId: string) =>
    ["enrollments", enrollmentId, "lessons", lessonId, "summary"] as const,
};

export const scheduleKeys = {
  all: ["schedules"] as const,
  byEnrollment: (enrollmentId: string) => ["schedules", enrollmentId] as const,
  list: (enrollmentId: string, from?: string, to?: string) =>
    ["schedules", enrollmentId, { from, to }] as const,
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

  async getSchedule(
    enrollmentId: string,
    params?: { from?: string; to?: string },
    signal?: AbortSignal,
  ): Promise<LessonScheduleResponse[]> {
    const searchParams = new URLSearchParams();
    if (params?.from) searchParams.set("from", params.from);
    if (params?.to) searchParams.set("to", params.to);
    const query = searchParams.toString();
    const url = `/enrollments/${enrollmentId}/schedule${query ? `?${query}` : ""}`;
    return apiRequest<LessonScheduleResponse[]>(url, {
      method: "GET",
      signal,
    });
  },

  async previewReschedule(
    enrollmentId: string,
    payload: SchedulePreviewRequest,
    signal?: AbortSignal,
  ): Promise<SchedulePreviewResponse> {
    return apiRequest<SchedulePreviewResponse>(
      `/enrollments/${enrollmentId}/schedule/preview`,
      {
        method: "POST",
        json: payload,
        signal,
      },
    );
  },

  async commitReschedule(
    enrollmentId: string,
    payload: ScheduleCommitRequest,
    idempotencyKey: string,
    signal?: AbortSignal,
  ): Promise<EnrollmentResponse> {
    return apiRequest<EnrollmentResponse>(
      `/enrollments/${enrollmentId}/schedule/commit`,
      {
        method: "POST",
        json: payload,
        idempotencyKey,
        signal,
      },
    );
  },

  async pause(
    enrollmentId: string,
    payload: LifecycleTransitionRequest,
    signal?: AbortSignal,
  ): Promise<EnrollmentResponse> {
    return apiRequest<EnrollmentResponse>(
      `/enrollments/${enrollmentId}/pause`,
      {
        method: "POST",
        json: payload,
        signal,
      },
    );
  },

  async resume(
    enrollmentId: string,
    payload: ScheduleCommitRequest,
    idempotencyKey: string,
    signal?: AbortSignal,
  ): Promise<EnrollmentResponse> {
    return apiRequest<EnrollmentResponse>(
      `/enrollments/${enrollmentId}/resume`,
      {
        method: "POST",
        json: payload,
        idempotencyKey,
        signal,
      },
    );
  },

  async abandon(
    enrollmentId: string,
    payload: LifecycleTransitionRequest,
    signal?: AbortSignal,
  ): Promise<EnrollmentResponse> {
    return apiRequest<EnrollmentResponse>(
      `/enrollments/${enrollmentId}/abandon`,
      {
        method: "POST",
        json: payload,
        signal,
      },
    );
  },
};
