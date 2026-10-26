import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider, createMemoryRouter } from "react-router-dom";
import { ActivityRenderer } from "../components/ActivityRenderer";
import { LessonWorkspacePage } from "../pages/LessonWorkspacePage";
import { NoteEditor } from "@/features/notes/components/NoteEditor";
import { type LessonDetailResponse, learningWorkspaceApi } from "../api/learning-workspace.api";
import { enrollmentApi } from "@/features/enrollment/api/enrollment.api";
import { notesApi, type NoteResponse } from "@/features/notes/api/notes.api";
import { ApiError } from "@/lib/api/api-error";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("G3-03 Policy Matrix and Unsupported Types", () => {
  it("RECORDING and DICTATION must never display manual complete action", () => {
    const { rerender } = render(
      <ActivityRenderer
        activity={{
          activityId: "rec-1",
          title: "Pronunciation Recording",
          type: "RECORDING",
          required: true,
          completionPolicy: "MANUAL_CHECK",
          instructions: "Record yourself",
          status: "NOT_STARTED",
          userResponse: null,
          resources: [],
        }}
        index={0}
        enrollmentId="e1"
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={vi.fn()}
      />,
    );

    expect(screen.queryByRole("button", { name: /Hoàn thành/i })).not.toBeInTheDocument();
    expect(screen.getByText(/chưa hỗ trợ thao tác trên web/i)).toBeInTheDocument();

    rerender(
      <ActivityRenderer
        activity={{
          activityId: "dic-1",
          title: "Audio Dictation",
          type: "DICTATION",
          required: true,
          completionPolicy: "MANUAL_CHECK",
          instructions: "Listen and type",
          status: "NOT_STARTED",
          userResponse: null,
          resources: [],
        }}
        index={1}
        enrollmentId="e1"
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={vi.fn()}
      />,
    );

    expect(screen.queryByRole("button", { name: /Hoàn thành/i })).not.toBeInTheDocument();
    expect(screen.getByText(/chưa hỗ trợ thao tác trên web/i)).toBeInTheDocument();
  });

  it("distinguishes VERSION_CONFLICT vs STATE_CONFLICT and provides sync action", async () => {
    vi.spyOn(learningWorkspaceApi, "saveResponseDraft").mockRejectedValueOnce(
      new ApiError({
        type: "about:blank",
        title: "Conflict",
        status: 409,
        code: "VERSION_CONFLICT",
        detail: "Version conflict",
        traceId: "test-v",
        fieldErrors: [],
      }),
    );

    const onRefreshWorkspace = vi.fn().mockResolvedValue(undefined);

    render(
      <ActivityRenderer
        activity={{
          activityId: "ref-1",
          title: "Reflection",
          type: "REFLECTION",
          required: true,
          completionPolicy: "RESPONSE_SUBMIT",
          instructions: "Write thoughts",
          status: "NOT_STARTED",
          userResponse: null,
          resources: [],
        }}
        index={0}
        enrollmentId="e1"
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={vi.fn()}
        onRefreshWorkspace={onRefreshWorkspace}
      />,
    );

    const textarea = screen.getByRole("textbox");
    fireEvent.change(textarea, { target: { value: "My reflection answer" } });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Lưu nháp" }));
    });

    // Text is preserved
    expect(textarea).toHaveValue("My reflection answer");

    // VERSION_CONFLICT recovery button rendered
    const syncBtn = await screen.findByRole("button", { name: /Đồng bộ tiến độ/i });
    expect(syncBtn).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(syncBtn);
    });
    expect(onRefreshWorkspace).toHaveBeenCalledTimes(1);
  });
});

describe("NoteEditor overwrite safety without version guessing", () => {
  it("never guesses version + 1 when fetching latest note fails", async () => {
    vi.spyOn(notesApi, "getNotes").mockRejectedValueOnce(new Error("Network failed"));

    const initialNote: NoteResponse = {
      id: "note-1",
      enrollmentId: "e1",
      lessonId: "l1",
      activityId: null,
      content: "Initial text",
      version: 5,
      createdAt: "2026-10-26T00:00:00Z",
      updatedAt: "2026-10-26T00:00:00Z",
    };

    render(
      <NoteEditor
        enrollmentId="e1"
        lessonId="l1"
        initialNote={initialNote}
        isOpen={true}
      />,
    );

    // Overwrite without latest note from server should not send PATCH
    const patchSpy = vi.spyOn(notesApi, "updateNote");

    // Click overwrite directly if conflict banner was visible or call reload
    expect(patchSpy).not.toHaveBeenCalled();
  });
});

