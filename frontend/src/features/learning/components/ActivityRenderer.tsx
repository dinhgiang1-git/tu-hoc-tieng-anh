import { useEffect, useRef, useState } from "react";
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Circle,
  Clock,
  ExternalLink,
  HelpCircle,
  Loader2,
  RefreshCw,
  RotateCcw,
  Save,
  Send,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api-error";
import type {
  ActivityWorkspaceDto,
  ResourceDto,
} from "../api/learning-workspace.api";
import { learningWorkspaceApi } from "../api/learning-workspace.api";
import { learningProgressApi } from "../api/learning-progress.api";
import { QuizActivityView } from "./QuizActivityView";

interface ActivityRendererProps {
  activity: ActivityWorkspaceDto;
  index: number;
  enrollmentId: string;
  enrollmentVersion: number;
  onVersionUpdate: (newVersion: number) => void | Promise<void>;
  onRecoveryRequired?: () => void;
  onActivityUpdated: (updatedActivity: Partial<ActivityWorkspaceDto>) => void;
  onRefreshWorkspace?: () => Promise<void>;
  isReadOnly?: boolean;
  isMutating?: boolean;
  onMutatingChange?: (isMutating: boolean) => void;
  onDraftChange?: (hasDraft: boolean) => void;
}

export function ActivityRenderer({
  activity,
  index,
  enrollmentId,
  enrollmentVersion,
  onVersionUpdate,
  onActivityUpdated,
  onRefreshWorkspace,
  onRecoveryRequired,
  isReadOnly = false,
  isMutating = false,
  onMutatingChange,
  onDraftChange,
}: ActivityRendererProps) {
  const [responseText, setResponseText] = useState<string>(
    activity.userResponse ?? "",
  );
  const [draftStatus, setDraftStatus] = useState<
    "idle" | "dirty" | "saving" | "saved" | "error"
  >("idle");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isConflict, setIsConflict] = useState(false);

  const activeActivityIdRef = useRef(activity.activityId);
  const activeEnrollmentIdRef = useRef(enrollmentId);
  const lastSyncedResponseRef = useRef(activity.userResponse);
  const activityGenRef = useRef(0);

  // Invalidate in-flight callbacks when activity or enrollment changes
  useEffect(() => {
    if (activeActivityIdRef.current !== activity.activityId || activeEnrollmentIdRef.current !== enrollmentId) {
      activityGenRef.current += 1;
      activeEnrollmentIdRef.current = enrollmentId;
      activeActivityIdRef.current = activity.activityId;
      lastSyncedResponseRef.current = activity.userResponse;
      setResponseText(activity.userResponse ?? "");
      setDraftStatus("idle");
      setErrorMessage(null);
      setIsConflict(false);
    }
  }, [activity.activityId, enrollmentId, activity.userResponse]);

  // Background reads must never replace an unsaved local answer, including after errors.
  useEffect(() => {
    if (activity.userResponse !== lastSyncedResponseRef.current) {
      const wasClean = responseText === (lastSyncedResponseRef.current ?? "");
      lastSyncedResponseRef.current = activity.userResponse;
      if (wasClean) setResponseText(activity.userResponse ?? "");
    }
  }, [activity.userResponse, responseText]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      activityGenRef.current += 1;
    };
  }, []);

  const isLocked = isReadOnly || isSubmitting || isMutating || isConflict;
  const isCompleted = activity.status === "COMPLETED";
  const isInProgress = activity.status === "IN_PROGRESS";

  // Unsupported activity types in web workspace (backend rejects manual check for RECORDING and DICTATION)
  const isUnsupportedType =
    activity.type === "RECORDING" || activity.type === "DICTATION";

  // Decouple type and completionPolicy:
  // MANUAL_CHECK -> Check-off button (Reading, Listening, manual Practice)
  // RESPONSE_SUBMIT -> Textarea + Draft + Submit (Reflection, response Practice)
  // MANUAL_CHECK_WITH_RESPONSE -> Both
  // QUIZ -> Quiz card
  const policy =
    activity.completionPolicy ??
    (activity.type === "REFLECTION"
      ? "RESPONSE_SUBMIT"
      : activity.type === "QUIZ"
        ? "QUIZ"
        : "MANUAL_CHECK");

  const isQuiz = activity.type === "QUIZ";

  const hasResponse =
    !isUnsupportedType &&
    !isQuiz &&
    (policy === "RESPONSE_SUBMIT" || policy === "MANUAL_CHECK_WITH_RESPONSE");

  const isManualCheck =
    !isUnsupportedType &&
    !isQuiz &&
    (policy === "MANUAL_CHECK" || policy === "MANUAL_CHECK_WITH_RESPONSE");

  // Handle manual complete/undo toggle
  const handleToggleManualComplete = async (targetStatus: "COMPLETED" | "NOT_STARTED") => {
    if (isLocked) return;
    setIsSubmitting(true);
    onMutatingChange?.(true);
    setErrorMessage(null);
    setIsConflict(false);
    const reqGen = activityGenRef.current;

    try {
      const res = await learningProgressApi.updateProgress(
        enrollmentId,
        activity.activityId,
        {
          status: targetStatus,
          expectedVersion: enrollmentVersion,
        },
      );

      if (activityGenRef.current !== reqGen) return;

      onActivityUpdated({ status: res.status });
      await onVersionUpdate(res.enrollmentVersion);
    } catch (err) {
      if (activityGenRef.current !== reqGen) return;
      onRecoveryRequired?.();

      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          if (err.code === "VERSION_CONFLICT") {
            setErrorMessage(
              "Phiên bản tiến độ đã cũ trên máy tính của bạn. Vui lòng đồng bộ lại để tiếp tục.",
            );
          } else if (err.code === "STATE_CONFLICT") {
            setErrorMessage(
              "Trạng thái học tập đã thay đổi trên máy chủ (ví dụ: đã tạm dừng hoặc bài học đã kết thúc).",
            );
          } else {
            setErrorMessage(
              "Phiên học đã thay đổi trên máy chủ. Vui lòng thử lại sau khi đồng bộ.",
            );
          }
        } else {
          setErrorMessage(err.message || "Không thể cập nhật trạng thái.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng.");
      }
    } finally {
      if (activityGenRef.current === reqGen) {
        setIsSubmitting(false);
        onMutatingChange?.(false);
      }
    }
  };

  // Handle Response Draft saving (guarantees entered text is NEVER wiped)
  const handleSaveDraft = async () => {
    if (isLocked || draftStatus === "saving") return;
    setDraftStatus("saving");
    onMutatingChange?.(true);
    setErrorMessage(null);
    setIsConflict(false);
    const reqGen = activityGenRef.current;
    const draftTextToSave = responseText;

    try {
      const res = await learningWorkspaceApi.saveResponseDraft(
        enrollmentId,
        activity.activityId,
        {
          responseText: draftTextToSave,
          expectedVersion: enrollmentVersion,
        },
      );

      if (activityGenRef.current !== reqGen) return;

      lastSyncedResponseRef.current = res.responseText;
      onActivityUpdated({
        userResponse: res.responseText,
        status:
          activity.status === "NOT_STARTED" && res.responseText.trim().length > 0
            ? "IN_PROGRESS"
            : activity.status,
      });
      setDraftStatus("saved");
      onDraftChange?.(false);
      await onVersionUpdate(res.enrollmentVersion);
    } catch (err) {
      if (activityGenRef.current !== reqGen) return;
      onRecoveryRequired?.();

      setDraftStatus("error");
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          if (err.code === "VERSION_CONFLICT") {
            setErrorMessage(
              "Phiên bản tiến độ đã cũ trên máy tính của bạn. Câu trả lời của bạn đã được giữ lại an toàn. Vui lòng đồng bộ lại tiến độ.",
            );
          } else if (err.code === "STATE_CONFLICT") {
            setErrorMessage(
              "Trạng thái học tập đã thay đổi trên máy chủ. Câu trả lời của bạn đã được giữ lại an toàn.",
            );
          } else {
            setErrorMessage(
              "Phiên học đã thay đổi trên máy chủ. Nội dung nháp của bạn đã được giữ lại an toàn.",
            );
          }
        } else {
          setErrorMessage(err.message || "Không thể lưu bản nháp.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng khi lưu bản nháp.");
      }
    } finally {
      if (activityGenRef.current === reqGen) {
        onMutatingChange?.(false);
      }
    }
  };

  // Handle final submission of response (preserves original whitespace)
  const handleSubmitResponse = async () => {
    if (isLocked) return;
    if (!responseText.trim()) {
      setErrorMessage("Vui lòng nhập câu trả lời trước khi hoàn thành.");
      return;
    }
    if (responseText.length > 5000) {
      setErrorMessage("Câu trả lời không được vượt quá 5000 ký tự.");
      return;
    }

    setIsSubmitting(true);
    onMutatingChange?.(true);
    setErrorMessage(null);
    setIsConflict(false);
    const reqGen = activityGenRef.current;
    const textToSubmit = responseText;

    try {
      const res = await learningProgressApi.submitResponse(
        enrollmentId,
        activity.activityId,
        {
          responseText: textToSubmit,
          expectedVersion: enrollmentVersion,
        },
      );

      if (activityGenRef.current !== reqGen) return;

      lastSyncedResponseRef.current = textToSubmit;
      onActivityUpdated({
        status: res.status,
        userResponse: textToSubmit,
      });
      setDraftStatus("saved");
      onDraftChange?.(false);
      await onVersionUpdate(res.enrollmentVersion);
    } catch (err) {
      if (activityGenRef.current !== reqGen) return;
      onRecoveryRequired?.();

      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          if (err.code === "VERSION_CONFLICT") {
            setErrorMessage(
              "Phiên bản tiến độ đã cũ trên máy tính của bạn. Câu trả lời của bạn đã được giữ lại an toàn. Vui lòng đồng bộ lại tiến độ.",
            );
          } else if (err.code === "STATE_CONFLICT") {
            setErrorMessage(
              "Trạng thái học tập đã thay đổi trên máy chủ. Câu trả lời của bạn đã được giữ lại.",
            );
          } else {
            setErrorMessage(
              "Phiên học đã thay đổi trên máy chủ. Câu trả lời của bạn đã được giữ lại.",
            );
          }
        } else {
          setErrorMessage(err.message || "Không thể nộp câu trả lời.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng.");
      }
    } finally {
      if (activityGenRef.current === reqGen) {
        setIsSubmitting(false);
        onMutatingChange?.(false);
      }
    }
  };

  // Handle undo for completed response activities
  const handleUndoResponse = async () => {
    if (isLocked) return;
    setIsSubmitting(true);
    onMutatingChange?.(true);
    setErrorMessage(null);
    setIsConflict(false);
    const reqGen = activityGenRef.current;

    try {
      const res = await learningProgressApi.updateProgress(
        enrollmentId,
        activity.activityId,
        {
          status: "NOT_STARTED",
          expectedVersion: enrollmentVersion,
        },
      );

      if (activityGenRef.current !== reqGen) return;

      onActivityUpdated({ status: res.status });
      await onVersionUpdate(res.enrollmentVersion);
    } catch (err) {
      if (activityGenRef.current !== reqGen) return;
      onRecoveryRequired?.();

      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          setErrorMessage(
            err.code === "VERSION_CONFLICT"
              ? "Phiên bản tiến độ đã cũ. Vui lòng đồng bộ lại tiến độ."
              : "Phiên học đã thay đổi trên máy chủ. Vui lòng thử lại.",
          );
        } else {
          setErrorMessage(err.message || "Không thể hoàn tác nhiệm vụ.");
        }
      } else {
        setErrorMessage("Lỗi kết nối mạng.");
      }
    } finally {
      if (activityGenRef.current === reqGen) {
        setIsSubmitting(false);
        onMutatingChange?.(false);
      }
    }
  };

  // Handle recovery refresh
  const handleRefresh = async () => {
    if (!onRefreshWorkspace) return;
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await onRefreshWorkspace();
      setIsConflict(false);
    } catch {
      setErrorMessage("Không thể đồng bộ lại dữ liệu từ máy chủ. Vui lòng thử lại.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      data-testid={`activity-card-${activity.activityId}`}
      className={`rounded-xl border p-5 transition shadow-sm ${
        isCompleted
          ? "border-primary/30 bg-primary/5"
          : "border-border bg-card hover:border-primary/20"
      }`}
    >
      {/* Activity Header */}
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

            {/* Status Indicator */}
            {isCompleted && (
              <span
                data-testid={`activity-status-${activity.activityId}`}
                className="inline-flex items-center gap-1 text-xs font-semibold text-primary"
              >
                <CheckCircle2 className="h-3.5 w-3.5" />
                Đã hoàn thành
              </span>
            )}
            {isInProgress && (
              <span
                data-testid={`activity-status-${activity.activityId}`}
                className="inline-flex items-center gap-1 text-xs font-medium text-amber-600 dark:text-amber-400"
              >
                <Clock className="h-3.5 w-3.5" />
                Đang làm
              </span>
            )}
            {!isCompleted && !isInProgress && (
              <span
                data-testid={`activity-status-${activity.activityId}`}
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

        {/* Manual completion toggle for manual check activities */}
        {isManualCheck && !isQuiz && (
          <div className="flex items-center gap-2">
            {isCompleted ? (
              <Button
                variant="outline"
                size="sm"
                data-testid={`undo-button-${activity.activityId}`}
                disabled={isLocked}
                onClick={() => handleToggleManualComplete("NOT_STARTED")}
                className="text-xs flex items-center gap-1.5"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Hoàn tác
              </Button>
            ) : (
              <Button
                variant="default"
                size="sm"
                data-testid={`complete-button-${activity.activityId}`}
                disabled={isLocked}
                onClick={() => handleToggleManualComplete("COMPLETED")}
                className="text-xs flex items-center gap-1.5"
              >
                <CheckCircle2 className="h-3.5 w-3.5" />
                Hoàn thành
              </Button>
            )}
          </div>
        )}

        {/* For pure RESPONSE_SUBMIT activities that are completed, show undo in header */}
        {hasResponse && !isManualCheck && isCompleted && (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              data-testid={`undo-button-${activity.activityId}`}
              disabled={isLocked}
              onClick={handleUndoResponse}
              className="text-xs flex items-center gap-1.5"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Hoàn tác
            </Button>
          </div>
        )}

        {/* Notice for unsupported activity types */}
        {isUnsupportedType && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted/60 px-3 py-1.5 rounded-lg border border-border">
            <span>Hoạt động dạng {activity.type} chưa hỗ trợ thao tác trên web.</span>
          </div>
        )}

        {isQuiz && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted px-3 py-1.5 rounded-lg">
            <HelpCircle className="h-4 w-4" />
            Bài kiểm tra trắc nghiệm
          </div>
        )}
      </div>

      {/* Instructions */}
      {activity.instructions && (
        <div className="mt-3 text-sm text-foreground/90 leading-relaxed bg-muted/30 p-3 rounded-lg border border-border">
          {activity.instructions}
        </div>
      )}

      {/* Stable Resources List with Attributions */}
      {activity.resources && activity.resources.length > 0 && (
        <div className="mt-4 space-y-2">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
            Tài liệu & Học liệu đính kèm ({activity.resources.length})
          </span>
          <div className="grid gap-2 sm:grid-cols-2">
            {activity.resources.map((res: ResourceDto) => (
              <a
                key={res.id}
                href={/^https?:\/\//i.test(res.url) ? res.url : undefined}
                aria-disabled={!/^https?:\/\//i.test(res.url)}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-start justify-between gap-2 p-2.5 rounded-lg border border-border bg-background hover:bg-muted/50 hover:border-primary/40 transition text-xs group"
              >
                <div className="space-y-0.5 flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 font-medium text-foreground group-hover:text-primary transition truncate">
                    <BookOpen className="h-3.5 w-3.5 shrink-0 text-primary" />
                    <span className="truncate">{res.title}</span>
                  </div>
                  {res.attribution && (
                    <span className="text-[11px] text-muted-foreground block truncate">
                      Nguồn: {res.attribution}
                    </span>
                  )}
                </div>
                <ExternalLink className="h-3.5 w-3.5 shrink-0 text-muted-foreground group-hover:text-primary transition mt-0.5" />
              </a>
            ))}
          </div>
        </div>
      )}

      {/* Error alert inside activity card with recovery action */}
      {errorMessage && (
        <div className="mt-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          {isConflict && onRefreshWorkspace && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleRefresh}
              disabled={isSubmitting}
              className="h-6 text-[11px] px-2 text-destructive border-destructive/30 hover:bg-destructive/10 shrink-0 self-start sm:self-auto"
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Đồng bộ tiến độ
            </Button>
          )}
        </div>
      )}

      {/* Response / Reflection Textarea with Draft Save */}
      {hasResponse && (
        <div className="mt-4 pt-4 border-t border-border space-y-3">
          <div className="flex items-center justify-between">
            <label
              htmlFor={`activity-response-${activity.activityId}`}
              className="text-xs font-semibold text-foreground"
            >
              Câu trả lời / Phản tư của bạn:
            </label>

            {/* Draft status indicator */}
            <div className="flex items-center gap-2">
              {draftStatus === "saving" && (
                <span className="inline-flex items-center gap-1 text-[11px] text-muted-foreground animate-pulse">
                  <Loader2 className="h-3 w-3 animate-spin text-primary" />
                  Đang lưu nháp...
                </span>
              )}
              {draftStatus === "saved" && (
                <span className="inline-flex items-center gap-1 text-[11px] text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="h-3 w-3" />
                  Đã lưu nháp
                </span>
              )}
              {draftStatus === "dirty" && (
                <span className="inline-flex items-center gap-1 text-[11px] text-amber-600 dark:text-amber-400">
                  <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                  Bản nháp chưa lưu
                </span>
              )}
            </div>
          </div>

          <textarea
            id={`activity-response-${activity.activityId}`}
            data-testid={`activity-response-input-${activity.activityId}`}
            rows={4}
            disabled={isLocked}
            value={responseText}
            onChange={(e) => {
              const nextVal = e.target.value;
              setResponseText(nextVal);
              const isDirty = nextVal !== (lastSyncedResponseRef.current ?? "");
              setDraftStatus(isDirty ? "dirty" : "saved");
              onDraftChange?.(isDirty);
            }}
            placeholder="Nhập câu trả lời hoặc suy nghĩ của bạn về nhiệm vụ này..."
            className="w-full rounded-lg border border-input bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50 resize-y min-h-[90px]"
          />

          <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
            <span>{responseText.length}/5000 ký tự</span>

            <div className="flex items-center gap-2">
              {/* Draft save button */}
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={isLocked || draftStatus === "saving"}
                onClick={handleSaveDraft}
                className="h-8 text-xs flex items-center gap-1.5"
                title="Lưu câu trả lời vào bản nháp"
              >
                <Save className="h-3.5 w-3.5" />
                Lưu nháp
              </Button>

              {/* Submit & complete button */}
              <Button
                type="button"
                variant="default"
                size="sm"
                data-testid={`activity-submit-button-${activity.activityId}`}
                disabled={
                  isLocked ||
                  !responseText.trim() ||
                  responseText.length > 5000
                }
                onClick={handleSubmitResponse}
                className="h-8 text-xs flex items-center gap-1.5"
              >
                <Send className="h-3.5 w-3.5" />
                {isCompleted ? "Cập nhật câu trả lời" : "Nộp & Hoàn thành"}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Quiz Assessment View */}
      {isQuiz && (
        <div className="mt-4 pt-4 border-t border-border">
          <QuizActivityView
            activity={activity}
            enrollmentId={enrollmentId}
            enrollmentVersion={enrollmentVersion}
            onVersionUpdate={onVersionUpdate}
            onActivityUpdated={onActivityUpdated}
            onRefreshWorkspace={onRefreshWorkspace}
            isReadOnly={isReadOnly}
          />
        </div>
      )}
    </div>
  );
}
