import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { EnrollmentDialog } from "../components/EnrollmentDialog";
import { calculateEstimatedPlan } from "../lib/schedule-estimator";
import { enrollmentApi, type DayOfWeek } from "../api/enrollment.api";
import { curriculaApi } from "@/features/curricula/api/curricula.api";
import { authApi } from "@/features/auth";
import { ApiError } from "@/lib/api/api-error";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

describe("EnrollmentDialog", () => {
  let queryClient: QueryClient;
  const mockOnClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = createTestQueryClient();

    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 25,
      focusedEnrollmentId: null,
    });
  });

  const renderDialog = (
    isOpen = true,
    totalRequiredLessons: number | null = 130,
    totalStages: number | null = 26,
  ) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <EnrollmentDialog
            curriculumId="c1111111-1111-1111-1111-111111111111"
            revisionId="r1111111-1111-1111-1111-111111111111"
            curriculumTitle="Lộ trình Phát âm Anh-Anh Thử nghiệm"
            totalRequiredLessons={totalRequiredLessons ?? undefined}
            totalStages={totalStages ?? undefined}
            isOpen={isOpen}
            onClose={mockOnClose}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it("renders form with accessible elements and default values", async () => {
    renderDialog(true);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Đăng ký lộ trình học")).toBeInTheDocument();
    expect(
      screen.getByText("Lộ trình Phát âm Anh-Anh Thử nghiệm"),
    ).toBeInTheDocument();

    const dateInput = await screen.findByLabelText(/Ngày bắt đầu học/i);
    expect(dateInput).toBeInTheDocument();

    const minutesInput = (await screen.findByLabelText(
      /Mục tiêu học mỗi ngày/i,
    )) as HTMLInputElement;
    expect(minutesInput.value).toBe("25");
  });

  it("validates that at least one study day is selected", async () => {
    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });

    // Uncheck MONDAY, WEDNESDAY, FRIDAY
    const monCheckbox = screen.getByLabelText(/T2 \(Thứ Hai\)/i);
    const wedCheckbox = screen.getByLabelText(/T4 \(Thứ Tư\)/i);
    const friCheckbox = screen.getByLabelText(/T6 \(Thứ Sáu\)/i);

    fireEvent.click(monCheckbox);
    fireEvent.click(wedCheckbox);
    fireEvent.click(friCheckbox);

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("Vui lòng chọn ít nhất một ngày học trong tuần"),
      ).toBeInTheDocument();
    });
  });

  it("validates min and max daily target minutes", async () => {
    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });
    const minutesInput = screen.getByLabelText(/Mục tiêu học mỗi ngày/i);
    fireEvent.change(minutesInput, { target: { value: "5" } });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText("Mục tiêu tối thiểu là 10 phút")).toBeInTheDocument();
    });

    fireEvent.change(minutesInput, { target: { value: "200" } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText("Mục tiêu tối đa là 180 phút")).toBeInTheDocument();
    });
  });

  it("preserves stable Idempotency-Key when retrying after network failure", async () => {
    const keysCaptured: string[] = [];

    vi.spyOn(enrollmentApi, "create").mockImplementation(
      async (_payload, idempotencyKey) => {
        keysCaptured.push(idempotencyKey);
        throw new Error("Network connection failed");
      },
    );

    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Không thể kết nối đến máy chủ. Bạn có thể nhấn Đăng ký lại/i),
      ).toBeInTheDocument();
    });

    // Click submit again without modifying fields
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(keysCaptured.length).toBe(2);
      expect(keysCaptured[0]).toBe(keysCaptured[1]); // MUST be the identical key on retry!
    });
  });

  it("generates a new Idempotency-Key when user modifies payload", async () => {
    const keysCaptured: string[] = [];

    vi.spyOn(enrollmentApi, "create").mockImplementation(
      async (_payload, idempotencyKey) => {
        keysCaptured.push(idempotencyKey);
        throw new Error("Network error");
      },
    );

    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(keysCaptured.length).toBe(1);
    });

    // User changes dailyTargetMinutes
    const minutesInput = screen.getByLabelText(/Mục tiêu học mỗi ngày/i);
    fireEvent.change(minutesInput, { target: { value: "45" } });

    // Submit again
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(keysCaptured.length).toBe(2);
      expect(keysCaptured[0]).not.toBe(keysCaptured[1]); // Must be a new key!
    });
  });

  it("displays helpful Vietnamese message on 409 STATE_CONFLICT without clearing form", async () => {
    vi.spyOn(enrollmentApi, "create").mockRejectedValue(
      new ApiError({
        type: "about:blank",
        title: "Xung đột trạng thái",
        status: 409,
        detail: "User already has an active enrollment for this curriculum",
        code: "STATE_CONFLICT",
        traceId: "trace-conflict-1",
        fieldErrors: [],
      }),
    );

    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });
    const minutesInput = screen.getByLabelText(
      /Mục tiêu học mỗi ngày/i,
    ) as HTMLInputElement;
    fireEvent.change(minutesInput, { target: { value: "40" } });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Bạn đã có một lộ trình đang hoạt động hoặc tạm dừng cho giáo trình này. Vui lòng kiểm tra lại trang Hôm nay.",
        ),
      ).toBeInTheDocument();
    });

    // Form inputs must not be wiped out!
    expect(minutesInput.value).toBe("40");
  });

  it("handles successful enrollment: closes dialog and navigates to /today", async () => {
    vi.spyOn(enrollmentApi, "create").mockResolvedValue({
      id: "e1111111-1111-1111-1111-111111111111",
      userId: "u1111111-1111-1111-1111-111111111111",
      curriculumId: "c1111111-1111-1111-1111-111111111111",
      revisionId: "r1111111-1111-1111-1111-111111111111",
      status: "ACTIVE",
      startDate: "2026-10-26",
      studyDays: ["MONDAY", "WEDNESDAY", "FRIDAY"],
      timeZone: "Asia/Ho_Chi_Minh",
      version: 1,
    });

    renderDialog(true);

    const submitBtn = await screen.findByRole("button", {
      name: /Xác nhận đăng ký/i,
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith("/today");
    });
  });

  it("closes when pressing Escape key", () => {
    renderDialog(true);

    fireEvent.keyDown(window, { key: "Escape" });
    expect(mockOnClose).toHaveBeenCalled();
  });

  describe("calculateEstimatedPlan", () => {
    it("counts calendar weeks across the autumn DST transition", () => {
      vi.stubEnv("TZ", "America/New_York");
      try {
        const days: DayOfWeek[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
        expect(calculateEstimatedPlan("2026-10-29", days, 7)).toEqual({
          endDateStr: "04/11/2026", totalWeeks: 1,
        });
      } finally {
        vi.unstubAllEnvs();
      }
    });

    it("rejects impossible dates and handles leap days", () => {
      expect(calculateEstimatedPlan("2026-02-30", ["MONDAY"], 1)).toBeNull();
      expect(calculateEstimatedPlan("2028-02-28", ["MONDAY", "TUESDAY", "WEDNESDAY"], 3))
        .toEqual({ endDateStr: "01/03/2028", totalWeeks: 1 });
    });
    it("calculates exact end date for 3 days/week matching evaluation report baseline (28/10/2026 -> 25/08/2027)", () => {
      const days: DayOfWeek[] = ["MONDAY", "WEDNESDAY", "FRIDAY"];
      const plan = calculateEstimatedPlan("2026-10-28", days, 130);
      expect(plan).not.toBeNull();
      expect(plan?.endDateStr).toBe("25/08/2027");
      expect(plan?.totalWeeks).toBe(44);
    });

    it("calculates exact end date for 5 days/week (26 weeks)", () => {
      const days: DayOfWeek[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"];
      const plan = calculateEstimatedPlan("2026-10-26", days, 130);
      expect(plan).not.toBeNull();
      expect(plan?.totalWeeks).toBe(26);
    });

    it("calculates exact end date for pilot curriculum with 5 required lessons (2 weeks)", () => {
      const days: DayOfWeek[] = ["MONDAY", "WEDNESDAY", "FRIDAY"];
      const plan = calculateEstimatedPlan("2026-10-28", days, 5);
      expect(plan).not.toBeNull();
      // Wed 28, Fri 30, Mon 2, Wed 4, Fri 6 Nov 2026
      expect(plan?.endDateStr).toBe("06/11/2026");
      expect(plan?.totalWeeks).toBe(2);
    });

    it("returns null when totalLessons <= 0, studyDays is empty, or date is invalid", () => {
      expect(calculateEstimatedPlan("2026-10-28", ["MONDAY"], 0)).toBeNull();
      expect(calculateEstimatedPlan("2026-10-28", [], 10)).toBeNull();
      expect(calculateEstimatedPlan("", ["MONDAY"], 10)).toBeNull();
    });
  });

  it("displays estimated completion card and distinguishes stages from personal schedule for Roadmap A (130 lessons)", async () => {
    renderDialog(true, 130, 26);

    const card = await screen.findByTestId("estimated-plan-card");
    expect(card).toBeInTheDocument();

    expect(screen.getByText("Thời gian hoàn thành dự kiến:")).toBeInTheDocument();
    expect(screen.getByText("26 chặng • 130 buổi học bắt buộc")).toBeInTheDocument();
    expect(screen.getByText(/3 buổi\/tuần • ~44 tuần/i)).toBeInTheDocument();
    expect(screen.getByText(/Lộ trình gồm 130 buổi học bắt buộc/i)).toBeInTheDocument();
  });

  it("displays dynamic estimation for Pilot curriculum with 5 required lessons (does not hardcode 130)", async () => {
    renderDialog(true, 5, 1);

    const card = await screen.findByTestId("estimated-plan-card");
    expect(card).toBeInTheDocument();

    expect(screen.getByText("1 chặng • 5 buổi học bắt buộc")).toBeInTheDocument();
    expect(screen.getByText(/3 buổi\/tuần • ~2 tuần/i)).toBeInTheDocument();
    expect(screen.getByText(/Lộ trình gồm 5 buổi học bắt buộc/i)).toBeInTheDocument();
  });

  it("shows fallback message when required lessons are not yet available", async () => {
    const outlineMock = vi.spyOn(curriculaApi, "outline").mockRejectedValue(new Error("Network error"));

    renderDialog(true, null, null);

    await waitFor(() => {
      expect(
        screen.getByText("Chưa tính được ngày dự kiến hoàn thành."),
      ).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Tải lại cấu trúc lộ trình" })).toBeInTheDocument();
    outlineMock.mockResolvedValue({ id: "r1", version: "1.0.0", status: "PUBLISHED", phases: [] });
    fireEvent.click(screen.getByRole("button", { name: "Tải lại cấu trúc lộ trình" }));
    await waitFor(() => expect(screen.queryByRole("button", { name: "Tải lại cấu trúc lộ trình" })).not.toBeInTheDocument());
  });

  it("dynamically resolves required lessons from curriculaApi.outline query when prop is omitted", async () => {
    vi.spyOn(curriculaApi, "outline").mockResolvedValue({
      id: "r1111111-1111-1111-1111-111111111111",
      version: "1.0.0",
      status: "PUBLISHED",
      phases: [
        {
          id: "p1",
          key: "phase1",
          title: "Phase 1",
          orderIndex: 0,
          weeks: [
            {
              id: "w1",
              key: "week1",
              title: "Week 1",
              weekNumber: 1,
              orderIndex: 0,
              lessons: [
                { id: "l1", key: "l1", title: "L1", orderIndex: 0, estimatedMinutes: 25, required: true },
                { id: "l2", key: "l2", title: "L2", orderIndex: 1, estimatedMinutes: 25, required: true },
                { id: "l3", key: "l3", title: "L3", orderIndex: 2, estimatedMinutes: 25, required: false }, // optional lesson
              ],
            },
          ],
        },
      ],
    });

    renderDialog(true, null, null);

    await waitFor(() => {
      // 2 required lessons out of 3 total lessons, 1 stage
      expect(screen.getByText("1 chặng • 2 buổi học bắt buộc")).toBeInTheDocument();
    });
  });
});
