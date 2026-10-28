import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { WeeklyProgressBarChart } from "../components/WeeklyProgressBarChart";
import type { WeeklyProgressItemDto } from "../api/analytics.api";

describe("WeeklyProgressBarChart", () => {
  const mockWeeklyProgress: WeeklyProgressItemDto[] = [
    {
      weekId: "w1",
      weekNumber: 1,
      weekTitle: "Nhập môn phát âm IPA",
      totalRequiredActivities: 10,
      completedRequiredActivities: 10,
      progressPercent: 100,
      totalLessons: 3,
      completedLessons: 3,
      estimatedMinutes: 90,
      status: "COMPLETED",
    },
    {
      weekId: "w2",
      weekNumber: 2,
      weekTitle: "Từ vựng gia đình & nghề nghiệp",
      totalRequiredActivities: 12,
      completedRequiredActivities: 6,
      progressPercent: 50.0,
      totalLessons: 4,
      completedLessons: 2,
      estimatedMinutes: 120,
      status: "IN_PROGRESS",
    },
    {
      weekId: "w3",
      weekNumber: 3,
      weekTitle: "Ngữ pháp thì hiện tại đơn",
      totalRequiredActivities: 8,
      completedRequiredActivities: 0,
      progressPercent: 0,
      totalLessons: 2,
      completedLessons: 0,
      estimatedMinutes: 60,
      status: "NOT_STARTED",
    },
  ];

  it("renders weekly chart items with titles, badges, and progress percentages", () => {
    render(<WeeklyProgressBarChart weeklyProgress={mockWeeklyProgress} />);

    expect(screen.getByTestId("weekly-progress-chart")).toBeInTheDocument();

    // Week 1
    expect(screen.getByTestId("week-item-1")).toBeInTheDocument();
    expect(screen.getByText("Nhập môn phát âm IPA")).toBeInTheDocument();
    expect(screen.getByText("Đã hoàn thành")).toBeInTheDocument();
    expect(screen.getByText("100%")).toBeInTheDocument();

    // Week 2
    expect(screen.getByTestId("week-item-2")).toBeInTheDocument();
    expect(screen.getByText("Từ vựng gia đình & nghề nghiệp")).toBeInTheDocument();
    expect(screen.getByText(/12 nhiệm vụ/)).toBeInTheDocument();


    // Week 3
    expect(screen.getByTestId("week-item-3")).toBeInTheDocument();
    expect(screen.getByText("Chưa bắt đầu")).toBeInTheDocument();
  });

  it("renders empty state when weeklyProgress is empty", () => {
    render(<WeeklyProgressBarChart weeklyProgress={[]} />);
    expect(screen.getByText("Chưa có dữ liệu tiến độ theo tuần.")).toBeInTheDocument();
  });
});
