import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type LessonActivitiesResponse = components["schemas"]["LessonActivitiesResponse"];
export type LessonActivityItemDto = components["schemas"]["LessonActivityItemDto"];
export type ProgressResponse = components["schemas"]["ProgressResponse"];
export type ProgressSummaryResponse = components["schemas"]["ProgressSummaryResponse"];
export type UpdateProgressRequest = components["schemas"]["UpdateProgressRequest"];
export type SubmitResponseRequest = components["schemas"]["SubmitResponseRequest"];
export type ActivityStatus = components["schemas"]["ActivityStatus"];
export type ActivityType = components["schemas"]["ActivityType"];
export type CompletionPolicy = components["schemas"]["CompletionPolicy"];

export const learningProgressKeys = {
  all: ["learning-progress"] as const,
  activities: (enrollmentId: string, lessonId: string) =>
    ["learning-progress", enrollmentId, "lessons", lessonId, "activities"] as const,
  summary: (enrollmentId: string) =>
    ["learning-progress", enrollmentId, "summary"] as const,
};

export const learningProgressApi = {
  async getLessonActivities(
    enrollmentId: string,
    lessonId: string,
    signal?: AbortSignal,
  ): Promise<LessonActivitiesResponse> {
    return apiRequest<LessonActivitiesResponse>(
      `/enrollments/${enrollmentId}/lessons/${lessonId}/activities`,
      {
        method: "GET",
        signal,
      },
    );
  },

  async getSummary(
    enrollmentId: string,
    signal?: AbortSignal,
  ): Promise<ProgressSummaryResponse> {
    return apiRequest<ProgressSummaryResponse>(`/enrollments/${enrollmentId}/summary`, {
      method: "GET",
      signal,
    });
  },

  async updateProgress(
    enrollmentId: string,
    activityId: string,
    payload: UpdateProgressRequest,
  ): Promise<ProgressResponse> {
    return apiRequest<ProgressResponse>(
      `/enrollments/${enrollmentId}/activities/${activityId}/progress`,
      {
        method: "PUT",
        json: payload,
      },
    );
  },

  async submitResponse(
    enrollmentId: string,
    activityId: string,
    payload: SubmitResponseRequest,
  ): Promise<ProgressResponse> {
    return apiRequest<ProgressResponse>(
      `/enrollments/${enrollmentId}/activities/${activityId}/response/submit`,
      {
        method: "POST",
        json: payload,
      },
    );
  },
};
