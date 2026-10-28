import {
  Award,
  Calendar,
  CalendarOff,
  CheckCircle2,
  Clock,
  Sparkles,
} from "lucide-react";
import type { DailyActivityHistoryDto } from "../api/analytics.api";

interface DailyActivityTimelineProps {
  dailyHistory: DailyActivityHistoryDto[];
}

function formatDateVietnamese(dateString: string): string {
  try {
    const parts = dateString.split("-");
    if (parts.length === 3) {
      const year = parts[0];
      const month = parts[1];
      const day = parts[2];
      const date = new Date(Number(year), Number(month) - 1, Number(day));
      const dayOfWeekNames = [
        "Chủ Nhật",
        "Thứ Hai",
        "Thứ Ba",
        "Thứ Tư",
        "Thứ Năm",
        "Thứ Sáu",
        "Thứ Bảy",
      ];
      const dayOfWeek = dayOfWeekNames[date.getDay()];
      return `${dayOfWeek}, ${day}/${month}/${year}`;
    }
  } catch {
    // fallback
  }
  return dateString;
}

function formatTime(isoString: string): string {
  try {
    const d = new Date(isoString);
    const hours = String(d.getHours()).padStart(2, "0");
    const minutes = String(d.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  } catch {
    return "";
  }
}

export function DailyActivityTimeline({
  dailyHistory,
}: DailyActivityTimelineProps) {
  if (!dailyHistory || dailyHistory.length === 0) {
    return (
      <div
        data-testid="daily-timeline-empty"
        className="rounded-xl border border-dashed border-border bg-card/60 p-8 text-center shadow-sm"
      >
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-muted">
          <CalendarOff className="h-6 w-6 text-muted-foreground" />
        </div>
        <h4 className="mt-3 text-sm font-semibold text-foreground">
          Chưa có dữ liệu học tập theo ngày
        </h4>
        <p className="mx-auto mt-1 max-w-sm text-xs text-muted-foreground">
          Hãy hoàn thành bài học hôm nay để bắt đầu ghi nhận tiến trình và tích lũy chuỗi ngày học thực tế!
        </p>
      </div>
    );
  }

  return (
    <div
      data-testid="daily-activity-timeline"
      className="rounded-xl border border-border bg-card p-6 shadow-sm"
    >
      <div className="flex items-center justify-between border-b border-border/60 pb-4">
        <div>
          <h3 className="text-base font-semibold text-foreground">
            Lịch Sử Học Tập Theo Ngày Thực Tế
          </h3>
          <p className="text-xs text-muted-foreground">
            Ghi nhận các hoạt động và bài kiểm tra hoàn thành theo từng ngày học
          </p>
        </div>
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Sparkles className="h-4 w-4 text-primary" />
          <span>{dailyHistory.length} ngày hoạt động</span>
        </div>
      </div>

      <div className="mt-6 space-y-6">
        {dailyHistory.map((day) => (
          <div
            key={day.date}
            data-testid={`daily-history-item-${day.date}`}
            className="rounded-lg border border-border/60 bg-background/50 p-4"
          >
            {/* Header ngày */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/40 pb-2.5">
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-primary" />
                <span className="font-semibold text-sm text-foreground">
                  {formatDateVietnamese(day.date)}
                </span>
              </div>
              <div className="flex items-center gap-2 text-xs">
                {day.completedActivitiesCount > 0 && (
                  <span className="rounded-full bg-emerald-50 px-2.5 py-0.5 font-medium text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300">
                    {day.completedActivitiesCount} nhiệm vụ xong
                  </span>
                )}
                {day.quizAttemptsCount > 0 && (
                  <span className="rounded-full bg-amber-50 px-2.5 py-0.5 font-medium text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
                    {day.quizAttemptsCount} lượt thi quiz
                  </span>
                )}
              </div>
            </div>

            {/* Danh sách các sự kiện trong ngày */}
            <div className="mt-3 space-y-2">
              {day.events && day.events.length > 0 ? (
                day.events.map((ev) => {
                  const isQuiz = ev.eventType === "QUIZ_SUBMITTED";
                  return (
                    <div
                      key={ev.id}
                      className="flex items-center justify-between gap-3 rounded-md px-2.5 py-1.5 text-xs transition-colors hover:bg-muted/40"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        {isQuiz ? (
                          <div className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400">
                            <Award className="h-3 w-3" />
                          </div>
                        ) : (
                          <div className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                            <CheckCircle2 className="h-3 w-3" />
                          </div>
                        )}
                        <div className="truncate">
                          <span className="font-medium text-foreground">
                            {ev.activityTitle}
                          </span>
                          {ev.lessonTitle && (
                            <span className="ml-2 text-muted-foreground">
                              ({ev.lessonTitle})
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex shrink-0 items-center gap-1 text-muted-foreground">
                        <Clock className="h-3 w-3" />
                        <span>{formatTime(ev.occurredAt)}</span>
                      </div>
                    </div>
                  );
                })
              ) : (
                <p className="text-xs text-muted-foreground italic">
                  Có hoạt động học tập được ghi nhận.
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
