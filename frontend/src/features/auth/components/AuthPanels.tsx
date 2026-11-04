import { useLayoutEffect, useRef, useState, type ReactNode } from "react";

interface AuthPanelsProps {
  activeKey: string;
  children: ReactNode;
}

export function AuthPanels({ activeKey, children }: AuthPanelsProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState<number | null>(null);

  useLayoutEffect(() => {
    const activePanel = containerRef.current?.querySelector<HTMLElement>(
      '[data-active="true"]',
    );
    if (!activePanel) return;

    const measure = () => {
      if (activePanel.dataset.active !== "true") return;
      const nextHeight = activePanel.getBoundingClientRect().height;
      if (nextHeight > 0) setHeight(nextHeight);
    };

    measure();
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(measure);
    observer.observe(activePanel);
    return () => observer.disconnect();
  }, [activeKey]);

  return (
    <div
      ref={containerRef}
      className="auth-panels"
      data-measured={height !== null}
      style={{ height: height ?? undefined }}
    >
      {children}
    </div>
  );
}
