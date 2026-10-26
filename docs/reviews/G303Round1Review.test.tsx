import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useAutosaveNote } from "@/features/notes/hooks/useAutosaveNote";
import { notesApi, type NoteResponse } from "@/features/notes/api/notes.api";
import { ApiError } from "@/lib/api/api-error";
import { ActivityRenderer } from "../components/ActivityRenderer";
import { learningWorkspaceApi } from "../api/learning-workspace.api";

vi.mock("react-router-dom", async () => ({
  ...await vi.importActual("react-router-dom"), useBlocker: vi.fn().mockReturnValue(undefined),
}));
const note: NoteResponse = { id: "n1", enrollmentId: "e", lessonId: "l1", activityId: null,
  content: "old", version: 0, createdAt: "2026-10-26T00:00:00Z", updatedAt: "2026-10-26T00:00:00Z" };
afterEach(() => { vi.restoreAllMocks(); vi.useRealTimers(); });

describe("G3-03 remaining review findings after repair 1", () => {
  it("saving response draft must preserve entered text while request is pending", () => {
    vi.spyOn(learningWorkspaceApi, "saveResponseDraft").mockImplementation(() => new Promise(() => {}));
    render(<ActivityRenderer activity={{ activityId: "a", title: "Reflection", type: "REFLECTION",
      required: true, completionPolicy: "RESPONSE_SUBMIT", instructions: "Write", status: "NOT_STARTED",
      userResponse: null, resources: [] }} index={0} enrollmentId="e" enrollmentVersion={1}
      onVersionUpdate={vi.fn()} onActivityUpdated={vi.fn()} />);
    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: "My unsaved answer" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp" }));
    expect(input).toHaveValue("My unsaved answer");
  });

  it("old lesson response must not replace the current lesson note", async () => {
    let resolve!: (n: NoteResponse) => void;
    vi.spyOn(notesApi, "updateNote").mockImplementation(() => new Promise(r => { resolve = r; }));
    const other = { ...note, id: "n2", lessonId: "l2", content: "lesson two" };
    const { result, rerender } = renderHook(({ initialNote }) => useAutosaveNote({
      enrollmentId: "e", lessonId: initialNote.lessonId ?? undefined, initialNote,
    }), { initialProps: { initialNote: note } });
    act(() => result.current.updateContent("lesson one update"));
    act(() => { void result.current.saveImmediately(); });
    rerender({ initialNote: other });
    expect(result.current.currentNote?.id).toBe("n2");
    await act(async () => { resolve({ ...note, content: "lesson one update", version: 1 }); });
    expect(result.current.currentNote?.id).toBe("n2");
    expect(result.current.content).toBe("lesson two");
  });

  it("conflict must cancel scheduled writes until explicit user resolution", async () => {
    vi.useFakeTimers();
    let reject!: (reason: Error) => void;
    const update = vi.spyOn(notesApi, "updateNote").mockImplementation(() => new Promise((_r, j) => { reject = j; }));
    const { result } = renderHook(() => useAutosaveNote({ enrollmentId: "e", lessonId: "l1", initialNote: note }));
    act(() => result.current.updateContent("A"));
    act(() => { void result.current.saveImmediately(); });
    act(() => result.current.updateContent("B"));
    await act(async () => { reject(new ApiError({ type: "about:blank", title: "Conflict", status: 409,
      code: "VERSION_CONFLICT", detail: "Conflict", traceId: "test", fieldErrors: [] })); });
    expect(result.current.status).toBe("conflict");
    await act(async () => { vi.advanceTimersByTime(701); });
    expect(update).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe("conflict");
  });
});
