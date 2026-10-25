import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type TodayResponse = components["schemas"]["TodayResponse"];
export type PrimaryActionDto = components["schemas"]["PrimaryActionDto"];
export type OverdueLessonDto = components["schemas"]["OverdueLessonDto"];
export type ProgressSummaryResponse = components["schemas"]["ProgressSummaryResponse"];

export const todayKeys = {
  all: ["today"] as const,
  dashboard: (enrollmentId?: string | null) =>
    ["today", enrollmentId ?? "focused"] as const,
};

export const todayApi = {
  async getToday(
    enrollmentId?: string | null,
    signal?: AbortSignal,
  ): Promise<TodayResponse> {
    const query = enrollmentId
      ? `?enrollmentId=${encodeURIComponent(enrollmentId)}`
      : "";
    return apiRequest<TodayResponse>(`/me/today${query}`, {
      method: "GET",
      signal,
    });
  },
};
