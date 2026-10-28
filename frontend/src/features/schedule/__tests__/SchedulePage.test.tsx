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

  it("shows 'Đổi lịch' button for ACTIVE enrollment and opens RescheduleDialog", async () => {
    renderSchedule();
    const rescheduleBtn = await screen.findByTestId("schedule-open-reschedule-button");
    expect(rescheduleBtn).toBeInTheDocument();
    expect(rescheduleBtn).toHaveTextContent("Đổi lịch");

    fireEvent.click(rescheduleBtn);
    expect(screen.getByTestId("reschedule-dialog")).toBeInTheDocument();
    expect(screen.getByText("Đổi lịch học")).toBeInTheDocument();

    // Close button works
    fireEvent.click(screen.getByTestId("reschedule-close-button"));
    expect(screen.queryByTestId("reschedule-dialog")).not.toBeInTheDocument();
  });

  it("hides 'Đổi lịch' button for PAUSED enrollment", async () => {
    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 45,
      focusedEnrollmentId: mockEnrollmentId2, // mockDetail2 is PAUSED
    });
    renderSchedule();
    await waitFor(() => {
      expect(screen.getByText("Luyện Phát Âm Chuẩn RP")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("schedule-open-reschedule-button")).not.toBeInTheDocument();
  });

  it("calculates preview, invalidates on input edit, and commits atomically", async () => {
    const previewSpy = vi.spyOn(enrollmentApi, "previewReschedule").mockResolvedValue({
      enrollmentVersion: 1,
      proposedEndDate: "2026-11-25",
      modifiedSchedules: [
        {
          lessonId: "l1111111-1111-1111-1111-111111111111",
          lessonTitle: "Bài 1: Làm quen",
          previousPlannedDate: "2026-10-26",
          newPlannedDate: "2026-10-28",
          originalDate: "2026-10-26",
        },
      ],
    });

    const commitSpy = vi.spyOn(enrollmentApi, "commitReschedule").mockResolvedValue({
      ...mockDetail1,
      version: 2,
    });

    renderSchedule();
    const openBtn = await screen.findByTestId("schedule-open-reschedule-button");
    fireEvent.click(openBtn);

    // Click preview
    const previewBtn = screen.getByTestId("reschedule-preview-button");
    fireEvent.click(previewBtn);

    await waitFor(() => {
      expect(previewSpy).toHaveBeenCalledWith(
        mockEnrollmentId,
        expect.objectContaining({
          dailyTargetMinutes: 30,
        }),
      );
    });

    // Preview result is shown
    expect(await screen.findByTestId("reschedule-preview-result")).toBeInTheDocument();
    expect(screen.getByText("25/11/2026")).toBeInTheDocument();
    expect(screen.getByText("Bài 1: Làm quen")).toBeInTheDocument();

    // Editing input invalidates preview
    const minutesInput = screen.getByTestId("reschedule-minutes-input");
    fireEvent.change(minutesInput, { target: { value: "45" } });
    expect(screen.queryByTestId("reschedule-preview-result")).not.toBeInTheDocument();

    // Preview again
    fireEvent.click(previewBtn);
    expect(await screen.findByTestId("reschedule-preview-result")).toBeInTheDocument();

    // Confirm commit
    const confirmBtn = screen.getByTestId("reschedule-confirm-button");
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(commitSpy).toHaveBeenCalledWith(
        mockEnrollmentId,
        expect.objectContaining({
          dailyTargetMinutes: 45,
          expectedVersion: 1,
        }),
        expect.any(String),
      );
    });

    // Modal closes on success
    await waitFor(() => {
      expect(screen.queryByTestId("reschedule-dialog")).not.toBeInTheDocument();
    });
  });

  it("opens pause dialog from ACTIVE enrollment and pauses successfully", async () => {
    const pauseSpy = vi.spyOn(enrollmentApi, "pause").mockResolvedValue({
      ...mockDetail1,
      status: "PAUSED",
      version: 2,
    });

    renderSchedule();
    const pauseBtn = await screen.findByTestId("schedule-pause-button");
    fireEvent.click(pauseBtn);

    expect(screen.getByTestId("schedule-pause-modal")).toBeInTheDocument();
    expect(screen.getByText("Tạm dừng lộ trình học")).toBeInTheDocument();

    const confirmPauseBtn = screen.getByTestId("schedule-confirm-pause-button");
    fireEvent.click(confirmPauseBtn);

    await waitFor(() => {
      expect(pauseSpy).toHaveBeenCalledWith(mockEnrollmentId, { expectedVersion: 1 });
    });

    await waitFor(() => {
      expect(screen.queryByTestId("schedule-pause-modal")).not.toBeInTheDocument();
    });
  });

  it("shows 'Tiếp tục học' button and paused banner for PAUSED enrollment, resumes with atomic commit", async () => {
    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 45,
      focusedEnrollmentId: mockEnrollmentId2, // PAUSED
    });

    const previewSpy = vi.spyOn(enrollmentApi, "previewReschedule").mockResolvedValue({
      enrollmentVersion: 2,
      proposedEndDate: "2026-11-30",
      modifiedSchedules: [],
    });

    const resumeSpy = vi.spyOn(enrollmentApi, "resume").mockResolvedValue({
      ...mockDetail2,
      status: "ACTIVE",
      version: 3,
    });

    renderSchedule();
    expect(await screen.findByTestId("schedule-paused-banner")).toBeInTheDocument();
    expect(screen.queryByTestId("schedule-open-reschedule-button")).not.toBeInTheDocument();

    const resumeBtn = screen.getByTestId("schedule-resume-button");
    expect(resumeBtn).toHaveTextContent("Tiếp tục học");
    fireEvent.click(resumeBtn);

    // Reschedule dialog opens in resume mode
    expect(screen.getByTestId("reschedule-dialog")).toBeInTheDocument();
    expect(screen.getByText("Tiếp tục lộ trình học")).toBeInTheDocument();

    // Preview
    fireEvent.click(screen.getByTestId("reschedule-preview-button"));
    await waitFor(() => {
      expect(previewSpy).toHaveBeenCalled();
    });

    // Confirm button has resume label
    const confirmBtn = screen.getByTestId("reschedule-confirm-button");
    expect(confirmBtn).toHaveTextContent("Xác nhận tiếp tục học");
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(resumeSpy).toHaveBeenCalledWith(
        mockEnrollmentId2,
        expect.objectContaining({
          expectedVersion: 2,
        }),
        expect.any(String),
      );
    });

    await waitFor(() => {
      expect(screen.queryByTestId("reschedule-dialog")).not.toBeInTheDocument();
    });
  });

  it("opens abandon dialog and abandons enrollment successfully", async () => {
    const abandonSpy = vi.spyOn(enrollmentApi, "abandon").mockResolvedValue({
      ...mockDetail1,
      status: "ABANDONED",
      version: 2,
    });

    renderSchedule();
    const abandonBtn = await screen.findByTestId("schedule-abandon-button");
    fireEvent.click(abandonBtn);

    expect(screen.getByTestId("schedule-abandon-modal")).toBeInTheDocument();
    expect(screen.getByText("Kết thúc lộ trình học")).toBeInTheDocument();

    const confirmAbandonBtn = screen.getByTestId("schedule-confirm-abandon-button");
    fireEvent.click(confirmAbandonBtn);

    await waitFor(() => {
      expect(abandonSpy).toHaveBeenCalledWith(mockEnrollmentId, { expectedVersion: 1 });
    });

    await waitFor(() => {
      expect(screen.queryByTestId("schedule-abandon-modal")).not.toBeInTheDocument();
    });
  });
});
