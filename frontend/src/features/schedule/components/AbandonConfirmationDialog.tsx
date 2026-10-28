import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, AlertTriangle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import {
  enrollmentApi,
  enrollmentKeys,
  scheduleKeys,
} from "@/features/enrollment/api/enrollment.api";
import { todayKeys } from "@/features/learning/api/today.api";

export interface AbandonConfirmationDialogProps {
  isOpen: boolean;
  onClose: () => void;
  enrollmentId: string;
  expectedVersion: number;
  curriculumTitle: string;
  onSuccess?: () => void;
}

export function AbandonConfirmationDialog({
  isOpen,
  onClose,
  enrollmentId,
  expectedVersion,
  curriculumTitle,
  onSuccess,
}: AbandonConfirmationDialogProps) {
  const queryClient = useQueryClient();
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const abandonMutation = useMutation({
    mutationFn: () =>
      enrollmentApi.abandon(enrollmentId, { expectedVersion }),
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
              "Lộ trình đã kết thúc hoặc dữ liệu đã thay đổi.",
          );
        } else {
          setErrorMessage(err.message || "Không thể kết thúc lộ trình. Vui lòng thử lại.");
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
      data-testid="schedule-abandon-modal"
    >
      <div className="relative w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-start justify-between gap-4 pb-4 border-b border-border">
          <div className="flex items-center gap-2.5">
            <div className="rounded-full bg-destructive/10 p-2 text-destructive">
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <h2 id={titleId} className="text-base font-bold text-foreground">
                Kết thúc lộ trình học
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
            aria-label="Đóng hộp thoại kết thúc"
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
            Bạn có chắc chắn muốn kết thúc lộ trình học <strong>{curriculumTitle}</strong>?
          </p>
          <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-destructive space-y-1">
            <p className="font-semibold">Lưu ý quan trọng:</p>
            <ul className="list-disc pl-4 space-y-1">
              <li>Lộ trình sẽ chuyển sang trạng thái <strong>Đã kết thúc (ABANDONED)</strong>.</li>
              <li>Bạn sẽ không thể tiếp tục hoặc điều chỉnh lịch học cũ này nữa.</li>
              <li>Bạn có thể đăng ký lại lộ trình này từ đầu bất cứ lúc nào.</li>
            </ul>
          </div>
        </div>

        <div className="mt-6 flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-2.5 border-t border-border pt-4">
          <Button
            type="button"
            variant="ghost"
            onClick={handleClose}
            disabled={abandonMutation.isPending}
            className="w-full sm:w-auto text-xs"
          >
            Hủy bỏ
          </Button>
          <SubmitButton
            type="button"
            variant="destructive"
            onClick={() => abandonMutation.mutate()}
            pending={abandonMutation.isPending}
            data-testid="schedule-confirm-abandon-button"
            className="w-full sm:w-auto text-xs font-semibold"
          >
            Xác nhận kết thúc
          </SubmitButton>
        </div>
      </div>
    </div>
  );
}
