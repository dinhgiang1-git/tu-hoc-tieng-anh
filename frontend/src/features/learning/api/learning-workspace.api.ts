import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type LessonDetailResponse = components["schemas"]["LessonDetailResponse"];
export type ActivityWorkspaceDto = components["schemas"]["ActivityWorkspaceDto"];
export type ActivityResponseDto = components["schemas"]["ActivityResponseDto"];
export type ResourceDto = components["schemas"]["ResourceDto"];
export type SaveResponseDraftRequest = components["schemas"]["SaveResponseDraftRequest"];

export const learningWorkspaceKeys = {
  all: ["learning-workspace"] as const,
  lesson: (enrollmentId: string, lessonId: string) =>
    [...learningWorkspaceKeys.all, enrollmentId, "lessons", lessonId] as const,
};

export const learningWorkspaceApi = {
  async getLessonWorkspace(
    enrollmentId: string,
    lessonId: string,
    signal?: AbortSignal,
  ): Promise<LessonDetailResponse> {
    return apiRequest<LessonDetailResponse>(
      `/enrollments/${enrollmentId}/lessons/${lessonId}`,
      {
        method: "GET",
        signal,
      },
    );
  },

  async saveResponseDraft(
    enrollmentId: string,
    activityId: string,
    payload: SaveResponseDraftRequest,
    signal?: AbortSignal,
  ): Promise<ActivityResponseDto> {
    return apiRequest<ActivityResponseDto>(
      `/enrollments/${enrollmentId}/activities/${activityId}/response`,
      {
        method: "PUT",
        json: payload,
        signal,
      },
    );
  },
};
