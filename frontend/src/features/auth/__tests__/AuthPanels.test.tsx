import { act, render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthPanels } from "../components/AuthPanels";

describe("AuthPanels", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("fits the active form and updates for resized content without reserving the hidden form", () => {
    const resizeCallbacks: (() => void)[] = [];
    vi.stubGlobal("ResizeObserver", class {
      constructor(callback: () => void) { resizeCallbacks.push(callback); }
      observe() {}
      disconnect() {}
    });
    vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(
      function (this: HTMLElement) {
        return new DOMRect(0, 0, 300, Number(this.dataset.testHeight ?? 0));
      },
    );

    const panels = (mode: string) => (
      <AuthPanels activeKey={mode}>
        <form data-active={mode === "login"} data-test-height="260" />
        <form data-active={mode === "register"} data-test-height="440" />
      </AuthPanels>
    );
    const { container, rerender } = render(panels("login"));
    const wrapper = container.firstElementChild;
    expect(wrapper).toHaveStyle({ height: "260px" });

    rerender(panels("register"));
    expect(wrapper).toHaveStyle({ height: "440px" });
    const registerPanel = container.querySelector('[data-active="true"]');
    expect(registerPanel).not.toBeNull();
    registerPanel?.setAttribute("data-test-height", "480");
    act(() => resizeCallbacks[1]?.());
    expect(wrapper).toHaveStyle({ height: "480px" });

    rerender(panels("login"));
    act(() => resizeCallbacks[1]?.());
    expect(wrapper).toHaveStyle({ height: "260px" });
  });
});
