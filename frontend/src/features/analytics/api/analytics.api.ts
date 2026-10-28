import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type EnrollmentAnalyticsResponse =
  components["schemas"]["EnrollmentAnalyticsResponse"];
export type StreakStatsDto = components["schemas"]["StreakStatsDto"];
export type QuizSummaryDto = components["schemas"]["QuizSummaryDto"];
export type WeeklyProgressItemDto = components["schemas"]["WeeklyProgressItemDto"];
export type DailyActivityHistoryDto = components["schemas"]["DailyActivityHistoryDto"];
export type DailyEventDto = components["schemas"]["DailyEventDto"];

export const analyticsKeys = {
  all: ["analytics"] as const,
  detail: (enrollmentId: string) => [...analyticsKeys.all, "detail", enrollmentId] as const,
};

export const analyticsApi = {
  async getEnrollmentAnalytics(
    enrollmentId: string,
    signal?: AbortSignal,
  ): Promise<EnrollmentAnalyticsResponse> {
    return apiRequest<EnrollmentAnalyticsResponse>(`/enrollments/${enrollmentId}/analytics`, {
      method: "GET",
      signal,
    });
  },
};
