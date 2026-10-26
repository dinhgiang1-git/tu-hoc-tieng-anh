import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { SchedulePage } from "../pages/SchedulePage";
import {
  enrollmentApi,
  type EnrollmentSummaryResponse,
  type EnrollmentDetailResponse,
  type LessonScheduleResponse,
} from "@/features/enrollment/api/enrollment.api";
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

describe("SchedulePage", () => {
  let queryClient: QueryClient;

  const mockEnrollmentId = "e1111111-1111-1111-1111-111111111111";
  const mockEnrollmentId2 = "e2222222-2222-2222-2222-222222222222";

  const mockEnrollmentSummaries: EnrollmentSummaryResponse[] = [
    {
      id: mockEnrollmentId,
      curriculumTitle: "Tiếng Anh Giao Tiếp Cơ Bản",
      revisionVersion: "1.0.0",
      status: "ACTIVE",
      completedRequiredActivities: 5,
      totalRequiredActivities: 20,
      progressPercent: 25,
      version: 1,
    },
    {
      id: mockEnrollmentId2,
      curriculumTitle: "Luyện Phát Âm Chuẩn RP",
      revisionVersion: "1.0.0",
      status: "PAUSED",
      completedRequiredActivities: 2,
      totalRequiredActivities: 15,
      progressPercent: 13.3,
      version: 2,
    },
  ];

  const mockDetail1: EnrollmentDetailResponse = {
    id: mockEnrollmentId,
    userId: "u1111111-1111-1111-1111-111111111111",
    curriculumId: "c1111111-1111-1111-1111-111111111111",
    revisionId: "r1111111-1111-1111-1111-111111111111",
    status: "ACTIVE",
    startDate: "2026-10-26",
    timeZone: "Asia/Ho_Chi_Minh",
    studyDays: ["MONDAY", "WEDNESDAY", "FRIDAY"],
    dailyTargetMinutes: 30,
    version: 1,
    estimatedEndDate: "2026-11-20",
  };

  const mockDetail2: EnrollmentDetailResponse = {
    id: mockEnrollmentId2,
    userId: "u1111111-1111-1111-1111-111111111111",
    curriculumId: "c2222222-2222-2222-2222-222222222222",
    revisionId: "r2222222-2222-2222-2222-222222222222",
    status: "PAUSED",
    startDate: "2026-10-20",
    timeZone: "Asia/Ho_Chi_Minh",
    studyDays: ["TUESDAY", "THURSDAY"],
    dailyTargetMinutes: 45,
    version: 2,
    estimatedEndDate: null,
  };

  afterEach(() => { vi.useRealTimers(); });

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = createTestQueryClient();

    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: mockEnrollmentId,
    });

    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue(mockEnrollmentSummaries);

    vi.spyOn(enrollmentApi, "getDetail").mockImplementation(async (id: string) => {
      if (id === mockEnrollmentId) return mockDetail1;
      if (id === mockEnrollmentId2) return mockDetail2;
      throw new Error("Not found");
    });

    vi.spyOn(enrollmentApi, "getSchedule").mockResolvedValue([]);
  });

  const renderSchedule = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SchedulePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it("renders loading skeleton while preferences or enrollments are pending", () => {
    vi.spyOn(enrollmentApi, "myEnrollments").mockReturnValue(new Promise(() => {}));
    renderSchedule();
    expect(screen.getByRole("status", { name: "Đang tải dữ liệu lịch học" })).toBeInTheDocument();
  });

  it("renders empty state with CTA to curricula when user has 0 enrollments", async () => {
    vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue([]);
    renderSchedule();

    await waitFor(() => {
      expect(screen.getByText("Bạn chưa đăng ký lộ trình nào")).toBeInTheDocument();
    });
    expect(
      screen.getByText("Hãy chọn một lộ trình phù hợp để bắt đầu học tập và theo dõi lịch học cá nhân."),
    ).toBeInTheDocument();
    const cta = screen.getByRole("link", { name: /Khám phá lộ trình/i });
    expect(cta).toHaveAttribute("href", "/curricula");
  });

  it("renders schedule header with curriculum title, study days, status badge, and estimatedEndDate", async () => {
    renderSchedule();

    await waitFor(() => {
      expect(screen.getByText("Đang học")).toBeInTheDocument();
    });

    expect(screen.getByText(/Múi giờ: Asia\/Ho_Chi_Minh/)).toBeInTheDocument();
    expect(screen.getByText(/Thứ Hai, Thứ Tư, Thứ Sáu/)).toBeInTheDocument();
    expect(screen.getByText("20/11/2026")).toBeInTheDocument();
  });

  it("renders week range and 7 day columns with lessons and rest day indicators", async () => {
    // Get Monday of current week for testing
    const now = new Date();
    const day = now.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() + diff);

    const pad = (n: number) => String(n).padStart(2, "0");
    const toIso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const mondayIso = toIso(monday);
    const wednesdayIso = toIso(new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 2));

    const mockLessons: LessonScheduleResponse[] = [
      {
        lessonId: "l1111111-1111-1111-1111-111111111111",
        lessonTitle: "Bài 1: Giới thiệu bản thân",
        plannedDate: mondayIso,
        originalDate: mondayIso,
        isCompleted: true,
        isOverdue: false,
      },
      {
        lessonId: "l2222222-2222-2222-2222-222222222222",
        lessonTitle: "Bài 2: Chào hỏi hàng ngày",
        plannedDate: wednesdayIso,
        originalDate: wednesdayIso,
        isCompleted: false,
        isOverdue: false,
      },
    ];

    vi.spyOn(enrollmentApi, "getSchedule").mockResolvedValue(mockLessons);

    renderSchedule();

    await waitFor(() => {
      expect(screen.getByText("Bài 1: Giới thiệu bản thân")).toBeInTheDocument();
    });

    expect(screen.getByText("Bài 2: Chào hỏi hàng ngày")).toBeInTheDocument();

    // Check completed badge
    expect(screen.getByTestId("badge-completed")).toBeInTheDocument();
    expect(screen.getByTestId("badge-completed")).toHaveTextContent("Đã hoàn thành");

    // Check scheduled badge
    expect(screen.getByTestId("badge-scheduled")).toBeInTheDocument();
    expect(screen.getByTestId("badge-scheduled")).toHaveTextContent("Theo lịch");

    // Check rest day indicators (Tuesday, Thursday, Saturday, Sunday)
    const restDays = screen.getAllByTestId("rest-day-indicator");
    expect(restDays.length).toBeGreaterThanOrEqual(4);

    // Check link to workspace
    const lessonLink = screen.getByTestId("lesson-link-l1111111-1111-1111-1111-111111111111");
    expect(lessonLink).toHaveAttribute(
      "href",
      `/enrollments/${mockEnrollmentId}/lessons/l1111111-1111-1111-1111-111111111111`,
    );
  });

  it("navigates weeks with Prev, Next, and Today buttons", async () => {
    const getScheduleSpy = vi.spyOn(enrollmentApi, "getSchedule").mockResolvedValue([]);

    renderSchedule();

    await waitFor(() => {
      expect(screen.getByTestId("schedule-week-range")).toBeInTheDocument();
    });

    const initialRange = screen.getByTestId("schedule-week-range").textContent;
    expect(initialRange).toBeTruthy();

    await waitFor(() => expect(getScheduleSpy).toHaveBeenCalledTimes(1));

    // Click Next Week
    fireEvent.click(screen.getByTestId("schedule-next-week"));
    await waitFor(() => {
      const nextRange = screen.getByTestId("schedule-week-range").textContent;
      expect(nextRange).not.toBe(initialRange);
    });

    await waitFor(() => expect(getScheduleSpy).toHaveBeenCalledTimes(2));

    // Click Prev Week (returns to initial week)
    fireEvent.click(screen.getByTestId("schedule-prev-week"));
    await waitFor(() => {
      expect(screen.getByTestId("schedule-week-range").textContent).toBe(initialRange);
    });

    // Click Prev Week again (goes to previous week)
    fireEvent.click(screen.getByTestId("schedule-prev-week"));
    await waitFor(() => {
      expect(screen.getByTestId("schedule-week-range").textContent).not.toBe(initialRange);
    });

    // Click Today (resets to initial week)
    fireEvent.click(screen.getByTestId("schedule-today"));
    await waitFor(() => {
      expect(screen.getByTestId("schedule-week-range").textContent).toBe(initialRange);
    });
  });

  it("switches enrollment via dropdown and fetches new enrollment details and schedule", async () => {
    renderSchedule();

    await waitFor(() => {
      expect(screen.getByTestId("schedule-enrollment-select")).toBeInTheDocument();
    });

    expect(screen.getByText("Tiếng Anh Giao Tiếp Cơ Bản")).toBeInTheDocument();

    // Select second enrollment
    fireEvent.change(screen.getByTestId("schedule-enrollment-select"), {
      target: { value: mockEnrollmentId2 },
    });

    await waitFor(() => {
      expect(screen.getByText("Luyện Phát Âm Chuẩn RP")).toBeInTheDocument();
    });

    expect(screen.getByText("Tạm dừng")).toBeInTheDocument();
    expect(screen.getByText(/Thứ Ba, Thứ Năm/)).toBeInTheDocument();
    expect(screen.getByText("Chưa có lịch")).toBeInTheDocument();
    expect(screen.getByText("(Lộ trình đang tạm dừng)")).toBeInTheDocument();
  });

  it("renders ErrorState when schedule query fails and allows retry", async () => {
    vi.spyOn(enrollmentApi, "getSchedule").mockRejectedValueOnce(new Error("Network error"));

    renderSchedule();

    await waitFor(() => {
      expect(
        screen.getByText("Không thể tải lịch học cho khoảng ngày này. Vui lòng thử lại."),
      ).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole("button", { name: "Thử lại" });
    expect(retryBtn).toBeInTheDocument();

    vi.spyOn(enrollmentApi, "getSchedule").mockResolvedValueOnce([]);
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(
        screen.queryByText("Không thể tải lịch học cho khoảng ngày này. Vui lòng thử lại."),
      ).not.toBeInTheDocument();
    });
  });

  it("displays originalDate note when lesson originalDate differs from plannedDate", async () => {
    const now = new Date();
    const day = now.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() + diff);

    const pad = (n: number) => String(n).padStart(2, "0");
    const toIso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const mondayIso = toIso(monday);

    const mockRescheduledLesson: LessonScheduleResponse[] = [
      {
        lessonId: "l3333333-3333-3333-3333-333333333333",
        lessonTitle: "Bài 3: Mua sắm",
        plannedDate: mondayIso,
        originalDate: "2026-10-23", // differed original date
        isCompleted: false,
        isOverdue: true,
      },
    ];

    vi.spyOn(enrollmentApi, "getSchedule").mockResolvedValue(mockRescheduledLesson);

    renderSchedule();

    await waitFor(() => {
      expect(screen.getByText("Bài 3: Mua sắm")).toBeInTheDocument();
    });

    expect(screen.getByTestId("badge-overdue")).toHaveTextContent("Quá hạn");
    expect(screen.getByText("Lịch gốc: 23/10/2026")).toBeInTheDocument();
  });
  it("uses the enrollment timezone to choose the current week", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-10-25T23:30:00Z"));
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({ ...mockDetail1, timeZone: "America/Los_Angeles" });
    renderSchedule();
    await waitFor(() => expect(enrollmentApi.getSchedule).toHaveBeenCalledWith(
      mockEnrollmentId, { from: "2026-10-19", to: "2026-10-25" }, expect.any(AbortSignal)));
  });

  it("does not present a failed enrollment request as an empty list", async () => {
    vi.spyOn(enrollmentApi, "myEnrollments").mockRejectedValue(new Error("offline"));
    renderSchedule();
    expect(await screen.findByText("Không thể tải danh sách lộ trình. Vui lòng thử lại.")).toBeInTheDocument();
    expect(screen.queryByText("Bạn chưa đăng ký lộ trình nào")).not.toBeInTheDocument();
  });

});
