import { useEffect, useRef, useState } from "react";
import { useParams, Link, useNavigate, useBlocker } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Clock,
  FileText,
  Loader2,
  Maximize2,
  Minimize2,
  Save,
  Sparkles,
} from "lucide-react";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { buttonVariants } from "@/components/ui/button-variants";
import { ApiError } from "@/lib/api/api-error";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { learningProgressKeys } from "@/features/learning/api/learning-progress.api";
import { todayKeys } from "@/features/learning/api/today.api";
import {
  learningWorkspaceApi,
  learningWorkspaceKeys,
  type ActivityWorkspaceDto,
} from "../api/learning-workspace.api";
import { notesApi, notesKeys, type NoteResponse } from "@/features/notes/api/notes.api";
import { MarkdownContent } from "../components/MarkdownContent";
import { ActivityRenderer } from "../components/ActivityRenderer";
import { NoteEditor } from "@/features/notes/components/NoteEditor";

export function LessonWorkspacePage() {
  const { enrollmentId = "", lessonId = "" } = useParams();
  return <LessonWorkspaceContent key={`${enrollmentId}/${lessonId}`} />;
}

function LessonWorkspaceContent() {
  const { enrollmentId = "", lessonId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [focusMode, setFocusMode] = useState(false);
  const [notesOpen, setNotesOpen] = useState(true);
  const [versionOverride, setVersionOverride] = useState<number | null>(null);
  const [activityOverrides, setActivityOverrides] = useState<
    Record<string, Partial<ActivityWorkspaceDto>>
  >({});
  const [isMutating, setIsMutating] = useState(false);
  const [syncError, setSyncError] = useState<string | null>(null);
  const [activityDrafts, setActivityDrafts] = useState<Record<string, boolean>>({});
  const [noteState, setNoteState] = useState<{
    hasUnsavedChanges: boolean;
    isSaving: boolean;
    isConflict: boolean;
  }>({
    hasUnsavedChanges: false,
    isSaving: false,
    isConflict: false,
  });
  const [isFlushingAndLeaving, setIsFlushingAndLeaving] = useState(false);
  const [blockerSaveError, setBlockerSaveError] = useState<string | null>(null);

  const noteSaveRef = useRef<(() => Promise<NoteResponse | null>) | null>(null);
  const hasUnsavedActivityDraft = Object.values(activityDrafts).some(Boolean);

  // 1. Fetch Lesson Workspace details
  const workspaceQuery = useQuery({
    queryKey: learningWorkspaceKeys.lesson(enrollmentId, lessonId),
    queryFn: ({ signal }) =>
      learningWorkspaceApi.getLessonWorkspace(enrollmentId, lessonId, signal),
    enabled: Boolean(enrollmentId && lessonId),
  });

  // 2. Fetch Enrollment details (for read-only check)
  const enrollmentDetailQuery = useQuery({
    queryKey: enrollmentKeys.detail(enrollmentId),
    queryFn: ({ signal }) => enrollmentApi.getDetail(enrollmentId, signal),
    enabled: Boolean(enrollmentId),
  });

  // 3. Fetch Lesson-level Note (size 100 guarantees lesson note is not missed)
  const notesQuery = useQuery({
    queryKey: notesKeys.list({ enrollmentId, lessonId }),
    queryFn: ({ signal }) =>
      notesApi.getLessonNote(enrollmentId, lessonId, signal),
    enabled: Boolean(enrollmentId && lessonId),
  });

  // Keyboard shortcut to toggle or exit focus mode (Escape)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && focusMode) {
        setFocusMode(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [focusMode]);

  // Unified navigation blocker condition: covers notes dirty/saving/conflict and activity drafts
  const hasPendingUnsavedWork =
    noteState.hasUnsavedChanges ||
    noteState.isSaving ||
    hasUnsavedActivityDraft ||
    isMutating;

  // Browser beforeunload protection
  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (hasPendingUnsavedWork) {
        event.preventDefault();
        event.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [hasPendingUnsavedWork]);

  // Unified React Router internal navigation blocker
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      hasPendingUnsavedWork &&
      currentLocation.pathname !== nextLocation.pathname,
  );

  // Handle "Lưu rồi rời" (save and proceed)
  const handleSaveAndProceed = async () => {
    setIsFlushingAndLeaving(true);
    setBlockerSaveError(null);

    try {
      if (noteState.hasUnsavedChanges && noteSaveRef.current) {
        const saved = await noteSaveRef.current();
        if (!saved) {
          setBlockerSaveError(
            "Không thể lưu các thay đổi gần nhất. Vui lòng thử lại hoặc ở lại xử lý xung đột.",
          );
          setIsFlushingAndLeaving(false);
          return;
        }
      }

      // If all saves resolved cleanly, proceed
      if (blocker.state === "blocked") {
        blocker.proceed();
      }
    } catch {
      setBlockerSaveError(
        "Không thể lưu các thay đổi gần nhất. Vui lòng thử lại hoặc chọn 'Rời khỏi không lưu'.",
      );
    } finally {
      setIsFlushingAndLeaving(false);
    }
  };

  const handleBlockerStay = () => {
    setBlockerSaveError(null);
    if (blocker.state === "blocked") {
      blocker.reset();
    }
  };

  const handleBlockerDiscardAndLeave = () => {
    setBlockerSaveError(null);
    if (blocker.state === "blocked") {
      blocker.proceed();
    }
  };

  if ((workspaceQuery.isPending || enrollmentDetailQuery.isPending) && !workspaceQuery.data) {
    return (
      <div className="space-y-4" role="status" aria-label="Đang tải không gian học tập">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (workspaceQuery.isError && !workspaceQuery.data) {
    const error = workspaceQuery.error;
    const isNotFound = error instanceof ApiError && error.isNotFound();
    return (
      <div className="space-y-6">
        <Link
          to="/today"
          className="inline-flex items-center gap-2 text-sm text-primary hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại trang Hôm nay
        </Link>
        <ErrorState
          message={
            isNotFound
              ? "Không tìm thấy buổi học hoặc buổi học không thuộc lộ trình đã đăng ký."
              : "Không thể tải không gian học tập. Vui lòng thử lại sau."
          }
          onRetry={isNotFound ? undefined : () => void workspaceQuery.refetch()}
        />
        <div className="flex justify-center">
          <Link to="/today" className={buttonVariants({ variant: "outline" })}>
            Về trang Hôm nay
          </Link>
        </div>
      </div>
    );
  }

  const workspace = workspaceQuery.data;
  const enrollmentStatus = enrollmentDetailQuery.data?.status;
  const isReadOnly =
    Boolean(syncError) || workspaceQuery.isError || workspaceQuery.isFetching ||
    enrollmentDetailQuery.isFetching || enrollmentDetailQuery.isError ||
    !enrollmentStatus ||
    enrollmentStatus === "PAUSED" ||
    enrollmentStatus === "ABANDONED";

  const enrollmentVersion = Math.max(
    versionOverride ?? 0,
    workspace?.enrollmentVersion ?? 0,
  );

  const activities = (workspace?.activities ?? []).map((act) => {
    const override = activityOverrides[act.activityId];
    return override ? { ...act, ...override } : act;
  });

  // Pick lesson-level note specifically (activityId === null)
  const initialNote =
    notesQuery.data ?? null;

  // Check if all required activities are complete
  const requiredActivities = activities.filter((a) => a.required);
  const isLessonComplete =
    requiredActivities.length > 0 &&
    requiredActivities.every((a) => a.status === "COMPLETED");

  const handleActivityUpdated = (
    activityId: string,
    updated: Partial<ActivityWorkspaceDto>,
  ) => {
    setActivityOverrides((prev) => ({
      ...prev,
      [activityId]: { ...prev[activityId], ...updated },
    }));
  };

  // Keep the whole enrollment locked until its authoritative reads reconcile.
  const handleVersionUpdate = async (newVersion: number) => {
    setIsMutating(true);
    setVersionOverride((prev) => Math.max(prev ?? 0, newVersion));
    try {
      await Promise.all([
        learningWorkspaceKeys.lesson(enrollmentId, lessonId),
        learningProgressKeys.summary(enrollmentId),
        learningProgressKeys.activities(enrollmentId, lessonId),
        todayKeys.all, enrollmentKeys.detail(enrollmentId), enrollmentKeys.my(),
      ].map(queryKey => queryClient.invalidateQueries({ queryKey }, { throwOnError: true })));
      const refreshed = queryClient.getQueryData<{ enrollmentVersion: number }>(
        learningWorkspaceKeys.lesson(enrollmentId, lessonId));
      if (!refreshed || refreshed.enrollmentVersion < newVersion) {
        throw new Error("Workspace version has not caught up");
      }
      setActivityOverrides({});
      setSyncError(null);
    } catch (error) {
      setSyncError("Chưa đồng bộ được tiến độ. Hãy tải lại trạng thái trước khi tiếp tục.");
      throw error;
    } finally {
      setIsMutating(false);
    }
  };

  return (
    <div
      className={
        focusMode
          ? "fixed inset-0 z-50 overflow-y-auto bg-background p-4 md:p-8 lg:p-12"
          : "space-y-6"
      }
      data-testid={focusMode ? "focus-mode-container" : undefined}
    >
      <div className={focusMode ? "mx-auto max-w-5xl space-y-6" : "space-y-6"}>
        {(syncError || workspaceQuery.isError || enrollmentDetailQuery.isError) && (
          <ErrorState message={syncError ?? "Không thể đồng bộ trạng thái bài học."}
            onRetry={() => { void handleVersionUpdate(enrollmentVersion).catch(() => {}); }} />
        )}
        {/* Top action bar: Focus mode & Navigation */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-4">
          <div className="flex items-center gap-2">
            {!focusMode && (
              <Link
                to="/today"
                className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition"
              >
                <ArrowLeft className="h-3.5 w-3.5" />
                Hôm nay
              </Link>
            )}

            {focusMode && (
              <span className="text-xs font-semibold text-primary uppercase tracking-wider">
                Chế độ tập trung
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* Notes toggle button */}
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setNotesOpen((prev) => !prev)}
              className="h-8 text-xs flex items-center gap-1.5"
              aria-label={notesOpen ? "Ẩn ghi chú" : "Hiện ghi chú"}
            >
              <FileText className="h-3.5 w-3.5" />
              <span>Ghi chú</span>
            </Button>

            {/* Focus Mode button */}
            <Button
              type="button"
              variant={focusMode ? "secondary" : "outline"}
              size="sm"
              onClick={() => setFocusMode((prev) => !prev)}
              className="h-8 text-xs flex items-center gap-1.5"
              title={focusMode ? "Thoát chế độ tập trung (Esc)" : "Bật chế độ tập trung"}
            >
              {focusMode ? (
                <>
                  <Minimize2 className="h-3.5 w-3.5" />
                  <span className="hidden sm:inline">Thoát tập trung (Esc)</span>
                </>
              ) : (
                <>
                  <Maximize2 className="h-3.5 w-3.5" />
                  <span className="hidden sm:inline">Chế độ tập trung</span>
                </>
              )}
            </Button>

            {/* Next Lesson navigation */}
            {workspace?.nextLessonId && (
              <Button
                type="button"
                variant="default"
                size="sm"
                onClick={() =>
                  navigate(`/enrollments/${enrollmentId}/lessons/${workspace.nextLessonId}`)
                }
                className="h-8 text-xs flex items-center gap-1"
              >
                <span>Bài tiếp</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
        </div>

        {/* Lesson Title & Meta */}
        <div className="space-y-2">
          <h1 className="text-2xl font-bold tracking-tight text-foreground md:text-3xl">
            {workspace?.title}
          </h1>
          <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              {workspace?.estimatedMinutes} phút
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <BookOpen className="h-3.5 w-3.5" />
              {workspace?.activities.length} nhiệm vụ
            </span>
            <span>•</span>

            {isMutating && (
              <>
                <span>•</span>
                <span className="inline-flex items-center gap-1 text-primary animate-pulse font-medium">
                  <Loader2 className="h-3 w-3 animate-spin" />
                  Đang đồng bộ máy chủ...
                </span>
              </>
            )}
          </div>
        </div>

        {/* Main Grid: Content + Notes Editor */}
        <div className="grid gap-6 lg:grid-cols-12 items-start">
          {/* Left Column: Lesson Content & Activities */}
          <div className={`space-y-6 ${notesOpen ? "lg:col-span-8" : "lg:col-span-12"}`}>
            {/* Lesson Markdown Reading Content Card */}
            <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
              <div className="flex items-center justify-between border-b border-border pb-3">
                <div className="flex items-center gap-2 text-sm font-semibold text-primary">
                  <BookOpen className="h-4 w-4" />
                  <span>Nội dung bài học</span>
                </div>
              </div>

              <MarkdownContent content={workspace?.markdownContent ?? ""} />
            </div>

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
                {workspace?.nextLessonId ? (
                  <Button
                    type="button"
                    onClick={() =>
                      navigate(
                        `/enrollments/${enrollmentId}/lessons/${workspace.nextLessonId}`,
                      )
                    }
                    className="flex items-center gap-1.5 text-xs font-semibold"
                  >
                    Bài học tiếp theo
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                ) : (
                  <Link
                    to="/today"
                    className={buttonVariants({ size: "sm" })}
                  >
                    Về trang Hôm nay
                  </Link>
                )}
              </div>
            )}

            {/* Activities List */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-foreground">
                Nhiệm vụ học tập ({activities.length})
              </h2>

              {activities.map((activity, idx) => (
                <ActivityRenderer
                  key={activity.activityId}
                  activity={activity}
                  index={idx}
                  enrollmentId={enrollmentId}
                  enrollmentVersion={enrollmentVersion}
                  onVersionUpdate={handleVersionUpdate}
                  onActivityUpdated={(updated) =>
                    handleActivityUpdated(activity.activityId, updated)
                  }
                  onRefreshWorkspace={() => handleVersionUpdate(enrollmentVersion)}
                  onRecoveryRequired={() => setSyncError("Cần đồng bộ lại tiến độ trước khi tiếp tục.")}
                  isReadOnly={isReadOnly}
                  isMutating={isMutating}
                  onMutatingChange={setIsMutating}
                  onDraftChange={(hasDraft) =>
                    setActivityDrafts((prev) => ({
                      ...prev,
                      [activity.activityId]: hasDraft,
                    }))
                  }
                />
              ))}
            </div>
          </div>

          {/* Right Column: Note Editor - Always mounted, visually hidden when notesOpen is false */}
          <div
            className={`lg:col-span-4 sticky top-4 space-y-4 ${
              notesOpen ? "" : "hidden"
            }`}
          >
            <NoteEditor
              enrollmentId={enrollmentId}
              lessonId={lessonId}
              initialNote={initialNote}
              isOpen={notesOpen}
              onToggleOpen={() => setNotesOpen((prev) => !prev)}
              isLoading={notesQuery.isLoading}
              isLoadError={notesQuery.isError}
              onRetryLoad={() => void notesQuery.refetch()}
              disableBlocker={true}
              onUnsavedStateChange={setNoteState}
              saveRef={noteSaveRef}
            />
          </div>
        </div>
      </div>

      {/* Unified Navigation Blocker Confirmation Modal */}
      {blocker?.state === "blocked" && (
        <div
          data-testid="workspace-navigation-blocker-modal"
          className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4"
        >
          <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-lg space-y-4">
            <div className="flex items-center gap-3 text-amber-600 dark:text-amber-400">
              <AlertTriangle className="h-6 w-6 shrink-0" />
              <h3 className="text-base font-semibold text-foreground">
                Bạn có thay đổi chưa lưu!
              </h3>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Bạn có nội dung ghi chú hoặc câu trả lời chưa hoàn tất lưu trên máy chủ.
              Nếu rời khỏi trang ngay, các nội dung chưa lưu có thể bị mất.
            </p>

            {blockerSaveError && (
              <div className="p-2.5 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
                {blockerSaveError}
              </div>
            )}

            <div className="flex flex-col sm:flex-row justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleBlockerStay}
                disabled={isFlushingAndLeaving}
                className="text-xs"
              >
                Ở lại trang
              </Button>
              <Button
                variant="destructive"
                size="sm"
                onClick={handleBlockerDiscardAndLeave}
                disabled={isFlushingAndLeaving}
                className="text-xs"
              >
                Rời khỏi không lưu
              </Button>
              {noteState.hasUnsavedChanges && !hasUnsavedActivityDraft && !isMutating && (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handleSaveAndProceed}
                  disabled={isFlushingAndLeaving}
                  className="text-xs flex items-center gap-1.5"
                >
                  {isFlushingAndLeaving ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Save className="h-3.5 w-3.5" />
                  )}
                  Lưu rồi rời
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
