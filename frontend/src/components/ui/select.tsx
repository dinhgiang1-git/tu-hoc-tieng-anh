import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { Check, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface SelectProps {
  value: string;
  options: { value: string; label: string }[];
  onValueChange: (value: string) => void;
  label: string;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export function Select({ value, options, onValueChange, label, placeholder = "Chọn…", disabled = false, className }: SelectProps) {
  const id = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef({ text: "", time: 0 });
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const selectedIndex = options.findIndex((option) => option.value === value);
  const isExpanded = isOpen && !disabled;

  useEffect(() => {
    if (!isExpanded) return;
    listRef.current?.focus();
    const closeOutside = (event: Event) => {
      if (event.target instanceof Node && !rootRef.current?.contains(event.target)) setIsOpen(false);
    };
    document.addEventListener("pointerdown", closeOutside);
    document.addEventListener("focusin", closeOutside);
    return () => {
      document.removeEventListener("pointerdown", closeOutside);
      document.removeEventListener("focusin", closeOutside);
    };
  }, [isExpanded]);

  useEffect(() => {
    if (isExpanded) listRef.current?.children[activeIndex]?.scrollIntoView?.({ block: "nearest" });
  }, [activeIndex, isExpanded]);

  const open = (fromEnd = false) => {
    if (disabled || options.length === 0) return;
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : fromEnd ? options.length - 1 : 0);
    searchRef.current = { text: "", time: 0 };
    setIsOpen(true);
  };
  const choose = (index: number) => {
    const option = options[index];
    if (!option) return;
    setIsOpen(false);
    triggerRef.current?.focus();
    if (option.value !== value) onValueChange(option.value);
  };
  const handleListKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const { key } = event;
    if (["ArrowDown", "ArrowUp", "Home", "End", "Enter", " ", "Escape"].includes(key)) event.preventDefault();
    if (key === "ArrowDown") setActiveIndex((index) => (index + 1) % options.length);
    else if (key === "ArrowUp") setActiveIndex((index) => (index - 1 + options.length) % options.length);
    else if (key === "Home") setActiveIndex(0);
    else if (key === "End") setActiveIndex(options.length - 1);
    else if (key === "Enter" || key === " ") choose(activeIndex);
    else if (key === "Escape") {
      setIsOpen(false);
      triggerRef.current?.focus();
    } else if (key === "Tab") {
      triggerRef.current?.focus();
      setIsOpen(false);
    }
    else if (key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      const now = event.timeStamp;
      const text = (now - searchRef.current.time < 700 ? searchRef.current.text : "") + key.toLocaleLowerCase();
      searchRef.current = { text, time: now };
      const match = options.findIndex((option) => option.label.toLocaleLowerCase().startsWith(text));
      if (match >= 0) setActiveIndex(match);
    }
  };

  return (
    <div ref={rootRef} className={cn("relative min-w-0", className)}>
      <button
        ref={triggerRef}
        type="button"
        aria-label={label}
        aria-describedby={`${id}-value`}
        aria-haspopup="listbox"
        aria-expanded={isExpanded}
        aria-controls={isExpanded ? `${id}-list` : undefined}
        disabled={disabled || options.length === 0}
        onClick={() => isExpanded ? setIsOpen(false) : open()}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            open(event.key === "ArrowUp");
          }
        }}
        className="flex h-9 w-full items-center justify-between gap-3 rounded-lg border border-border bg-card px-3 text-sm font-medium text-foreground shadow-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
      >
        <span id={`${id}-value`} className="truncate">{options[selectedIndex]?.label ?? placeholder}</span>
        <ChevronDown aria-hidden="true" className={cn("h-4 w-4 shrink-0 text-muted-foreground", isExpanded && "rotate-180")} />
      </button>
      {isExpanded && (
        <div
          ref={listRef}
          id={`${id}-list`}
          role="listbox"
          aria-label={label}
          aria-activedescendant={options[activeIndex] ? `${id}-option-${activeIndex}` : undefined}
          tabIndex={-1}
          onKeyDown={handleListKeyDown}
          className="absolute right-0 top-full z-50 mt-2 max-h-72 w-full min-w-64 max-w-[calc(100vw-2rem)] overflow-y-auto rounded-xl border border-border bg-popover p-1.5 text-popover-foreground shadow-xl outline-none"
        >
          {options.map((option, index) => (
            <div
              key={option.value}
              id={`${id}-option-${index}`}
              role="option"
              aria-selected={option.value === value}
              onPointerMove={() => setActiveIndex(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => choose(index)}
              className={cn("flex cursor-pointer items-start gap-3 rounded-lg px-3 py-3 text-sm leading-5", index === activeIndex && "bg-accent text-accent-foreground")}
            >
              <Check aria-hidden="true" className={cn("mt-0.5 h-4 w-4 shrink-0 text-primary", option.value !== value && "invisible")} />
              <span>{option.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
