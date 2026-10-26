import { apiRequest, voidDecoder } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type NoteResponse = components["schemas"]["NoteResponse"];
export type NotePageResponse = components["schemas"]["NotePageResponse"];
export type CreateNoteRequest = components["schemas"]["CreateNoteRequest"];
export type UpdateNoteRequest = components["schemas"]["UpdateNoteRequest"];

export interface NoteQueryParams {
  enrollmentId?: string;
  lessonId?: string;
  page?: number;
  size?: number;
}

export const notesKeys = {
  all: ["notes"] as const,
  lists: () => [...notesKeys.all, "list"] as const,
  list: (params?: { enrollmentId?: string; lessonId?: string }) =>
    [...notesKeys.lists(), params] as const,
  detail: (id: string) => [...notesKeys.all, "detail", id] as const,
};

export const notesApi = {
  async getLessonNote(enrollmentId: string, lessonId: string, signal?: AbortSignal): Promise<NoteResponse | null> {
    let page = 0;
    for (;;) {
      const result = await notesApi.getNotes({ enrollmentId, lessonId, page, size: 100 }, signal);
      const note = result.items.find(item => item.enrollmentId === enrollmentId &&
        item.lessonId === lessonId && item.activityId === null);
      if (note) return note;
      if (++page >= result.totalPages) return null;
    }
  },
  async getNotes(
    params?: NoteQueryParams,
    signal?: AbortSignal,
  ): Promise<NotePageResponse> {
    const searchParams = new URLSearchParams();
    if (params?.enrollmentId) {
      searchParams.set("enrollmentId", params.enrollmentId);
    }
    if (params?.lessonId) {
      searchParams.set("lessonId", params.lessonId);
    }
    if (params?.page !== undefined) {
      searchParams.set("page", params.page.toString());
    }
    if (params?.size !== undefined) {
      searchParams.set("size", params.size.toString());
    }

    const query = searchParams.toString();
    const path = `/me/notes${query ? `?${query}` : ""}`;

    return apiRequest<NotePageResponse>(path, {
      method: "GET",
      signal,
    });
  },

  async createNote(
    payload: CreateNoteRequest,
    signal?: AbortSignal,
  ): Promise<NoteResponse> {
    return apiRequest<NoteResponse>("/notes", {
      method: "POST",
      json: payload,
      signal,
    });
  },

  async updateNote(
    id: string,
    payload: UpdateNoteRequest,
    signal?: AbortSignal,
  ): Promise<NoteResponse> {
    return apiRequest<NoteResponse>(`/notes/${id}`, {
      method: "PATCH",
      json: payload,
      signal,
    });
  },

  async deleteNote(
    id: string,
    expectedVersion: number,
    signal?: AbortSignal,
  ): Promise<void> {
    return apiRequest<void>(
      `/notes/${id}?expectedVersion=${expectedVersion}`,
      {
        method: "DELETE",
        signal,
      },
      voidDecoder,
    );
  },
};
