import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ProgressMetricCards } from "../components/ProgressMetricCards";

describe("ProgressMetricCards", () => {
  const defaultProps = {
    completedRequiredActivities: 12,
    totalRequiredActivities: 45,
    progressPercent: 26.7,
    completedLessons: 3,
    totalLessons: 12,
    estimatedCompletedMinutes: 90,
    estimatedTotalMinutes: 360,
    status: "ACTIVE",
    streak: {
      currentStreakDays: 3,
      longestStreakDays: 5,
      totalActiveDays: 7,
      lastActiveDate: "2026-10-28",
    },
    quizSummary: {
      totalQuizzesTaken: 2,
      passedQuizzes: 2,
      averageScorePercent: 85.0,
    },
  };

  it("renders all core metric cards with correct values", () => {
    render(<ProgressMetricCards {...defaultProps} />);

    // Progress
    const progressCard = screen.getByTestId("metric-progress");
    expect(within(progressCard).getByText("26.7%")).toBeInTheDocument();
    expect(within(progressCard).getByText(/12 \/ 45 nhiệm vụ/)).toBeInTheDocument();

    // Streak
    const streakCard = screen.getByTestId("metric-streak");
    expect(within(streakCard).getByText("3")).toBeInTheDocument();
    expect(within(streakCard).getByText(/5 ngày/)).toBeInTheDocument();
    expect(within(streakCard).getByText(/7 ngày học/)).toBeInTheDocument();

    // Duration
    const durationCard = screen.getByTestId("metric-duration");
    expect(within(durationCard).getByText("90")).toBeInTheDocument();
    expect(within(durationCard).getByText(/360 phút/)).toBeInTheDocument();

    // Lessons
    const lessonsCard = screen.getByTestId("metric-lessons");
    expect(within(lessonsCard).getByText("3")).toBeInTheDocument();
    expect(within(lessonsCard).getByText(/\/ 12 bài/)).toBeInTheDocument();
    expect(within(lessonsCard).getByText("Đang học")).toBeInTheDocument();

    // Quiz summary
    const quizCard = screen.getByTestId("metric-quiz-summary");
    expect(within(quizCard).getByText("85%")).toBeInTheDocument();
  });


  it("handles null progressPercent and missing quizSummary gracefully", () => {
    render(
      <ProgressMetricCards
        {...defaultProps}
        progressPercent={null}
        quizSummary={null}
        status="COMPLETED"
      />,
    );

    expect(screen.getByText("0%")).toBeInTheDocument();
    expect(screen.getByText("Đã hoàn thành")).toBeInTheDocument();
    expect(screen.queryByTestId("metric-quiz-summary")).not.toBeInTheDocument();
  });
});
