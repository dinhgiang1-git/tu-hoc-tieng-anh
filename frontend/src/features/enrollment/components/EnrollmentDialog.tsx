import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AlertCircle, Calendar, Clock, Globe, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys, type User } from "@/features/auth";
import { enrollmentApi, enrollmentKeys, type DayOfWeek } from "../api/enrollment.api";
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

const COMMON_TIMEZONES = [
  "Asia/Ho_Chi_Minh",
  "Asia/Bangkok",
  "Asia/Tokyo",
  "Asia/Singapore",
  "Europe/London",
  "America/New_York",
  "America/Los_Angeles",
  "UTC",
];

const enrollmentSchema = z.object({
  startDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Vui lòng chọn ngày hợp lệ (YYYY-MM-DD)"),
  studyDays: z
    .array(
      z.enum([
        "MONDAY",
        "TUESDAY",
        "WEDNESDAY",
        "THURSDAY",
        "FRIDAY",
        "SATURDAY",
        "SUNDAY",
      ]),
    )
    .min(1, "Vui lòng chọn ít nhất một ngày học trong tuần"),
  dailyTargetMinutes: z
    .number()
    .min(10, "Mục tiêu tối thiểu là 10 phút")
    .max(180, "Mục tiêu tối đa là 180 phút"),
  timeZone: z.string().min(1, "Múi giờ không được để trống"),
});

export type EnrollmentFormValues = z.infer<typeof enrollmentSchema>;

