import { useEffect, useId, useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, AlertTriangle, ArrowRight, Calendar, CheckCircle2, Clock, Info, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import {
  enrollmentApi,
  enrollmentKeys,
  scheduleKeys,
  type DayOfWeek,
  type ScheduleCommitRequest,
  type SchedulePreviewRequest,
  type SchedulePreviewResponse,
} from "@/features/enrollment/api/enrollment.api";
import { todayKeys } from "@/features/learning/api/today.api";

const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: "MONDAY", label: "T2 (Thứ Hai)" },
  { value: "TUESDAY", label: "T3 (Thứ Ba)" },
  { value: "WEDNESDAY", label: "T4 (Thứ Tư)" },
  { value: "THURSDAY", label: "T5 (Thứ Năm)" },
  { value: "FRIDAY", label: "T6 (Thứ Sáu)" },
  { value: "SATURDAY", label: "T7 (Thứ Bảy)" },
  { value: "SUNDAY", label: "CN (Chủ Nhật)" },
];

function getTodayStringInTimeZone(tz: string): string {
  try {
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: tz,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).formatToParts(new Date());
    const part = (type: string) => parts.find((p) => p.type === type)?.value;
    return `${part("year")}-${part("month")}-${part("day")}`;
  } catch {
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }
}

function formatDateVn(iso?: string | null): string {
  if (!iso) return "Chưa có lịch";
  const parts = iso.split("-");
  if (parts.length !== 3) return iso;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

export interface RescheduleDialogProps {
  isOpen: boolean;
  onClose: () => void;
  enrollmentId: string;
  curriculumTitle: string;
  defaultDailyTargetMinutes: number;
  defaultStudyDays: DayOfWeek[];
  timeZone: string;
  mode?: "reschedule" | "resume";
  onSuccess?: () => void;
}

interface PreviewState {
  data: SchedulePreviewResponse;
  inputSnapshotKey: string;
  idempotencyKey: string;
}

interface RescheduleFormProps {
  onClose: () => void;
  enrollmentId: string;
  defaultDailyTargetMinutes: number;
  defaultStudyDays: DayOfWeek[];
  timeZone: string;
  mode?: "reschedule" | "resume";
  onSuccess?: () => void;
}

function RescheduleForm({
  onClose,
  enrollmentId,
  defaultDailyTargetMinutes,
  defaultStudyDays,
  timeZone,
  mode = "reschedule",
  onSuccess,
}: RescheduleFormProps) {
  const queryClient = useQueryClient();
  const todayIso = useMemo(() => getTodayStringInTimeZone(timeZone), [timeZone]);

  // Form states initialized fresh on mount
  const [resumeDate, setResumeDate] = useState<string>(todayIso);
  const [studyDays, setStudyDays] = useState<DayOfWeek[]>(() =>
    defaultStudyDays.length > 0 ? defaultStudyDays : ["MONDAY", "WEDNESDAY", "FRIDAY"],
  );
  const [dailyTargetMinutes, setDailyTargetMinutes] = useState<number>(() => defaultDailyTargetMinutes || 30);

  // Preview and error states
  const [previewState, setPreviewState] = useState<PreviewState | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isConflict, setIsConflict] = useState<boolean>(false);

  // Helper to compute snapshot key for current form values
  const currentInputKey = `${resumeDate}|${[...studyDays].sort().join(",")}|${dailyTargetMinutes}`;

  // Invalidate preview if user changes any form field
  const handleResumeDateChange = (val: string) => {
    setResumeDate(val);
    setPreviewState(null);
    setErrorMessage(null);
    setIsConflict(false);
  };

  const handleDayToggle = (day: DayOfWeek) => {
    setStudyDays((prev) => {
      const next = prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day];
      return next;
    });
    setPreviewState(null);
    setErrorMessage(null);
    setIsConflict(false);
  };

  const handleMinutesChange = (val: number) => {
    setDailyTargetMinutes(val);
    setPreviewState(null);
    setErrorMessage(null);
    setIsConflict(false);
  };

  // Preview mutation
  const previewMutation = useMutation({
    mutationFn: (payload: SchedulePreviewRequest) =>
      enrollmentApi.previewReschedule(enrollmentId, payload),
    onSuccess: (data) => {
      setPreviewState({
        data,
        inputSnapshotKey: currentInputKey,
        idempotencyKey: crypto.randomUUID(),
      });
      setErrorMessage(null);
      setIsConflict(false);
    },
    onError: (err) => {
      setPreviewState(null);
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          setErrorMessage(
            err.message ||
              (mode === "resume"
                ? "Lộ trình không ở trạng thái hợp lệ để tiếp tục (yêu cầu PAUSED)."
                : "Lộ trình không ở trạng thái hợp lệ để đổi lịch (chỉ áp dụng cho ACTIVE)."),
          );
        } else {
          setErrorMessage(err.message || "Không thể xem trước lịch học. Vui lòng thử lại.");
        }
      } else {
        setErrorMessage("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
      }
    },
  });

  // Commit mutation
  const commitMutation = useMutation({
    mutationFn: (variables: { payload: ScheduleCommitRequest; key: string }) =>
      mode === "resume"
        ? enrollmentApi.resume(enrollmentId, variables.payload, variables.key)
        : enrollmentApi.commitReschedule(enrollmentId, variables.payload, variables.key),
    onSuccess: () => {
      // Invalidate relevant queries
      void queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      void queryClient.invalidateQueries({ queryKey: enrollmentKeys.all });
      void queryClient.invalidateQueries({ queryKey: todayKeys.all });
      onSuccess?.();
      onClose();
    },
    onError: (err) => {
      if (err instanceof ApiError && err.status === 409) {
        setIsConflict(true);
        // Clear preview snapshot so learner is forced to re-preview
        setPreviewState(null);
        setErrorMessage(
          mode === "resume"
            ? "Dữ liệu lộ trình hoặc trạng thái đã thay đổi trong khi bạn đang xem trước. Vui lòng bấm 'Xem trước lịch mới' để tính toán lại phương án mới nhất."
            : "Dữ liệu lộ trình hoặc tiến độ học đã thay đổi trong khi bạn đang xem trước. Vui lòng bấm 'Xem trước lịch mới' để tính toán lại phương án mới nhất.",
        );
      } else if (err instanceof ApiError) {
        setErrorMessage(err.message || "Không thể áp dụng lịch học mới. Vui lòng thử lại.");
      } else {
        setErrorMessage(
          mode === "resume"
            ? "Lỗi kết nối mạng. Bạn có thể bấm 'Xác nhận tiếp tục học' để thử lại an toàn."
            : "Lỗi kết nối mạng. Bạn có thể bấm 'Xác nhận đổi lịch' để thử lại an toàn.",
        );
      }
    },
  });

  const handlePreview = () => {
    if (!resumeDate) {
      setErrorMessage("Vui lòng chọn ngày tiếp tục học.");
      return;
    }
    if (studyDays.length === 0) {
      setErrorMessage("Vui lòng chọn ít nhất một ngày học trong tuần.");
      return;
    }
    if (dailyTargetMinutes < 10 || dailyTargetMinutes > 180) {
      setErrorMessage("Mục tiêu phút mỗi ngày phải từ 10 đến 180 phút.");
      return;
    }

    setErrorMessage(null);
    setIsConflict(false);
    previewMutation.mutate({
      resumeDate,
      studyDays,
      dailyTargetMinutes,
    });
  };

  const handleConfirm = () => {
    if (!previewState) return;

    commitMutation.mutate({
      payload: {
        resumeDate,
        studyDays,
        dailyTargetMinutes,
        expectedVersion: previewState.data.enrollmentVersion,
      },
      key: previewState.idempotencyKey,
    });
  };

  const isPreviewValid = Boolean(previewState && previewState.inputSnapshotKey === currentInputKey);

  return (
    <>
      {/* Error / Conflict Alert */}
      {errorMessage && (
        <div
          className={`mt-4 flex items-start gap-3 rounded-lg border p-3.5 text-sm ${
            isConflict
              ? "border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-200"
              : "border-destructive/30 bg-destructive/10 text-destructive dark:border-destructive/50 dark:bg-destructive/20"
          }`}
          data-testid="reschedule-error-message"
          role="alert"
        >
          {isConflict ? (
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
          ) : (
            <AlertCircle className="h-5 w-5 shrink-0 text-destructive mt-0.5" />
          )}
          <div className="space-y-1">
            <p className="font-semibold">{isConflict ? "Xung đột dữ liệu" : "Có lỗi xảy ra"}</p>
            <p>{errorMessage}</p>
          </div>
        </div>
      )}

      {/* Notice about completed lessons */}
      <div className="mt-4 flex items-start gap-2.5 rounded-lg border border-blue-200 bg-blue-50/70 p-3 text-xs text-blue-900 dark:border-blue-900/50 dark:bg-blue-950/30 dark:text-blue-300">
        <Info className="h-4 w-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
        <p>
          <strong>Lưu ý:</strong> Tất cả bài học và hoạt động đã hoàn thành sẽ được giữ nguyên kết quả và
          lịch học gốc. Chỉ các bài học chưa hoàn thành mới được phân bổ lại theo phương án mới.
          {mode === "resume" && " Sau khi xác nhận, lộ trình sẽ được kích hoạt lại (Đang học)."}
        </p>
      </div>

      {/* Form Controls */}
      <div className="mt-5 space-y-4">
        {/* Resume Date */}
        <div className="space-y-1.5">
          <Label htmlFor="reschedule-resume-date" className="text-sm font-medium text-foreground flex items-center gap-1.5">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            Ngày tiếp tục học
          </Label>
          <Input
            id="reschedule-resume-date"
            data-testid="reschedule-resume-date-input"
            type="date"
            min={todayIso}
            value={resumeDate}
            onChange={(e) => handleResumeDateChange(e.target.value)}
            className="w-full sm:w-64"
          />
          <p className="text-xs text-muted-foreground">
            Chọn ngày bắt đầu lịch học mới (từ hôm nay trở đi). Nếu ngày này không nằm trong các thứ học, hệ thống sẽ tự động bắt đầu từ ngày học kế tiếp.
          </p>
        </div>

        {/* Study Days */}
        <div className="space-y-2">
          <Label className="text-sm font-medium text-foreground">
            Ngày học trong tuần <span className="text-destructive">*</span>
          </Label>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {DAYS_OF_WEEK.map((day) => {
              const isSelected = studyDays.includes(day.value);
              return (
                <label
                  key={day.value}
                  className={`flex items-center gap-2 rounded-lg border p-2.5 text-xs font-medium cursor-pointer transition-colors ${
                    isSelected
                      ? "border-primary bg-primary/10 text-primary font-semibold dark:bg-primary/20"
                      : "border-border bg-card text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => handleDayToggle(day.value)}
                    className="sr-only"
                    data-testid={`reschedule-day-${day.value}`}
                  />
                  <div
                    className={`h-4 w-4 rounded border flex items-center justify-center shrink-0 ${
                      isSelected
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-muted-foreground/40 bg-transparent"
                    }`}
                  >
                    {isSelected && <CheckCircle2 className="h-3 w-3" />}
                  </div>
                  <span>{day.label}</span>
                </label>
              );
            })}
          </div>
          {studyDays.length === 0 && (
            <p className="text-xs text-destructive">Vui lòng chọn ít nhất một ngày học trong tuần.</p>
          )}
        </div>

        {/* Daily Target Minutes */}
        <div className="space-y-1.5">
          <Label htmlFor="reschedule-target-minutes" className="text-sm font-medium text-foreground flex items-center gap-1.5">
            <Clock className="h-4 w-4 text-muted-foreground" />
            Mục tiêu học mỗi ngày (phút)
          </Label>
          <div className="flex items-center gap-3">
            <Input
              id="reschedule-target-minutes"
              data-testid="reschedule-minutes-input"
              type="number"
              min={10}
              max={180}
              step={5}
              value={dailyTargetMinutes}
              onChange={(e) => handleMinutesChange(Number(e.target.value))}
              className="w-32"
            />
            <span className="text-xs text-muted-foreground">phút/ngày (từ 10 đến 180 phút)</span>
          </div>
        </div>
      </div>

      {/* Action: Preview Button */}
      <div className="mt-5 pt-3 border-t border-border flex justify-end">
        <Button
          type="button"
          variant="outline"
          onClick={handlePreview}
          disabled={previewMutation.isPending || commitMutation.isPending}
          data-testid="reschedule-preview-button"
          className="w-full sm:w-auto"
        >
          {previewMutation.isPending ? "Đang tính toán..." : "Xem trước lịch mới"}
        </Button>
      </div>

      {/* Preview Results Panel */}
      {previewMutation.isPending && (
        <div className="mt-4 rounded-xl border border-border bg-muted/40 p-4 text-center text-sm text-muted-foreground">
          Đang tính toán phương án phân bổ lịch học...
        </div>
      )}

      {isPreviewValid && previewState && (
        <div
          className="mt-4 rounded-xl border border-primary/30 bg-primary/5 p-4 space-y-4 animate-in fade-in duration-200"
          data-testid="reschedule-preview-result"
        >
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-primary/20 pb-2.5">
            <h3 className="text-sm font-bold text-foreground flex items-center gap-1.5">
              <CheckCircle2 className="h-4 w-4 text-primary" />
              Kết quả xem trước phương án
            </h3>
            <div className="text-xs">
              <span className="text-muted-foreground">Ngày kết thúc dự kiến: </span>
              <span className="font-bold text-primary">
                {formatDateVn(previewState.data.proposedEndDate)}
              </span>
            </div>
          </div>

          {previewState.data.modifiedSchedules.length > 0 ? (
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">
                Có <strong>{previewState.data.modifiedSchedules.length}</strong> bài học chưa hoàn thành sẽ được điều chỉnh ngày học:
              </p>
              <div className="max-h-56 overflow-y-auto rounded-lg border border-border bg-card">
                <table className="w-full text-left text-xs">
                  <thead className="bg-muted text-muted-foreground sticky top-0 border-b border-border">
                    <tr>
                      <th className="p-2.5 font-semibold">Bài học</th>
                      <th className="p-2.5 font-semibold">Ngày hiện tại</th>
                      <th className="p-2.5 font-semibold">Ngày mới đề xuất</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {previewState.data.modifiedSchedules.map((item) => (
                      <tr key={item.lessonId} className="hover:bg-accent/40">
                        <td className="p-2.5 font-medium text-foreground">{item.lessonTitle}</td>
                        <td className="p-2.5 text-muted-foreground">{formatDateVn(item.previousPlannedDate)}</td>
                        <td className="p-2.5 font-semibold text-primary flex items-center gap-1">
                          <ArrowRight className="h-3 w-3 text-muted-foreground" />
                          {formatDateVn(item.newPlannedDate)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-border bg-card p-3 text-xs text-muted-foreground text-center">
              Không có bài học nào thay đổi ngày học. Chỉ cập nhật cấu hình ngày học trong tuần và mục tiêu phút.
            </div>
          )}

          {/* Confirm Actions */}
          <div className="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              disabled={commitMutation.isPending}
              className="w-full sm:w-auto"
            >
              Hủy bỏ
            </Button>
            <SubmitButton
              type="button"
              variant="default"
              onClick={handleConfirm}
              pending={commitMutation.isPending}
              data-testid="reschedule-confirm-button"
              className="w-full sm:w-auto font-semibold"
            >
              {mode === "resume" ? "Xác nhận tiếp tục học" : "Xác nhận đổi lịch"}
            </SubmitButton>
          </div>
        </div>
      )}
    </>
  );
}

export function RescheduleDialog({
  isOpen,
  onClose,
  enrollmentId,
  curriculumTitle,
  defaultDailyTargetMinutes,
  defaultStudyDays,
  timeZone,
  mode = "reschedule",
  onSuccess,
}: RescheduleDialogProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const previousActiveElementRef = useRef<HTMLElement | null>(null);

  // Focus management & Escape key handling
  useEffect(() => {
    if (!isOpen) return;

    previousActiveElementRef.current = document.activeElement as HTMLElement | null;

    const timer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 50);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
        return;
      }

      if (e.key === "Tab" && dialogRef.current) {
        const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
        );
        if (focusable.length === 0) return;

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      clearTimeout(timer);
      window.removeEventListener("keydown", handleKeyDown);
      previousActiveElementRef.current?.focus();
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      ref={dialogRef}
      data-testid="reschedule-dialog"
    >
      <div className="relative w-full max-w-2xl rounded-2xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 pb-4 border-b border-border">
          <div>
            <h2 id={titleId} className="text-lg font-bold text-foreground">
              {mode === "resume" ? "Tiếp tục lộ trình học" : "Đổi lịch học"}
            </h2>
            <p className="text-sm text-muted-foreground mt-0.5 line-clamp-1">{curriculumTitle}</p>
          </div>
          <Button
            ref={closeButtonRef}
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-muted-foreground hover:text-foreground"
            onClick={onClose}
            aria-label={mode === "resume" ? "Đóng hộp thoại tiếp tục học" : "Đóng hộp thoại đổi lịch"}
            data-testid="reschedule-close-button"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        <RescheduleForm
          onClose={onClose}
          enrollmentId={enrollmentId}
          defaultDailyTargetMinutes={defaultDailyTargetMinutes}
          defaultStudyDays={defaultStudyDays}
          timeZone={timeZone}
          mode={mode}
          onSuccess={onSuccess}
        />
      </div>
    </div>
  );
}
