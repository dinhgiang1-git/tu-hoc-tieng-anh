import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReviewPage } from "../pages/ReviewPage";
import { reviewApi, type ReviewSummaryResponse } from "../api/review.api";
import { enrollmentApi, type EnrollmentSummaryResponse } from "@/features/enrollment/api/enrollment.api";
import { authApi } from "@/features/auth";

describe("ReviewPage", () => {
  const enrollmentId = "11111111-1111-1111-1111-111111111111";
  const lessonId = "22222222-2222-2222-2222-222222222222";
  const questionId = "q-1";

  let queryClient: QueryClient;

  const mockEnrollments: EnrollmentSummaryResponse[] = [
    {
      id: enrollmentId,
      curriculumTitle: "Tiếng Anh Giao Tiếp Cơ Bản",
      revisionVersion: "1.0",
      status: "ACTIVE",
      completedRequiredActivities: 5,
      totalRequiredActivities: 20,
      progressPercent: 25,
      version: 1,
    },
  ];

  const mockSummary: ReviewSummaryResponse = {
    enrollmentId,
    mistakes: [
      {
        questionId,
        activityId: "act-quiz-1",
        activityTitle: "Bài kiểm tra phát âm nguyên âm",
        lessonId,
        lessonTitle: "Buổi 1: Nguyên âm cơ bản",
        prompt: "Chọn từ có âm /iː/ khác với các từ còn lại:",
        options: [
          { id: "opt-1", content: "see", orderIndex: 0 },
          { id: "opt-2", content: "tea", orderIndex: 1 },
          { id: "opt-3", content: "sit", orderIndex: 2 },
          { id: "opt-4", content: "key", orderIndex: 3 },
        ],
        resolved: false,
        lastAttemptedAt: "2026-10-28T07:00:00Z",
      },
    ],
    bookmarkedItems: [
      {
        id: "bm-1",
        enrollmentId,
        lessonId,
        activityId: null,
        lessonTitle: "Buổi 1: Nguyên âm cơ bản",
        activityTitle: null,
        activityType: null,
        note: null,
        createdAt: "2026-10-28T07:00:00Z",
      },
      {
        id: "bm-2",
        enrollmentId,
        lessonId,
        activityId: "act-quiz-1",
        lessonTitle: "Buổi 1: Nguyên âm cơ bản",
        activityTitle: "Bài kiểm tra phát âm nguyên âm",
        activityType: "QUIZ",
        note: null,
        createdAt: "2026-10-28T07:05:00Z",
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      theme: "system",
      dailyReminderTime: "20:00",
      focusedEnrollmentId: enrollmentId,
    } as any);
    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue(mockEnrollments);
  });

  const renderReviewPage = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/review"]}>
          <Routes>
            <Route path="/review" element={<ReviewPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it("renders review tabs with mistake and bookmark counts", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue(mockSummary);

    renderReviewPage();

    expect(await screen.findByTestId("tab-mistakes")).toBeInTheDocument();
    expect(screen.getByTestId("tab-bookmarks")).toBeInTheDocument();

    // Check mistake card prompt
    expect(
      screen.getByText("Chọn từ có âm /iː/ khác với các từ còn lại:"),
    ).toBeInTheDocument();
    expect(screen.getByText("Cần ôn lại")).toBeInTheDocument();
  });

  it("allows answering a mistake question and displays instant feedback", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue(mockSummary);
    const checkSpy = vi
      .spyOn(reviewApi, "checkMistakeQuestion")
      .mockResolvedValue({
        questionId,
        selectedOptionId: "opt-3",
        correctOptionId: "opt-3",
        isCorrect: true,
        resolved: true,
        explanation: "Từ 'sit' phát âm là /ɪ/, các từ còn lại đều là /iː/.",
      });

    renderReviewPage();

    await screen.findByText("Chọn từ có âm /iː/ khác với các từ còn lại:");

    // Select option 3 ("sit")
    const sitOption = screen.getByText("sit");
    fireEvent.click(sitOption);

    // Click check answer button
    const checkBtn = screen.getByTestId(`check-button-${questionId}`);
    fireEvent.click(checkBtn);

    await waitFor(() => {
      expect(checkSpy).toHaveBeenCalledWith(enrollmentId, questionId, {
        selectedOptionId: "opt-3",
      });
    });

    expect(
      await screen.findByText("Chính xác! Câu hỏi đã được giải quyết."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Từ 'sit' phát âm là /ɪ/, các từ còn lại đều là /iː/."),
    ).toBeInTheDocument();
  });

  it("switches to bookmarks tab and allows deleting a bookmark", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue(mockSummary);
    const deleteSpy = vi.spyOn(reviewApi, "deleteBookmark").mockResolvedValue();

    renderReviewPage();

    const bookmarksTab = await screen.findByTestId("tab-bookmarks");
    fireEvent.click(bookmarksTab);

    // Should display both bookmarks (lesson and activity)
    expect(await screen.findByTestId("bookmarked-item-bm-1")).toBeInTheDocument();
    expect(screen.getByTestId("bookmarked-item-bm-2")).toBeInTheDocument();

    // Click delete bookmark on bm-1
    const deleteBtn = screen.getByTestId("delete-bookmark-bm-1");
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith("bm-1");
    });
  });

  it("renders empty state when there are no mistakes or bookmarks", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue({
      enrollmentId,
      mistakes: [],
      bookmarkedItems: [],
    });

    renderReviewPage();

    expect(
      await screen.findByText("Không có câu hỏi nào cần ôn lại"),
    ).toBeInTheDocument();

    const bookmarksTab = screen.getByTestId("tab-bookmarks");
    fireEvent.click(bookmarksTab);

    expect(
      await screen.findByText("Chưa có bài học nào được đánh dấu"),
    ).toBeInTheDocument();
  });
});
