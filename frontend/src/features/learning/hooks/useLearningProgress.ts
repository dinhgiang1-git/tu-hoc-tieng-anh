import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  learningProgressApi,
  learningProgressKeys,
  type LessonActivitiesResponse,
  type ProgressSummaryResponse,
  type UpdateProgressRequest,
  type SubmitResponseRequest,
} from "../api/learning-progress.api";
import { todayKeys } from "../api/today.api";
import { enrollmentKeys } from "@/features/enrollment/api/enrollment.api";

export function useLessonActivities(enrollmentId: string, lessonId: string) {
  return useQuery<LessonActivitiesResponse>({
    queryKey: learningProgressKeys.activities(enrollmentId, lessonId),
    queryFn: ({ signal }) =>
      learningProgressApi.getLessonActivities(enrollmentId, lessonId, signal),
    enabled: Boolean(enrollmentId && lessonId),
  });
}

export function useEnrollmentSummary(enrollmentId: string) {
  return useQuery<ProgressSummaryResponse>({
    queryKey: learningProgressKeys.summary(enrollmentId),
    queryFn: ({ signal }) => learningProgressApi.getSummary(enrollmentId, signal),
    enabled: Boolean(enrollmentId),
  });
}

export function useUpdateProgress(enrollmentId: string, lessonId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      activityId,
      payload,
    }: {
      activityId: string;
      payload: UpdateProgressRequest;
    }) => learningProgressApi.updateProgress(enrollmentId, activityId, payload),
    onSuccess: async (data) => {
      // Synchronously update local cache with server confirmed data
      queryClient.setQueryData<LessonActivitiesResponse>(
        learningProgressKeys.activities(enrollmentId, lessonId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            enrollmentVersion: data.enrollmentVersion,
            activities: old.activities.map((a) =>
              a.id === data.activityId
                ? {
                    ...a,
                    status: data.status,
                  }
                : a,
            ),
          };
        },
      );
      queryClient.setQueryData<ProgressSummaryResponse>(
        learningProgressKeys.summary(enrollmentId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            enrollmentVersion: data.enrollmentVersion,
            completedRequiredActivities: data.completedRequiredActivities,
            totalRequiredActivities: data.totalRequiredActivities,
            progressPercent: data.progressPercent,
          };
        },
      );

      // Invalidate queries to reconcile state across pages
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: learningProgressKeys.activities(enrollmentId, lessonId),
        }),
        queryClient.invalidateQueries({
          queryKey: learningProgressKeys.summary(enrollmentId),
        }),
        queryClient.invalidateQueries({
          queryKey: todayKeys.all,
        }),
        queryClient.invalidateQueries({
          queryKey: enrollmentKeys.detail(enrollmentId),
        }),
        queryClient.invalidateQueries({
          queryKey: enrollmentKeys.my(),
        }),
      ]);
    },
  });
}

export function useSubmitReflection(enrollmentId: string, lessonId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      activityId,
      payload,
    }: {
      activityId: string;
      payload: SubmitResponseRequest;
    }) => learningProgressApi.submitResponse(enrollmentId, activityId, payload),
    onSuccess: async (data, variables) => {
      queryClient.setQueryData<LessonActivitiesResponse>(
        learningProgressKeys.activities(enrollmentId, lessonId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            enrollmentVersion: data.enrollmentVersion,
            activities: old.activities.map((a) =>
              a.id === data.activityId
                ? {
                    ...a,
                    status: data.status,
                    responseText: variables.payload.responseText,
                  }
                : a,
            ),
          };
        },
      );
      queryClient.setQueryData<ProgressSummaryResponse>(
        learningProgressKeys.summary(enrollmentId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            enrollmentVersion: data.enrollmentVersion,
            completedRequiredActivities: data.completedRequiredActivities,
            totalRequiredActivities: data.totalRequiredActivities,
            progressPercent: data.progressPercent,
          };
        },
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: learningProgressKeys.activities(enrollmentId, lessonId),
        }),
        queryClient.invalidateQueries({
          queryKey: learningProgressKeys.summary(enrollmentId),
        }),
        queryClient.invalidateQueries({
          queryKey: todayKeys.all,
        }),
        queryClient.invalidateQueries({
          queryKey: enrollmentKeys.detail(enrollmentId),
        }),
        queryClient.invalidateQueries({
          queryKey: enrollmentKeys.my(),
        }),
      ]);
    },
  });
}
