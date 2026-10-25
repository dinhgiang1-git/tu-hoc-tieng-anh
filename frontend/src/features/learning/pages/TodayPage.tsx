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
  Compass,
  GraduationCap,
  PauseCircle,
  Sparkles,
} from "lucide-react";
import { buttonVariants } from "@/components/ui/button-variants";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/shared/PageHeader";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys } from "@/features/auth";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { todayApi, todayKeys } from "../api/today.api";

export function TodayPage() {
  const queryClient = useQueryClient();
  const [overdueAccordionOpen, setOverdueAccordionOpen] = useState(false);

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

  // Loading state
  if (preferencesQuery.isPending || todayQuery.isPending) {
    return (
      <div className="space-y-6" role="status" aria-label="Đang tải dữ liệu Hôm nay">
        <div className="space-y-2">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-48 w-full rounded-2xl" />
        <Skeleton className="h-32 w-full rounded-2xl" />
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

  return (
    <div className="space-y-6">
      {/* Page Header with Enrollment Switcher */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-border">
        <PageHeader
          title="Hôm nay"
          description={
            today.todayDate
              ? `Kế hoạch học tập ngày ${today.todayDate}`
              : "Bắt đầu bài học theo lộ trình của bạn."
          }
        />

        {enrollments.length >= 1 && (
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <label
              htmlFor="enrollment-switcher"
              className="text-xs font-medium text-muted-foreground whitespace-nowrap"
            >
              Lộ trình:
            </label>
            <select
              id="enrollment-switcher"
              value={today.enrollmentId ?? focusedEnrollmentId ?? ""}
              onChange={handleEnrollmentChange}
              disabled={switchEnrollmentMutation.isPending}
              className="rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground shadow-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              {!today.enrollmentId && !focusedEnrollmentId && (
                <option value="" disabled>
                  -- Chọn lộ trình học --
                </option>
              )}
              {enrollments.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.curriculumTitle} ({item.status})
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {switchEnrollmentMutation.isError && (
        <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-4 text-xs font-medium text-destructive">
          Không thể chuyển đổi lộ trình học. Vui lòng thử lại.
        </div>
      )}

      {/* 1. State: NO_ENROLLMENT */}
      {today.state === "NO_ENROLLMENT" && (
        <div className="space-y-6">
          <EmptyState
            title="Bạn chưa đăng ký lộ trình học nào"
            description={
              enrollments.length > 0
                ? "Hoặc bạn chưa chọn lộ trình ưu tiên trong các lộ trình đã đăng ký bên dưới."
                : "Khám phá các lộ trình học phát âm Anh-Anh (RP) chuẩn quốc tế và đăng ký ngay để xây dựng thói quen học tập mỗi ngày."
            }
            icon={Compass}
            action={
              <Link
                to="/curricula"
                className={buttonVariants({ className: "gap-2" })}
              >
                <BookOpen className="h-4 w-4" />
                Khám phá danh mục lộ trình
              </Link>
            }
          />

          {enrollments.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-4">
              <div>
                <h3 className="text-base font-bold text-foreground">
                  Lộ trình bạn đã đăng ký
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Chọn một lộ trình để kích hoạt và bắt đầu học ngay hôm nay:
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
                        Trạng thái: {item.status} • Tiến độ: {item.progressPercent ?? 0}%
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
      )}

      {/* Active Enrollment Header Banner & Progress */}
      {today.curriculumTitle && (
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <span className="text-xs font-medium text-primary uppercase tracking-wider">
                Lộ trình đang học
              </span>
              <h2 className="text-lg font-bold text-foreground">
                {today.curriculumTitle}
              </h2>
            </div>
            {today.isRestDay && (
              <span className="inline-flex items-center gap-1.5 self-start sm:self-auto rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
                <Calendar className="h-3.5 w-3.5" />
                Hôm nay là ngày nghỉ
              </span>
            )}
          </div>

          {/* Progress Summary */}
          {today.progress && (
            <div className="space-y-2 pt-2 border-t border-border">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span className="font-medium text-foreground">
                  Tiến độ lộ trình:{" "}
                  <strong>
                    {today.progress.progressPercent != null
                      ? `${today.progress.progressPercent}%`
                      : "0%"}
                  </strong>
                </span>
                <span>
                  {today.progress.completedRequiredActivities} /{" "}
                  {today.progress.totalRequiredActivities} hoạt động bắt buộc
                </span>
              </div>
              <Progress value={today.progress.progressPercent ?? 0} />
            </div>
          )}
        </div>
      )}

      {/* 2. State: PAUSED */}
      {today.state === "PAUSED" && (
        <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-6 shadow-sm space-y-3">
          <div className="flex items-center gap-3">
            <PauseCircle className="h-7 w-7 text-amber-600 dark:text-amber-400 shrink-0" />
            <div>
              <h3 className="text-base font-bold text-amber-900 dark:text-amber-200">
                Lộ trình học đang tạm dừng
              </h3>
              <p className="text-xs text-amber-800 dark:text-amber-300 mt-1">
                Lộ trình này hiện đang ở trạng thái tạm dừng. Bạn không có bài học cần thực
                hiện hôm nay. Tính năng tiếp tục lộ trình và điều chỉnh lịch linh hoạt sẽ
                được hỗ trợ trong Giai đoạn G4.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* 3. State: IN_PROGRESS */}
      {today.state === "IN_PROGRESS" && today.primaryAction && (
        <div className="rounded-2xl border border-primary/30 bg-gradient-to-br from-primary/10 via-card to-card p-6 shadow-sm space-y-5">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/20 px-2.5 py-0.5 text-xs font-semibold text-primary">
                <Clock className="h-3.5 w-3.5" />
                Đang học dở
              </span>
              <h3 className="text-xl font-bold text-foreground">
                {today.primaryAction.lessonTitle || "Bài học đang dở"}
              </h3>
              <p className="text-sm text-muted-foreground">
                {today.primaryAction.message}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
            {today.primaryAction.estimatedMinutes != null && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-primary" />
                Thời lượng: {today.primaryAction.estimatedMinutes} phút
              </span>
            )}
            {today.primaryAction.completedActivities != null &&
              today.primaryAction.totalActivities != null && (
                <span className="inline-flex items-center gap-1">
                  <CheckCircle2 className="h-3.5 w-3.5 text-primary" />
                  Đã hoàn thành {today.primaryAction.completedActivities} /{" "}
                  {today.primaryAction.totalActivities} hoạt động
                </span>
              )}
          </div>

          {today.primaryAction.lessonId && today.enrollmentId && (
            <div>
              <Link
                to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                className={buttonVariants({ size: "lg", className: "gap-2" })}
              >
                Tiếp tục học
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          )}
        </div>
      )}

      {/* 4. State: DUE_TODAY */}
      {today.state === "DUE_TODAY" && today.primaryAction && (
        <div className="rounded-2xl border border-primary/30 bg-gradient-to-br from-primary/10 via-card to-card p-6 shadow-sm space-y-5">
          <div className="space-y-1">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/20 px-2.5 py-0.5 text-xs font-semibold text-primary">
              <Calendar className="h-3.5 w-3.5" />
              Đến hạn hôm nay
            </span>
            <h3 className="text-xl font-bold text-foreground">
              {today.primaryAction.lessonTitle || "Bài học hôm nay"}
            </h3>
            <p className="text-sm text-muted-foreground">
              {today.primaryAction.message}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
            {today.primaryAction.estimatedMinutes != null && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-primary" />
                Thời lượng: {today.primaryAction.estimatedMinutes} phút
              </span>
            )}
          </div>

          {today.primaryAction.lessonId && today.enrollmentId && (
            <div>
              <Link
                to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                className={buttonVariants({ size: "lg", className: "gap-2" })}
              >
                Bắt đầu bài học
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          )}
        </div>
      )}

      {/* 5. State: OVERDUE */}
      {today.state === "OVERDUE" && today.primaryAction && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-500/30 bg-gradient-to-br from-amber-500/10 via-card to-card p-6 shadow-sm space-y-5">
            <div className="space-y-1">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/20 px-2.5 py-0.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
                <AlertTriangle className="h-3.5 w-3.5" />
                Bài học quá hạn
              </span>
              <h3 className="text-xl font-bold text-foreground">
                {today.primaryAction.lessonTitle || "Bài học quá hạn"}
              </h3>
              <p className="text-sm text-muted-foreground">
                {today.primaryAction.message}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
              {today.primaryAction.estimatedMinutes != null && (
                <span className="inline-flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5 text-amber-600" />
                  Thời lượng: {today.primaryAction.estimatedMinutes} phút
                </span>
              )}
            </div>

            {today.primaryAction.lessonId && today.enrollmentId && (
              <div>
                <Link
                  to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                  className={buttonVariants({ size: "lg", className: "gap-2" })}
                >
                  Học bù ngay
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            )}
          </div>

          {/* Overdue Lessons Collapsible Accordion (if more than 1 overdue) */}
          {today.overdueLessons.length > 1 && (
            <div className="rounded-xl border border-border bg-card p-4 space-y-3">
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
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
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
                            className: "h-7 text-xs",
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

      {/* 6. State: REST_DAY */}
      {today.state === "REST_DAY" && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-border bg-gradient-to-br from-muted/50 via-card to-card p-6 shadow-sm space-y-3">
            <div className="flex items-center gap-3">
              <Calendar className="h-6 w-6 text-primary shrink-0" />
              <div>
                <h3 className="text-base font-bold text-foreground">
                  Hôm nay là ngày nghỉ theo lịch
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Bạn không có bài học nào đến hạn hôm nay. Hãy nghỉ ngơi để nạp năng
                  lượng, hoặc bạn có thể học trước bài tiếp theo bên dưới.
                </p>
              </div>
            </div>
          </div>

          {/* Optional upcoming lesson suggestion on rest day */}
          {today.primaryAction?.type === "UPCOMING" && today.primaryAction.lessonId && (
            <div className="rounded-2xl border border-primary/20 bg-card p-6 shadow-sm space-y-4">
              <div className="space-y-1">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                  <Sparkles className="h-3.5 w-3.5" />
                  Học trước
                </span>
                <h4 className="text-base font-bold text-foreground">
                  {today.primaryAction.lessonTitle}
                </h4>
                <p className="text-xs text-muted-foreground">
                  {today.primaryAction.message}
                </p>
              </div>

              {today.enrollmentId && (
                <Link
                  to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                  className={buttonVariants({ variant: "outline", className: "gap-2" })}
                >
                  Học trước bài tiếp theo
                  <ArrowRight className="h-4 w-4" />
                </Link>
              )}
            </div>
          )}
        </div>
      )}

      {/* 7. State: UPCOMING */}
      {today.state === "UPCOMING" && today.primaryAction && (
        <div className="rounded-2xl border border-primary/30 bg-gradient-to-br from-primary/5 via-card to-card p-6 shadow-sm space-y-5">
          <div className="space-y-1">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/15 px-2.5 py-0.5 text-xs font-semibold text-primary">
              <Sparkles className="h-3.5 w-3.5" />
              Bài học sắp tới
            </span>
            <h3 className="text-xl font-bold text-foreground">
              {today.primaryAction.lessonTitle || "Bài học tiếp theo"}
            </h3>
            <p className="text-sm text-muted-foreground">
              {today.primaryAction.message}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
            {today.primaryAction.estimatedMinutes != null && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-primary" />
                Thời lượng: {today.primaryAction.estimatedMinutes} phút
              </span>
            )}
          </div>

          {today.primaryAction.lessonId && today.enrollmentId && (
            <div>
              <Link
                to={`/enrollments/${today.enrollmentId}/lessons/${today.primaryAction.lessonId}`}
                className={buttonVariants({ size: "lg", className: "gap-2" })}
              >
                Học trước bài này
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          )}
        </div>
      )}

      {/* 8. State: CURRICULUM_COMPLETED */}
      {today.state === "CURRICULUM_COMPLETED" && (
        <div className="rounded-2xl border border-emerald-500/30 bg-gradient-to-br from-emerald-500/10 via-card to-card p-8 shadow-sm text-center space-y-4">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/20 text-emerald-600 dark:text-emerald-400">
            <GraduationCap className="h-8 w-8" />
          </div>
          <div className="space-y-2 max-w-md mx-auto">
            <h3 className="text-xl font-bold text-foreground">
              Chúc mừng! Bạn đã hoàn thành toàn bộ lộ trình học!
            </h3>
            <p className="text-sm text-muted-foreground">
              Bạn đã hoàn thành xuất sắc tất cả các bài học và hoạt động bắt buộc trong lộ
              trình này. Hãy tiếp tục khám phá các lộ trình mới để nâng cao kỹ năng!
            </p>
          </div>

          <div className="pt-2">
            <Link
              to="/curricula"
              className={buttonVariants({ className: "gap-2" })}
            >
              <BookOpen className="h-4 w-4" />
              Khám phá lộ trình khác
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
