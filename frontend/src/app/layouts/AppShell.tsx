import { useCallback, useState } from "react";
import { AlertCircle, GraduationCap, Menu, PanelLeftClose, PanelLeftOpen } from "lucide-react";
import { Navigate, Outlet } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/ErrorState";
import { useAuth } from "@/features/auth";
import { AppSidebar } from "./AppSidebar";

export function AppShell() {
  const {
    user,
    isLoading,
    isError,
    authError,
    isAuthenticated,
    logoutError,
    clearLogoutError,
    logout,
    isLoggingOut,
    refetchUser,
  } = useAuth();

  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [desktopCollapsed, setDesktopCollapsed] = useState(false);

  // Stable callbacks declared before any early return (F1)
  const handleCloseDrawer = useCallback(() => {
    setMobileDrawerOpen(false);
  }, []);

  const handleOpenDrawer = useCallback(() => {
    setMobileDrawerOpen(true);
  }, []);

  const handleToggleDesktopCollapse = useCallback(() => {
    setDesktopCollapsed((prev) => !prev);
  }, []);

  // 1. Loading state
  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary animate-pulse">
            <GraduationCap className="h-7 w-7" />
          </div>
          <div className="space-y-2 text-center">
            <Skeleton className="h-4 w-32 mx-auto" />
            <Skeleton className="h-3 w-48 mx-auto" />
          </div>
        </div>
      </div>
    );
  }

  // 2. Fatal auth query error (500, network error): render error state with retry (F3)
  if (isError) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background p-4">
        <div className="w-full max-w-md">
          <ErrorState
            message={
              authError?.message ||
              "Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền."
            }
            onRetry={() => refetchUser()}
          />
        </div>
      </div>
    );
  }

  // 3. Anonymous: redirect to /login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      {/* Sidebar with stable handleCloseDrawer (F1) */}
      <AppSidebar
        isOpen={mobileDrawerOpen}
        onClose={handleCloseDrawer}
        collapsed={desktopCollapsed}
      />

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top Navbar */}
        <header className="flex h-14 items-center justify-between border-b border-border bg-card px-4 md:px-6">
          <div className="flex items-center gap-3">
            {/* Mobile Hamburger Button */}
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={handleOpenDrawer}
              aria-label="Mở menu điều hướng"
              aria-expanded={mobileDrawerOpen}
            >
              <Menu className="h-5 w-5" />
            </Button>

            {/* Desktop Collapse Toggle */}
            <Button
              variant="ghost"
              size="icon"
              className="hidden md:inline-flex h-8 w-8 text-muted-foreground hover:text-foreground"
              onClick={handleToggleDesktopCollapse}
              aria-label={desktopCollapsed ? "Mở rộng thanh bên" : "Thu gọn thanh bên"}
              title={desktopCollapsed ? "Mở rộng thanh bên" : "Thu gọn thanh bên"}
            >
              {desktopCollapsed ? (
                <PanelLeftOpen className="h-4 w-4" />
              ) : (
                <PanelLeftClose className="h-4 w-4" />
              )}
            </Button>

            <span className="text-sm font-semibold text-foreground md:hidden">
              Tự Học Tiếng Anh
            </span>
          </div>

          <div className="flex items-center gap-3">
            {user && (
              <div className="flex items-center gap-2">
                <span className="hidden sm:inline-block text-xs font-medium text-muted-foreground">
                  Xin chào,{" "}
                  <strong className="text-foreground">
                    {user.fullName || user.displayName || user.username}
                  </strong>
                </span>
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary font-semibold text-xs border border-primary/20">
                  {(user.fullName || user.displayName || user.username)
                    .charAt(0)
                    .toUpperCase()}
                </div>
              </div>
            )}
          </div>
        </header>

        {/* Logout Failure Error Banner (F2) */}
        {logoutError && (
          <div className="flex items-center justify-between bg-destructive/15 border-b border-destructive/30 px-4 py-2 text-sm text-destructive">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{logoutError}</span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                className="h-7 text-xs border-destructive/40 text-destructive hover:bg-destructive/10"
                onClick={() => logout()}
                disabled={isLoggingOut}
              >
                {isLoggingOut ? "Đang thử lại..." : "Thử lại đăng xuất"}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs text-muted-foreground"
                onClick={clearLogoutError}
              >
                Đóng
              </Button>
            </div>
          </div>
        )}

        {/* Content Scroll View */}
        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8">
          <div className="mx-auto max-w-5xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
