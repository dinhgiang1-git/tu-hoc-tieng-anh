import { useEffect, useRef } from "react";
import {
  BookOpen,
  Calendar,
  Compass,
  GraduationCap,
  LoaderCircle,
  LogOut,
  RotateCcw,
  Settings,
  TrendingUp,
  User as UserIcon,
  X,
} from "lucide-react";

import { Link, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth";
import { cn } from "@/lib/utils";

interface AppSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  collapsed: boolean;
}

export function AppSidebar({ isOpen, onClose, collapsed }: AppSidebarProps) {
  const { user, logout, isLoggingOut } = useAuth();
  const location = useLocation();

  const prevPathRef = useRef(location.pathname);
  const drawerRef = useRef<HTMLElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  // 1. Only close mobile drawer when pathname ACTUALLY changes, not on initial mount or re-render (F1)
  useEffect(() => {
    if (prevPathRef.current !== location.pathname) {
      prevPathRef.current = location.pathname;
      onClose();
    }
  }, [location.pathname, onClose]);

  // 2. Handle Escape key to close mobile drawer
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  // 3. Focus trap inside mobile drawer when opened (F1)
  useEffect(() => {
    if (!isOpen) return;

    // Focus close button on open
    const timer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 50);

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== "Tab" || !drawerRef.current) return;

      const focusable = drawerRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault();
          last.focus();
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };

    window.addEventListener("keydown", handleTabKey);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("keydown", handleTabKey);
    };
  }, [isOpen]);

  const navItems = [
    {
      title: "Hôm nay",
      href: "/",
      icon: Compass,
      match: location.pathname === "/" || location.pathname === "/today",
    },
    {
      title: "Lộ trình học",
        href: "/curricula",
      icon: BookOpen,
        match: location.pathname.startsWith("/curricula"),
    },
    {
      title: "Lịch học",
      href: "/schedule",
      icon: Calendar,
      match: location.pathname.startsWith("/schedule"),
    },
    {
      title: "Ôn tập",
      href: "/review",
      icon: RotateCcw,
      match: location.pathname.startsWith("/review"),
    },
    {
      title: "Tiến độ",
      href: "/progress",
      icon: TrendingUp,
      match: location.pathname.startsWith("/progress"),
    },
    {
      title: "Tùy chọn học tập",

      href: "/preferences",
      icon: Settings,
      match: location.pathname.startsWith("/preferences"),
    },
  ];

  const renderSidebarContent = (isMobile: boolean) => (
    <div className="flex h-full flex-col justify-between">
      <div className="space-y-4 py-4">
        {/* Brand / Logo */}
        <div className="flex items-center justify-between px-4">
          <Link
            to="/"
            className="flex items-center gap-2 font-semibold text-foreground transition-colors hover:text-primary"
          >
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-sm">
              <GraduationCap className="h-5 w-5" />
            </div>
            {(!collapsed || isMobile) && (
              <div className="flex flex-col">
                <span className="text-base font-bold leading-tight">
                  Tự Học Tiếng Anh
                </span>
                <span className="text-[11px] font-medium text-muted-foreground">
                  Chuẩn Anh - Anh (RP)
                </span>
              </div>
            )}
          </Link>

          {/* Mobile close button (only in mobile drawer) */}
          {isMobile && (
            <Button
              ref={closeButtonRef}
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              onClick={onClose}
              aria-label="Đóng menu"
            >
              <X className="h-5 w-5" />
            </Button>
          )}
        </div>

        {/* Navigation list */}
        <nav className="space-y-1 px-2" aria-label="Điều hướng chính">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  item.match
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  collapsed && "justify-center px-2",
                )}
                title={collapsed ? item.title : undefined}
              >
                <Icon className="h-5 w-5 shrink-0" />
                {!collapsed && <span>{item.title}</span>}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* User profile & Logout */}
      <div className="border-t border-border p-3 space-y-2">
        {user && (
          <div
            className={cn(
              "flex items-center gap-3 rounded-lg p-2 bg-muted/40",
              collapsed && "justify-center p-1",
            )}
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-semibold text-sm">
              {(user.fullName || user.displayName || user.username)
                .charAt(0)
                .toUpperCase() || <UserIcon className="h-4 w-4" />}
            </div>
            {!collapsed && (
              <div className="flex flex-col overflow-hidden text-left">
                <span className="truncate text-sm font-semibold text-foreground">
                  {user.fullName || user.displayName || user.username}
                </span>
                <span className="truncate text-xs text-muted-foreground">
                  @{user.username} • {user.role}
                </span>
              </div>
            )}
          </div>
        )}

        <Button
          variant="ghost"
          size={collapsed ? "icon" : "default"}
          onClick={() => void logout()}
          disabled={isLoggingOut}
          className={cn(
            "w-full text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors",
            !collapsed && "justify-start gap-2",
          )}
          title="Đăng xuất"
          aria-busy={isLoggingOut}
        >
          {isLoggingOut ? (
            <LoaderCircle className="h-4 w-4 animate-spin shrink-0 text-destructive" />
          ) : (
            <LogOut className="h-4 w-4 shrink-0" />
          )}
          {!collapsed && (
            <span>{isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}</span>
          )}
        </Button>
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-background/80 backdrop-blur-sm md:hidden transition-opacity"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Mobile Drawer with inert and focus trap (F1) */}
      <aside
        ref={drawerRef}
        role="dialog"
        aria-modal="true"
        aria-label="Menu điều hướng"
        aria-hidden={!isOpen}
        inert={!isOpen}
        style={{ visibility: isOpen ? "visible" : "hidden" }}
        className={cn(
          "fixed inset-y-0 left-0 z-50 w-72 bg-card border-r border-border transition-transform duration-300 ease-in-out md:hidden shadow-xl",
          isOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {renderSidebarContent(true)}
      </aside>

      {/* Desktop Sidebar */}
      <aside
        className={cn(
          "hidden md:flex flex-col border-r border-border bg-card transition-all duration-300 ease-in-out",
          collapsed ? "w-16" : "w-64",
        )}
      >
        {renderSidebarContent(false)}
      </aside>
    </>
  );
}