function getTodayString(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

interface EnrollmentFormProps {
  curriculumId: string;
  revisionId: string;
  defaultDailyTargetMinutes: number;
  defaultTimeZone: string;
  onClose: () => void;
}

function EnrollmentForm({
  curriculumId,
  revisionId,
  defaultDailyTargetMinutes,
  defaultTimeZone,
  onClose,
}: EnrollmentFormProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [submitErrorMessage, setSubmitErrorMessage] = useState<string | null>(null);

  const idempotencyKeyRef = useRef<string>(crypto.randomUUID());
  const lastSubmittedPayloadStringRef = useRef<string>("");

  const form = useForm<EnrollmentFormValues>({
    resolver: zodResolver(enrollmentSchema),
    defaultValues: {
      startDate: getTodayString(),
      studyDays: ["MONDAY", "WEDNESDAY", "FRIDAY"],
      dailyTargetMinutes: defaultDailyTargetMinutes,
      timeZone: defaultTimeZone,
    },
  });

  const enrollMutation = useMutation({
    mutationFn: async (values: EnrollmentFormValues) => {
      const payloadString = JSON.stringify(values);
      // If payload changed from a previous submission, assign a new key
      if (
        lastSubmittedPayloadStringRef.current !== "" &&
        payloadString !== lastSubmittedPayloadStringRef.current
      ) {
        idempotencyKeyRef.current = crypto.randomUUID();
      }
      lastSubmittedPayloadStringRef.current = payloadString;

      return enrollmentApi.create(
        {
          curriculumId,
          revisionId,
          startDate: values.startDate,
          studyDays: values.studyDays,
          dailyTargetMinutes: values.dailyTargetMinutes,
          timeZone: values.timeZone,
        },
        idempotencyKeyRef.current,
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: todayKeys.all });
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.all });
      queryClient.invalidateQueries({ queryKey: authKeys.preferences() });
      onClose();
      navigate("/today");
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.problem.code === "STATE_CONFLICT") {
          setSubmitErrorMessage(
            "Bạn đã có một lộ trình đang hoạt động hoặc tạm dừng cho giáo trình này. Vui lòng kiểm tra lại trang Hôm nay.",
          );
        } else if (error.problem.code === "IDEMPOTENCY_CONFLICT") {
          setSubmitErrorMessage(
            "Yêu cầu đăng ký bị xung đột mã phiên. Vui lòng thử lại sau giây lát.",
          );
        } else {
          setSubmitErrorMessage(
            error.problem.detail || "Không thể đăng ký lộ trình. Vui lòng thử lại.",
          );
        }
      } else {
        setSubmitErrorMessage(
          "Không thể kết nối đến máy chủ. Bạn có thể nhấn Đăng ký lại để thử lại an toàn.",
        );
      }
    },
  });

  const currentStudyDays = form.watch("studyDays") || [];

  const handleStudyDayToggle = (day: DayOfWeek) => {
    const isSelected = currentStudyDays.includes(day);
    const updated = isSelected
      ? currentStudyDays.filter((d) => d !== day)
      : [...currentStudyDays, day];
    form.setValue("studyDays", updated, { shouldValidate: true });
  };

  const onSubmit = (values: EnrollmentFormValues) => {
    setSubmitErrorMessage(null);
    enrollMutation.mutate(values);
  };

  return (
    <>
      {/* Global Error Banner */}
      {submitErrorMessage && (
        <div
          role="alert"
          className="mt-4 flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/10 p-3.5 text-sm text-destructive"
        >
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <span className="font-semibold block">Đăng ký không thành công</span>
            <span>{submitErrorMessage}</span>
          </div>
        </div>
      )}

      {/* Form */}
      <form
        noValidate
        onSubmit={form.handleSubmit(onSubmit)}
        className="mt-4 space-y-5"
      >
        {/* Start Date */}
        <div className="space-y-1.5">
          <Label
            htmlFor="start-date-input"
            className="flex items-center gap-1.5 text-sm font-semibold text-foreground"
          >
            <Calendar className="h-4 w-4 text-primary" />
            Ngày bắt đầu học
          </Label>
          <Input
            id="start-date-input"
            type="date"
            {...form.register("startDate")}
            disabled={enrollMutation.isPending}
            aria-describedby={
              form.formState.errors.startDate ? "start-date-error" : undefined
            }
          />
          {form.formState.errors.startDate && (
            <p id="start-date-error" className="text-xs text-destructive">
              {form.formState.errors.startDate.message}
            </p>
          )}
        </div>

        {/* Study Days (Checkboxes) */}
        <div className="space-y-2">
          <Label className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
            <Calendar className="h-4 w-4 text-primary" />
            Các ngày học trong tuần
          </Label>
          <div
            className="grid grid-cols-2 sm:grid-cols-3 gap-2"
            role="group"
            aria-label="Chọn ngày học trong tuần"
          >
            {DAYS_OF_WEEK.map(({ value, label }) => {
              const checked = currentStudyDays.includes(value);
              return (
                <label
                  key={value}
                  className={`flex items-center gap-2 p-2.5 rounded-lg border text-xs font-medium cursor-pointer transition-colors ${
                    checked
                      ? "border-primary bg-primary/10 text-primary font-semibold"
                      : "border-border bg-card hover:bg-muted text-muted-foreground"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => handleStudyDayToggle(value)}
                    disabled={enrollMutation.isPending}
                    className="rounded border-border text-primary focus:ring-primary h-4 w-4"
                  />
                  <span>{label}</span>
                </label>
              );
            })}
          </div>
          {form.formState.errors.studyDays && (
            <p className="text-xs text-destructive">
              {form.formState.errors.studyDays.message}
            </p>
          )}
        </div>

        {/* Daily Target Minutes */}
        <div className="space-y-1.5">
          <Label
            htmlFor="daily-target-input"
            className="flex items-center gap-1.5 text-sm font-semibold text-foreground"
          >
            <Clock className="h-4 w-4 text-primary" />
            Mục tiêu học mỗi ngày (phút)
          </Label>
          <Input
            id="daily-target-input"
            type="number"
            min={10}
            max={180}
            {...form.register("dailyTargetMinutes", { valueAsNumber: true })}
            disabled={enrollMutation.isPending}
            aria-describedby={
              form.formState.errors.dailyTargetMinutes
                ? "daily-target-error"
                : "daily-target-hint"
            }
          />
          <p id="daily-target-hint" className="text-xs text-muted-foreground">
            Khoảng hợp lệ: 10 đến 180 phút mỗi ngày.
          </p>
          {form.formState.errors.dailyTargetMinutes && (
            <p id="daily-target-error" className="text-xs text-destructive">
              {form.formState.errors.dailyTargetMinutes.message}
            </p>
          )}
        </div>

        {/* Timezone */}
        <div className="space-y-1.5">
          <Label
            htmlFor="timezone-select"
            className="flex items-center gap-1.5 text-sm font-semibold text-foreground"
          >
            <Globe className="h-4 w-4 text-primary" />
            Múi giờ học tập
          </Label>
          <div className="space-y-1">
            <select
              id="timezone-select"
              {...form.register("timeZone")}
              disabled={enrollMutation.isPending}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              {COMMON_TIMEZONES.map((tz) => (
                <option key={tz} value={tz}>
                  {tz}
                </option>
              ))}
            </select>
          </div>
          {form.formState.errors.timeZone && (
            <p className="text-xs text-destructive">
              {form.formState.errors.timeZone.message}
            </p>
          )}
        </div>

        {/* Action buttons */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={enrollMutation.isPending}
          >
            Hủy
          </Button>
          <SubmitButton
            pending={enrollMutation.isPending}
            disabled={enrollMutation.isPending}
          >
            Xác nhận đăng ký
          </SubmitButton>
        </div>
      </form>
    </>
  );
}

export interface EnrollmentDialogProps {
  curriculumId: string;
  revisionId: string;
  curriculumTitle: string;
  isOpen: boolean;
  onClose: () => void;
}

export function EnrollmentDialog({
  curriculumId,
  revisionId,
  curriculumTitle,
  isOpen,
  onClose,
}: EnrollmentDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const previousActiveElementRef = useRef<HTMLElement | null>(null);

  const queryClient = useQueryClient();
  const currentUser = queryClient.getQueryData<User>(authKeys.me());

  // Query user preferences for default values
  const { data: preferences, isPending: isPreferencesPending } = useQuery({
    queryKey: authKeys.preferences(),
    queryFn: ({ signal }) => authApi.getPreferences(signal),
    enabled: isOpen,
  });

  const defaultDailyTargetMinutes = preferences?.dailyTargetMinutes ?? 30;
  const defaultTimeZone =
    currentUser?.timeZone ||
    Intl.DateTimeFormat().resolvedOptions().timeZone ||
    "Asia/Ho_Chi_Minh";

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
      aria-labelledby="enrollment-dialog-title"
      ref={dialogRef}
    >
      <div className="relative w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 pb-4 border-b border-border">
          <div>
            <h2
              id="enrollment-dialog-title"
              className="text-lg font-bold text-foreground"
            >
              Đăng ký lộ trình học
            </h2>
            <p className="text-sm text-muted-foreground mt-0.5 line-clamp-1">
              {curriculumTitle}
            </p>
          </div>
          <Button
            ref={closeButtonRef}
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-muted-foreground hover:text-foreground"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {isPreferencesPending ? (
          <div
            className="space-y-4 py-6"
            role="status"
            aria-label="Đang tải thông tin tùy chọn"
          >
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : (
          <EnrollmentForm
            curriculumId={curriculumId}
            revisionId={revisionId}
            defaultDailyTargetMinutes={defaultDailyTargetMinutes}
            defaultTimeZone={defaultTimeZone}
            onClose={onClose}
          />
        )}
      </div>
    </div>
  );
}
