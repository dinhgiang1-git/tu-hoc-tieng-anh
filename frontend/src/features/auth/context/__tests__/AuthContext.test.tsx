import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { AuthProvider } from "../AuthContext";
import { useAuth } from "../../hooks/useAuth";
import { csrfSession } from "@/lib/api/csrf-session";
import { ApiError } from "@/lib/api/api-error";
import { authKeys } from "../../api/auth.keys";
import { authApi } from "../../api/auth.api";

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        retryDelay: 0,
        staleTime: Infinity,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

function createWrapper(queryClient: QueryClient) {
  return function TestWrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>{children}</AuthProvider>
      </QueryClientProvider>
    );
  };
}

describe("AuthContext - F2 & F3 Lifecycle, Error Discrimination, & Logout", () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
    csrfSession.clear();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("distinguishes 401 from 500 on /me: 401 maps strictly to anonymous (user=null, isError=false)", async () => {
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              type: "about:blank",
              title: "Chưa xác thực",
              status: 401,
              detail: "Bạn chưa đăng nhập.",
              code: "AUTHENTICATION_REQUIRED",
              traceId: "trace-401",
              fieldErrors: [],
            }),
            {
              status: 401,
              headers: { "Content-Type": "application/problem+json" },
            },
          ),
        );
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.authError).toBeNull();
  });

  it("distinguishes 500 from 401 on /me: 500 triggers isError=true and preserves error for retry UI", async () => {
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              type: "about:blank",
              title: "Lỗi máy chủ",
              status: 500,
              detail: "Máy chủ cơ sở dữ liệu tạm thời không phản hồi.",
              code: "INTERNAL_ERROR",
              traceId: "trace-500",
              fieldErrors: [],
            }),
            {
              status: 500,
              headers: { "Content-Type": "application/problem+json" },
            },
          ),
        );
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.isError).toBe(true);
    expect(result.current.authError).not.toBeNull();
    expect(result.current.authError?.message).toContain(
      "Máy chủ cơ sở dữ liệu tạm thời không phản hồi.",
    );
  });

  it("distinguishes network error on /me: triggers isError=true with friendly connection message", async () => {
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.reject(new TypeError("Failed to fetch"));
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.isError).toBe(true);
    expect(result.current.authError).not.toBeNull();
    expect(result.current.authError?.message).toContain("Không thể kết nối");
  });

  it("F2 logout failure: preserves user and session, sets logoutError, does not fake success", async () => {
    // 1. Initial successful /me login
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              id: "usr-1",
              username: "hoangnam",
              fullName: "Hoàng Nam",
              role: "LEARNER",
              timeZone: "Asia/Ho_Chi_Minh",
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-valid" }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        // Logout fails on server with 500
        return Promise.resolve(
          new Response(
            JSON.stringify({
              type: "about:blank",
              title: "Lỗi đăng xuất",
              status: 500,
              detail: "Không thể hủy session trên Redis/Database.",
              code: "INTERNAL_ERROR",
              traceId: "trace-logout-500",
              fieldErrors: [],
            }),
            {
              status: 500,
              headers: { "Content-Type": "application/problem+json" },
            },
          ),
        );
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });
    expect(result.current.user?.username).toBe("hoangnam");

    // 2. Perform logout (fails with 500)
    await act(async () => {
      try {
        await result.current.logout();
      } catch {
        // Expected throw
      }
    });

    // 3. User remains authenticated, logoutError is displayed, no fake success!
    expect(result.current.user).not.toBeNull();
    expect(result.current.logoutError).toContain("Không thể hủy session trên Redis/Database.");
    expect(result.current.isLoggingOut).toBe(false);

    // 4. Test clearLogoutError
    act(() => {
      result.current.clearLogoutError();
    });
    expect(result.current.logoutError).toBeNull();
  });

  it("prevents double-clicks during logout: subsequent logout calls are ignored while isLoggingOut is active", async () => {
    let logoutCallCount = 0;
    let resolveLogout: (response: Response) => void;

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              id: "usr-1",
              username: "hoangnam",
              fullName: "Hoàng Nam",
              role: "LEARNER",
              timeZone: "Asia/Ho_Chi_Minh",
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-valid" }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        logoutCallCount++;
        return new Promise((resolve) => {
          resolveLogout = resolve;
        });
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });

    // Preset CSRF token to prevent async refresh before logout fetch
    (csrfSession as unknown as { token: string | null }).token = "csrf-token-valid";

    // Trigger first logout
    let logoutPromise1: Promise<void>;
    act(() => {
      logoutPromise1 = result.current.logout();
    });

    // Trigger duplicate logout call while first is in-flight (double-click simulation)
    let logoutPromise2: Promise<void>;
    act(() => {
      logoutPromise2 = result.current.logout();
    });

    // Wait until logout fetch has been reached
    await waitFor(() => {
      expect(logoutCallCount).toBe(1);
    });

    // Resolve logout on server with 204
    await act(async () => {
      resolveLogout!(new Response(null, { status: 204 }));
      await Promise.all([logoutPromise1!, logoutPromise2!]);
    });

    expect(logoutCallCount).toBe(1); // Only 1 network request was made!
    expect(result.current.isLoggingOut).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it("session expired lifecycle: 401 AUTHENTICATION_REQUIRED on background query clears user and CSRF", async () => {
    // 1. Initial login
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/me")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              id: "usr-1",
              username: "hoangnam",
              fullName: "Hoàng Nam",
              role: "LEARNER",
              timeZone: "Asia/Ho_Chi_Minh",
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });

    // Preset CSRF
    (csrfSession as unknown as { token: string | null }).token = "valid-token";
    expect(await csrfSession.getToken()).toBe("valid-token");

    // 2. Simulate another query hitting 401 AUTHENTICATION_REQUIRED
    const clearSpy = vi.spyOn(csrfSession, "clear");
    act(() => {
      queryClient.getQueryCache().notify({
        type: "updated",
        query: queryClient.getQueryCache().build(queryClient, { queryKey: ["preferences"] }),
        action: {
          type: "error",
          error: new ApiError({
            type: "about:blank",
            title: "Hết phiên",
            status: 401,
            detail: "Phiên làm việc đã kết thúc.",
            code: "AUTHENTICATION_REQUIRED",
            traceId: "trace-session-expired",
            fieldErrors: [],
          }),
        },
      });
    });

    // 3. User session is cleared, CSRF token is cleared
    await waitFor(() => {
      expect(result.current.user).toBeNull();
    });
    expect(clearSpy).toHaveBeenCalled();
  });

  it("finite CSRF recovery: refresh resets inflightPromise on error, allowing finite retries without deadlocks", async () => {
    let callCount = 0;
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        callCount++;
        if (callCount === 1) {
          return Promise.resolve(new Response(null, { status: 503 }));
        }
        return Promise.resolve(
          new Response(
            JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "recovered-csrf-token" }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    // First attempt fails cleanly
    await expect(csrfSession.refresh()).rejects.toThrow("Failed to fetch CSRF token: status 503");

    // Second recovery attempt succeeds cleanly
    const token = await csrfSession.refresh();
    expect(token).toBe("recovered-csrf-token");
    expect(callCount).toBe(2);
  });

  it("R1 regression: mutation 401 must expire the cached authenticated session", async () => {
    const userFixture = {
      id: "A",
      username: "account-a",
      fullName: "Account A",
      timeZone: "Europe/London",
      role: "LEARNER",
    };

    globalThis.fetch = vi.fn().mockImplementation(async (url) => {
      if (String(url).endsWith("/auth/csrf")) {
        return new Response(
          JSON.stringify({ token: "t", headerName: "X-CSRF-TOKEN" }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (String(url).endsWith("/me")) {
        return new Response(JSON.stringify(userFixture), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      return new Response(
        JSON.stringify({
          status: 401,
          code: "AUTHENTICATION_REQUIRED",
          detail: "AUTHENTICATION_REQUIRED",
          title: "AUTHENTICATION_REQUIRED",
          fieldErrors: [],
          traceId: "review",
        }),
        { status: 401, headers: { "Content-Type": "application/problem+json" } },
      );
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    await act(async () => {
      await queryClient
        .getMutationCache()
        .build(queryClient, {
          mutationFn: () => authApi.updatePreferences({ dailyTargetMinutes: 45 }),
        })
        .execute(undefined)
        .catch(() => {});
    });

    await waitFor(() => expect(result.current.user).toBeNull(), { timeout: 1000 });
  });

  it("R1 regression: query 401 must remove cached personal data as well as the user", async () => {
    const userFixture = {
      id: "A",
      username: "account-a",
      fullName: "Account A",
      timeZone: "Europe/London",
      role: "LEARNER",
    };

    globalThis.fetch = vi.fn().mockImplementation(async (url) => {
      if (String(url).endsWith("/me")) {
        return new Response(JSON.stringify(userFixture), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      return new Response(
        JSON.stringify({
          status: 401,
          code: "AUTHENTICATION_REQUIRED",
          detail: "AUTHENTICATION_REQUIRED",
          title: "AUTHENTICATION_REQUIRED",
          fieldErrors: [],
          traceId: "review",
        }),
        { status: 401, headers: { "Content-Type": "application/problem+json" } },
      );
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    queryClient.setQueryData(authKeys.preferences(), {
      dailyTargetMinutes: 90,
      focusedEnrollmentId: null,
    });
    queryClient.setQueryData([...authKeys.me(), "private-child"], "old personal data");

    await act(async () => {
      await queryClient
        .fetchQuery({
          queryKey: authKeys.preferences(),
          queryFn: () => authApi.getPreferences(),
          staleTime: 0,
        })
        .catch(() => {});
    });

    await waitFor(() => expect(result.current.user).toBeNull());
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();
    expect(queryClient.getQueryData([...authKeys.me(), "private-child"])).toBeUndefined();
    expect(queryClient.getQueryData(authKeys.me())).toBeNull();
  });

  it("R1 regression: A het phien -> B dang nhap khong thay cache cua A", async () => {
    const userA = {
      id: "A",
      username: "account-a",
      fullName: "Account A",
      timeZone: "Europe/London",
      role: "LEARNER",
    };
    const userB = {
      id: "B",
      username: "account-b",
      fullName: "Account B",
      timeZone: "Asia/Ho_Chi_Minh",
      role: "LEARNER",
    };

    let currentUserResponse = userA;
    let rejectPreferences: (reason: Error) => void = () => {};

    globalThis.fetch = vi.fn().mockImplementation(async (url, options) => {
      if (String(url).endsWith("/auth/csrf")) {
        return new Response(
          JSON.stringify({ token: "csrf-tok", headerName: "X-CSRF-TOKEN" }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (String(url).endsWith("/auth/login")) {
        currentUserResponse = userB;
        return new Response(JSON.stringify(userB), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (String(url).endsWith("/me")) {
        return new Response(JSON.stringify(currentUserResponse), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (String(url).includes("/me/preferences") && options?.method === "PATCH") {
        // Preference mutation fails with 401 AUTHENTICATION_REQUIRED
        return new Response(
          JSON.stringify({
            status: 401,
            code: "AUTHENTICATION_REQUIRED",
            detail: "Phiên của A đã hết hạn trên server",
            title: "AUTHENTICATION_REQUIRED",
            fieldErrors: [],
            traceId: "review-session-expired",
          }),
          { status: 401, headers: { "Content-Type": "application/problem+json" } },
        );
      }
      if (String(url).endsWith("/me/preferences")) {
        return new Promise<Response>((_resolve, reject) => {
          rejectPreferences = reject;
        });
      }
      return Promise.reject(new Error("Unexpected URL: " + String(url)));
    });

    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), {
      wrapper: createWrapper(queryClient),
    });

    // 1. Account A is authenticated
    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));
    expect(result.current.user?.username).toBe("account-a");

    // 2. Account A caches personal data
    queryClient.setQueryData(["personal", "account-a"], {
      learningGoal: "Master RP British accent",
    });
    queryClient.setQueryData(authKeys.preferences(), {
      dailyTargetMinutes: 90,
      focusedEnrollmentId: null,
    });
    expect(queryClient.getQueryData(["personal", "account-a"])).toEqual({
      learningGoal: "Master RP British accent",
    });

    // 3. Account A session expires on mutation
    await act(async () => {
      await queryClient
        .getMutationCache()
        .build(queryClient, {
          mutationFn: () => authApi.updatePreferences({ dailyTargetMinutes: 30 }),
        })
        .execute(undefined)
        .catch(() => {});
    });

    // 4. Session expiration purges user and all personal cache
    await waitFor(() => expect(result.current.user).toBeNull());
    expect(queryClient.getQueryData(["personal", "account-a"])).toBeUndefined();
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();

    // 5. Account B logs in
    await act(async () => {
      await result.current.login({ username: "account-b", password: "password123" });
    });

    // 6. Account B is authenticated, and Account A's cache is completely absent
    await waitFor(() => expect(result.current.user?.username).toBe("account-b"));
    expect(queryClient.getQueryData(["personal", "account-a"])).toBeUndefined();
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();

    const pendingPreferences = queryClient.fetchQuery({
      queryKey: authKeys.preferences(),
      queryFn: () => authApi.getPreferences(),
    });
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();
    rejectPreferences(new TypeError("Network unavailable for account B"));
    await expect(pendingPreferences).rejects.toThrow();
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();
  });

  it("logout purges real preferences and nested auth data while preserving only anonymous /me", async () => {
    globalThis.fetch = vi.fn().mockImplementation(async (url) => {
      if (String(url).endsWith("/auth/csrf")) {
        return new Response(JSON.stringify({ token: "token", headerName: "X-CSRF-TOKEN" }), {
          status: 200, headers: { "Content-Type": "application/json" },
        });
      }
      if (String(url).endsWith("/auth/logout")) return new Response(null, { status: 204 });
      return new Response(JSON.stringify({ id: "A", username: "account-a", role: "LEARNER" }), {
        status: 200, headers: { "Content-Type": "application/json" },
      });
    });
    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => useAuth(), { wrapper: createWrapper(queryClient) });
    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));
    queryClient.setQueryData(authKeys.preferences(), { dailyTargetMinutes: 90, focusedEnrollmentId: null });
    queryClient.setQueryData([...authKeys.me(), "private-child"], "old personal data");
    await act(async () => { await result.current.logout(); });
    expect(result.current.user).toBeNull();
    expect(queryClient.getQueryData(authKeys.me())).toBeNull();
    expect(queryClient.getQueryData(authKeys.preferences())).toBeUndefined();
    expect(queryClient.getQueryData([...authKeys.me(), "private-child"])).toBeUndefined();
  });
});
