import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BookmarkButton } from "../components/BookmarkButton";
import { reviewApi, type ReviewSummaryResponse } from "../api/review.api";

describe("BookmarkButton", () => {
  const enrollmentId = "11111111-1111-1111-1111-111111111111";
  const lessonId = "22222222-2222-2222-2222-222222222222";
  const activityId = "act-1";

  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  const mockReviewSummaryEmpty: ReviewSummaryResponse = {
    enrollmentId,
    mistakes: [],
    bookmarkedItems: [],
  };

  const mockReviewSummaryWithBookmark: ReviewSummaryResponse = {
    enrollmentId,
    mistakes: [],
    bookmarkedItems: [
      {
        id: "bm-1",
        enrollmentId,
        lessonId,
        activityId,
        lessonTitle: "Bài 1",
        activityTitle: "Nhiệm vụ 1",
        createdAt: new Date().toISOString(),
      },
    ],
  };

  it("renders unbookmarked state and toggles on click to create bookmark", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue(mockReviewSummaryEmpty);
    const createSpy = vi.spyOn(reviewApi, "createBookmark").mockResolvedValue({
      id: "bm-new",
      enrollmentId,
      lessonId,
      activityId,
      lessonTitle: "Bài 1",
      activityTitle: "Nhiệm vụ 1",
      createdAt: new Date().toISOString(),
    });

    render(
      <QueryClientProvider client={queryClient}>
        <BookmarkButton
          enrollmentId={enrollmentId}
          lessonId={lessonId}
          activityId={activityId}
        />
      </QueryClientProvider>,
    );

    const btn = await screen.findByTestId(`bookmark-activity-${activityId}`);
    await waitFor(() => expect(btn).not.toBeDisabled());
    expect(btn).toHaveTextContent("Đánh dấu ôn tập");

    fireEvent.click(btn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        enrollmentId,
        lessonId,
        activityId,
      });
    });
  });

  it("renders bookmarked state and toggles on click to delete bookmark", async () => {
    vi.spyOn(reviewApi, "getReviewSummary").mockResolvedValue(mockReviewSummaryWithBookmark);
    const deleteSpy = vi.spyOn(reviewApi, "deleteBookmark").mockResolvedValue();

    render(
      <QueryClientProvider client={queryClient}>
        <BookmarkButton
          enrollmentId={enrollmentId}
          lessonId={lessonId}
          activityId={activityId}
        />
      </QueryClientProvider>,
    );

    const btn = await screen.findByTestId(`bookmark-activity-${activityId}`);
    await waitFor(() => expect(btn).toHaveTextContent("Đã đánh dấu"));
    expect(btn).not.toBeDisabled();

    fireEvent.click(btn);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith("bm-1");
    });
  });
});
