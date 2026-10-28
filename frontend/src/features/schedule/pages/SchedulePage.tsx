import { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  Calendar,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Coffee,
  Globe,
  PauseCircle,
  Play,
  XCircle,
} from "lucide-react";
import { buttonVariants } from "@/components/ui/button-variants";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/shared/PageHeader";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { authApi, authKeys } from "@/features/auth";
import {
  enrollmentApi,
  enrollmentKeys,
  scheduleKeys,
  type DayOfWeek,
} from "@/features/enrollment/api/enrollment.api";
import { RescheduleDialog } from "../components/RescheduleDialog";
import { PauseConfirmationDialog } from "../components/PauseConfirmationDialog";
import { AbandonConfirmationDialog } from "../components/AbandonConfirmationDialog";

const VIETNAMESE_DAYS: Record<DayOfWeek, string> = {
  MONDAY: "Thứ Hai",
  TUESDAY: "Thứ Ba",
  WEDNESDAY: "Thứ Tư",
  THURSDAY: "Thứ Năm",
  FRIDAY: "Thứ Sáu",
  SATURDAY: "Thứ Bảy",
  SUNDAY: "Chủ Nhật",
};

const DAY_NAMES_ORDER: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const STATUS_CONFIG: Record<string, { label: string; className: string }> = {
  ACTIVE: {
    label: "Đang học",
    className:
      "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border-emerald-300",
  },
  PAUSED: {
    label: "Tạm dừng",
    className:
      "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300 border-amber-300",
  },
  COMPLETED: {
    label: "Đã hoàn thành",
    className:
      "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border-blue-300",
  },
  ABANDONED: {
    label: "Đã kết thúc",
    className:
      "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300 border-gray-300",
  },
};

