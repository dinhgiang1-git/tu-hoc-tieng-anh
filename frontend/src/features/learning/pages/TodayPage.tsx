import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  Calendar,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  GraduationCap,
  Headphones,
  PauseCircle,
  Play,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { buttonVariants } from "@/components/ui/button-variants";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/shared/PageHeader";
import { ErrorState } from "@/components/shared/ErrorState";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys } from "@/features/auth";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { todayApi, todayKeys } from "../api/today.api";
import { IpaSoundboardModal } from "../components/IpaSoundboardModal";

function formatTodayDate(dateStr?: string | null): string {
  if (!dateStr) return "";
  const parts = dateStr.split("-").map(Number);
  if (parts.length !== 3 || isNaN(parts[0]) || isNaN(parts[1]) || isNaN(parts[2])) {
    return dateStr;
  }
  const [year, month, day] = parts;
  const d = new Date(year, month - 1, day);
  const weekdayNames = [
    "Chủ Nhật",
    "Thứ Hai",
    "Thứ Ba",
    "Thứ Tư",
    "Thứ Năm",
    "Thứ Sáu",
    "Thứ Bảy",
  ];
  const weekday = weekdayNames[d.getDay()];
  return `${weekday}, ${day} tháng ${month}, ${year}`;
}

export function TodayPage() {
  const queryClient = useQueryClient();
  const [overdueAccordionOpen, setOverdueAccordionOpen] = useState(false);
  const [ipaModalOpen, setIpaModalOpen] = useState(false);

  // 1. Fetch user preferences (to obtain current focusedEnrollmentId)
  const preferencesQuery = useQuery({
    queryKey: authKeys.preferences(),
    queryFn: ({ signal }) => authApi.getPreferences(signal),
  });

  const focusedEnrollmentId = preferencesQuery.data?.focusedEnrollmentId ?? null;

  // 2. Fetch all user enrollments (for switcher dropdown)
  const myEnrollmentsQuery = useQuery({
    queryKey: enrollmentKeys.my(),
    queryFn: ({ signal }) => enrollmentApi.myEnrollments(signal),
  });

  // 3. Fetch Today dashboard data
  const todayQuery = useQuery({
    queryKey: todayKeys.dashboard(focusedEnrollmentId),
    queryFn: ({ signal }) => todayApi.getToday(focusedEnrollmentId, signal),
  });

  // 4. Mutation to change focused enrollment
  const switchEnrollmentMutation = useMutation({
    mutationFn: (newEnrollmentId: string) =>
      authApi.updatePreferences({ focusedEnrollmentId: newEnrollmentId }),
    onSuccess: (updatedPreferences) => {
      queryClient.setQueryData(authKeys.preferences(), updatedPreferences);
      queryClient.invalidateQueries({ queryKey: todayKeys.all });
    },
  });

  // Failed supporting reads must not become an empty enrollment list or a spinner.
  if (preferencesQuery.isError || myEnrollmentsQuery.isError) {
    return (
      <ErrorState
        message={preferencesQuery.isError
          ? "Không thể tải tùy chọn học tập. Vui lòng thử lại."
          : "Không thể tải danh sách lộ trình. Vui lòng thử lại."}
        onRetry={() => {
          if (preferencesQuery.isError) void preferencesQuery.refetch();
          if (myEnrollmentsQuery.isError) void myEnrollmentsQuery.refetch();
        }}
      />
    );
  }

  // Loading state
  if (preferencesQuery.isPending || myEnrollmentsQuery.isPending || todayQuery.isPending) {
    return (
      <div className="space-y-6" role="status" aria-label="Đang tải dữ liệu Hôm nay">
        <div className="space-y-2">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-4 w-72" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_280px] gap-6">
          <Skeleton className="h-64 w-full rounded-2xl" />
          <Skeleton className="h-64 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  // Error state
  if (todayQuery.isError) {
    const error = todayQuery.error;
    const isNotFound = error instanceof ApiError && error.isNotFound();
    return (
      <ErrorState
        message={
          isNotFound
            ? "Không tìm thấy dữ liệu lộ trình học. Vui lòng chọn lại lộ trình hoặc làm mới."
            : "Không thể tải thông tin ngày học hôm nay. Vui lòng thử lại."
        }
        onRetry={() => void todayQuery.refetch()}
      />
    );
  }

  const today = todayQuery.data;
  const enrollments = myEnrollmentsQuery.data ?? [];

  const handleEnrollmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const nextId = e.target.value;
    if (nextId && nextId !== focusedEnrollmentId) {
      switchEnrollmentMutation.mutate(nextId);
    }
  };

  const formattedDate = formatTodayDate(today.todayDate);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pb-5 border-b border-border">
        <PageHeader
          title="Hôm nay"
          description={
            formattedDate
              ? formattedDate
              : today.todayDate
                ? `Kế hoạch học tập ngày ${today.todayDate}`
                : "Bắt đầu bài học theo lộ trình của bạn."
          }
        />

        <div className="flex flex-wrap items-center gap-3 self-start sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIpaModalOpen(true)}
            className="h-9 text-xs gap-1.5 border-border bg-card hover:bg-muted text-foreground font-medium shadow-xs"
            title="Tra cứu & nghe 44 âm ngữ âm IPA"
          >
            <Headphones className="h-4 w-4 text-primary" />
            <span>Bảng IPA</span>
          </Button>

          {enrollments.length > 1 && (
            <div className="flex items-center gap-2">
              <label
                htmlFor="enrollment-switcher"
                className="sr-only"
              >
                Lộ trình:
              </label>
              <select
                id="enrollment-switcher"
                value={today.enrollmentId ?? focusedEnrollmentId ?? ""}
                onChange={handleEnrollmentChange}
                disabled={switchEnrollmentMutation.isPending}
                className="max-w-[min(100%,24rem)] rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                {!today.enrollmentId && !focusedEnrollmentId && (
                  <option value="" disabled>
                    -- Chọn lộ trình học --
                  </option>
                )}
                {enrollments.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.curriculumTitle} ({item.status === "ACTIVE" ? "Đang học" : item.status})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      </div>

      {switchEnrollmentMutation.isError && (
        <div className="rounded-xl border border-destructive/20 bg-destructive/10 p-3.5 text-xs font-medium text-destructive">
          Không thể chuyển đổi lộ trình học. Vui lòng thử lại.
        </div>
      )}

      {/* State: NO_ENROLLMENT */}
      {today.state === "NO_ENROLLMENT" ? (
        <div className="space-y-6">
          <div className="rounded-2xl border border-border bg-card p-6 md:p-8 shadow-xs space-y-5">

            <div className="space-y-2 max-w-2xl">
              <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
                Bạn chưa đăng ký lộ trình học nào
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed">
                Chọn lộ trình để bắt đầu.
              </p>
            </div>

            <div className="pt-2 flex flex-wrap gap-3">
              <Link
                to="/curricula"
                className={buttonVariants({ size: "default", className: "gap-2 shadow-xs font-semibold" })}
              >
                <BookOpen className="h-4 w-4" />
                Khám phá danh mục lộ trình
              </Link>
            </div>
          </div>

          {enrollments.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
              <div>
                <h3 className="text-base font-bold text-foreground">
                  Lộ trình bạn đã đăng ký
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Chọn lộ trình muốn học.
                </p>
              </div>
              <div className="divide-y divide-border border-t border-border pt-2 space-y-3">
                {enrollments.map((item) => (
                  <div
                    key={item.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3"
                  >
                    <div>
                      <h4 className="text-sm font-semibold text-foreground">
                        {item.curriculumTitle}
                      </h4>
                      <p className="text-xs text-muted-foreground">
                        Trạng thái: {item.status === "ACTIVE" ? "Đang học" : item.status} • Tiến độ: {item.progressPercent ?? 0}%
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => switchEnrollmentMutation.mutate(item.id)}
                      disabled={switchEnrollmentMutation.isPending}
                      className={buttonVariants({ size: "sm" })}
                    >
                      Tiếp tục học lộ trình này
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* Active States: 2-Column Desktop Grid */
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_280px] gap-6 items-start">
          {/* Main Action Area (Left Column) */}
          <div className="space-y-6 min-w-0">
            {/* 1. State: PAUSED */}
            {today.state === "PAUSED" && (
              <article
                className="rounded-2xl border border-amber-500/30 bg-card p-6 shadow-xs space-y-4"
                data-testid="today-paused-state"
              >
                <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/15 text-amber-700 dark:text-amber-400 px-3 py-1 text-xs font-semibold">
                  <PauseCircle className="h-3.5 w-3.5" />
                  Đã tạm dừng
                </span>

                <div className="space-y-1.5">
                  <h2 className="text-2xl font-bold text-foreground">
                    Lộ trình học đang tạm dừng
                  </h2>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Tiến độ đã được giữ lại.
                  </p>
                </div>

                <div className="pt-2">
                  <Link
                    to="/schedule"
                    className={buttonVariants({ size: "default", className: "gap-2" })}
                    data-testid="today-resume-cta-button"
                  >
                    <Play className="h-4 w-4" />
                    Xem lịch và tiếp tục học
                  </Link>
                </div>
              </article>
            )}

            {/* 2. State: IN_PROGRESS */}
            {today.state === "IN_PROGRESS" && today.primaryAction && (
              <article className="rounded-2xl border border-border bg-card p-6 md:p-8 shadow-xs space-y-5">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary text-primary px-3 py-1 text-xs font-semibold">
                  <Clock className="h-3.5 w-3.5" />
                  Đang học dở
                </span>

                <div className="space-y-2">
                  <h2 className="text-2xl font-bold text-foreground">
                    {today.primaryAction.lessonTitle || "Bài học đang dở"}
                  </h2>
                </div>

                <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1">
                  {today.primaryAction.estimatedMinutes != null && (
                    <span className="inline-flex items-center gap-1.5">
                      <Clock className="h-4 w-4 text-primary" />
                      Thời lượng: {today.primaryAction.estimatedMinutes} phút
                    </span>
                  )}
                  {today.primaryAction.completedActivities != null &&
                    today.primaryAction.totalActivities != null && (
                      <span className="inline-flex items-center gap-1.5">
                        <CheckCircle2 className="h-4 w-4 text-primary" />
                        Đã hoàn thành {today.primaryAction.completedActivities} /{" "}
                        {today.primaryAction.totalActivities} hoạt động
                      </span>
                    )}
                </div>

                {today.primaryAction.lessonId && today.enrollmentId && (
                  <div className="pt-2">
                    <Link
                      to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                      className={buttonVariants({ size: "lg", className: "gap-2 font-semibold" })}
                    >
                      Tiếp tục học
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  </div>
                )}
              </article>
            )}

            {/* 3. State: DUE_TODAY */}
            {today.state === "DUE_TODAY" && today.primaryAction && (
              <article className="rounded-2xl border border-border bg-card p-6 md:p-8 shadow-xs space-y-5">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary text-primary px-3 py-1 text-xs font-semibold">
                  <Calendar className="h-3.5 w-3.5" />
                  Đến hạn hôm nay
                </span>

                <div className="space-y-2">
                  <h2 className="text-2xl font-bold text-foreground">
                    {today.primaryAction.lessonTitle || "Bài học hôm nay"}
                  </h2>
                </div>

                <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1">
                  {today.primaryAction.estimatedMinutes != null && (
                    <span className="inline-flex items-center gap-1.5">
                      <Clock className="h-4 w-4 text-primary" />
                      Thời lượng: {today.primaryAction.estimatedMinutes} phút
                    </span>
                  )}
                </div>

                {today.primaryAction.lessonId && today.enrollmentId && (
                  <div className="pt-2">
                    <Link
                      to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                      className={buttonVariants({ size: "lg", className: "gap-2 font-semibold" })}
                    >
                      Bắt đầu bài học
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  </div>
                )}
              </article>
            )}

            {/* 4. State: OVERDUE */}
            {today.state === "OVERDUE" && today.primaryAction && (
              <div className="space-y-4">
                <article className="rounded-2xl border border-destructive/30 bg-card p-6 md:p-8 shadow-xs space-y-5">
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-destructive/10 text-destructive px-3 py-1 text-xs font-semibold">
                    <AlertTriangle className="h-3.5 w-3.5" />
                    Bài học quá hạn
                  </span>

                  <div className="space-y-2">
                    <h2 className="text-2xl font-bold text-foreground">
                      {today.primaryAction.lessonTitle || "Bài học quá hạn"}
                    </h2>
                    <p className="text-sm text-muted-foreground leading-relaxed">
                      {today.primaryAction.message}
                    </p>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1">
                    {today.primaryAction.estimatedMinutes != null && (
                      <span className="inline-flex items-center gap-1.5">
                        <Clock className="h-4 w-4 text-destructive" />
                        Thời lượng: {today.primaryAction.estimatedMinutes} phút
                      </span>
                    )}
                  </div>

                  {today.primaryAction.lessonId && today.enrollmentId && (
                    <div className="pt-2">
                      <Link
                        to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                        className={buttonVariants({ size: "lg", className: "gap-2 font-semibold" })}
                      >
                        Học bù ngay
                        <ArrowRight className="h-4 w-4" />
                      </Link>
                    </div>
                  )}
                </article>

                {/* Overdue Lessons Collapsible Accordion (if more than 1 overdue) */}
                {today.overdueLessons.length > 1 && (
                  <div className="rounded-xl border border-border bg-card p-4 space-y-3 shadow-xs">
                    <button
                      type="button"
                      onClick={() => setOverdueAccordionOpen((prev) => !prev)}
                      className="flex w-full items-center justify-between text-sm font-semibold text-foreground hover:text-primary transition-colors"
                      aria-expanded={overdueAccordionOpen}
                    >
                      <span>
                        Các bài học quá hạn khác ({today.overdueLessons.length - 1} bài)
                      </span>
                      {overdueAccordionOpen ? (
                        <ChevronUp className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <ChevronDown className="h-4 w-4 text-muted-foreground" />
                      )}
                    </button>

                    {overdueAccordionOpen && (
                      <ul className="divide-y divide-border border-t border-border pt-2 space-y-2">
                        {today.overdueLessons.slice(1).map((item) => (
                          <li
                            key={item.lessonId}
                            className="flex items-center justify-between pt-2 text-xs"
                          >
                            <div>
                              <span className="font-semibold text-foreground block">
                                {item.lessonTitle}
                              </span>
                              <span className="text-muted-foreground">
                                Lịch: {item.plannedDate} ({item.overdueDays} ngày trước)
                              </span>
                            </div>
                            {today.enrollmentId && (
                              <Link
                                to={`/enrollments/${today.enrollmentId}/lessons/${item.lessonId}`}
                                className={buttonVariants({
                                  variant: "outline",
                                  size: "sm",
                                  className: "h-7 text-xs border-border",
                                })}
                              >
                                Học bù
                              </Link>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* 5. State: REST_DAY */}
            {today.state === "REST_DAY" && (
              <article className="rounded-2xl border border-border bg-card p-6 md:p-8 shadow-xs space-y-5">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary text-primary px-3 py-1 text-xs font-semibold">
                  <Calendar className="h-3.5 w-3.5" />
                  Ngày nghỉ theo lịch
                </span>

                <div className="space-y-2">
                  <h2 className="text-2xl font-bold text-foreground">
                    Hôm nay là ngày nghỉ theo lịch
                  </h2>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Bạn có thể học trước nếu muốn.
                  </p>
                </div>

                {today.primaryAction?.type === "UPCOMING" && today.primaryAction.lessonId && (
                  <div className="pt-2 border-t border-border/70 space-y-3">
                    <div className="space-y-1">
                      <span className="text-xs font-semibold text-primary uppercase tracking-wider block">
                        Gợi ý học trước
                      </span>
                      <h3 className="text-lg font-bold text-foreground">
                        {today.primaryAction.lessonTitle}
                      </h3>
                    </div>

                    {today.enrollmentId && (
                      <Link
                        to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                        className={buttonVariants({ variant: "outline", size: "default", className: "gap-2" })}
                      >
                        Học trước bài tiếp theo
                        <ArrowRight className="h-4 w-4" />
                      </Link>
                    )}
                  </div>
                )}
              </article>
            )}

            {/* 6. State: UPCOMING */}
            {today.state === "UPCOMING" && today.primaryAction && (
              <article className="rounded-2xl border border-border bg-card p-6 md:p-8 shadow-xs space-y-5">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary text-primary px-3 py-1 text-xs font-semibold">
                  <Sparkles className="h-3.5 w-3.5" />
                  Bài học sắp tới
                </span>

                <div className="space-y-2">
                  <h2 className="text-2xl font-bold text-foreground">
                    {today.primaryAction.lessonTitle || "Bài học tiếp theo"}
                  </h2>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    {today.primaryAction.message}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1">
                  {today.primaryAction.estimatedMinutes != null && (
                    <span className="inline-flex items-center gap-1.5">
                      <Clock className="h-4 w-4 text-primary" />
                      Thời lượng: {today.primaryAction.estimatedMinutes} phút
                    </span>
                  )}
                </div>

                {today.primaryAction.lessonId && today.enrollmentId && (
                  <div className="pt-2">
                    <Link
                      to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                      className={buttonVariants({ size: "lg", className: "gap-2 font-semibold" })}
                    >
                      Học trước bài này
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  </div>
                )}
              </article>
            )}

            {/* 7. State: CURRICULUM_COMPLETED */}
            {today.state === "CURRICULUM_COMPLETED" && (
              <article className="rounded-2xl border border-primary/30 bg-card p-6 md:p-8 shadow-xs space-y-5 text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary text-primary">
                  <GraduationCap className="h-8 w-8" />
                </div>
                <div className="space-y-2 max-w-md mx-auto">
                  <h2 className="text-2xl font-bold text-foreground">
                    Chúc mừng! Bạn đã hoàn thành toàn bộ lộ trình học!
                  </h2>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Các nhiệm vụ bắt buộc đã hoàn thành.
                  </p>
                </div>

                <div className="pt-2 flex justify-center gap-3">
                  <Link
                    to="/curricula"
                    className={buttonVariants({ className: "gap-2 font-semibold" })}
                  >
                    <BookOpen className="h-4 w-4" />
                    Khám phá lộ trình khác
                  </Link>
                </div>
              </article>
            )}

          </div>

          {/* Right Rail: Course Summary & Progress */}
          <aside className="space-y-6 shrink-0">
            {today.curriculumTitle && (
              <section className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
                <div>
                  <p className="text-[11px] font-semibold text-primary uppercase tracking-wider">
                    Lộ trình của bạn
                  </p>
                  {enrollments.length <= 1 && (
                    <h3 className="text-base font-semibold text-foreground mt-1">
                      {today.curriculumTitle}
                    </h3>
                  )}
                </div>

                {today.progress && (
                  <div className="space-y-2 pt-2 border-t border-border/70">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span className="font-medium text-foreground">Tiến độ nhiệm vụ</span>
                      <strong className="text-foreground">
                        {today.progress.progressPercent != null
                          ? `${today.progress.progressPercent}%`
                          : "Chưa có dữ liệu"}
                      </strong>
                    </div>
                    {today.progress.progressPercent != null && (
                      <Progress value={today.progress.progressPercent} className="h-2" />
                    )}
                    <p className="text-xs text-muted-foreground">
                      {today.progress.completedRequiredActivities} /{" "}
                      {today.progress.totalRequiredActivities} hoạt động bắt buộc
                    </p>
                  </div>
                )}

                <div className="pt-2 border-t border-border/70 flex items-center justify-between">
                  <Link
                    to="/curricula"
                    className="text-xs font-semibold text-primary hover:underline"
                  >
                    Xem lộ trình học →
                  </Link>
                </div>
              </section>
            )}

          </aside>
        </div>
      )}

      {/* IPA Reference Modal */}
      <IpaSoundboardModal
        isOpen={ipaModalOpen}
        onClose={() => setIpaModalOpen(false)}
      />
    </div>
  );
}
