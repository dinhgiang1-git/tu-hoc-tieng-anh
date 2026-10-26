import { act, fireEvent, render, renderHook, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useAutosaveNote } from "@/features/notes/hooks/useAutosaveNote";
import { notesApi, type NoteResponse } from "@/features/notes/api/notes.api";
import { ActivityRenderer } from "../components/ActivityRenderer";
import { learningProgressApi } from "../api/learning-progress.api";
import { learningWorkspaceApi } from "../api/learning-workspace.api";
import { enrollmentApi } from "@/features/enrollment/api/enrollment.api";
import { LessonWorkspacePage } from "../pages/LessonWorkspacePage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";

vi.mock("react-router-dom", async () => ({
  ...await vi.importActual("react-router-dom"), useBlocker: vi.fn().mockReturnValue(undefined),
}));
const note: NoteResponse = { id: "n", enrollmentId: "e", lessonId: "l", activityId: null,
  content: "original", version: 0, createdAt: "2026-10-26T00:00:00Z", updatedAt: "2026-10-26T00:00:00Z" };
afterEach(() => { vi.restoreAllMocks(); vi.useRealTimers(); });

describe("G3-03 reviewer reproductions", () => {
  it("focus mode must preserve an unsaved lesson note", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValue({
      lessonId: "l", title: "Lesson", markdownContent: "Text", estimatedMinutes: 10,
      required: true, enrollmentVersion: 1, nextLessonId: null, activities: [],
    });
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({ id: "e", userId: "u", curriculumId: "c",
      revisionId: "r", status: "ACTIVE", version: 1, startDate: "2026-10-26", timeZone: "Asia/Ho_Chi_Minh",
      studyDays: ["MONDAY"], dailyTargetMinutes: 30 });
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 });
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
      <Routes><Route path="/enrollments/:enrollmentId/lessons/:lessonId" element={<LessonWorkspacePage />} /></Routes>
    </MemoryRouter></QueryClientProvider>);
    fireEvent.change(await screen.findByLabelText("Nội dung ghi chú cá nhân"), { target: { value: "Unsaved note" } });
    fireEvent.click(screen.getByRole("button", { name: "Chế độ tập trung" }));
    expect(screen.getByLabelText("Nội dung ghi chú cá nhân")).toHaveValue("Unsaved note");
  });

  it("saving existing note must not reset editor or version to initial server snapshot", async () => {
    vi.spyOn(notesApi, "updateNote").mockResolvedValue({ ...note, content: "updated", version: 1 });
    const { result } = renderHook(() => useAutosaveNote({ enrollmentId: "e", lessonId: "l", initialNote: note }));
    act(() => result.current.updateContent("updated"));
    await act(async () => { await result.current.saveImmediately(); });
    expect(result.current.content).toBe("updated");
    expect(result.current.currentNote?.version).toBe(1);
  });

  it("A response must not mark unsent B saved before debounce fires", async () => {
    vi.useFakeTimers();
    let resolve!: (n: NoteResponse) => void;
    vi.spyOn(notesApi, "updateNote").mockImplementation(() => new Promise(r => { resolve = r; }));
    const { result } = renderHook(() => useAutosaveNote({ enrollmentId: "e", lessonId: "l", initialNote: note }));
    act(() => result.current.updateContent("A"));
    act(() => { void result.current.saveImmediately(); });
    act(() => result.current.updateContent("B"));
    await act(async () => { resolve({ ...note, content: "A", version: 1 }); });
    expect(result.current.hasUnsavedChanges).toBe(true);
    expect(result.current.status).not.toBe("saved");
    expect(result.current.content).toBe("B");
  });

  it("manual PRACTICE must expose a manual complete action", () => {
    render(<ActivityRenderer activity={{ activityId: "a", title: "Practice", type: "PRACTICE",
      required: true, completionPolicy: "MANUAL_CHECK", instructions: "Read aloud", status: "NOT_STARTED",
      userResponse: null, resources: [] }} index={0} enrollmentId="e" enrollmentVersion={1}
      onVersionUpdate={vi.fn()} onActivityUpdated={vi.fn()} />);
    expect(screen.getByRole("button", { name: /^Hoàn thành$/ })).toBeInTheDocument();
  });

  it("workspace must lock all activities while an enrollment mutation is pending", async () => {
    vi.spyOn(learningWorkspaceApi, "getLessonWorkspace").mockResolvedValue({
      lessonId: "l", title: "Lesson", markdownContent: "Text", estimatedMinutes: 10,
      required: true, enrollmentVersion: 1, nextLessonId: null,
      activities: ["a", "b"].map(activityId => ({ activityId, title: activityId, type: "READING" as const,
        required: true, completionPolicy: "MANUAL_CHECK" as const, instructions: "Read",
        status: "NOT_STARTED" as const, userResponse: null, resources: [] })),
    });
    vi.spyOn(enrollmentApi, "getDetail").mockResolvedValue({ id: "e", userId: "u", curriculumId: "c",
      revisionId: "r", status: "ACTIVE", version: 1, startDate: "2026-10-26", timeZone: "Asia/Ho_Chi_Minh",
      studyDays: ["MONDAY"], dailyTargetMinutes: 30 });
    vi.spyOn(notesApi, "getNotes").mockResolvedValue({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 });
    const update = vi.spyOn(learningProgressApi, "updateProgress").mockImplementation(() => new Promise(() => {}));
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><MemoryRouter initialEntries={["/enrollments/e/lessons/l"]}>
      <Routes><Route path="/enrollments/:enrollmentId/lessons/:lessonId" element={<LessonWorkspacePage />} /></Routes>
    </MemoryRouter></QueryClientProvider>);
    const buttons = await screen.findAllByRole("button", { name: /^Hoàn thành$/ });
    fireEvent.click(buttons[0]);
    await waitFor(() => expect(update).toHaveBeenCalledTimes(1));
    expect(buttons[1]).toBeDisabled();
  });
});