/** Formats a Date object to "YYYY-MM-DD" */
function toIsoDate(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/** Formats "YYYY-MM-DD" to Vietnamese display "DD/MM/YYYY" */
function formatDateVn(iso: string): string {
  if (!iso) return "";
  const parts = iso.split("-");
  if (parts.length !== 3) return iso;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

/** Gets the Monday Date of the week containing the given date */
function getMondayOfWeek(d: Date): Date {
  const date = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const day = date.getDay(); // 0 is Sunday, 1 is Monday, ..., 6 is Saturday
  const diff = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + diff);
  return date;
}

/** Adds n days to a date */
function addDays(d: Date, n: number): Date {
  const result = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  result.setDate(result.getDate() + n);
  return result;
}

export function SchedulePage() {
  // 1. Fetch user preferences (for default focused enrollment)
  const preferencesQuery = useQuery({
    queryKey: authKeys.preferences(),
    queryFn: ({ signal }) => authApi.getPreferences(signal),
  });

  // 2. Fetch all user enrollments (for switcher dropdown)
  const myEnrollmentsQuery = useQuery({
    queryKey: enrollmentKeys.my(),
    queryFn: ({ signal }) => enrollmentApi.myEnrollments(signal),
  });

  // Local state for week navigation (offset in weeks: 0 = current week, -1 = prev, +1 = next)
  const [weekOffset, setWeekOffset] = useState(0);

  // Local state for selected enrollment in this view (does not mutate preferences)
  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<string | null>(null);

  // Reschedule and lifecycle dialog states
  const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);
  const [rescheduleMode, setRescheduleMode] = useState<"reschedule" | "resume">("reschedule");
  const [isPauseOpen, setIsPauseOpen] = useState(false);
  const [isAbandonOpen, setIsAbandonOpen] = useState(false);

  const enrollments = useMemo(
    () => myEnrollmentsQuery.data ?? [],
    [myEnrollmentsQuery.data],
  );
  const focusedEnrollmentId = preferencesQuery.data?.focusedEnrollmentId ?? null;

  // Compute active enrollment ID
  const activeEnrollmentId = useMemo(() => {
    if (selectedEnrollmentId && enrollments.some((e) => e.id === selectedEnrollmentId)) {
      return selectedEnrollmentId;
    }
    if (focusedEnrollmentId && enrollments.some((e) => e.id === focusedEnrollmentId)) {
      return focusedEnrollmentId;
    }
    return enrollments[0]?.id ?? null;
  }, [selectedEnrollmentId, enrollments, focusedEnrollmentId]);

  const activeEnrollment = useMemo(
    () => enrollments.find((e) => e.id === activeEnrollmentId) ?? null,
    [enrollments, activeEnrollmentId],
  );

  // 3. Fetch detailed enrollment info (for estimatedEndDate, studyDays, timeZone, status)
  const enrollmentDetailQuery = useQuery({
    queryKey: enrollmentKeys.detail(activeEnrollmentId ?? ""),
    queryFn: ({ signal }) => enrollmentApi.getDetail(activeEnrollmentId!, signal),
    enabled: Boolean(activeEnrollmentId),
  });

  // Calculate Monday and Sunday of the selected week
  const [currentDate] = useState(() => new Date());
  const timeZone = enrollmentDetailQuery.data?.timeZone ?? "UTC";
  const todayIso = useMemo(() => {
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone, year: "numeric", month: "2-digit", day: "2-digit",
    }).formatToParts(currentDate);
    const part = (type: string) => parts.find(p => p.type === type)?.value;
    return `${part("year")}-${part("month")}-${part("day")}`;
  }, [currentDate, timeZone]);

  const { from, to, daysOfWeek } = useMemo(() => {
    const currentMonday = getMondayOfWeek(new Date(`${todayIso}T12:00:00`));
    const targetMonday = addDays(currentMonday, weekOffset * 7);
    const fromStr = toIsoDate(targetMonday);
    const toStr = toIsoDate(addDays(targetMonday, 6));

    const days = DAY_NAMES_ORDER.map((dayName, idx) => {
      const dayDate = addDays(targetMonday, idx);
      const iso = toIsoDate(dayDate);
      return {
        dayName,
        labelVn: VIETNAMESE_DAYS[dayName],
        date: dayDate,
        isoDate: iso,
        dateVn: formatDateVn(iso),
        isToday: iso === todayIso,
      };
    });

    return {
      from: fromStr,
      to: toStr,
      daysOfWeek: days,
    };
  }, [todayIso, weekOffset]);

  // 4. Fetch schedule items for the selected week
  const scheduleQuery = useQuery({
    queryKey: scheduleKeys.list(activeEnrollmentId ?? "", from, to),
    queryFn: ({ signal }) =>
      enrollmentApi.getSchedule(activeEnrollmentId!, { from, to }, signal),
    enabled: Boolean(activeEnrollmentId) && enrollmentDetailQuery.isSuccess,
  });

  // Loading initial user data
  if (preferencesQuery.isPending || myEnrollmentsQuery.isPending) {
    return (
      <div className="space-y-6" role="status" aria-label="Đang tải dữ liệu lịch học">
        <PageHeader title="Lịch học" description="Theo dõi lịch học và tiến độ theo từng tuần." />
        <Skeleton className="h-32 w-full rounded-2xl" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    );
  }

  if (myEnrollmentsQuery.isError) {
    return <ErrorState message="Không thể tải danh sách lộ trình. Vui lòng thử lại."
      onRetry={() => void myEnrollmentsQuery.refetch()} />;
  }

  // Empty state: User has no enrollments
  if (enrollments.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title="Lịch học" description="Theo dõi lịch học và tiến độ theo từng tuần." />
        <EmptyState
          icon={Calendar}
          title="Bạn chưa đăng ký lộ trình nào"
          description="Hãy chọn một lộ trình phù hợp để bắt đầu học tập và theo dõi lịch học cá nhân."
          action={
            <Link to="/curricula" className={buttonVariants({ variant: "default" })}>
              <BookOpen className="mr-2 h-4 w-4" />
              Khám phá lộ trình
            </Link>
          }
        />
      </div>
    );
  }

  const enrollmentDetail = enrollmentDetailQuery.data;
  const statusInfo = STATUS_CONFIG[enrollmentDetail?.status ?? "ACTIVE"] ?? STATUS_CONFIG.ACTIVE;

  // Format study days in Vietnamese
  const studyDaysList = enrollmentDetail?.studyDays ?? [];
  const studyDaysFormatted = studyDaysList
    .map((day) => VIETNAMESE_DAYS[day] ?? day)
    .join(", ");

  const schedules = scheduleQuery.data ?? [];

  return (
    <div className="space-y-6">
      {/* Top Header & Switcher */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          title="Lịch học"
          description="Kế hoạch học tập hàng tuần theo lịch đăng ký cá nhân."
        />

        {enrollments.length > 1 && (
          <div className="flex items-center gap-2">
            <label
              htmlFor="schedule-enrollment-select"
              className="text-xs font-medium text-muted-foreground whitespace-nowrap"
            >
              Chọn lộ trình:
            </label>
            <select
              id="schedule-enrollment-select"
              data-testid="schedule-enrollment-select"
              className="h-9 rounded-md border border-input bg-card px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              value={activeEnrollmentId ?? ""}
              onChange={(e) => setSelectedEnrollmentId(e.target.value)}
              aria-label="Chọn lộ trình xem lịch"
            >
              {enrollments.map((enr) => (
                <option key={enr.id} value={enr.id}>
                  {enr.curriculumTitle}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Enrollment Information Banner */}
      {enrollmentDetailQuery.isPending ? (
        <Skeleton className="h-32 w-full rounded-2xl" />
      ) : enrollmentDetailQuery.isError ? (
        <ErrorState
          message="Không thể tải thông tin chi tiết lộ trình học."
          onRetry={() => void enrollmentDetailQuery.refetch()}
        />
      ) : enrollmentDetail ? (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-foreground">
                {activeEnrollment?.curriculumTitle ?? "Lộ trình học"}
              </h2>
              <span
                className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${statusInfo.className}`}
              >
                {statusInfo.label}
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {enrollmentDetail.status === "ACTIVE" && (
                <>
                  <button
                    type="button"
                    data-testid="schedule-open-reschedule-button"
                    onClick={() => {
                      setRescheduleMode("reschedule");
                      setIsRescheduleOpen(true);
                    }}
                    className={buttonVariants({ variant: "outline", size: "sm" })}
                  >
                    <Calendar className="mr-1.5 h-3.5 w-3.5 text-primary" />
                    Đổi lịch
                  </button>
                  <button
                    type="button"
                    data-testid="schedule-pause-button"
                    onClick={() => setIsPauseOpen(true)}
                    className={buttonVariants({ variant: "outline", size: "sm" })}
                  >
                    <PauseCircle className="mr-1.5 h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />
                    Tạm dừng
                  </button>
                  <button
                    type="button"
                    data-testid="schedule-abandon-button"
                    onClick={() => setIsAbandonOpen(true)}
                    className={buttonVariants({ variant: "ghost", size: "sm" })}
                  >
                    <XCircle className="mr-1.5 h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
                    Kết thúc
                  </button>
                </>
              )}

              {enrollmentDetail.status === "PAUSED" && (
                <>
                  <button
                    type="button"
                    data-testid="schedule-resume-button"
                    onClick={() => {
                      setRescheduleMode("resume");
                      setIsRescheduleOpen(true);
                    }}
                    className={buttonVariants({ variant: "default", size: "sm" })}
                  >
                    <Play className="mr-1.5 h-3.5 w-3.5" />
                    Tiếp tục học
                  </button>
                  <button
                    type="button"
                    data-testid="schedule-abandon-button"
                    onClick={() => setIsAbandonOpen(true)}
                    className={buttonVariants({ variant: "ghost", size: "sm" })}
                  >
                    <XCircle className="mr-1.5 h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
                    Kết thúc
                  </button>
                </>
              )}

              <div className="flex items-center gap-2 text-xs text-muted-foreground ml-1">
                <Globe className="h-3.5 w-3.5" />
                <span>Múi giờ: {enrollmentDetail.timeZone}</span>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-y-2 gap-x-6 text-sm text-muted-foreground pt-1 border-t border-border/60">
            <div>
              <span className="font-medium text-foreground">Ngày học trong tuần: </span>
              <span>{studyDaysFormatted || "Chưa thiết lập"}</span>
            </div>

            <div>
              <span className="font-medium text-foreground">Kết thúc theo lịch hiện tại: </span>
              <span className="text-foreground font-semibold">
                {enrollmentDetail.estimatedEndDate
                  ? formatDateVn(enrollmentDetail.estimatedEndDate)
                  : "Chưa có lịch"}
              </span>
              {enrollmentDetail.status === "PAUSED" && (
                <span className="ml-1.5 text-xs text-amber-600 dark:text-amber-400">
                  (Lộ trình đang tạm dừng)
                </span>
              )}
              {enrollmentDetail.status === "ABANDONED" && (
                <span className="ml-1.5 text-xs text-muted-foreground">
                  (Lộ trình đã kết thúc)
                </span>
              )}
            </div>
          </div>

          {enrollmentDetail.status === "PAUSED" && (
            <div
              className="flex items-center gap-2.5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-200"
              data-testid="schedule-paused-banner"
            >
              <PauseCircle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
              <p>
                <strong>Lộ trình đang tạm dừng:</strong> Tiến độ học được giữ nguyên, hệ thống không tính bài quá hạn. Bấm <strong>"Tiếp tục học"</strong> để chọn ngày học lại và xếp lịch cho các bài học chưa hoàn thành.
              </p>
            </div>
          )}
        </div>
      ) : null}

      {/* Week Navigation Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card p-4 shadow-sm">
        <div className="flex items-center gap-2">
          <button
            type="button"
            data-testid="schedule-prev-week"
            onClick={() => setWeekOffset((prev) => prev - 1)}
            className={buttonVariants({ variant: "outline", size: "sm" })}
            aria-label="Tuần trước"
          >
            <ChevronLeft className="mr-1 h-4 w-4" />
            Tuần trước
          </button>

          <button
            type="button"
            data-testid="schedule-today"
            onClick={() => setWeekOffset(0)}
            className={buttonVariants({
              variant: weekOffset === 0 ? "secondary" : "outline",
              size: "sm",
            })}
            aria-label="Tuần này"
          >
            <Calendar className="mr-1 h-4 w-4" />
            Tuần này
          </button>

          <button
            type="button"
            data-testid="schedule-next-week"
            onClick={() => setWeekOffset((prev) => prev + 1)}
            className={buttonVariants({ variant: "outline", size: "sm" })}
            aria-label="Tuần sau"
          >
            Tuần sau
            <ChevronRight className="ml-1 h-4 w-4" />
          </button>
        </div>

        <div className="text-sm font-semibold text-foreground">
          Khoảng ngày:{" "}
          <span data-testid="schedule-week-range" className="font-bold text-primary">
            {formatDateVn(from)} - {formatDateVn(to)}
          </span>
        </div>
      </div>

      {/* Schedule 7-Day Content */}
      {scheduleQuery.isPending ? (
        <div className="grid gap-3 grid-cols-1 md:grid-cols-2 lg:grid-cols-7" role="status" aria-label="Đang tải lịch các ngày">
          {Array.from({ length: 7 }).map((_, i) => (
            <Skeleton key={i} className="h-64 rounded-xl" />
          ))}
        </div>
      ) : scheduleQuery.isError ? (
        <ErrorState
          message="Không thể tải lịch học cho khoảng ngày này. Vui lòng thử lại."
          onRetry={() => void scheduleQuery.refetch()}
        />
      ) : (
        <div className="grid gap-3 grid-cols-1 md:grid-cols-2 lg:grid-cols-7">
          {daysOfWeek.map((day) => {
            const isStudyDay = studyDaysList.includes(day.dayName);
            const dayLessons = schedules.filter((s) => s.plannedDate === day.isoDate);

            return (
              <div
                key={day.isoDate}
                className={`flex flex-col rounded-xl border bg-card p-3 shadow-sm transition-all ${
                  day.isToday
                    ? "border-primary ring-1 ring-primary/30 bg-primary/[0.02]"
                    : "border-border"
                }`}
              >
                {/* Day Header */}
                <div className="flex items-center justify-between border-b border-border/60 pb-2 mb-3">
                  <div>
                    <span className="text-xs font-semibold text-foreground uppercase tracking-wider block">
                      {day.labelVn}
                    </span>
                    <span className="text-xs text-muted-foreground">{day.dateVn}</span>
                  </div>

                  {day.isToday && (
                    <span className="rounded bg-primary px-1.5 py-0.5 text-[10px] font-bold text-primary-foreground">
                      Hôm nay
                    </span>
                  )}
                </div>

                {/* Day Content */}
                <div className="flex-1 flex flex-col gap-2">
                  {dayLessons.length > 0 ? (
                    dayLessons.map((lesson) => (
                      <div
                        key={lesson.lessonId}
                        className="flex flex-col justify-between gap-3 rounded-lg border border-border/80 bg-background/80 p-3 shadow-xs hover:border-border transition-colors"
                      >
                        <div className="space-y-1.5">
                          {/* Status Badge */}
                          <div>
                            {lesson.isCompleted ? (
                              <span
                                data-testid="badge-completed"
                                className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                              >
                                <CheckCircle2 className="h-3 w-3" />
                                Đã hoàn thành
                              </span>
                            ) : lesson.isOverdue ? (
                              <span
                                data-testid="badge-overdue"
                                className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300"
                              >
                                <AlertTriangle className="h-3 w-3" />
                                Quá hạn
                              </span>
                            ) : (
                              <span
                                data-testid="badge-scheduled"
                                className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300"
                              >
                                <Clock className="h-3 w-3" />
                                Theo lịch
                              </span>
                            )}
                          </div>

                          {/* Lesson Title */}
                          <h4
                            className="font-medium text-xs leading-snug text-foreground line-clamp-2"
                            title={lesson.lessonTitle}
                          >
                            {lesson.lessonTitle}
                          </h4>

                          {/* Original Date (if rescheduled) */}
                          {lesson.originalDate &&
                            lesson.originalDate !== lesson.plannedDate && (
                              <p className="text-[10px] text-muted-foreground">
                                Lịch gốc: {formatDateVn(lesson.originalDate)}
                              </p>
                            )}
                        </div>

                        {/* Action Link to Workspace */}
                        <Link
                          to={`/enrollments/${activeEnrollmentId}/lessons/${lesson.lessonId}`}
                          data-testid={`lesson-link-${lesson.lessonId}`}
                          className={buttonVariants({
                            variant: lesson.isCompleted ? "outline" : "default",
                            size: "sm",
                            className: "h-7 text-xs w-full justify-center mt-1",
                          })}
                        >
                          {lesson.isCompleted ? "Xem lại" : "Vào bài học"}
                          <ArrowRight className="ml-1 h-3 w-3" />
                        </Link>
                      </div>
                    ))
                  ) : !isStudyDay ? (
                    <div
                      data-testid="rest-day-indicator"
                      className="flex-1 flex flex-col items-center justify-center p-3 rounded-lg border border-dashed border-border/70 text-center text-muted-foreground bg-muted/20 min-h-[90px]"
                    >
                      <Coffee className="h-4 w-4 mb-1 text-muted-foreground/70" />
                      <span className="text-[11px] font-medium">Ngày nghỉ theo lịch</span>
                    </div>
                  ) : (
                    <div className="flex-1 flex flex-col items-center justify-center p-3 rounded-lg border border-dashed border-border/60 text-center text-muted-foreground/70 bg-muted/10 min-h-[90px]">
                      <span className="text-[11px]">Không có bài học</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Reschedule / Resume Dialog Modal */}
      {enrollmentDetail && activeEnrollmentId && (
        <RescheduleDialog
          isOpen={isRescheduleOpen}
          onClose={() => setIsRescheduleOpen(false)}
          enrollmentId={activeEnrollmentId}
          curriculumTitle={activeEnrollment?.curriculumTitle ?? "Lộ trình học"}
          defaultDailyTargetMinutes={enrollmentDetail.dailyTargetMinutes}
          defaultStudyDays={enrollmentDetail.studyDays}
          timeZone={enrollmentDetail.timeZone}
          mode={rescheduleMode}
        />
      )}

      {/* Pause Confirmation Dialog Modal */}
      {enrollmentDetail && activeEnrollmentId && (
        <PauseConfirmationDialog
          isOpen={isPauseOpen}
          onClose={() => setIsPauseOpen(false)}
          enrollmentId={activeEnrollmentId}
          expectedVersion={enrollmentDetail.version}
          curriculumTitle={activeEnrollment?.curriculumTitle ?? "Lộ trình học"}
        />
      )}

      {/* Abandon Confirmation Dialog Modal */}
      {enrollmentDetail && activeEnrollmentId && (
        <AbandonConfirmationDialog
          isOpen={isAbandonOpen}
          onClose={() => setIsAbandonOpen(false)}
          enrollmentId={activeEnrollmentId}
          expectedVersion={enrollmentDetail.version}
          curriculumTitle={activeEnrollment?.curriculumTitle ?? "Lộ trình học"}
        />
      )}
    </div>
  );
}
