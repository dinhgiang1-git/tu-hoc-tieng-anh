import { useState } from "react";
import { Link } from "react-router-dom";
import {
  CheckCircle2,
  Circle,
  Clock,
  RotateCcw,
  Send,
  AlertCircle,
  HelpCircle,
  Sparkles,
  ArrowRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/ErrorState";
import { ApiError } from "@/lib/api/api-error";
import {
  useLessonActivities,
  useUpdateProgress,
  useSubmitReflection,
} from "../hooks/useLearningProgress";
import type { LessonActivityItemDto } from "../api/learning-progress.api";

interface ActivityProgressPanelProps {
  enrollmentId: string;
  lessonId: string;
  isEnrollmentReadOnly?: boolean;
}

export function ActivityProgressPanel({
  enrollmentId,
  lessonId,
  isEnrollmentReadOnly = false,
}: ActivityProgressPanelProps) {
  const activitiesQuery = useLessonActivities(enrollmentId, lessonId);
  const updateProgressMutation = useUpdateProgress(enrollmentId, lessonId);
  const submitReflectionMutation = useSubmitReflection(enrollmentId, lessonId);

  const [draftEdits, setDraftEdits] = useState<Record<string, string>>({});
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isConflict, setIsConflict] = useState(false);

  const isSyncing =
    updateProgressMutation.isPending ||
    submitReflectionMutation.isPending ||
    activitiesQuery.isFetching;

  const isActionDisabled =
    isSyncing || isEnrollmentReadOnly || isConflict || activitiesQuery.isError;

  if (activitiesQuery.isPending) {
    return (
      <div className="space-y-4" role="status" aria-label="Đang tải danh sách nhiệm vụ">
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-28 w-full rounded-xl" />
        <Skeleton className="h-28 w-full rounded-xl" />
      </div>
    );
  }

  if (activitiesQuery.isError && !activitiesQuery.data) {
    return (
      <ErrorState
        message="Không thể tải danh sách nhiệm vụ của buổi học. Vui lòng thử lại."
        onRetry={() => void activitiesQuery.refetch()}
      />
    );
  }

  const { enrollmentVersion, activities } = activitiesQuery.data;

  const handleUpdateProgress = async (
    activity: LessonActivityItemDto,
    targetStatus: "COMPLETED" | "NOT_STARTED" | "IN_PROGRESS",
  ) => {
    setErrorMessage(null);
    setIsConflict(false);
    try {
      await updateProgressMutation.mutateAsync({
        activityId: activity.id,
        payload: {
          status: targetStatus,
          expectedVersion: enrollmentVersion,
        },
      });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          if (err.code === "VERSION_CONFLICT") {
            setIsConflict(true);
            setErrorMessage(
              "Dữ liệu đã thay đổi trên hệ thống. Câu trả lời của bạn vẫn được giữ nguyên. Vui lòng tải lại trạng thái rồi thử lại.",
            );
          } else {
            setIsConflict(false);
            setErrorMessage(
              err.problem?.detail || err.message || "Xung đột trạng thái lộ trình học.",
            );
          }
        } else if (err.code === "RESPONSE_REQUIRED") {
          setErrorMessage("Nhiệm vụ yêu cầu nhập câu trả lời trước khi hoàn thành.");
        } else {
          setErrorMessage(err.message || "Đã xảy ra lỗi khi cập nhật tiến độ.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng. Vui lòng thử lại sau.");
      }
    }
  };

  const handleSubmitResponse = async (activity: LessonActivityItemDto) => {
    const text =
      (draftEdits[activity.id] !== undefined
        ? draftEdits[activity.id]
        : activity.responseText) || "";
    const trimmed = text.trim();
    if (!trimmed) {
      setErrorMessage("Vui lòng nhập câu trả lời trước khi nộp.");
      return;
    }
    if (text.length > 5000) {
      setErrorMessage("Câu trả lời không được vượt quá 5000 ký tự.");
      return;
    }

    setErrorMessage(null);
    setIsConflict(false);
    try {
      await submitReflectionMutation.mutateAsync({
        activityId: activity.id,
        payload: {
          responseText: text,
          expectedVersion: enrollmentVersion,
        },
      });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          if (err.code === "VERSION_CONFLICT") {
            setIsConflict(true);
            setErrorMessage(
              "Dữ liệu đã thay đổi trên hệ thống. Câu trả lời của bạn vẫn được giữ nguyên. Vui lòng tải lại trạng thái rồi thử lại.",
            );
          } else {
            setIsConflict(false);
            setErrorMessage(
              err.problem?.detail || err.message || "Xung đột trạng thái lộ trình học.",
            );
          }
        } else {
          setErrorMessage(err.message || "Đã xảy ra lỗi khi nộp bài.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng. Vui lòng thử lại sau.");
      }
    }
  };

  const handleRefetchConflict = async () => {
    const result = await activitiesQuery.refetch();
    if (result.isSuccess) {
      setIsConflict(false);
      setErrorMessage(null);
    }
  };

  // Determine if all required activities of the lesson are completed
  const requiredActivities = activities.filter((a) => a.required);
  const isLessonComplete =
    requiredActivities.length > 0 &&
    requiredActivities.every((a) => a.status === "COMPLETED");

  return (
    <div className="space-y-6" data-testid="activity-progress-panel">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-foreground">
          Nhiệm vụ học tập ({activities.length})
        </h2>
        <span className="text-xs text-muted-foreground">
          Phiên bản lộ trình: v{enrollmentVersion}
        </span>
      </div>

      {/* Error / Conflict Alert */}
      {errorMessage && (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive"
        >
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="flex-1 space-y-2">
            <p>{errorMessage}</p>
            {isConflict && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleRefetchConflict}
                disabled={activitiesQuery.isFetching}
                className="mt-2 text-xs"
              >
                Tải lại trạng thái mới nhất
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Background Refetch Error Banner */}
      {activitiesQuery.isError && activitiesQuery.data && (
        <div
          role="alert"
          className="flex items-center justify-between gap-3 rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive"
        >
          <div className="flex items-center gap-2">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <span>Không thể đồng bộ dữ liệu mới nhất từ máy chủ.</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefetchConflict}
            disabled={activitiesQuery.isFetching}
            className="text-xs"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Lesson Complete Banner */}
      {isLessonComplete && (
        <div
          data-testid="lesson-complete-banner"
          className="flex flex-col sm:flex-row items-center justify-between gap-4 rounded-xl border border-primary/30 bg-primary/10 p-5 text-foreground shadow-sm"
        >
          <div className="flex items-center gap-3">
            <Sparkles className="h-6 w-6 text-primary shrink-0" />
            <div>
              <h3 className="font-semibold text-primary">
                Hoàn thành buổi học!
              </h3>
              <p className="text-xs text-muted-foreground">
                Bạn đã hoàn thành tất cả nhiệm vụ bắt buộc của buổi này.
              </p>
            </div>
          </div>
          <Link
            to="/today"
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition"
          >
            Về trang Hôm nay
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      )}

      {activities.length === 0 ? (
        <div className="rounded-xl border border-border bg-card p-6 text-center text-sm text-muted-foreground">
          Buổi học này chưa có nhiệm vụ nào.
        </div>
      ) : (
        <div className="space-y-4">
          {activities.map((activity, index) => {
            const isCompleted = activity.status === "COMPLETED";
            const isInProgress = activity.status === "IN_PROGRESS";
            const isReflectionOrResponse =
              activity.type === "REFLECTION" ||
              activity.completionPolicy === "RESPONSE_SUBMIT" ||
              activity.completionPolicy === "MANUAL_CHECK_WITH_RESPONSE";
            const isQuiz = activity.type === "QUIZ";
            const currentText =
              (draftEdits[activity.id] !== undefined
                ? draftEdits[activity.id]
                : activity.responseText) || "";

            return (
              <div
                key={activity.id}
                data-testid={`activity-item-${activity.id}`}
                className={`rounded-xl border p-5 transition shadow-sm ${
                  isCompleted
                    ? "border-primary/30 bg-primary/5"
                    : "border-border bg-card hover:border-primary/20"
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1.5 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-muted-foreground">
                        #{index + 1}
                      </span>
                      <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium text-foreground">
                        {activity.type}
                      </span>
                      {activity.required ? (
                        <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                          Bắt buộc
                        </span>
                      ) : (
                        <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                          Tự chọn
                        </span>
                      )}

                      {/* Status Badge */}
                      {isCompleted && (
                        <span
                          data-testid={`activity-status-${activity.id}`}
                          className="inline-flex items-center gap-1 text-xs font-semibold text-primary"
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Đã hoàn thành
                        </span>
                      )}
                      {isInProgress && (
                        <span
                          data-testid={`activity-status-${activity.id}`}
                          className="inline-flex items-center gap-1 text-xs font-medium text-amber-600 dark:text-amber-400"
                        >
                          <Clock className="h-3.5 w-3.5" />
                          Đang học
                        </span>
                      )}
                      {!isCompleted && !isInProgress && (
                        <span
                          data-testid={`activity-status-${activity.id}`}
                          className="inline-flex items-center gap-1 text-xs text-muted-foreground"
                        >
                          <Circle className="h-3.5 w-3.5" />
                          Chưa bắt đầu
                        </span>
                      )}
                    </div>

                    <h3 className="text-base font-semibold text-foreground">
                      {activity.title}
                    </h3>
                  </div>

                  {/* Actions for non-reflection / manual check activities */}
                  {!isReflectionOrResponse && !isQuiz && (
                    <div className="flex items-center gap-2">
                      {isCompleted ? (
                        <Button
                          variant="outline"
                          size="sm"
                          data-testid={`undo-button-${activity.id}`}
                          disabled={isActionDisabled}
                          onClick={() => handleUpdateProgress(activity, "NOT_STARTED")}
                          className="text-xs flex items-center gap-1.5"
                        >
                          <RotateCcw className="h-3.5 w-3.5" />
                          Hoàn tác
                        </Button>
                      ) : (
                        <Button
                          variant="default"
                          size="sm"
                          data-testid={`complete-button-${activity.id}`}
                          disabled={isActionDisabled}
                          onClick={() => handleUpdateProgress(activity, "COMPLETED")}
                          className="text-xs flex items-center gap-1.5"
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Hoàn thành
                        </Button>
                      )}
                    </div>
                  )}

                  {/* Quiz notice */}
                  {isQuiz && (
                    <div className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted px-3 py-1.5 rounded-lg">
                      <HelpCircle className="h-4 w-4" />
                      Bài kiểm tra (mở ở G5)
                    </div>
                  )}
                </div>

                {/* Reflection / Text Submission Form */}
                {isReflectionOrResponse && (
                  <div className="mt-4 pt-4 border-t border-border space-y-3">
                    <label
                      htmlFor={`textarea-${activity.id}`}
                      className="block text-xs font-semibold text-foreground"
                    >
                      Câu trả lời / Phản tư của bạn:
                    </label>
                    <textarea
                      id={`textarea-${activity.id}`}
                      data-testid={`reflection-input-${activity.id}`}
                      rows={3}
                      disabled={isActionDisabled}
                      value={currentText}
                      onChange={(e) =>
                        setDraftEdits((prev) => ({
                          ...prev,
                          [activity.id]: e.target.value,
                        }))
                      }
                      placeholder="Nhập câu trả lời hoặc suy nghĩ của bạn về nhiệm vụ này..."
                      className="w-full rounded-lg border border-input bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                    />
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>{currentText.length}/5000 ký tự</span>
                      <div className="flex items-center gap-2">
                        {isCompleted && (
                          <Button
                            variant="outline"
                            size="sm"
                            data-testid={`undo-button-${activity.id}`}
                            disabled={isActionDisabled}
                            onClick={() => handleUpdateProgress(activity, "IN_PROGRESS")}
                            className="text-xs flex items-center gap-1"
                          >
                            <RotateCcw className="h-3.5 w-3.5" />
                            Hoàn tác
                          </Button>
                        )}
                        <Button
                          variant="default"
                          size="sm"
                          data-testid={`submit-button-${activity.id}`}
                          disabled={
                            isActionDisabled ||
                            !currentText.trim() ||
                            currentText.length > 5000
                          }
                          onClick={() => handleSubmitResponse(activity)}
                          className="text-xs flex items-center gap-1.5"
                        >
                          <Send className="h-3.5 w-3.5" />
                          {isCompleted ? "Cập nhật câu trả lời" : "Nộp & Hoàn thành"}
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
