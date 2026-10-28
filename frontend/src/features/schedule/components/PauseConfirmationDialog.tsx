import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, PauseCircle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import {
  enrollmentApi,
  enrollmentKeys,
  scheduleKeys,
} from "@/features/enrollment/api/enrollment.api";
import { todayKeys } from "@/features/learning/api/today.api";

export interface PauseConfirmationDialogProps {
  isOpen: boolean;
  onClose: () => void;
  enrollmentId: string;
  expectedVersion: number;
  curriculumTitle: string;
  onSuccess?: () => void;
}

export function PauseConfirmationDialog({
  isOpen,
  onClose,
  enrollmentId,
  expectedVersion,
  curriculumTitle,
  onSuccess,
}: PauseConfirmationDialogProps) {
  const queryClient = useQueryClient();
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const pauseMutation = useMutation({
    mutationFn: () =>
      enrollmentApi.pause(enrollmentId, { expectedVersion }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      void queryClient.invalidateQueries({ queryKey: enrollmentKeys.all });
      void queryClient.invalidateQueries({ queryKey: todayKeys.all });
      onSuccess?.();
      onClose();
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setErrorMessage(
            err.message ||
              "Lộ trình không ở trạng thái hợp lệ để tạm dừng hoặc đã có thay đổi mới.",
          );
        } else {
          setErrorMessage(err.message || "Không thể tạm dừng lộ trình. Vui lòng thử lại.");
        }
      } else {
        setErrorMessage("Lỗi kết nối đến máy chủ. Vui lòng thử lại.");
      }
    },
  });

  const handleClose = useCallback(() => {
    setErrorMessage(null);
    onClose();
  }, [onClose]);

  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 50);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        handleClose();
        return;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, handleClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      ref={dialogRef}
      data-testid="schedule-pause-modal"
    >
      <div className="relative w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-start justify-between gap-4 pb-4 border-b border-border">
          <div className="flex items-center gap-2.5">
            <div className="rounded-full bg-amber-500/10 p-2 text-amber-600 dark:text-amber-400">
              <PauseCircle className="h-5 w-5" />
            </div>
            <div>
              <h2 id={titleId} className="text-base font-bold text-foreground">
                Tạm dừng lộ trình học
              </h2>
              <p className="text-xs text-muted-foreground line-clamp-1">{curriculumTitle}</p>
            </div>
          </div>
          <Button
            ref={closeButtonRef}
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-muted-foreground hover:text-foreground"
            onClick={handleClose}
            aria-label="Đóng hộp thoại tạm dừng"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {errorMessage && (
          <div
            className="mt-4 flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive"
            role="alert"
          >
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <p>{errorMessage}</p>
          </div>
        )}

        <div className="mt-4 space-y-3 text-xs text-muted-foreground leading-relaxed">
          <p>
            Bạn có chắc chắn muốn tạm dừng lộ trình học <strong>{curriculumTitle}</strong>?
          </p>
          <ul className="list-disc pl-4 space-y-1.5 text-foreground/80">
            <li>Toàn bộ tiến độ và kết quả các bài đã học sẽ được bảo lưu nguyên vẹn.</li>
            <li>Hệ thống sẽ tạm dừng nhắc nhở và không tính các bài học tiếp theo là quá hạn.</li>
            <li>Bạn có thể chọn tiếp tục học và xếp lại lịch mới bất cứ khi nào bạn sẵn sàng.</li>
          </ul>
        </div>

        <div className="mt-6 flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-2.5 border-t border-border pt-4">
          <Button
            type="button"
            variant="ghost"
            onClick={handleClose}
            disabled={pauseMutation.isPending}
            className="w-full sm:w-auto text-xs"
          >
            Hủy bỏ
          </Button>
          <SubmitButton
            type="button"
            variant="default"
            onClick={() => pauseMutation.mutate()}
            pending={pauseMutation.isPending}
            data-testid="schedule-confirm-pause-button"
            className="w-full sm:w-auto text-xs font-semibold bg-amber-600 hover:bg-amber-700 text-white dark:bg-amber-600 dark:hover:bg-amber-700"
          >
            Xác nhận tạm dừng
          </SubmitButton>
        </div>
      </div>
    </div>
  );
}
