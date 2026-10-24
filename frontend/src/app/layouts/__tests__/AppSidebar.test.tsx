import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AppSidebar } from "../AppSidebar";

// Mock useAuth
const mockLogout = vi.fn();
vi.mock("@/features/auth", () => ({
  useAuth: () => ({
    user: {
      id: "u-1",
      username: "testuser",
      fullName: "Test User",
      role: "LEARNER",
    },
    logout: mockLogout,
    isLoggingOut: false,
  }),
}));

describe("AppSidebar - F1 Mobile Drawer & Accessibility", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("when isOpen is false: drawer is hidden, inert, and inaccessible to keyboard focus", () => {
    const handleClose = vi.fn();
    const { container } = render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={false} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    const dialog = container.querySelector('aside[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(dialog).toHaveAttribute("aria-hidden", "true");
    expect(dialog).toHaveAttribute("inert");
    expect(dialog?.getAttribute("style")).toContain("visibility: hidden");
  });

  it("when isOpen is true: drawer is visible and focus moves to close button", async () => {
    const handleClose = vi.fn();
    const { container } = render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    const dialog = container.querySelector('aside[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(dialog).toHaveAttribute("aria-hidden", "false");
    expect(dialog).not.toHaveAttribute("inert");
    expect(dialog?.getAttribute("style")).toContain("visibility: visible");

    const closeBtn = screen.getByRole("button", { name: "Đóng menu" });
    await waitFor(() => {
      expect(document.activeElement).toBe(closeBtn);
    });
  });

  it("calls onClose when Escape key is pressed while drawer is open", () => {
    const handleClose = vi.fn();
    render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    fireEvent.keyDown(window, { key: "Escape" });
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it("does not call onClose on re-render when pathname is unchanged (verifies F1 stability)", () => {
    const handleClose = vi.fn();
    const { rerender } = render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    expect(handleClose).not.toHaveBeenCalled();

    // Re-render with new callback instance
    const newHandleClose = vi.fn();
    rerender(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={newHandleClose} collapsed={false} />
      </MemoryRouter>,
    );

    expect(handleClose).not.toHaveBeenCalled();
    expect(newHandleClose).not.toHaveBeenCalled();
  });

  it("traps focus: Shift+Tab on first element wraps to last element", async () => {
    const handleClose = vi.fn();
    const { container } = render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    const dialog = container.querySelector('aside[role="dialog"]') as HTMLElement;
    expect(dialog).not.toBeNull();

    const focusable = dialog.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
    );
    expect(focusable.length).toBeGreaterThan(1);

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    first.focus();
    expect(document.activeElement).toBe(first);

    fireEvent.keyDown(window, { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(last);
  });

  it("traps focus: Tab on last element wraps to first element", async () => {
    const handleClose = vi.fn();
    const { container } = render(
      <MemoryRouter initialEntries={["/"]}>
        <AppSidebar isOpen={true} onClose={handleClose} collapsed={false} />
      </MemoryRouter>,
    );

    const dialog = container.querySelector('aside[role="dialog"]') as HTMLElement;
    const focusable = dialog.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
    );

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    last.focus();
    expect(document.activeElement).toBe(last);

    fireEvent.keyDown(window, { key: "Tab", shiftKey: false });
    expect(document.activeElement).toBe(first);
  });
});
