import { BarChart3, CheckCircle2, Clock, ListChecks } from "lucide-react";
import type { WeeklyProgressItemDto } from "../api/analytics.api";

interface WeeklyProgressBarChartProps {
  weeklyProgress: WeeklyProgressItemDto[];
}

export function WeeklyProgressBarChart({
  weeklyProgress,
}: WeeklyProgressBarChartProps) {
  if (!weeklyProgress || weeklyProgress.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-6 text-center shadow-sm">
        <BarChart3 className="mx-auto h-8 w-8 text-muted-foreground opacity-50" />
        <p className="mt-2 text-sm text-muted-foreground">
          Chưa có dữ liệu tiến độ theo tuần.
        </p>
      </div>
    );
  }

  return (
    <div
      data-testid="weekly-progress-chart"
      className="rounded-xl border border-border bg-card p-6 shadow-sm"
    >
      <div className="flex items-center justify-between border-b border-border/60 pb-4">
        <div>
          <h3 className="text-base font-semibold text-foreground">
            Tiến độ Theo Tuần Lộ trình
          </h3>
          <p className="text-xs text-muted-foreground">
            Tỷ lệ hoàn thành nhiệm vụ bắt buộc của từng tuần
          </p>
        </div>
        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
            <span className="text-muted-foreground">Hoàn thành</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-primary" />
            <span className="text-muted-foreground">Đang học</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-muted-foreground/30" />
            <span className="text-muted-foreground">Chưa học</span>
          </div>
        </div>
      </div>

      <div className="mt-6 space-y-5">
        {weeklyProgress.map((week) => {
          const percent = week.progressPercent ?? 0;
          const isDone = week.status === "COMPLETED";
          const inProgress = week.status === "IN_PROGRESS";

          const barColor = isDone
            ? "bg-emerald-500"
            : inProgress
            ? "bg-primary"
            : "bg-muted-foreground/30";

          const statusBadge = isDone ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300">
              <CheckCircle2 className="h-3 w-3" /> Đã hoàn thành
            </span>
          ) : inProgress ? (
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              Đang học ({percent}%)
            </span>
          ) : (
            <span className="inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
              Chưa bắt đầu
            </span>
          );

          return (
            <div
              key={week.weekId}
              data-testid={`week-item-${week.weekNumber}`}
              className="group rounded-lg border border-border/50 bg-background/50 p-4 transition-all hover:border-border hover:bg-muted/20"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <span className="flex h-6 w-6 items-center justify-center rounded-md bg-secondary text-xs font-bold text-foreground">
                    T{week.weekNumber}
                  </span>
                  <span className="font-medium text-foreground">
                    {week.weekTitle}
                  </span>
                </div>
                {statusBadge}
              </div>

              {/* Thanh tiến độ trực quan */}
              <div className="mt-3">
                <div className="flex items-center justify-between text-xs text-muted-foreground mb-1.5">
                  <span>Tiến độ nhiệm vụ</span>
                  <span className="font-semibold text-foreground">
                    {percent}%
                  </span>
                </div>
                <div className="h-2.5 w-full overflow-hidden rounded-full bg-secondary">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ease-out ${barColor}`}
                    style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
                  />
                </div>
              </div>

              {/* Thông số chi tiết chân thanh bar */}
              <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <ListChecks className="h-3.5 w-3.5" />
                  <span>
                    <strong className="text-foreground">
                      {week.completedRequiredActivities}
                    </strong>{" "}
                    / {week.totalRequiredActivities} nhiệm vụ
                  </span>
                </div>
                <span>•</span>
                <div>
                  <span>
                    <strong className="text-foreground">
                      {week.completedLessons}
                    </strong>{" "}
                    / {week.totalLessons} bài học
                  </span>
                </div>
                <span>•</span>
                <div className="flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  <span>{week.estimatedMinutes} phút</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
