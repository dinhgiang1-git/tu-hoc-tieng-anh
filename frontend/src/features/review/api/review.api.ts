import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type ReviewSummaryResponse = components["schemas"]["ReviewSummaryResponse"];
export type BookmarkResponse = components["schemas"]["BookmarkResponse"];
export type MistakeItemResponse = components["schemas"]["MistakeItemResponse"];
export type CreateBookmarkRequest = components["schemas"]["CreateBookmarkRequest"];
export type CheckQuestionRequest = components["schemas"]["CheckQuestionRequest"];
export type QuestionCheckResponse = components["schemas"]["QuestionCheckResponse"];

export const reviewKeys = {
  all: ["review"] as const,
  summary: (enrollmentId: string) => [...reviewKeys.all, "summary", enrollmentId] as const,
};

export const reviewApi = {
  async getReviewSummary(
    enrollmentId: string,
    signal?: AbortSignal,
  ): Promise<ReviewSummaryResponse> {
    return apiRequest<ReviewSummaryResponse>(`/enrollments/${enrollmentId}/review`, {
      method: "GET",
      signal,
    });
  },

  async createBookmark(payload: CreateBookmarkRequest): Promise<BookmarkResponse> {
    return apiRequest<BookmarkResponse>("/review-bookmarks", {
      method: "POST",
      json: payload,
    });
  },

  async deleteBookmark(id: string): Promise<void> {
    return apiRequest<void>(`/review-bookmarks/${id}`, {
      method: "DELETE",
    });
  },

  async checkMistakeQuestion(
    enrollmentId: string,
    questionId: string,
    payload: CheckQuestionRequest,
  ): Promise<QuestionCheckResponse> {
    return apiRequest<QuestionCheckResponse>(
      `/enrollments/${enrollmentId}/review/questions/${questionId}/checks`,
      {
        method: "POST",
        json: payload,
      },
    );
  },
};
