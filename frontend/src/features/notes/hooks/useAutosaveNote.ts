import { useCallback, useEffect, useRef, useState } from "react";
import { notesApi, type NoteResponse } from "../api/notes.api";
import { ApiError } from "@/lib/api/api-error";

export type AutosaveStatus = "idle" | "dirty" | "saving" | "saved" | "error" | "conflict";

export interface UseAutosaveNoteOptions {
  enrollmentId?: string;
  lessonId?: string;
  initialNote?: NoteResponse | null;
  debounceMs?: number;
  disableBlocker?: boolean;
  onSaved?: (note: NoteResponse) => void;
  onConflict?: (error: ApiError) => void;
}

export function useAutosaveNote({
  enrollmentId,
  lessonId,
  initialNote,
  debounceMs = 700,
  disableBlocker: _disableBlocker = false,
  onSaved,
  onConflict,
}: UseAutosaveNoteOptions) {
  const [content, setContent] = useState<string>(initialNote?.content ?? "");
  const [currentNote, setCurrentNote] = useState<NoteResponse | null>(initialNote ?? null);
  const [status, setStatus] = useState<AutosaveStatus>("idle");
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState<boolean>(false);
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(
    initialNote?.updatedAt ? new Date(initialNote.updatedAt) : null,
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const contentRef = useRef<string>(content);
  const currentNoteRef = useRef<NoteResponse | null>(currentNote);
  const lastPersistedContentRef = useRef<string>(initialNote?.content ?? "");
  const inFlightRef = useRef<boolean>(false);
  const queuedContentRef = useRef<string | null>(null);
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const scopeRef = useRef({
    enrollmentId,
    lessonId,
    noteId: initialNote?.id ?? null,
  });
  const generationRef = useRef<number>(0);
  const deletedNoteIdRef = useRef<string | null>(null);
  const isConflictLatchedRef = useRef<boolean>(false);
  const saveResolversRef = useRef<Array<(note: NoteResponse | null) => void>>([]);

  const drainResolvers = (saved: NoteResponse | null) => {
    const resolvers = saveResolversRef.current;
    saveResolversRef.current = [];
    resolvers.forEach((res) => res(saved));
  };

  useEffect(() => {
    contentRef.current = content;
  }, [content]);

  useEffect(() => {
    currentNoteRef.current = currentNote;
  }, [currentNote]);

  // Handle lesson/scope changes or initial note loading asynchronously
  useEffect(() => {
    const currentNoteId = initialNote?.id ?? null;
    const isNewScope =
      scopeRef.current.enrollmentId !== enrollmentId ||
      scopeRef.current.lessonId !== lessonId;

    if (isNewScope) {
      // Invalidate in-flight continuation from previous scope/generation
      generationRef.current += 1;
      isConflictLatchedRef.current = false;
      scopeRef.current = {
        enrollmentId,
        lessonId,
        noteId: currentNoteId,
      };

      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
      inFlightRef.current = false;
      queuedContentRef.current = null;
      drainResolvers(null);

      const freshContent = initialNote?.content ?? "";
      setContent(freshContent);
      contentRef.current = freshContent;
      lastPersistedContentRef.current = freshContent;
      setCurrentNote(initialNote ?? null);
      currentNoteRef.current = initialNote ?? null;
      setHasUnsavedChanges(false);
      setStatus("idle");
      setErrorMessage(null);
      setLastSavedAt(initialNote?.updatedAt ? new Date(initialNote.updatedAt) : null);
    } else if (
      !currentNoteRef.current &&
      initialNote && initialNote.id !== deletedNoteIdRef.current &&
      !hasUnsavedChanges &&
      contentRef.current === ""
    ) {
      // Async initial load resolved while user has not touched the editor
      scopeRef.current.noteId = currentNoteId;
      setContent(initialNote.content);
      contentRef.current = initialNote.content;
      lastPersistedContentRef.current = initialNote.content;
      setCurrentNote(initialNote);
      currentNoteRef.current = initialNote;
      setLastSavedAt(initialNote.updatedAt ? new Date(initialNote.updatedAt) : null);
    }
  }, [enrollmentId, lessonId, initialNote, hasUnsavedChanges]);

  // Perform serialized save with coalescing loop returning a promise
  const performSave = useCallback(
    async (textToSave: string): Promise<NoteResponse | null> => {
      if (isConflictLatchedRef.current) {
        return null;
      }

      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }

      if (inFlightRef.current) {
        queuedContentRef.current = textToSave;
        return new Promise<NoteResponse | null>((resolve) => {
          saveResolversRef.current.push(resolve);
        });
      }

      inFlightRef.current = true;
      setStatus("saving");
      setErrorMessage(null);

      let text: string | null = textToSave;
      const requestGen = generationRef.current;

      const savePromise = new Promise<NoteResponse | null>((resolve) => {
        saveResolversRef.current.push(resolve);
      });

      void (async () => {
        while (text !== null) {
          if (generationRef.current !== requestGen || isConflictLatchedRef.current) {
            inFlightRef.current = false;
            queuedContentRef.current = null;
            return;
          }

          try {
            const activeNote = currentNoteRef.current;
            let savedNote: NoteResponse;

            if (!activeNote) {
              savedNote = await notesApi.createNote({
                enrollmentId,
                lessonId,
                content: text,
              });
            } else {
              savedNote = await notesApi.updateNote(activeNote.id, {
                content: text,
                expectedVersion: activeNote.version,
              });
            }

            // Drop stale response if scope changed or component unmounted while request was in-flight
            if (generationRef.current !== requestGen) {
              return;
            }

            setCurrentNote(savedNote);
            currentNoteRef.current = savedNote;
            lastPersistedContentRef.current = text;
            setLastSavedAt(new Date());
            onSaved?.(savedNote);

            // If another save was queued while this save was in flight, coalesce
            if (queuedContentRef.current !== null && !isConflictLatchedRef.current) {
              text = queuedContentRef.current;
              queuedContentRef.current = null;
            } else {
              text = null;
              inFlightRef.current = false;

              // Check if user has typed something that hasn't been sent yet
              const isDirty =
                contentRef.current !== savedNote.content ||
                debounceTimerRef.current !== null;

              if (isDirty) {
                setHasUnsavedChanges(true);
                setStatus("dirty");
              } else {
                setHasUnsavedChanges(false);
                setStatus("saved");
              }

              drainResolvers(savedNote);
            }
          } catch (err) {
            // Drop stale error if scope changed
            if (generationRef.current !== requestGen) {
              return;
            }

            inFlightRef.current = false;
            queuedContentRef.current = null;
            text = null;

            if (err instanceof ApiError && err.status === 409) {
              // Latch conflict: cancel all timers, clear queue, and block auto-save
              if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
                debounceTimerRef.current = null;
              }
              isConflictLatchedRef.current = true;
              setStatus("conflict");
              setErrorMessage(
                "Ghi chú đã bị thay đổi ở một phiên khác. Vui lòng tải lại hoặc chọn giải pháp.",
              );
              onConflict?.(err);
            } else {
              setStatus("error");
              setErrorMessage(
                err instanceof ApiError
                  ? err.message
                  : "Chưa lưu được ghi chú. Vui lòng thử lại.",
              );
            }

            drainResolvers(null);
          }
        }
      })();

      return savePromise;
    },
    [enrollmentId, lessonId, onSaved, onConflict],
  );

  // Debounced user input handler
  const updateContent = useCallback(
    (newText: string) => {
      setContent(newText);
      contentRef.current = newText;

      // When conflict is latched, update local text and dirty flag, but do NOT schedule debounce
      if (isConflictLatchedRef.current) {
        if (debounceTimerRef.current) {
          clearTimeout(debounceTimerRef.current);
          debounceTimerRef.current = null;
        }
        setHasUnsavedChanges(true);
        setStatus("conflict");
        return;
      }

      const isDirty = newText !== lastPersistedContentRef.current;
      setHasUnsavedChanges(isDirty || inFlightRef.current);
      setStatus((prev) =>
        prev === "conflict"
          ? "conflict"
          : isDirty
            ? "dirty"
            : inFlightRef.current
              ? "saving"
              : "saved",
      );
      setErrorMessage(null);

      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }

      debounceTimerRef.current = setTimeout(() => {
        debounceTimerRef.current = null;
        void performSave(newText);
      }, debounceMs);
    },
    [debounceMs, performSave],
  );

  // Immediate save flush returning promise
  const saveImmediately = useCallback((): Promise<NoteResponse | null> => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    if (isConflictLatchedRef.current) {
      return Promise.resolve(null);
    }
    return performSave(contentRef.current);
  }, [performSave]);

  // Reload note to resolve conflict
  const reloadNote = useCallback(async (reloadedNote: NoteResponse) => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    isConflictLatchedRef.current = false;
    queuedContentRef.current = null;
    inFlightRef.current = false;
    drainResolvers(null);

    setCurrentNote(reloadedNote);
    currentNoteRef.current = reloadedNote;
    setContent(reloadedNote.content);
    contentRef.current = reloadedNote.content;
    lastPersistedContentRef.current = reloadedNote.content;
    setHasUnsavedChanges(false);
    setStatus("saved");
    setErrorMessage(null);
    setLastSavedAt(reloadedNote.updatedAt ? new Date(reloadedNote.updatedAt) : null);
  }, []);

  // Force overwrite (resolves conflict by updating expectedVersion)
  const overwriteConflict = useCallback(
    async (freshVersion: number, latest?: NoteResponse) => {
      if (latest) currentNoteRef.current = latest;
      if (!currentNoteRef.current) return;
      isConflictLatchedRef.current = false;
      currentNoteRef.current = {
        ...currentNoteRef.current,
        version: freshVersion,
      };
      return performSave(contentRef.current);
    },
    [performSave],
  );

  // Safely delete note and reset hook state
  const deleteNote = useCallback(async () => {
    if (!currentNoteRef.current) return;
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    isConflictLatchedRef.current = false;
    queuedContentRef.current = null;
    inFlightRef.current = false;
    drainResolvers(null);

    const deleting = currentNoteRef.current;
    const requestGen = ++generationRef.current;
    try {
      await notesApi.deleteNote(deleting.id, deleting.version);
    } catch (error) {
      if (requestGen === generationRef.current) {
        setStatus("error"); setHasUnsavedChanges(true);
        setErrorMessage("Không thể xóa ghi chú. Vui lòng thử lại.");
      }
      throw error;
    }
    if (requestGen !== generationRef.current) return;
    deletedNoteIdRef.current = deleting.id;

    setCurrentNote(null);
    currentNoteRef.current = null;
    setContent("");
    contentRef.current = "";
    lastPersistedContentRef.current = "";
    setHasUnsavedChanges(false);
    setStatus("idle");
    setErrorMessage(null);
    setLastSavedAt(null);
  }, []);

  // Clean up timer and invalidate generation on unmount
  useEffect(() => {
    return () => {
      generationRef.current += 1;
      drainResolvers(null);
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // Browser beforeunload protection
  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (hasUnsavedChanges || inFlightRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [hasUnsavedChanges]);

  return {
    content,
    updateContent,
    saveImmediately,
    reloadNote,
    overwriteConflict,
    deleteNote,
    status,
    lastSavedAt,
    errorMessage,
    currentNote,
    hasUnsavedChanges,
    blocker: undefined,
  };
}