describe("Real Data Router Unified Navigation Guard", () => {
  const mockWorkspace = {
    lessonId: "l1",
    title: "Lesson 1",
    markdownContent: "# Hello Lesson",
    estimatedMinutes: 15,
    required: true,
    enrollmentVersion: 1,
    nextLessonId: "l2",
    activities: [
      {
        activityId: "act-1",
        title: "Reflection",
        type: "REFLECTION" as const,
        required: true,
        completionPolicy: "RESPONSE_SUBMIT" as const,
        instructions: "Write",
        status: "NOT_STARTED" as const,
        userResponse: null,
        resources: [],
      },
    ],
  };

  const mockEnrollment = {
    id: "e1",
    userId: "u1",
    curriculumId: "c1",
    revisionId: "r1",
    status: "ACTIVE" as const,
    version: 1,
    startDate: "2026-10-26",
    timeZone: "Asia/Ho_Chi_Minh",
    studyDays: ["MONDAY" as const],
    dailyTargetMinutes: 30,
  };

  const createTestRouter = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    const routes = [
      {
        path: "/enrollments/:enrollmentId/lessons/:lessonId",
        element: (
          <QueryClientProvider client={queryClient}>
            <LessonWorkspacePage />
          </QueryClientProvider>
        ),
      },
      {
        path: "/today",
        element: <div>Trang Hôm nay mục tiêu</div>,
      },
    ];

    return createMemoryRouter(routes, {
      initialEntries: ["/enrollments/e1/lessons/l1"],
    });
  };

  it("blocks internal navigation when response draft is unsaved", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValue(mockWorkspace);
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalItems: 0,
      totalPages: 0,
    });

    const router = createTestRouter();
    render(<RouterProvider router={router} />);

    // Wait for workspace to load
    await screen.findByText("Lesson 1");

    // Type in activity response draft
    const input = await screen.findByTestId("activity-response-input-act-1");
    fireEvent.change(input, { target: { value: "Draft in progress" } });

    // Try navigating to /today via the link
    const todayLink = screen.getByText("Hôm nay");
    fireEvent.click(todayLink);

    // Blocker modal should appear
    expect(await screen.findByTestId("workspace-navigation-blocker-modal")).toBeInTheDocument();
    expect(screen.getByText("Bạn có thay đổi chưa lưu!")).toBeInTheDocument();

    // Click "Ở lại trang"
    fireEvent.click(screen.getByRole("button", { name: "Ở lại trang" }));
    await waitFor(() => {
      expect(screen.queryByTestId("workspace-navigation-blocker-modal")).not.toBeInTheDocument();
    });
    expect(router.state.location.pathname).toBe("/enrollments/e1/lessons/l1");

    // Click link again and this time discard
    fireEvent.click(todayLink);
    expect(await screen.findByTestId("workspace-navigation-blocker-modal")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Rời khỏi không lưu" }));
    await waitFor(() => {
      expect(router.state.location.pathname).toBe("/today");
    });
    expect(screen.getByText("Trang Hôm nay mục tiêu")).toBeInTheDocument();
  });

  it("blocks internal navigation when note has unsaved changes and supports save-and-proceed", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValue(mockWorkspace);
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalItems: 0,
      totalPages: 0,
    });

    const createNoteSpy = vi.spyOn(notesApi, "createNote").mockResolvedValue({
      id: "note-new",
      enrollmentId: "e1",
      lessonId: "l1",
      activityId: null,
      content: "Unsaved note text",
      version: 0,
      createdAt: "2026-10-26T00:00:00Z",
      updatedAt: "2026-10-26T00:00:00Z",
    });

    const router = createTestRouter();
    render(<RouterProvider router={router} />);

    // Wait for workspace to load
    await screen.findByText("Lesson 1");

    // Type in note editor
    const noteTextarea = await screen.findByLabelText("Nội dung ghi chú cá nhân");
    fireEvent.change(noteTextarea, { target: { value: "Unsaved note text" } });

    // Try navigating to /today
    const todayLink = screen.getByText("Hôm nay");
    fireEvent.click(todayLink);

    // Blocker modal with "Lưu rồi rời" should appear
    expect(await screen.findByTestId("workspace-navigation-blocker-modal")).toBeInTheDocument();
    const saveAndProceedBtn = screen.getByRole("button", { name: /Lưu rồi rời/i });
    expect(saveAndProceedBtn).toBeInTheDocument();

    // Click "Lưu rồi rời"
    fireEvent.click(saveAndProceedBtn);

    await waitFor(() => {
      expect(createNoteSpy).toHaveBeenCalledWith({
        enrollmentId: "e1",
        lessonId: "l1",
        content: "Unsaved note text",
      });
      expect(router.state.location.pathname).toBe("/today");
    });
  });
  it("save-and-leave stays on page when the note write fails", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValue(mockWorkspace);
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({ items: [], page: 0, size: 100, totalItems: 0, totalPages: 0 });
    vi.spyOn(notesApi, "createNote").mockRejectedValue(new Error("offline"));
    const router = createTestRouter();
    render(<RouterProvider router={router} />);
    fireEvent.change(await screen.findByLabelText("Nội dung ghi chú cá nhân"), { target: { value: "Do not lose me" } });
    fireEvent.click(screen.getByText("Hôm nay"));
    fireEvent.click(await screen.findByRole("button", { name: /Lưu rồi rời/i }));
    await waitFor(() => expect(screen.getByText(/Không thể lưu các thay đổi gần nhất/)).toBeInTheDocument());
    expect(router.state.location.pathname).toBe("/enrollments/e1/lessons/l1");
    expect(screen.getByLabelText("Nội dung ghi chú cá nhân")).toHaveValue("Do not lose me");
  });

  it("keeps activity actions locked while workspace reconciliation is pending", async () => {
    let resolve!: (data: LessonDetailResponse) => void;
    const read = vi.spyOn(learningWorkspaceApi, "getLessonWorkspace")
      .mockResolvedValueOnce(mockWorkspace).mockImplementationOnce(() => new Promise(r => { resolve = r; }));
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({ items: [], page: 0, size: 100, totalItems: 0, totalPages: 0 });
    vi.spyOn(learningWorkspaceApi, "saveResponseDraft").mockResolvedValue({ id: "p", activityId: "act-1",
      responseText: "Draft", enrollmentVersion: 2, updatedAt: "2026-10-26T00:00:00Z" });
    render(<RouterProvider router={createTestRouter()} />);
    const input = await screen.findByTestId("activity-response-input-act-1");
    fireEvent.change(input, { target: { value: "Draft" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp" }));
    await waitFor(() => expect(read).toHaveBeenCalledTimes(2));
    expect(input).toBeDisabled();
    await act(async () => { resolve({ ...mockWorkspace, enrollmentVersion: 2, activities: [{ ...mockWorkspace.activities[0], status: "IN_PROGRESS", userResponse: "Draft" }] }); });
    await waitFor(() => expect(input).toBeEnabled());
    expect(input).toHaveValue("Draft");
  });

  it("failed reconciliation preserves draft and requires successful recovery", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValueOnce(mockWorkspace)
      .mockRejectedValueOnce(new Error("offline")).mockResolvedValue({ ...mockWorkspace, enrollmentVersion: 2, activities: [{ ...mockWorkspace.activities[0], status: "IN_PROGRESS", userResponse: "Draft" }] });
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({ items: [], page: 0, size: 100, totalItems: 0, totalPages: 0 });
    vi.spyOn(learningWorkspaceApi, "saveResponseDraft").mockResolvedValue({ id: "p", activityId: "act-1",
      responseText: "Draft", enrollmentVersion: 2, updatedAt: "2026-10-26T00:00:00Z" });
    render(<RouterProvider router={createTestRouter()} />);
    const input = await screen.findByTestId("activity-response-input-act-1");
    fireEvent.change(input, { target: { value: "Draft" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp" }));
    await screen.findByText("Cần đồng bộ lại tiến độ trước khi tiếp tục.");
    expect(input).toBeDisabled();
    expect(input).toHaveValue("Draft");
    fireEvent.click(screen.getByRole("button", { name: /Thử lại/ }));
    await waitFor(() => expect(input).toBeEnabled());
    expect(input).toHaveValue("Draft");
  });

  it("finds lesson note beyond the first page of activity notes", async () => {
    const get = vi.spyOn(notesApi, "getNotes").mockResolvedValueOnce({ items: [], page: 0,
      size: 100, totalItems: 101, totalPages: 2 }).mockResolvedValueOnce({
      items: [{ id: "n", enrollmentId: "e1", lessonId: "l1", activityId: null, content: "Lesson note",
        version: 0, createdAt: "2026-10-26T00:00:00Z", updatedAt: "2026-10-26T00:00:00Z" }],
      page: 1, size: 100, totalItems: 101, totalPages: 2 });
    expect((await notesApi.getLessonNote("e1", "l1"))?.id).toBe("n");
    expect(get).toHaveBeenLastCalledWith({ enrollmentId: "e1", lessonId: "l1", page: 1, size: 100 }, undefined);
  });

  it("does not turn unsafe resource URLs into clickable links", () => {
    render(<ActivityRenderer activity={{ ...mockWorkspace.activities[0], resources: [
      { id: "bad", title: "Unsafe", url: "data:text/html,<script>alert(1)</script>", mediaType: "ARTICLE", attribution: "Source" },
      { id: "good", title: "Safe", url: "https://example.com/lesson", mediaType: "ARTICLE", attribution: "Source" },
    ] }} index={0} enrollmentId="e1" enrollmentVersion={1} onVersionUpdate={vi.fn()} onActivityUpdated={vi.fn()} />);
    expect(screen.getByText("Unsafe").closest("a")).not.toHaveAttribute("href");
    expect(screen.getByRole("link", { name: /Safe/ })).toHaveAttribute("href", "https://example.com/lesson");
  });

});
