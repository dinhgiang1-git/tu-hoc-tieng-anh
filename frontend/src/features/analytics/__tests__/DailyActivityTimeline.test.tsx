import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DailyActivityTimeline } from "../components/DailyActivityTimeline";
import type { DailyActivityHistoryDto } from "../api/analytics.api";

describe("DailyActivityTimeline", () => {
  const mockDailyHistory: DailyActivityHistoryDto[] = [
    {
      date: "2026-10-28",
      completedActivitiesCount: 2,
      quizAttemptsCount: 1,
      events: [
        {
          id: "ev1",
          eventType: "COMPLETED",
          occurredAt: "2026-10-28T09:30:00Z",
          activityTitle: "Luyện phát âm nguyên âm /i:/",
          lessonTitle: "Bài 1: Nguyên âm cơ bản",
        },
        {
          id: "ev2",
          eventType: "QUIZ_SUBMITTED",
          occurredAt: "2026-10-28T10:15:00Z",
          activityTitle: "Kiểm tra trắc nghiệm Unit 1",
          lessonTitle: "Bài 1: Nguyên âm cơ bản",
        },
      ],
    },
  ];

  it("renders daily history items and events correctly", () => {
    render(<DailyActivityTimeline dailyHistory={mockDailyHistory} />);

    expect(screen.getByTestId("daily-activity-timeline")).toBeInTheDocument();
    expect(screen.getByTestId("daily-history-item-2026-10-28")).toBeInTheDocument();
    expect(screen.getByText("2 nhiệm vụ xong")).toBeInTheDocument();
    expect(screen.getByText("1 lượt thi quiz")).toBeInTheDocument();
    expect(screen.getByText("Luyện phát âm nguyên âm /i:/")).toBeInTheDocument();
    expect(screen.getByText("Kiểm tra trắc nghiệm Unit 1")).toBeInTheDocument();
  });

  it("renders empty state when dailyHistory is empty", () => {
    render(<DailyActivityTimeline dailyHistory={[]} />);

    expect(screen.getByTestId("daily-timeline-empty")).toBeInTheDocument();
    expect(
      screen.getByText("Chưa có dữ liệu học tập theo ngày"),
    ).toBeInTheDocument();
  });
});
