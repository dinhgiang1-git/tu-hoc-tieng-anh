import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { LessonSummaryPage } from "../pages/LessonSummaryPage";
import {
  enrollmentApi,
  type LessonSummaryResponse,
} from "@/features/enrollment/api/enrollment.api";
import { ApiError } from "@/lib/api/api-error";

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
}

describe("LessonSummaryPage", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = createTestQueryClient();
  });

  const renderPage = (enrollmentId: string, lessonId: string) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/enrollments/${enrollmentId}/lessons/${lessonId}`]}
        >
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonSummaryPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it("renders lesson summary with real lesson data and friendly notice", async () => {
    const mockSummary: LessonSummaryResponse = {
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      lessonId: "l1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Khoá học Phát âm RP Chuẩn",
      lessonTitle: "Bài 1: Nguyên âm đơn ngắn",
      estimatedMinutes: 25,
      plannedDate: "2026-10-26",
      isRequired: true,
    };

    vi.spyOn(enrollmentApi, "getLessonSummary").mockResolvedValue(mockSummary);

    renderPage(
      "e1111111-1111-1111-1111-111111111111",
      "l1111111-1111-1111-1111-111111111111",
    );

    await waitFor(() => {
      expect(screen.getByText("Bài 1: Nguyên âm đơn ngắn")).toBeInTheDocument();
      expect(
        screen.getByText("Lộ trình: Khoá học Phát âm RP Chuẩn"),
      ).toBeInTheDocument();
      expect(screen.getByText("2026-10-26")).toBeInTheDocument();
      expect(screen.getByText("25 phút")).toBeInTheDocument();
      expect(screen.getByText("Bắt buộc")).toBeInTheDocument();
      expect(
        screen.getByText(
          "Không gian học tập tương tác chi tiết đang chuẩn bị ra mắt",
        ),
      ).toBeInTheDocument();
      expect(
        screen.getByText(
          "Nội dung học chi tiết và bài tập tương tác sẽ sớm được cập nhật trong phiên bản tiếp theo.",
        ),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Quay lại trang Hôm nay/i }),
      ).toBeInTheDocument();
    });
  });

  it("renders ErrorState when lesson is not found or not in enrollment revision (404)", async () => {
    vi.spyOn(enrollmentApi, "getLessonSummary").mockRejectedValue(
      new ApiError({
        type: "about:blank",
        title: "Không tìm thấy tài nguyên",
        status: 404,
        detail: "Lesson does not belong to enrollment revision",
        code: "RESOURCE_NOT_FOUND",
        traceId: "trace-404",
        fieldErrors: [],
      }),
    );

    renderPage(
      "e9999999-9999-9999-9999-999999999999",
      "l9999999-9999-9999-9999-999999999999",
    );

    await waitFor(() => {
      expect(
        screen.getByText(
          "Không tìm thấy buổi học hoặc buổi học không thuộc lộ trình đã đăng ký.",
        ),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Về trang Hôm nay/i }),
      ).toBeInTheDocument();
    });
  });
});
