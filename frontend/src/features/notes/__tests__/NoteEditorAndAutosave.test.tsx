import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import { NoteEditor } from "../components/NoteEditor";
import { notesApi, type NoteResponse } from "../api/notes.api";
import { ApiError } from "@/lib/api/api-error";

// Mock router useBlocker
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useBlocker: vi.fn().mockReturnValue(undefined),
  };
});

describe("NoteEditor and useAutosaveNote", () => {
  const enrollmentId = "11111111-1111-1111-1111-111111111111";
  const lessonId = "22222222-2222-2222-2222-222222222222";

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("creates note after 700ms debounce on initial input", async () => {
    const createdNote: NoteResponse = {
      id: "note-1",
      enrollmentId,
      lessonId,
      activityId: null,
      content: "First note draft",
      version: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const createNoteSpy = vi
      .spyOn(notesApi, "createNote")
      .mockResolvedValueOnce(createdNote);

    render(
      <NoteEditor enrollmentId={enrollmentId} lessonId={lessonId} initialNote={null} />,
    );

    const textarea = screen.getByLabelText("Nội dung ghi chú cá nhân");
    expect(textarea).toHaveValue("");

    // Type text into note
    fireEvent.change(textarea, { target: { value: "First note draft" } });
    expect(screen.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();

    // Fast-forward past 700ms debounce
    await act(async () => {
      vi.advanceTimersByTime(750);
    });

    await waitFor(() => {
      expect(createNoteSpy).toHaveBeenCalledTimes(1);
    });

    expect(createNoteSpy).toHaveBeenCalledWith({
      enrollmentId,
      lessonId,
      content: "First note draft",
    });

    expect(await screen.findByText("Đã lưu")).toBeInTheDocument();
  });

  it("updates existing note with expectedVersion", async () => {
    const existingNote: NoteResponse = {
      id: "note-1",
      enrollmentId,
      lessonId,
      activityId: null,
      content: "Existing text",
      version: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const updatedNote: NoteResponse = {
      ...existingNote,
      content: "Existing text updated",
      version: 1,
      updatedAt: new Date().toISOString(),
    };

    const updateNoteSpy = vi
      .spyOn(notesApi, "updateNote")
      .mockResolvedValueOnce(updatedNote);

    render(
      <NoteEditor
        enrollmentId={enrollmentId}
        lessonId={lessonId}
        initialNote={existingNote}
      />,
    );

    const textarea = screen.getByLabelText("Nội dung ghi chú cá nhân");
    expect(textarea).toHaveValue("Existing text");

    fireEvent.change(textarea, { target: { value: "Existing text updated" } });
    expect(screen.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(750);
    });

    await waitFor(() => {
      expect(updateNoteSpy).toHaveBeenCalledWith("note-1", {
        content: "Existing text updated",
        expectedVersion: 0,
      });
    });

    expect(await screen.findByText("Đã lưu")).toBeInTheDocument();
  });

  it("handles 409 version conflict by displaying conflict banner and reload options", async () => {
    const existingNote: NoteResponse = {
      id: "note-1",
      enrollmentId,
      lessonId,
      activityId: null,
      content: "Initial text",
      version: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const conflictProblem = {
      type: "about:blank",
      title: "Xung đột dữ liệu",
      status: 409,
      detail: "Ghi chú đã được cập nhật ở phiên khác.",
      code: "VERSION_CONFLICT",
      traceId: "trace-1",
      fieldErrors: [],
    };

    vi.spyOn(notesApi, "updateNote").mockRejectedValueOnce(
      new ApiError(conflictProblem),
    );

    render(
      <NoteEditor
        enrollmentId={enrollmentId}
        lessonId={lessonId}
        initialNote={existingNote}
      />,
    );

    const textarea = screen.getByLabelText("Nội dung ghi chú cá nhân");
    fireEvent.change(textarea, { target: { value: "Conflicting edit" } });

    await act(async () => {
      vi.advanceTimersByTime(750);
    });

    expect(
      await screen.findByText("Ghi chú đã bị thay đổi ở một phiên học khác!"),
    ).toBeInTheDocument();
    expect(screen.getByText("Xung đột phiên bản")).toBeInTheDocument();
  });

  it("triggers immediate save via Ctrl+S without waiting for debounce", async () => {
    const createdNote: NoteResponse = {
      id: "note-1",
      enrollmentId,
      lessonId,
      activityId: null,
      content: "Saved with shortcut",
      version: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const createNoteSpy = vi
      .spyOn(notesApi, "createNote")
      .mockResolvedValueOnce(createdNote);

    render(
      <NoteEditor enrollmentId={enrollmentId} lessonId={lessonId} initialNote={null} />,
    );

    const textarea = screen.getByLabelText("Nội dung ghi chú cá nhân");
    fireEvent.change(textarea, { target: { value: "Saved with shortcut" } });

    // Press Ctrl+S
    fireEvent.keyDown(window, { key: "s", ctrlKey: true });

    await waitFor(() => {
      expect(createNoteSpy).toHaveBeenCalledTimes(1);
    });

    expect(createNoteSpy).toHaveBeenCalledWith({
      enrollmentId,
      lessonId,
      content: "Saved with shortcut",
    });
  });

  it("deletes note with expectedVersion when confirmed", async () => {
    const existingNote: NoteResponse = {
      id: "note-1",
      enrollmentId,
      lessonId,
      activityId: null,
      content: "Text to be deleted",
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const deleteNoteSpy = vi
      .spyOn(notesApi, "deleteNote")
      .mockResolvedValueOnce(undefined);

    render(
      <NoteEditor
        enrollmentId={enrollmentId}
        lessonId={lessonId}
        initialNote={existingNote}
      />,
    );

    const deleteBtn = screen.getByRole("button", { name: /Xóa/i });
    fireEvent.click(deleteBtn);

    const confirmBtn = screen.getByRole("button", { name: "Xác nhận" });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(deleteNoteSpy).toHaveBeenCalledWith("note-1", 1);
    });

    expect(screen.getByLabelText("Nội dung ghi chú cá nhân")).toHaveValue("");
  });
});
