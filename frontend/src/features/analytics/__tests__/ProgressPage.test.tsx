import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ProgressPage } from "../pages/ProgressPage";
import { analyticsApi, type EnrollmentAnalyticsResponse } from "../api/analytics.api";
import { enrollmentApi, type EnrollmentSummaryResponse } from "@/features/enrollment/api/enrollment.api";
import { authApi } from "@/features/auth";

describe("ProgressPage", () => {
  const enrollmentId1 = "11111111-1111-1111-1111-111111111111";
  const enrollmentId2 = "22222222-2222-2222-2222-222222222222";

  let queryClient: QueryClient;

  const mockEnrollments: EnrollmentSummaryResponse[] = [
    {
      id: enrollmentId1,
      curriculumTitle: "Lộ trình Tiếng Anh Giao Tiếp Toàn Diện",
      revisionVersion: "1.0",
      status: "ACTIVE",
      completedRequiredActivities: 12,
      totalRequiredActivities: 45,
      progressPercent: 26.7,
      version: 4,
    },
    {
      id: enrollmentId2,
      curriculumTitle: "Lộ trình Tiếng Anh Thương Mại B2",
      revisionVersion: "1.0",
      status: "ACTIVE",
      completedRequiredActivities: 5,
      totalRequiredActivities: 30,
      progressPercent: 16.7,
      version: 1,
    },
  ];

  const mockAnalyticsData: EnrollmentAnalyticsResponse = {
    enrollmentId: enrollmentId1,
    curriculumTitle: "Lộ trình Tiếng Anh Giao Tiếp Toàn Diện",
    status: "ACTIVE",
    startDate: "2026-10-01",
    timeZone: "Asia/Ho_Chi_Minh",
    enrollmentVersion: 4,
    completedRequiredActivities: 12,
    totalRequiredActivities: 45,
    progressPercent: 26.7,
    completedLessons: 3,
    totalLessons: 12,
    estimatedCompletedMinutes: 90,
    estimatedTotalMinutes: 360,
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
    weeklyProgress: [
      {
        weekId: "w1",
        weekNumber: 1,
        weekTitle: "Nhập môn phát âm IPA & Từ vựng căn bản",
        totalRequiredActivities: 15,
        completedRequiredActivities: 12,
        progressPercent: 80.0,
        totalLessons: 4,
        completedLessons: 3,
        estimatedMinutes: 120,
        status: "IN_PROGRESS",
      },
    ],
    dailyHistory: [
      {
        date: "2026-10-28",
        completedActivitiesCount: 3,
        quizAttemptsCount: 1,
        events: [
          {
            id: "ev-1",
            eventType: "COMPLETED",
            occurredAt: "2026-10-28T09:30:00Z",
            activityTitle: "Luyện âm /i:/ và /ɪ/",
            lessonTitle: "Nguyên âm đơn cơ bản",
          },
        ],
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
      },
    });

    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: enrollmentId1,
    });

    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue(mockEnrollments);
    vi.spyOn(analyticsApi, "getEnrollmentAnalytics").mockResolvedValue(mockAnalyticsData);
  });

  const renderProgressPage = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/progress"]}>
          <Routes>
            <Route path="/progress" element={<ProgressPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

  it("renders page title, KPI cards, weekly chart, and daily timeline", async () => {
    renderProgressPage();

    await waitFor(() => {
      expect(
        screen.getByText("Lộ trình Tiếng Anh Giao Tiếp Toàn Diện"),
      ).toBeInTheDocument();
    });

    // Check KPI cards
    expect(screen.getByTestId("metric-progress")).toBeInTheDocument();
    expect(screen.getByText("26.7%")).toBeInTheDocument();
    expect(screen.getByTestId("metric-streak")).toBeInTheDocument();
    expect(screen.getByTestId("metric-duration")).toBeInTheDocument();
    expect(screen.getByText("90")).toBeInTheDocument();


    // Check Weekly progress chart
    expect(screen.getByTestId("weekly-progress-chart")).toBeInTheDocument();
    expect(
      screen.getByText("Nhập môn phát âm IPA & Từ vựng căn bản"),
    ).toBeInTheDocument();

    // Check Daily history timeline
    expect(screen.getByTestId("daily-activity-timeline")).toBeInTheDocument();
    expect(screen.getByText("Luyện âm /i:/ và /ɪ/")).toBeInTheDocument();
  });

  it("renders empty state when learner has no enrollments", async () => {
    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue([]);
    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: null,
    });

    renderProgressPage();

    await waitFor(() => {
      expect(
        screen.getByText("Bạn chưa đăng ký lộ trình học nào"),
      ).toBeInTheDocument();
    });

    expect(screen.getByText("Khám phá lộ trình")).toBeInTheDocument();
  });

  it("renders error state when analytics API fails and allows retry", async () => {
    vi.spyOn(analyticsApi, "getEnrollmentAnalytics").mockRejectedValue(
      new Error("Server error"),
    );

    renderProgressPage();

    await waitFor(() => {
      expect(
        screen.getByText(
          "Không thể tải dữ liệu thống kê tiến độ học tập. Vui lòng thử lại sau.",
        ),
      ).toBeInTheDocument();
    });

    // When clicking retry, call again
    vi.spyOn(analyticsApi, "getEnrollmentAnalytics").mockResolvedValue(mockAnalyticsData);
    fireEvent.click(screen.getByText("Thử lại"));

    await waitFor(() => {
      expect(screen.getByText("26.7%")).toBeInTheDocument();
    });
  });

  it("allows switching enrollment using the dropdown", async () => {
    renderProgressPage();

    await waitFor(() => {
      expect(screen.getByTestId("enrollment-switcher")).toBeInTheDocument();
    });

    const switcher = screen.getByTestId("enrollment-switcher") as HTMLSelectElement;
    fireEvent.change(switcher, { target: { value: enrollmentId2 } });

    await waitFor(() => {
      expect(analyticsApi.getEnrollmentAnalytics).toHaveBeenCalledWith(
        enrollmentId2,
        expect.anything(),
      );
    });
  });
});
