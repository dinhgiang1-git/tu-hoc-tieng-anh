import {
  Award,
  BookOpen,
  CheckCircle2,
  Clock,
  Flame,
  TrendingUp,
} from "lucide-react";
import type {
  QuizSummaryDto,
  StreakStatsDto,
} from "../api/analytics.api";

interface ProgressMetricCardsProps {
  completedRequiredActivities: number;
  totalRequiredActivities: number;
  progressPercent: number | null;
  completedLessons: number;
  totalLessons: number;
  estimatedCompletedMinutes: number;
  estimatedTotalMinutes: number;
  status: string;
  streak: StreakStatsDto;
  quizSummary?: QuizSummaryDto | null;
}

export function ProgressMetricCards({
  completedRequiredActivities,
  totalRequiredActivities,
  progressPercent,
  completedLessons,
  totalLessons,
  estimatedCompletedMinutes,
  estimatedTotalMinutes,
  status,
  streak,
  quizSummary,
}: ProgressMetricCardsProps) {
  const percentValue = progressPercent ?? 0;

  const statusBadge =
    status === "COMPLETED"
      ? { text: "Đã hoàn thành", color: "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300" }
      : status === "PAUSED"
      ? { text: "Đang tạm dừng", color: "bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300" }
      : status === "ABANDONED"
      ? { text: "Đã hủy", color: "bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300" }
      : { text: "Đang học", color: "bg-primary/10 text-primary" };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: Tổng tiến độ */}
        <div
          data-testid="metric-progress"
          className="rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">
              Tiến độ lộ trình
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <TrendingUp className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-3xl font-bold tracking-tight text-foreground">
              {percentValue}%
            </div>
            <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-secondary">
              <div
                className="h-full rounded-full bg-primary transition-all duration-500 ease-out"
                style={{ width: `${Math.min(100, Math.max(0, percentValue))}%` }}
              />
            </div>
            <p className="mt-2.5 text-xs text-muted-foreground">
              {completedRequiredActivities} / {totalRequiredActivities} nhiệm vụ bắt buộc
            </p>
          </div>
        </div>

        {/* Card 2: Streak chuỗi ngày học */}
        <div
          data-testid="metric-streak"
          className="rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">
              Chuỗi ngày học
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500/10 text-amber-500">
              <Flame className="h-5 w-5 fill-amber-500/20" />
            </div>
          </div>
          <div className="mt-3">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold tracking-tight text-foreground">
                {streak.currentStreakDays}
              </span>
              <span className="text-sm font-medium text-muted-foreground">ngày liên tiếp</span>
            </div>
            <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
              <span>Kỷ lục: <strong className="text-foreground">{streak.longestStreakDays} ngày</strong></span>
              <span>Tổng: <strong className="text-foreground">{streak.totalActiveDays} ngày học</strong></span>
            </div>
          </div>
        </div>

        {/* Card 3: Thời lượng nội dung hoàn thành */}
        <div
          data-testid="metric-duration"
          className="rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">
              Thời lượng bài học
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-sky-500/10 text-sky-500">
              <Clock className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold tracking-tight text-foreground">
                {estimatedCompletedMinutes}
              </span>
              <span className="text-sm font-medium text-muted-foreground">phút</span>
            </div>
            <p className="mt-4 text-xs text-muted-foreground">
              trên tổng <strong className="text-foreground">{estimatedTotalMinutes} phút</strong> nội dung toàn lộ trình
            </p>
          </div>
        </div>

        {/* Card 4: Bài học đã hoàn thành */}
        <div
          data-testid="metric-lessons"
          className="rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-muted-foreground">
              Bài học hoàn thành
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-500">
              <BookOpen className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold tracking-tight text-foreground">
                {completedLessons}
              </span>
              <span className="text-sm font-medium text-muted-foreground">
                / {totalLessons} bài
              </span>
            </div>
            <div className="mt-4 flex items-center justify-between">
              <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusBadge.color}`}>
                {statusBadge.text}
              </span>
              {completedLessons === totalLessons && totalLessons > 0 && (
                <span className="flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="h-3.5 w-3.5" /> Hoàn tất
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Thẻ phụ: Kiểm tra trắc nghiệm (nếu có dữ liệu) */}
      {quizSummary && quizSummary.totalQuizzesTaken > 0 && (
        <div
          data-testid="metric-quiz-summary"
          className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border/80 bg-muted/40 px-4 py-3 text-sm"
        >
          <div className="flex items-center gap-2 text-foreground font-medium">
            <Award className="h-4 w-4 text-amber-500" />
            <span>Thành tích Kiểm tra Trắc nghiệm:</span>
          </div>
          <div className="flex items-center gap-4 text-muted-foreground">
            <span>
              Đạt <strong className="text-foreground">{quizSummary.passedQuizzes}</strong> /{" "}
              {quizSummary.totalQuizzesTaken} bài kiểm tra
            </span>
            <span>•</span>
            <span>
              Điểm trung bình:{" "}
              <strong className="text-primary font-semibold">
                {quizSummary.averageScorePercent ?? 0}%
              </strong>
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
