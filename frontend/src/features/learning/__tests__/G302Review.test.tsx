import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ActivityProgressPanel } from "../components/ActivityProgressPanel";
import { LessonSummaryPage } from "../pages/LessonSummaryPage";
import { learningProgressApi, type LessonActivitiesResponse } from "../api/learning-progress.api";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { ApiError } from "@/lib/api/api-error";

describe("G302Review Frontend Regressions", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const createTestClient = () =>
    new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

  it("R2-1: cannot send another mutation with stale version while refresh is pending", async () => {
    vi.spyOn(learningProgressApi, "getLessonActivities")
      .mockResolvedValueOnce({
        lessonId: "l",
        enrollmentVersion: 1,
        activities: ["a", "b"].map((id) => ({
          id,
          title: id,
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "NOT_STARTED" as const,
          responseText: null,
        })),
      })
      .mockImplementation(() => new Promise(() => {}));

    const update = vi.spyOn(learningProgressApi, "updateProgress").mockResolvedValue({
      activityId: "a",
      status: "COMPLETED",
      enrollmentVersion: 2,
      completedRequiredActivities: 1,
      totalRequiredActivities: 2,
      progressPercent: 50,
    });

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <ActivityProgressPanel enrollmentId="e" lessonId="l" />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByTestId("complete-button-a"));
    await waitFor(() => expect(update).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByTestId("complete-button-b")).toBeDisabled());
  });

  it("R2-2: subsequent action uses version 2 once fresh data arrives", async () => {
    let resolveRefetch!: (val: LessonActivitiesResponse) => void;
    const refetchPromise = new Promise<LessonActivitiesResponse>((resolve) => {
      resolveRefetch = resolve;
    });

    vi.spyOn(learningProgressApi, "getLessonActivities")
      .mockResolvedValueOnce({
        lessonId: "l",
        enrollmentVersion: 1,
        activities: ["a", "b"].map((id) => ({
          id,
          title: id,
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "NOT_STARTED" as const,
          responseText: null,
        })),
      })
      .mockImplementationOnce(() => refetchPromise);

    const update = vi
      .spyOn(learningProgressApi, "updateProgress")
      .mockResolvedValueOnce({
        activityId: "a",
        status: "COMPLETED",
        enrollmentVersion: 2,
        completedRequiredActivities: 1,
        totalRequiredActivities: 2,
        progressPercent: 50,
      })
      .mockResolvedValueOnce({
        activityId: "b",
        status: "COMPLETED",
        enrollmentVersion: 3,
        completedRequiredActivities: 2,
        totalRequiredActivities: 2,
        progressPercent: 100,
      });

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <ActivityProgressPanel enrollmentId="e" lessonId="l" />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Initial click on activity 'a'
    fireEvent.click(await screen.findByTestId("complete-button-a"));
    await waitFor(() => expect(update).toHaveBeenCalledTimes(1));

    // Resolve the background refetch with updated version 2
    resolveRefetch!({
      lessonId: "l",
      enrollmentVersion: 2,
      activities: [
        {
          id: "a",
          title: "a",
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "COMPLETED" as const,
          responseText: null,
        },
        {
          id: "b",
          title: "b",
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "NOT_STARTED" as const,
          responseText: null,
        },
      ],
    });

    // Now button 'b' becomes enabled
    await waitFor(() => expect(screen.getByTestId("complete-button-b")).toBeEnabled());

    // Click 'b'
    fireEvent.click(screen.getByTestId("complete-button-b"));
    await waitFor(() => expect(update).toHaveBeenCalledTimes(2));

    // Must have sent expectedVersion: 2
    expect(update).toHaveBeenLastCalledWith(
      "e",
      "b",
      expect.objectContaining({ expectedVersion: 2 }),
    );
  });

  it("R2-3: refetch error shows recovery banner and retry restores functionality", async () => {
    vi.spyOn(learningProgressApi, "getLessonActivities")
      .mockResolvedValueOnce({
        lessonId: "l",
        enrollmentVersion: 1,
        activities: [{
          id: "a",
          title: "a",
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "NOT_STARTED" as const,
          responseText: null,
        }],
      })
      .mockRejectedValueOnce(new Error("Network Refetch Error"))
      .mockResolvedValueOnce({
        lessonId: "l",
        enrollmentVersion: 2,
        activities: [{
          id: "a",
          title: "a",
          type: "READING" as const,
          required: true,
          completionPolicy: "MANUAL_CHECK" as const,
          status: "COMPLETED" as const,
          responseText: null,
        }],
      });

    vi.spyOn(learningProgressApi, "updateProgress").mockResolvedValue({
      activityId: "a",
      status: "COMPLETED",
      enrollmentVersion: 2,
      completedRequiredActivities: 1,
      totalRequiredActivities: 1,
      progressPercent: 100,
    });

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <ActivityProgressPanel enrollmentId="e" lessonId="l" />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByTestId("complete-button-a"));

    // Refetch fails, error banner appears
    await waitFor(() =>
      expect(
        screen.getByText("Không thể đồng bộ dữ liệu mới nhất từ máy chủ."),
      ).toBeInTheDocument(),
    );

    expect(screen.getByTestId("undo-button-a")).toBeDisabled();

    // Click retry
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

    // Reconciled successfully
    await waitFor(() =>
      expect(
        screen.queryByText("Không thể đồng bộ dữ liệu mới nhất từ máy chủ."),
      ).not.toBeInTheDocument(),
    );
  });

  it("R2-4: conflict preserves user draft text and does not auto-resubmit", async () => {
    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue({
      lessonId: "l",
      enrollmentVersion: 1,
      activities: [{
        id: "ref1",
        title: "Reflection 1",
        type: "REFLECTION" as const,
        required: true,
        completionPolicy: "RESPONSE_SUBMIT" as const,
        status: "NOT_STARTED" as const,
        responseText: null,
      }],
    });

    const conflictProblem = {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      detail: "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.",
      code: "VERSION_CONFLICT",
      traceId: "test-trace",
      fieldErrors: [],
    };
    const submitSpy = vi
      .spyOn(learningProgressApi, "submitResponse")
      .mockRejectedValue(new ApiError(conflictProblem));

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <ActivityProgressPanel enrollmentId="e" lessonId="l" />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const textarea = await screen.findByTestId("reflection-input-ref1");
    fireEvent.change(textarea, { target: { value: "Draft reflection text" } });
    expect(textarea).toHaveValue("Draft reflection text");

    fireEvent.click(screen.getByTestId("submit-button-ref1"));

    // Check conflict message
    await waitFor(() =>
      expect(
        screen.getByText(
          "Dữ liệu đã thay đổi trên hệ thống. Câu trả lời của bạn vẫn được giữ nguyên. Vui lòng tải lại trạng thái rồi thử lại.",
        ),
      ).toBeInTheDocument(),
    );

    // Text in textarea is retained
    expect(textarea).toHaveValue("Draft reflection text");

    // Button is disabled after conflict
    expect(screen.getByTestId("submit-button-ref1")).toBeDisabled();

    // Exactly one call was made, no auto-resubmit
    expect(submitSpy).toHaveBeenCalledTimes(1);

    // Click "Tải lại trạng thái mới nhất"
    fireEvent.click(screen.getByRole("button", { name: "Tải lại trạng thái mới nhất" }));

    // Draft text is STILL preserved
    await waitFor(() => expect(textarea).toHaveValue("Draft reflection text"));
  });

  it("failed conflict refresh keeps actions locked until a successful retry", async () => {
    const initial = {
      lessonId: "l", enrollmentVersion: 1,
      activities: [{ id: "a", title: "Reading", type: "READING" as const,
        required: true, completionPolicy: "MANUAL_CHECK" as const,
        status: "NOT_STARTED" as const, responseText: null }],
    };
    vi.spyOn(learningProgressApi, "getLessonActivities")
      .mockResolvedValueOnce(initial)
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValue({ ...initial, enrollmentVersion: 2 });
    const update = vi.spyOn(learningProgressApi, "updateProgress")
      .mockRejectedValue(new ApiError({ type: "about:blank", title: "Conflict",
        status: 409, code: "VERSION_CONFLICT", detail: "Conflict",
        traceId: "test", fieldErrors: [] }));
    render(<QueryClientProvider client={createTestClient()}>
      <MemoryRouter><ActivityProgressPanel enrollmentId="e" lessonId="l" /></MemoryRouter>
    </QueryClientProvider>);
    fireEvent.click(await screen.findByTestId("complete-button-a"));
    fireEvent.click(await screen.findByRole("button", { name: "Tải lại trạng thái mới nhất" }));
    await screen.findByText("Không thể đồng bộ dữ liệu mới nhất từ máy chủ.");
    expect(screen.getByTestId("complete-button-a")).toBeDisabled();
    expect(update).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    await waitFor(() => expect(screen.getByTestId("complete-button-a")).toBeEnabled());
    expect(screen.getByText("Phiên bản lộ trình: v2")).toBeInTheDocument();
    expect(update).toHaveBeenCalledTimes(1);
  });

  it("preserves draft when background lifecycle refresh fails and allows retry", async () => {
    vi.spyOn(enrollmentApi, "getLessonSummary").mockResolvedValue({
      enrollmentId: "e", lessonId: "l", lessonTitle: "Lesson", curriculumTitle: "Course",
      isRequired: true, estimatedMinutes: 30, plannedDate: "2026-10-26",
    });
    const detail = {
      id: "e", userId: "u", curriculumId: "c", revisionId: "r", status: "ACTIVE" as const,
      startDate: "2026-10-26", timeZone: "Asia/Ho_Chi_Minh",
      studyDays: ["MONDAY" as const], dailyTargetMinutes: 30, version: 1,
    };
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValueOnce(detail)
      .mockRejectedValueOnce(new Error("offline")).mockResolvedValue(detail);
    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue({
      lessonId: "l", enrollmentVersion: 1,
      activities: [{ id: "ref1", title: "Reflection", type: "REFLECTION",
        required: true, completionPolicy: "RESPONSE_SUBMIT", status: "NOT_STARTED", responseText: null }],
    });
    const client = createTestClient();
    render(<QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}><Routes>
        <Route path="/enrollments/:enrollmentId/lessons/:lessonId" element={<LessonSummaryPage />} />
      </Routes></MemoryRouter>
    </QueryClientProvider>);
    fireEvent.change(await screen.findByTestId("reflection-input-ref1"), { target: { value: "My draft" } });
    await client.invalidateQueries({ queryKey: enrollmentKeys.detail("e") });
    await screen.findByText("Không thể đồng bộ trạng thái lộ trình. Câu trả lời đang nhập vẫn được giữ nguyên.");
    expect(screen.getByTestId("reflection-input-ref1")).toHaveValue("My draft");
    expect(screen.getByTestId("submit-button-ref1")).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: /Thử lại/ }));
    await waitFor(() => expect(screen.getByTestId("submit-button-ref1")).toBeEnabled());
    expect(screen.getByTestId("reflection-input-ref1")).toHaveValue("My draft");
  });

  it("R2-5: LessonSummaryPage blocks panel when enrollment detail is pending or error", async () => {
    vi.spyOn(enrollmentApi, "getLessonSummary").mockResolvedValue({
      enrollmentId: "e",
      lessonId: "l",
      lessonTitle: "Lesson 1",
      curriculumTitle: "Curriculum 1",
      isRequired: true,
      estimatedMinutes: 30,
      plannedDate: "2026-10-26",
    });

    // Detail pending
    vi.spyOn(enrollmentApi, "getDetail").mockImplementation(() => new Promise(() => {}));

    const client = createTestClient();
    const { unmount } = render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonSummaryPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Detail pending -> shows skeleton, panel is not rendered
    expect(screen.getByLabelText("Đang tải thông tin bài học")).toBeInTheDocument();
    expect(screen.queryByTestId("activity-progress-panel")).not.toBeInTheDocument();

    unmount();

    // Detail error -> shows error state
    vi.spyOn(enrollmentApi, "getDetail").mockRejectedValue(
      new ApiError({
        type: "about:blank",
        title: "Internal Error",
        status: 500,
        detail: "Server Error",
        code: "INTERNAL_ERROR",
        traceId: "test-trace",
        fieldErrors: [],
      }),
    );

    const client2 = createTestClient();
    render(
      <QueryClientProvider client={client2}>
        <MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonSummaryPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() =>
      expect(
        screen.getByText("Không thể tải thông tin lộ trình học. Vui lòng thử lại sau."),
      ).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("activity-progress-panel")).not.toBeInTheDocument();
  });

  it("R2-6: LessonSummaryPage sets read-only when enrollment is PAUSED or ABANDONED", async () => {
    vi.spyOn(enrollmentApi, "getLessonSummary").mockResolvedValue({
      enrollmentId: "e",
      lessonId: "l",
      lessonTitle: "Lesson 1",
      curriculumTitle: "Curriculum 1",
      isRequired: true,
      estimatedMinutes: 30,
      plannedDate: "2026-10-26",
    });

    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({
      id: "e",
      userId: "u",
      curriculumId: "c",
      revisionId: "r",
      status: "PAUSED",
      startDate: "2026-10-26",
      timeZone: "Asia/Ho_Chi_Minh",
      studyDays: ["MONDAY"],
      dailyTargetMinutes: 30,
      version: 1,
    });

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue({
      lessonId: "l",
      enrollmentVersion: 1,
      activities: [{
        id: "a",
        title: "Reading Activity",
        type: "READING" as const,
        required: true,
        completionPolicy: "MANUAL_CHECK" as const,
        status: "NOT_STARTED" as const,
        responseText: null,
      }],
    });

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonSummaryPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Button should be disabled because enrollment is PAUSED
    await waitFor(() =>
      expect(screen.getByTestId("complete-button-a")).toBeDisabled(),
    );
  });

  it("R2-7: LessonSummaryPage preserves valid undo on COMPLETED enrollment", async () => {
    vi.spyOn(enrollmentApi, "getLessonSummary").mockResolvedValue({
      enrollmentId: "e",
      lessonId: "l",
      lessonTitle: "Lesson 1",
      curriculumTitle: "Curriculum 1",
      isRequired: true,
      estimatedMinutes: 30,
      plannedDate: "2026-10-26",
    });

    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({
      id: "e",
      userId: "u",
      curriculumId: "c",
      revisionId: "r",
      status: "COMPLETED",
      startDate: "2026-10-26",
      timeZone: "Asia/Ho_Chi_Minh",
      studyDays: ["MONDAY"],
      dailyTargetMinutes: 30,
      version: 5,
    });

    vi.spyOn(learningProgressApi, "getLessonActivities").mockResolvedValue({
      lessonId: "l",
      enrollmentVersion: 5,
      activities: [{
        id: "a",
        title: "Reading Activity",
        type: "READING" as const,
        required: true,
        completionPolicy: "MANUAL_CHECK" as const,
        status: "COMPLETED" as const,
        responseText: null,
      }],
    });

    const client = createTestClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonSummaryPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // On COMPLETED enrollment, undo button is NOT disabled!
    await waitFor(() =>
      expect(screen.getByTestId("undo-button-a")).toBeEnabled(),
    );
  });
});
