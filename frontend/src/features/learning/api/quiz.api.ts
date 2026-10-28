import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type QuizDetailResponse = components["schemas"]["QuizDetailResponse"];
export type QuizQuestionDto = components["schemas"]["QuizQuestionDto"];
export type QuizOptionDto = components["schemas"]["QuizOptionDto"];
export type QuizAttemptSummaryDto = components["schemas"]["QuizAttemptSummaryDto"];
export type QuizResultResponse = components["schemas"]["QuizResultResponse"];
export type QuestionResultDto = components["schemas"]["QuestionResultDto"];
export type QuizSubmitRequest = components["schemas"]["QuizSubmitRequest"];
export type QuizAnswerItemRequest = components["schemas"]["QuizAnswerItemRequest"];

export const quizKeys = {
  all: ["quiz"] as const,
  detail: (enrollmentId: string, activityId: string) =>
    [...quizKeys.all, enrollmentId, "activities", activityId] as const,
};

export const quizApi = {
  async getQuiz(
    enrollmentId: string,
    activityId: string,
    signal?: AbortSignal,
  ): Promise<QuizDetailResponse> {
    return apiRequest<QuizDetailResponse>(
      `/enrollments/${enrollmentId}/activities/${activityId}/quiz`,
      {
        method: "GET",
        signal,
      },
    );
  },

  async submitQuiz(
    enrollmentId: string,
    activityId: string,
    payload: QuizSubmitRequest,
    idempotencyKey?: string,
  ): Promise<QuizResultResponse> {
    return apiRequest<QuizResultResponse>(
      `/enrollments/${enrollmentId}/activities/${activityId}/quiz/submit`,
      {
        method: "POST",
        json: payload,
        idempotencyKey,
      },
    );
  },
};
