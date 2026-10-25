import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { EnrollmentDialog } from "../components/EnrollmentDialog";
import { enrollmentApi } from "../api/enrollment.api";
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

  const renderDialog = (isOpen = true) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <EnrollmentDialog
            curriculumId="c1111111-1111-1111-1111-111111111111"
            revisionId="r1111111-1111-1111-1111-111111111111"
            curriculumTitle="Lộ trình Phát âm Anh-Anh Thử nghiệm"
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
});
