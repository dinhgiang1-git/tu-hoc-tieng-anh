import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { LessonWorkspacePage } from "../pages/LessonWorkspacePage";
import { learningWorkspaceApi, type LessonDetailResponse } from "../api/learning-workspace.api";
import { enrollmentApi, type EnrollmentDetailResponse } from "@/features/enrollment/api/enrollment.api";
import { notesApi, type NotePageResponse } from "@/features/notes/api/notes.api";

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useBlocker: vi.fn().mockReturnValue(undefined),
  };
});

describe("LessonWorkspacePage", () => {
  const enrollmentId = "11111111-1111-1111-1111-111111111111";
  const lessonId = "22222222-2222-2222-2222-222222222222";
  const nextLessonId = "33333333-3333-3333-3333-333333333333";

  let queryClient: QueryClient;

  const mockWorkspace: LessonDetailResponse = {
    lessonId,
    title: "Buổi 1: Nguyên âm cơ bản",
    markdownContent: `
# Phát âm chuẩn IPA
Bảng nguyên âm cơ bản trong Tiếng Anh:
| Ký hiệu | Ví dụ |
|---|---|
| /iː/ | see |
| /ɪ/ | sit |
`,
    estimatedMinutes: 30,
    required: true,
    enrollmentVersion: 1,
    nextLessonId,
    activities: [
      {
        activityId: "act-1",
        title: "Luyện đọc cặp âm /iː/ và /ɪ/",
        type: "PRACTICE",
        required: true,
        completionPolicy: "MANUAL_CHECK",
        instructions: "Nghe audio và lặp lại 5 lần.",
        status: "NOT_STARTED",
        userResponse: null,
        resources: [
          {
            id: "res-1",
            title: "Audio BBC Vowels",
            url: "https://bbc.co.uk/vowels",
            mediaType: "AUDIO",
            attribution: "BBC Learning English",
          },
        ],
      },
    ],
  };

  const mockEnrollment: EnrollmentDetailResponse = {
    id: enrollmentId,
    userId: "user-1",
    curriculumId: "curr-1",
    revisionId: "rev-1",
    version: 1,
    status: "ACTIVE",
    startDate: "2026-10-26",
    timeZone: "Asia/Ho_Chi_Minh",
    studyDays: ["MONDAY", "WEDNESDAY", "FRIDAY"],
    dailyTargetMinutes: 30,
  };

  const mockNotes: NotePageResponse = {
    items: [],
    page: 0,
    size: 20,
    totalItems: 0,
    totalPages: 0,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
      },
    });
  });

  const renderComponent = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/enrollments/${enrollmentId}/lessons/${lessonId}`]}
        >
          <Routes>
            <Route
              path="/enrollments/:enrollmentId/lessons/:lessonId"
              element={<LessonWorkspacePage />}
            />
            <Route path="/today" element={<div>Trang Hôm Nay</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

  it("renders lesson workspace with markdown content, activity, resources, and note editor", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValueOnce(
      mockWorkspace,
    );
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValueOnce(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValueOnce(mockNotes);

    renderComponent();

    // Verify Title & Meta
    expect(
      await screen.findByRole("heading", { name: "Buổi 1: Nguyên âm cơ bản" }),
    ).toBeInTheDocument();

    // Verify Markdown content rendered
    expect(screen.getByText("Phát âm chuẩn IPA")).toBeInTheDocument();
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByText("/iː/")).toBeInTheDocument();

    // Verify Activity rendered
    expect(
      screen.getByText("Luyện đọc cặp âm /iː/ và /ɪ/"),
    ).toBeInTheDocument();
    expect(screen.getByText("Audio BBC Vowels")).toBeInTheDocument();
    expect(
      screen.getByText("Nguồn: BBC Learning English"),
    ).toBeInTheDocument();

    // Verify Note editor rendered
    expect(screen.getByTestId("note-editor")).toBeInTheDocument();

    // Verify Next lesson button exists
    expect(screen.getByRole("button", { name: /Bài tiếp/i })).toBeInTheDocument();
  });

  it("toggles focus mode on and off via button and Escape key", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValueOnce(
      mockWorkspace,
    );
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValueOnce(mockEnrollment);
    vi.spyOn(notesApi, "getNotes").mockResolvedValueOnce(mockNotes);

    renderComponent();

    const focusBtn = await screen.findByRole("button", {
      name: /Chế độ tập trung/i,
    });
    fireEvent.click(focusBtn);

    // In focus mode
    expect(screen.getByTestId("focus-mode-container")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Thoát tập trung/i }),
    ).toBeInTheDocument();

    // Press Escape to exit focus mode
    fireEvent.keyDown(window, { key: "Escape" });

    // Focus mode exited
    expect(screen.queryByTestId("focus-mode-container")).toBeNull();
  });

  it("saves activity response draft with expectedVersion", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValueOnce({
      ...mockWorkspace,
      activities: [
        {
          ...mockWorkspace.activities[0],
          completionPolicy: "RESPONSE_SUBMIT",
        },
      ],
    }).mockResolvedValue({ ...mockWorkspace, enrollmentVersion: 2,
      activities: [{ ...mockWorkspace.activities[0], completionPolicy: "RESPONSE_SUBMIT",
        status: "IN_PROGRESS", userResponse: "Bản ghi phản tư nháp" }] });
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({ ...mockEnrollment, version: 2 });
    vi.spyOn(notesApi, "getNotes").mockResolvedValueOnce(mockNotes);

    const saveDraftSpy = vi
      .spyOn(learningWorkspaceApi, "saveResponseDraft")
      .mockResolvedValueOnce({
        id: "prog-1",
        activityId: "act-1",
        responseText: "Bản ghi phản tư nháp",
        updatedAt: new Date().toISOString(),
        enrollmentVersion: 2,
      });

    renderComponent();

    const textarea = await screen.findByTestId("activity-response-input-act-1");
    fireEvent.change(textarea, { target: { value: "Bản ghi phản tư nháp" } });

    const saveDraftBtn = screen.getByRole("button", { name: /Lưu nháp/i });
    fireEvent.click(saveDraftBtn);

    await waitFor(() => {
      expect(saveDraftSpy).toHaveBeenCalledWith(
        enrollmentId,
        "act-1",
        {
          responseText: "Bản ghi phản tư nháp",
          expectedVersion: 1,
        },
      );
    });

    expect(await screen.findByText("Đã lưu nháp")).toBeInTheDocument();
    // Enrollment version updated
    expect(textarea).toHaveValue("Bản ghi phản tư nháp");
  });
});
