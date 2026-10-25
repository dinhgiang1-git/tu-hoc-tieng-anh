import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { TodayPage } from "../pages/TodayPage";
import { todayApi } from "../api/today.api";
import { enrollmentApi } from "@/features/enrollment/api/enrollment.api";
import { authApi } from "@/features/auth";

it("review: one existing enrollment with null focus can be selected", async () => {
  vi.spyOn(authApi, "getPreferences").mockResolvedValue({ dailyTargetMinutes: 30, focusedEnrollmentId: null });
  vi.spyOn(enrollmentApi, "myEnrollments").mockResolvedValue([{
    id: "existing-enrollment", curriculumTitle: "Existing course", revisionVersion: "1.0.0", status: "ACTIVE",
    completedRequiredActivities: 0, totalRequiredActivities: 19, progressPercent: 0, version: 1,
  }]);
  vi.spyOn(todayApi, "getToday").mockResolvedValue({ state: "NO_ENROLLMENT", todayDate: "2026-10-28", isRestDay: false,
    enrollmentId: null, revisionId: null, curriculumTitle: null, enrollmentVersion: null, primaryAction: null, progress: null, overdueLessons: [] });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}><MemoryRouter><TodayPage /></MemoryRouter></QueryClientProvider>);
  await screen.findByText("Bạn chưa đăng ký lộ trình học nào");
  expect(screen.getByRole("combobox", { name: "Lộ trình:" })).toBeInTheDocument();
});
