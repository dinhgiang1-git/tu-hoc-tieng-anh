import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { TodayPage } from "../pages/TodayPage";
import { todayApi, type TodayResponse } from "../api/today.api";
import { enrollmentApi, type EnrollmentSummaryResponse } from "@/features/enrollment/api/enrollment.api";
import { authApi } from "@/features/auth";

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
}

describe("TodayPage", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = createTestQueryClient();

    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: "e1111111-1111-1111-1111-111111111111",
    });

    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue([
      {
        id: "e1111111-1111-1111-1111-111111111111",
        curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
        revisionVersion: "1.0.0",
        status: "ACTIVE",
        completedRequiredActivities: 4,
        totalRequiredActivities: 19,
        progressPercent: 21.1,
        version: 1,
      },
    ]);
  });

  const renderToday = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TodayPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it.each(["preferences", "enrollments"] as const)(
    "shows a retryable %s error even while the dashboard is pending",
    async (source) => {
      vi.spyOn(todayApi, "getToday").mockReturnValue(new Promise(() => {}));
      if (source === "preferences") {
        vi.mocked(authApi.getPreferences).mockRejectedValue(new Error("Offline"));
      } else {
        vi.mocked(enrollmentApi.myEnrollments).mockRejectedValue(new Error("Offline"));
      }
      renderToday();
      expect(await screen.findByText(source === "preferences"
        ? "Không thể tải tùy chọn học tập. Vui lòng thử lại."
        : "Không thể tải danh sách lộ trình. Vui lòng thử lại.")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
      expect(screen.queryByText("Bạn chưa đăng ký lộ trình học nào")).not.toBeInTheDocument();
    },
  );

  it("renders state 1: NO_ENROLLMENT with empty state CTA linking to /curricula", async () => {
    const noEnrollmentResponse: TodayResponse = {
      state: "NO_ENROLLMENT",
      enrollmentId: null,
      revisionId: null,
      curriculumTitle: null,
      todayDate: "2026-10-25",
      isRestDay: false,
      enrollmentVersion: null,
      primaryAction: null,
      overdueLessons: [],
      progress: null,
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(noEnrollmentResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Bạn chưa đăng ký lộ trình học nào"),
      ).toBeInTheDocument();
      const ctaLink = screen.getByRole("link", {
        name: /Khám phá danh mục lộ trình/i,
      });
      expect(ctaLink).toBeInTheDocument();
      expect(ctaLink).toHaveAttribute("href", "/curricula");
    });
  });

  it("renders state 2: PAUSED with status message and no broken resume button", async () => {
    const pausedResponse: TodayResponse = {
      state: "PAUSED",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-25",
      isRestDay: false,
      enrollmentVersion: 2,
      primaryAction: null,
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 5,
        totalRequiredActivities: 19,
        progressPercent: 26.3,
        enrollmentVersion: 2,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(pausedResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Lộ trình học đang tạm dừng"),
      ).toBeInTheDocument();
      expect(
        screen.getByText("Tiến độ đã được giữ lại."),
      ).toBeInTheDocument();
      expect(screen.getByTestId("today-resume-cta-button")).toBeInTheDocument();
    });
  });

  it("renders state 3: IN_PROGRESS with lesson title, minutes, completed activities ratio, and CTA", async () => {
    const inProgressResponse: TodayResponse = {
      state: "IN_PROGRESS",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-25",
      isRestDay: false,
      enrollmentVersion: 1,
      primaryAction: {
        type: "IN_PROGRESS",
        message: "Bạn đang học dở Buổi 2. Hãy tiếp tục để hoàn thành mục tiêu!",
        lessonId: "l2222222-2222-2222-2222-222222222222",
        lessonTitle: "Buổi 2: Phụ âm /p/ và /b/",
        estimatedMinutes: 20,
        completedActivities: 2,
        totalActivities: 4,
      },
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 4,
        totalRequiredActivities: 19,
        progressPercent: 21.1,
        enrollmentVersion: 1,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(inProgressResponse);

    renderToday();

    await waitFor(() => {
      expect(screen.getByText("Buổi 2: Phụ âm /p/ và /b/")).toBeInTheDocument();
      expect(screen.getByText("Thời lượng: 20 phút")).toBeInTheDocument();
      expect(
        screen.getByText("Đã hoàn thành 2 / 4 hoạt động"),
      ).toBeInTheDocument();
      const cta = screen.getByRole("link", { name: /Tiếp tục học/i });
      expect(cta).toHaveAttribute(
        "href",
        "/enrollments/e1111111-1111-1111-1111-111111111111/lessons/l2222222-2222-2222-2222-222222222222",
      );
    });
  });

  it("renders state 4: DUE_TODAY with scheduled lesson title and start CTA", async () => {
    const dueTodayResponse: TodayResponse = {
      state: "DUE_TODAY",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-26",
      isRestDay: false,
      enrollmentVersion: 1,
      primaryAction: {
        type: "DUE_TODAY",
        message: "Hôm nay là ngày học của bạn. Bắt đầu Buổi 1!",
        lessonId: "l1111111-1111-1111-1111-111111111111",
        lessonTitle: "Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh",
        estimatedMinutes: 25,
        completedActivities: 0,
        totalActivities: 4,
      },
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 0,
        totalRequiredActivities: 19,
        progressPercent: 0,
        enrollmentVersion: 1,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(dueTodayResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh"),
      ).toBeInTheDocument();
      const startLink = screen.getByRole("link", { name: /Bắt đầu bài học/i });
      expect(startLink).toBeInTheDocument();
      expect(startLink).toHaveAttribute(
        "href",
        "/enrollments/e1111111-1111-1111-1111-111111111111/lessons/l1111111-1111-1111-1111-111111111111",
      );
    });
  });

  it("renders state 5: REST_DAY with peaceful banner and upcoming lesson suggestion", async () => {
    const restDayResponse: TodayResponse = {
      state: "REST_DAY",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-27",
      isRestDay: true,
      enrollmentVersion: 1,
      primaryAction: {
        type: "UPCOMING",
        message: "Bài học tiếp theo vào Thứ Tư (28/10/2026). Bạn có thể học trước nếu muốn!",
        lessonId: "l2222222-2222-2222-2222-222222222222",
        lessonTitle: "Buổi 2: Phụ âm /p/ và /b/",
        estimatedMinutes: 20,
        completedActivities: 0,
        totalActivities: 4,
      },
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 4,
        totalRequiredActivities: 19,
        progressPercent: 21.1,
        enrollmentVersion: 1,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(restDayResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Hôm nay là ngày nghỉ theo lịch"),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Học trước bài tiếp theo/i }),
      ).toBeInTheDocument();
    });
  });

  it("renders state 6: OVERDUE with primary overdue lesson and accordion for secondary overdues", async () => {
    const overdueResponse: TodayResponse = {
      state: "OVERDUE",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-31",
      isRestDay: true,
      enrollmentVersion: 1,
      primaryAction: {
        type: "OVERDUE",
        message: "Buổi 1 đã quá hạn 5 ngày. Hãy học bù để duy trì tiến độ!",
        lessonId: "l1111111-1111-1111-1111-111111111111",
        lessonTitle: "Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh",
        estimatedMinutes: 25,
        completedActivities: 0,
        totalActivities: 4,
      },
      overdueLessons: [
        {
          lessonId: "l1111111-1111-1111-1111-111111111111",
          lessonTitle: "Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh",
          plannedDate: "2026-10-26",
          overdueDays: 5,
        },
        {
          lessonId: "l2222222-2222-2222-2222-222222222222",
          lessonTitle: "Buổi 2: Phụ âm /p/ và /b/",
          plannedDate: "2026-10-28",
          overdueDays: 3,
        },
      ],
      progress: {
        completedRequiredActivities: 0,
        totalRequiredActivities: 19,
        progressPercent: 0,
        enrollmentVersion: 1,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(overdueResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh"),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Học bù ngay/i }),
      ).toBeInTheDocument();
      expect(
        screen.getByText("Các bài học quá hạn khác (1 bài)"),
      ).toBeInTheDocument();
    });

    // Expand accordion
    fireEvent.click(screen.getByText("Các bài học quá hạn khác (1 bài)"));

    await waitFor(() => {
      expect(screen.getByText("Buổi 2: Phụ âm /p/ và /b/")).toBeInTheDocument();
      expect(screen.getByText("Lịch: 2026-10-28 (3 ngày trước)")).toBeInTheDocument();
    });
  });

  it("renders state 7: UPCOMING ahead of schedule", async () => {
    const upcomingResponse: TodayResponse = {
      state: "UPCOMING",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-25",
      isRestDay: false,
      enrollmentVersion: 1,
      primaryAction: {
        type: "UPCOMING",
        message: "Lộ trình bắt đầu vào ngày mai (2026-10-26). Bạn có thể học trước!",
        lessonId: "l1111111-1111-1111-1111-111111111111",
        lessonTitle: "Buổi 1: Giới thiệu Nguyên âm và Phụ âm IPA Anh-Anh",
        estimatedMinutes: 25,
        completedActivities: 0,
        totalActivities: 4,
      },
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 0,
        totalRequiredActivities: 19,
        progressPercent: 0,
        enrollmentVersion: 1,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(upcomingResponse);

    renderToday();

    await waitFor(() => {
      expect(screen.getByText("Bài học sắp tới")).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Học trước bài này/i }),
      ).toBeInTheDocument();
    });
  });

  it("renders state 8: CURRICULUM_COMPLETED with congratulatory banner and no null lesson links", async () => {
    const completedResponse: TodayResponse = {
      state: "CURRICULUM_COMPLETED",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình Phát âm Anh-Anh Thử nghiệm",
      todayDate: "2026-10-25",
      isRestDay: false,
      enrollmentVersion: 5,
      primaryAction: {
        type: "CURRICULUM_COMPLETED",
        message: "Chúc mừng bạn đã hoàn thành toàn bộ lộ trình!",
        lessonId: null, // Critical: must be null, no broken link!
        lessonTitle: null,
        estimatedMinutes: null,
        completedActivities: 19,
        totalActivities: 19,
      },
      overdueLessons: [],
      progress: {
        completedRequiredActivities: 19,
        totalRequiredActivities: 19,
        progressPercent: 100,
        enrollmentVersion: 5,
      },
    };

    vi.spyOn(todayApi, "getToday").mockResolvedValue(completedResponse);

    renderToday();

    await waitFor(() => {
      expect(
        screen.getByText("Chúc mừng! Bạn đã hoàn thành toàn bộ lộ trình học!"),
      ).toBeInTheDocument();
      // Ensure no broken lesson link with null
      expect(screen.queryByRole("link", { name: /Tiếp tục học/i })).not.toBeInTheDocument();
      expect(screen.queryByRole("link", { name: /null/i })).not.toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Khám phá lộ trình khác/i }),
      ).toBeInTheDocument();
    });
  });

  it("switches focused enrollment via dropdown when user has multiple enrollments", async () => {
    const enrollments: EnrollmentSummaryResponse[] = [
      {
        id: "e1111111-1111-1111-1111-111111111111",
        curriculumTitle: "Lộ trình 1",
        revisionVersion: "1.0.0",
        status: "ACTIVE",
        completedRequiredActivities: 0,
        totalRequiredActivities: 19,
        progressPercent: 0,
        version: 1,
      },
      {
        id: "e2222222-2222-2222-2222-222222222222",
        curriculumTitle: "Lộ trình 2",
        revisionVersion: "1.0.0",
        status: "ACTIVE",
        completedRequiredActivities: 0,
        totalRequiredActivities: 19,
        progressPercent: 0,
        version: 1,
      },
    ];

    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue(enrollments);
    const updatePrefSpy = vi.spyOn(authApi, "updatePreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: "e2222222-2222-2222-2222-222222222222",
    });

    vi.spyOn(todayApi, "getToday").mockResolvedValue({
      state: "REST_DAY",
      enrollmentId: "e1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      curriculumTitle: "Lộ trình 1",
      todayDate: "2026-10-25",
      isRestDay: true,
      enrollmentVersion: 1,
      primaryAction: null,
      overdueLessons: [],
      progress: null,
    });

    renderToday();

    await waitFor(() => {
      expect(screen.getByRole("combobox")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "e2222222-2222-2222-2222-222222222222" },
    });

    await waitFor(() => {
      expect(updatePrefSpy).toHaveBeenCalledWith({
        focusedEnrollmentId: "e2222222-2222-2222-2222-222222222222",
      });
    });
  });
});
