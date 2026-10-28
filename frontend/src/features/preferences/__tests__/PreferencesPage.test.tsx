import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { authApi, authKeys } from "@/features/auth";
import { PreferencesPage } from "../pages/PreferencesPage";

describe("PreferencesPage", () => {
  let client: QueryClient;

  beforeEach(() => {
    vi.restoreAllMocks();
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    vi.spyOn(authApi, "getPreferences").mockResolvedValue({
      dailyTargetMinutes: 30,
      focusedEnrollmentId: null,
    });
    vi.spyOn(authApi, "updatePreferences").mockImplementation(async (payload) => ({
      dailyTargetMinutes: payload.dailyTargetMinutes ?? 30,
      focusedEnrollmentId: null,
    }));
  });

  async function renderPage() {
    render(<QueryClientProvider client={client}><PreferencesPage /></QueryClientProvider>);
    const input = screen.getByRole("spinbutton", { name: "Thời lượng mỗi ngày" });
    await waitFor(() => expect(input).toHaveValue(30));
    return input;
  }

  it("shows the saved value, preserves a draft during refresh, and retains the saved result", async () => {
    const input = await renderPage();
    const save = screen.getByRole("button", { name: "Lưu mục tiêu" });
    expect(save).toBeDisabled();
    fireEvent.change(input, { target: { value: "45" } });
    await act(async () => {
      await client.invalidateQueries({ queryKey: authKeys.preferences() });
    });
    expect(input).toHaveValue(45);
    fireEvent.click(save);
    await screen.findByText("Đã lưu mục tiêu");
    expect(authApi.updatePreferences).toHaveBeenCalledExactlyOnceWith({ dailyTargetMinutes: 45 });
    expect(input).toHaveValue(45);
    expect(save).toBeDisabled();
  });

  it.each(["", "9", "181", "30.5"])("does not submit invalid minutes: %s", async (value) => {
    const input = await renderPage();
    fireEvent.change(input, { target: { value } });
    expect(screen.getByRole("button", { name: "Lưu mục tiêu" })).toBeDisabled();
    expect(input).toHaveAccessibleDescription("Nhập số phút nguyên từ 10 đến 180.");
    fireEvent.submit(input.closest("form")!);
    expect(authApi.updatePreferences).not.toHaveBeenCalled();
  });

  it("keeps the draft on save failure and allows retry", async () => {
    vi.mocked(authApi.updatePreferences).mockRejectedValueOnce(new Error("Offline"));
    const input = await renderPage();
    fireEvent.click(screen.getByRole("button", { name: "60 phút" }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu mục tiêu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Không thể lưu mục tiêu");
    expect(input).toHaveValue(60);
    fireEvent.click(screen.getByRole("button", { name: "Lưu mục tiêu" }));
    await screen.findByText("Đã lưu mục tiêu");
    expect(input).toHaveValue(60);
  });
});
