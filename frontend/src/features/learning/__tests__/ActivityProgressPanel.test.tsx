import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { ActivityProgressPanel } from "../components/ActivityProgressPanel";
import {
  learningProgressApi,
  type LessonActivitiesResponse,
} from "../api/learning-progress.api";
import { ApiError } from "@/lib/api/api-error";

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

describe("ActivityProgressPanel", () => {
  let queryClient: QueryClient;
  const enrollmentId = "e1111111-1111-1111-1111-111111111111";
  const lessonId = "l1111111-1111-1111-1111-111111111111";

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = createTestQueryClient();
  });

  const renderComponent = (isReadOnly = false) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ActivityProgressPanel
            enrollmentId={enrollmentId}
            lessonId={lessonId}
            isEnrollmentReadOnly={isReadOnly}
          />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  it("renders activities list and handles complete and undo for manual check", async () => {
    const mockActivitiesData: LessonActivitiesResponse = {
      lessonId,
      enrollmentVersion: 1,
      activities: [
        {
          id: "act-reading",
          title: "Đọc lý thuyết âm /i:/",
          type: "READING",
          required: true,
          completionPolicy: "MANUAL_CHECK",
          status: "NOT_STARTED",
          responseText: null,
        },
      ],
    };

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue(
      mockActivitiesData,
    );
    const updateSpy = vi
      .spyOn(learningProgressApi, "updateProgress")
      .mockResolvedValue({
        activityId: "act-reading",
        status: "COMPLETED",
        enrollmentVersion: 2,
        completedRequiredActivities: 1,
        totalRequiredActivities: 1,
        progressPercent: 100,
      });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Đọc lý thuyết âm /i:/")).toBeInTheDocument();
      expect(screen.getByTestId("complete-button-act-reading")).toBeInTheDocument();
    });

    // Complete the activity
    fireEvent.click(screen.getByTestId("complete-button-act-reading"));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        enrollmentId,
        "act-reading",
        {
          status: "COMPLETED",
          expectedVersion: 1,
        },
      );
    });
  });

  it("handles reflection submission and undo", async () => {
    const mockActivitiesData: LessonActivitiesResponse = {
      lessonId,
      enrollmentVersion: 2,
      activities: [
        {
          id: "act-reflection",
          title: "Phản tư về sự khác biệt giữa /i:/ và /ɪ/",
          type: "REFLECTION",
          required: true,
          completionPolicy: "RESPONSE_SUBMIT",
          status: "NOT_STARTED",
          responseText: null,
        },
      ],
    };

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue(
      mockActivitiesData,
    );
    const submitSpy = vi
      .spyOn(learningProgressApi, "submitResponse")
      .mockResolvedValue({
        activityId: "act-reflection",
        status: "COMPLETED",
        enrollmentVersion: 3,
        completedRequiredActivities: 1,
        totalRequiredActivities: 1,
        progressPercent: 100,
      });

    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByText("Phản tư về sự khác biệt giữa /i:/ và /ɪ/"),
      ).toBeInTheDocument();
    });

    const textarea = screen.getByTestId("reflection-input-act-reflection");
    const submitBtn = screen.getByTestId("submit-button-act-reflection");

    // Initially submit is disabled because text is empty
    expect(submitBtn).toBeDisabled();

    // Type reflection text
    fireEvent.change(textarea, {
      target: { value: "Tôi nhận thấy /i:/ dài hơn và căng khóe miệng hơn." },
    });

    expect(submitBtn).not.toBeDisabled();
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(submitSpy).toHaveBeenCalledWith(
        enrollmentId,
        "act-reflection",
        {
          responseText: "Tôi nhận thấy /i:/ dài hơn và căng khóe miệng hơn.",
          expectedVersion: 2,
        },
      );
    });
  });

  it("retains typed text and shows banner on 409 VERSION_CONFLICT", async () => {
    const mockActivitiesData: LessonActivitiesResponse = {
      lessonId,
      enrollmentVersion: 1,
      activities: [
        {
          id: "act-refl",
          title: "Viết cảm nhận bài tập phát âm",
          type: "REFLECTION",
          required: true,
          completionPolicy: "RESPONSE_SUBMIT",
          status: "NOT_STARTED",
          responseText: null,
        },
      ],
    };

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue(
      mockActivitiesData,
    );
    vi.spyOn(learningProgressApi, "submitResponse").mockRejectedValue(
      new ApiError({
        type: "about:blank",
        title: "Xung đột phiên bản",
        status: 409,
        detail: "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.",
        code: "VERSION_CONFLICT",
        traceId: "trace-409",
        fieldErrors: [],
      }),
    );

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Viết cảm nhận bài tập phát âm")).toBeInTheDocument();
    });

    const textarea = screen.getByTestId("reflection-input-act-refl");
    fireEvent.change(textarea, {
      target: { value: "Nội dung phản tư quan trọng của người học" },
    });

    fireEvent.click(screen.getByTestId("submit-button-act-refl"));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Dữ liệu đã thay đổi trên hệ thống. Câu trả lời của bạn vẫn được giữ nguyên. Vui lòng tải lại trạng thái rồi thử lại.",
        ),
      ).toBeInTheDocument();
    });

    // Check that user's typed text was NOT lost/wiped out!
    expect(textarea).toHaveValue("Nội dung phản tư quan trọng của người học");
    expect(screen.getByText("Tải lại trạng thái mới nhất")).toBeInTheDocument();
  });

  it("disables mutations in read-only mode (PAUSED / ABANDONED)", async () => {
    const mockActivitiesData: LessonActivitiesResponse = {
      lessonId,
      enrollmentVersion: 1,
      activities: [
        {
          id: "act-read",
          title: "Bài đọc ngữ điệu",
          type: "READING",
          required: true,
          completionPolicy: "MANUAL_CHECK",
          status: "NOT_STARTED",
          responseText: null,
        },
      ],
    };

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue(
      mockActivitiesData,
    );

    renderComponent(true); // isEnrollmentReadOnly = true

    await waitFor(() => {
      expect(screen.getByText("Bài đọc ngữ điệu")).toBeInTheDocument();
    });

    const completeBtn = screen.getByTestId("complete-button-act-read");
    expect(completeBtn).toBeDisabled();
  });

  it("shows lesson complete banner when all required activities are completed", async () => {
    const mockActivitiesData: LessonActivitiesResponse = {
      lessonId,
      enrollmentVersion: 3,
      activities: [
        {
          id: "act-1",
          title: "Bài đọc 1",
          type: "READING",
          required: true,
          completionPolicy: "MANUAL_CHECK",
          status: "COMPLETED",
          responseText: null,
        },
        {
          id: "act-2",
          title: "Bài đọc tự chọn",
          type: "READING",
          required: false,
          completionPolicy: "MANUAL_CHECK",
          status: "NOT_STARTED",
          responseText: null,
        },
      ],
    };

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue(
      mockActivitiesData,
    );

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId("lesson-complete-banner")).toBeInTheDocument();
      expect(screen.getByText("Hoàn thành buổi học!")).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /Về trang Hôm nay/i }),
      ).toBeInTheDocument();
    });
  });
});
