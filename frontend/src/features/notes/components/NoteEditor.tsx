import { useEffect, useState } from "react";
import {
  AlertCircle,
  AlertTriangle,
  Check,
  ChevronDown,
  ChevronRight,
  FileText,
  Loader2,
  RefreshCw,
  Save,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAutosaveNote, type AutosaveStatus } from "../hooks/useAutosaveNote";
import { notesApi, type NoteResponse } from "../api/notes.api";

export interface NoteEditorProps {
  enrollmentId?: string;
  lessonId?: string;
  initialNote?: NoteResponse | null;
  isOpen?: boolean;
  onToggleOpen?: () => void;
  isLoading?: boolean;
  isLoadError?: boolean;
  onRetryLoad?: () => void;
  disableBlocker?: boolean;
  onUnsavedStateChange?: (state: {
    hasUnsavedChanges: boolean;
    isSaving: boolean;
    isConflict: boolean;
  }) => void;
  saveRef?: React.MutableRefObject<(() => Promise<NoteResponse | null>) | null>;
  className?: string;
}

export function NoteEditor({
  enrollmentId,
  lessonId,
  initialNote,
  isOpen = true,
  onToggleOpen,
  isLoading = false,
  isLoadError = false,
  onRetryLoad,
  disableBlocker = false,
  onUnsavedStateChange,
  saveRef,
  className = "",
}: NoteEditorProps) {
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [customError, setCustomError] = useState<string | null>(null);

  const {
    content,
    updateContent,
    saveImmediately,
    reloadNote,
    overwriteConflict,
    deleteNote,
    status,
    lastSavedAt,
    errorMessage,
    currentNote,
    hasUnsavedChanges,
  } = useAutosaveNote({
    enrollmentId,
    lessonId,
    initialNote,
    disableBlocker,
  });

  useEffect(() => {
    if (saveRef) {
      saveRef.current = saveImmediately;
    }
  }, [saveRef, saveImmediately]);

  useEffect(() => {
    onUnsavedStateChange?.({
      hasUnsavedChanges,
      isSaving: status === "saving",
      isConflict: status === "conflict",
    });
  }, [hasUnsavedChanges, status, onUnsavedStateChange]);

  // Keyboard shortcut Ctrl+S / Cmd+S
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isLoading && !isLoadError && (e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        void saveImmediately();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [saveImmediately, isLoading, isLoadError]);

  const handleDelete = async () => {
    if (!currentNote) return;
    setIsDeleting(true);
    try {
      await deleteNote();
      setDeleteConfirmOpen(false);
    } catch {
      // Error handled
    } finally {
      setIsDeleting(false);
    }
  };

  const handleReload = async () => {
    if (!enrollmentId || !lessonId) return;
    setCustomError(null);
    try {
      const lessonNote = await notesApi.getLessonNote(enrollmentId, lessonId);
      if (lessonNote) {
        await reloadNote(lessonNote);
      } else {
        setCustomError("Không tìm thấy ghi chú trên máy chủ để tải lại.");
      }
    } catch {
      setCustomError("Không thể tải lại ghi chú từ máy chủ. Vui lòng thử lại sau.");
    }
  };

  const handleOverwrite = async () => {
    if (!enrollmentId || !lessonId) return;
    setCustomError(null);
    try {
      const latest = await notesApi.getLessonNote(enrollmentId, lessonId);
      if (latest) {
        await overwriteConflict(latest.version, latest);
      } else {
        // Absolutely NO version guessing fallback!
        setCustomError(
          "Không tìm thấy phiên bản ghi chú trên máy chủ để ghi đè. Nội dung của bạn đã được giữ lại. Vui lòng thử lại sau.",
        );
      }
    } catch {
      // Absolutely NO version guessing fallback!
      setCustomError(
        "Không thể đọc phiên bản mới nhất từ máy chủ để ghi đè. Bản nháp của bạn đã được giữ lại an toàn. Vui lòng thử lại sau.",
      );
    }
  };

  const renderStatusBadge = (currentStatus: AutosaveStatus) => {
    if (isLoading) {
      return (
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground animate-pulse">
          <Loader2 className="h-3 w-3 animate-spin text-primary" />
          Đang tải...
        </span>
      );
    }

    switch (currentStatus) {
      case "saving":
        return (
          <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground font-medium animate-pulse">
            <Loader2 className="h-3 w-3 animate-spin text-primary" />
            Đang lưu...
          </span>
        );
      case "saved":
        return (
          <span className="inline-flex items-center gap-1.5 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
            <Check className="h-3 w-3" />
            Đã lưu
            {lastSavedAt && (
              <span className="text-[11px] text-muted-foreground">
                ({lastSavedAt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })})
              </span>
            )}
          </span>
        );
      case "dirty":
        return (
          <span className="inline-flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400 font-medium">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-500 animate-ping" />
            Có thay đổi chưa lưu
          </span>
        );
      case "conflict":
        return (
          <span className="inline-flex items-center gap-1.5 text-xs text-destructive font-medium">
            <AlertCircle className="h-3 w-3 text-destructive" />
            Xung đột phiên bản
          </span>
        );
      case "error":
        return (
          <span className="inline-flex items-center gap-1.5 text-xs text-destructive font-medium">
            <AlertTriangle className="h-3 w-3" />
            Lỗi lưu
          </span>
        );
      case "idle":
      default:
        return null;
    }
  };

  const charCount = content.length;
  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;
  const displayError = customError || errorMessage;

  return (
    <div
      className={`rounded-xl border border-border bg-card shadow-sm transition-all overflow-hidden ${className}`}
      data-testid="note-editor"
    >
      {/* Header / Accordion trigger */}
      <div className="flex items-center justify-between px-4 py-3 bg-muted/30 border-b border-border">
        <button
          type="button"
          onClick={onToggleOpen}
          className="flex items-center gap-2 text-sm font-semibold text-foreground hover:text-primary transition-colors focus:outline-none"
          aria-expanded={isOpen}
          aria-controls="note-editor-content"
        >
          {isOpen ? (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
          )}
          <FileText className="h-4 w-4 text-primary" />
          <span>Ghi chú của bạn</span>
        </button>

        <div className="flex items-center gap-3">
          {renderStatusBadge(status)}

          {isOpen && !isLoading && !isLoadError && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => void saveImmediately()}
              disabled={status === "saving" || status === "saved"}
              title="Lưu ngay (Ctrl+S)"
              className="h-7 px-2 text-xs text-muted-foreground hover:text-foreground"
            >
              <Save className="h-3.5 w-3.5 mr-1" />
              Lưu ngay
            </Button>
          )}
        </div>
      </div>

      {/* Editor Body */}
      {isOpen && (
        <div id="note-editor-content" className="p-4 space-y-3">
          {/* Loading state */}
          {isLoading && (
            <div className="space-y-3" role="status" aria-label="Đang tải ghi chú">
              <div className="flex items-center gap-2 text-xs text-muted-foreground animate-pulse">
                <Loader2 className="h-3.5 w-3.5 animate-spin text-primary" />
                <span>Đang tải ghi chú của buổi học...</span>
              </div>
              <div className="h-28 w-full rounded-lg bg-muted/40 animate-pulse" />
            </div>
          )}

          {/* Load Error state */}
          {!isLoading && isLoadError && (
            <div className="flex flex-col gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
              <div className="flex items-center gap-2 font-medium">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>Không thể tải ghi chú bài học từ máy chủ.</span>
              </div>
              <p className="text-muted-foreground">
                Để tránh tạo trùng lặp hoặc mất dữ liệu, vui lòng thử tải lại trước khi chỉnh sửa.
              </p>
              {onRetryLoad && (
                <div className="pt-1">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={onRetryLoad}
                    className="h-7 text-xs"
                  >
                    <RefreshCw className="h-3 w-3 mr-1" />
                    Thử tải lại
                  </Button>
                </div>
              )}
            </div>
          )}

          {/* Normal editor view when loaded */}
          {!isLoading && !isLoadError && (
            <>
              {/* Conflict Alert Banner */}
              {status === "conflict" && (
                <div className="flex flex-col gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
                  <div className="flex items-center gap-2 font-semibold">
                    <AlertCircle className="h-4 w-4 shrink-0" />
                    <span>Ghi chú đã bị thay đổi ở một phiên học khác!</span>
                  </div>
                  <p className="text-muted-foreground">
                    Để tránh mất dữ liệu, bạn có thể tải lại nội dung mới nhất từ máy chủ hoặc ghi đè
                    bằng nội dung đang nhập.
                  </p>
                  <div className="flex items-center gap-2 pt-1">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleReload}
                      className="h-7 text-xs"
                    >
                      <RefreshCw className="h-3 w-3 mr-1" />
                      Tải lại phiên bản mới
                    </Button>
                    {currentNote && (
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={() => void handleOverwrite()}
                        className="h-7 text-xs"
                      >
                        Ghi đè bằng bản này
                      </Button>
                    )}
                  </div>
                </div>
              )}

              {/* General Error Alert */}
              {displayError && status !== "conflict" && (
                <div className="flex items-center justify-between p-2.5 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
                  <div className="flex items-center gap-1.5">
                    <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                    <span>{displayError}</span>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => void saveImmediately()}
                    className="h-6 text-[11px] px-2 text-destructive border-destructive/30 hover:bg-destructive/10"
                  >
                    Thử lại
                  </Button>
                </div>
              )}

              {/* Note Input Textarea */}
              <div className="relative">
                <textarea
                  id="learner-lesson-note"
                  value={content}
                  onChange={(e) => updateContent(e.target.value)}
                  placeholder="Ghi lại các từ mới, cấu trúc ngữ pháp cần lưu ý hoặc thắc mắc cá nhân..."
                  rows={5}
                  className="w-full rounded-lg border border-input bg-background p-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 resize-y min-h-[100px] leading-relaxed"
                  aria-label="Nội dung ghi chú cá nhân"
                />
              </div>

              {/* Footer with stats & actions */}
              <div className="flex items-center justify-between text-xs text-muted-foreground pt-1">
                <div className="flex items-center gap-3">
                  <span>{wordCount} từ</span>
                  <span>•</span>
                  <span>{charCount} ký tự</span>
                </div>

                <div className="flex items-center gap-2">
                  {currentNote && content.length > 0 && (
                    <>
                      {deleteConfirmOpen ? (
                        <div className="flex items-center gap-1">
                          <span className="text-[11px] text-destructive mr-1">Xóa?</span>
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            disabled={isDeleting || status === "saving"}
                            onClick={handleDelete}
                            className="h-6 px-2 text-[11px]"
                          >
                            {isDeleting ? "Đang xóa..." : "Xác nhận"}
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteConfirmOpen(false)}
                            className="h-6 px-1.5 text-[11px]"
                          >
                            Hủy
                          </Button>
                        </div>
                      ) : (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setDeleteConfirmOpen(true)}
                          className="h-6 px-2 text-[11px] text-muted-foreground hover:text-destructive"
                          title="Xóa ghi chú này"
                        >
                          <Trash2 className="h-3 w-3 mr-1" />
                          Xóa
                        </Button>
                      )}
                    </>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
