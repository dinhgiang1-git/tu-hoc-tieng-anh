import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Select } from "../select";

const options = [
  { value: "a", label: "Anh-Anh" },
  { value: "b", label: "Nền tảng" },
  { value: "c", label: "Giao tiếp" },
];

describe("Select", () => {
  it("navigates without changing the saved selection until Enter", () => {
    const onValueChange = vi.fn();
    render(<Select label="Lộ trình" value="a" options={options} onValueChange={onValueChange} />);
    const trigger = screen.getByRole("button", { name: "Lộ trình" });
    fireEvent.keyDown(trigger, { key: "ArrowDown" });
    const list = screen.getByRole("listbox");
    expect(list).toHaveFocus();
    expect(screen.getByRole("option", { name: "Anh-Anh" })).toHaveAttribute("aria-selected", "true");
    fireEvent.keyDown(list, { key: "ArrowDown" });
    expect(onValueChange).not.toHaveBeenCalled();
    expect(list).toHaveAttribute("aria-activedescendant", screen.getByRole("option", { name: "Nền tảng" }).id);
    fireEvent.keyDown(list, { key: "Enter" });
    expect(onValueChange).toHaveBeenCalledExactlyOnceWith("b");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it("supports typeahead, Home/End and Escape without submitting", () => {
    const onValueChange = vi.fn();
    render(<Select label="Lộ trình" value="b" options={options} onValueChange={onValueChange} />);
    const trigger = screen.getByRole("button", { name: "Lộ trình" });
    fireEvent.click(trigger);
    const list = screen.getByRole("listbox");
    fireEvent.keyDown(list, { key: "g" });
    expect(list).toHaveAttribute("aria-activedescendant", screen.getByRole("option", { name: "Giao tiếp" }).id);
    fireEvent.keyDown(list, { key: "Home" });
    expect(list).toHaveAttribute("aria-activedescendant", screen.getByRole("option", { name: "Anh-Anh" }).id);
    fireEvent.keyDown(list, { key: "End" });
    expect(list).toHaveAttribute("aria-activedescendant", screen.getByRole("option", { name: "Giao tiếp" }).id);
    fireEvent.keyDown(list, { key: "Escape" });
    expect(onValueChange).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
    expect(trigger).toHaveAttribute("aria-expanded", "false");
  });

  it("selects by pointer, dismisses outside and blocks interaction when disabled", () => {
    const onValueChange = vi.fn();
    const { rerender } = render(<Select label="Lộ trình" value="a" options={options} onValueChange={onValueChange} />);
    const trigger = screen.getByRole("button", { name: "Lộ trình" });
    fireEvent.click(trigger);
    fireEvent.click(screen.getByRole("option", { name: "Giao tiếp" }));
    expect(onValueChange).toHaveBeenCalledExactlyOnceWith("c");
    fireEvent.click(trigger);
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    rerender(<Select label="Lộ trình" value="a" options={options} onValueChange={onValueChange} disabled />);
    fireEvent.click(trigger);
    expect(trigger).toBeDisabled();
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });
});
