import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  BookOpen,
  Calendar,
  ChevronDown,
  RotateCw,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { buttonVariants } from "@/components/ui/button-variants";
import { PageHeader } from "@/components/shared/PageHeader";

import { ErrorState } from "@/components/shared/ErrorState";
import { EmptyState } from "@/components/shared/EmptyState";
import { authApi, authKeys } from "@/features/auth";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { analyticsApi, analyticsKeys } from "../api/analytics.api";
import { ProgressMetricCards } from "../components/ProgressMetricCards";
import { WeeklyProgressBarChart } from "../components/WeeklyProgressBarChart";
import { DailyActivityTimeline } from "../components/DailyActivityTimeline";

export function ProgressPage() {
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

  const enrollments = useMemo(
    () => myEnrollmentsQuery.data ?? [],
    [myEnrollmentsQuery.data],
  );
  const focusedEnrollmentId = preferencesQuery.data?.focusedEnrollmentId ?? null;

  // Local state for selected enrollment in progress view
  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<string | null>(null);

  // Resolve target enrollment ID
  const effectiveEnrollmentId = useMemo(() => {
    if (selectedEnrollmentId) return selectedEnrollmentId;
    if (
      focusedEnrollmentId &&
      enrollments.some((e) => e.id === focusedEnrollmentId)
    ) {
      return focusedEnrollmentId;
    }
    return enrollments[0]?.id ?? null;
  }, [selectedEnrollmentId, focusedEnrollmentId, enrollments]);

  // 3. Fetch analytics data
  const analyticsQuery = useQuery({
    queryKey: analyticsKeys.detail(effectiveEnrollmentId ?? ""),
    queryFn: ({ signal }) =>
      analyticsApi.getEnrollmentAnalytics(effectiveEnrollmentId!, signal),
    enabled: !!effectiveEnrollmentId,
    staleTime: 0,
    refetchOnMount: "always",
  });

  const isLoadingEnrollments =
    preferencesQuery.isLoading || myEnrollmentsQuery.isLoading;
  const isLoadingAnalytics = analyticsQuery.isLoading;
  const isRefreshing = analyticsQuery.isFetching && !analyticsQuery.isLoading;

  const analyticsData = analyticsQuery.data;

  return (
    <div className="container mx-auto max-w-6xl space-y-8 py-6 px-4">
      {/* Page Header */}
      <PageHeader
        title="Tiến độ học tập"
        description="Thống kê trực quan tiến trình học tập, biểu đồ theo tuần và lịch sử học tập theo ngày thực tế."
        actions={
          <div className="flex items-center gap-3">
            {enrollments.length > 1 && (
              <div className="relative">
                <select
                  data-testid="enrollment-switcher"
                  className="h-9 appearance-none rounded-lg border border-border bg-card px-3 pr-8 text-xs font-medium text-foreground shadow-sm transition-colors hover:bg-muted/50 focus:outline-none focus:ring-1 focus:ring-primary"
                  value={effectiveEnrollmentId ?? ""}
                  onChange={(e) => setSelectedEnrollmentId(e.target.value)}
                >
                  {enrollments.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.curriculumTitle || "Lộ trình học"} ({item.status})
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              </div>
            )}

            <Button
              variant="outline"
              size="sm"
              onClick={() => analyticsQuery.refetch()}
              disabled={isRefreshing || !effectiveEnrollmentId}
              data-testid="refresh-analytics-button"
              className="gap-1.5"
            >
              <RotateCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin text-primary" : ""}`} />
              <span>{isRefreshing ? "Đang đồng bộ..." : "Làm mới"}</span>
            </Button>
          </div>
        }
      />

      {/* Loading Skeleton */}
      {(isLoadingEnrollments || (isLoadingAnalytics && !analyticsData)) && (
        <div data-testid="analytics-loading" className="space-y-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="h-32 animate-pulse rounded-xl border border-border bg-muted/40"
              />
            ))}
          </div>
          <div className="h-64 animate-pulse rounded-xl border border-border bg-muted/30" />
          <div className="h-48 animate-pulse rounded-xl border border-border bg-muted/20" />
        </div>
      )}

      {/* No Enrollments Empty State */}
      {!isLoadingEnrollments && enrollments.length === 0 && (
        <EmptyState
          title="Bạn chưa đăng ký lộ trình học nào"
          description="Hãy khám phá các lộ trình tiếng Anh phù hợp và đăng ký để bắt đầu theo dõi tiến độ học tập của bạn."
          action={
            <Link
              to="/curricula"
              className={buttonVariants({ variant: "default" })}
            >
              <BookOpen className="mr-2 h-4 w-4" />
              Khám phá lộ trình
            </Link>
          }
        />
      )}

      {/* Error State */}
      {analyticsQuery.isError && (
        <ErrorState
          message="Không thể tải dữ liệu thống kê tiến độ học tập. Vui lòng thử lại sau."
          onRetry={() => analyticsQuery.refetch()}
        />
      )}

      {/* Success Content */}
      {analyticsData && (
        <div className="space-y-8">
          {/* Banner lộ trình hiện tại */}
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border/70 bg-card p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <BookOpen className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-foreground">
                  {analyticsData.curriculumTitle}
                </h2>
                <div className="flex items-center gap-3 text-xs text-muted-foreground mt-0.5">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    Bắt đầu: {analyticsData.startDate}
                  </span>
                  <span>•</span>
                  <span>Múi giờ: {analyticsData.timeZone}</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Link
                to="/today"
                className={buttonVariants({ variant: "outline", size: "sm" })}
              >
                Vào bài học hôm nay
              </Link>
              <Link
                to="/schedule"
                className={buttonVariants({ variant: "ghost", size: "sm" })}
              >
                Xem lịch học
              </Link>
            </div>
          </div>


          {/* KPI Metrics */}
          <ProgressMetricCards
            completedRequiredActivities={analyticsData.completedRequiredActivities}
            totalRequiredActivities={analyticsData.totalRequiredActivities}
            progressPercent={analyticsData.progressPercent}
            completedLessons={analyticsData.completedLessons}
            totalLessons={analyticsData.totalLessons}
            estimatedCompletedMinutes={analyticsData.estimatedCompletedMinutes}
            estimatedTotalMinutes={analyticsData.estimatedTotalMinutes}
            status={analyticsData.status}
            streak={analyticsData.streak}
            quizSummary={analyticsData.quizSummary}
          />

          {/* Biểu đồ thanh tiến độ theo tuần */}
          <WeeklyProgressBarChart weeklyProgress={analyticsData.weeklyProgress} />

          {/* Lịch sử học tập theo ngày thực tế */}
          <DailyActivityTimeline dailyHistory={analyticsData.dailyHistory} />
        </div>
      )}
    </div>
  );
}
