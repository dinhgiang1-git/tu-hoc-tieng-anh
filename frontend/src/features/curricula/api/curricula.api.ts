import { apiRequest } from "@/lib/api/api-client";
import type { components } from "@/generated/api-types";

export type CurriculumSummary = components["schemas"]["CurriculumSummaryResponse"];
export type CurriculumDetail = components["schemas"]["CurriculumDetailResponse"];
export type RevisionOutline = components["schemas"]["RevisionOutlineResponse"];

export const curriculaKeys = {
  all: ["curricula"] as const,
  detail: (slug: string) => ["curricula", "detail", slug] as const,
  outline: (revisionId: string) => ["revisions", "outline", revisionId] as const,
};

export const curriculaApi = {
  list: (signal?: AbortSignal) =>
    apiRequest<CurriculumSummary[]>("/curricula", { signal }),
  detail: (slug: string, signal?: AbortSignal) =>
    apiRequest<CurriculumDetail>(`/curricula/${encodeURIComponent(slug)}`, { signal }),
  outline: (revisionId: string, signal?: AbortSignal) =>
    apiRequest<RevisionOutline>(`/revisions/${encodeURIComponent(revisionId)}/outline`, { signal }),
};
