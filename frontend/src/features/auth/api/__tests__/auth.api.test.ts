import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { authApi } from "../auth.api";
import { csrfSession } from "@/lib/api/csrf-session";
import { ApiError } from "@/lib/api/api-error";

describe("authApi.logout - F2 Server Confirmation & Retry", () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
    csrfSession.clear();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    csrfSession.clear();
  });

  it("completes logout and clears CSRF session when server returns 204", async () => {
    const clearSpy = vi.spyOn(csrfSession, "clear");

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-1" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    await authApi.logout();

    expect(clearSpy).toHaveBeenCalled();
  });

  it("treats 401 on logout as successful session termination on server", async () => {
    const clearSpy = vi.spyOn(csrfSession, "clear");

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-1" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              type: "about:blank",
              title: "Chưa xác thực",
              status: 401,
              detail: "Phiên đăng nhập đã hết hạn.",
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

    // Should resolve cleanly without throwing
    await expect(authApi.logout()).resolves.toBeUndefined();
    expect(clearSpy).toHaveBeenCalled();
  });

  it("retries once on 403 CSRF_INVALID with a fresh CSRF token and succeeds if retry returns 204", async () => {
    let logoutCallCount = 0;
    const refreshSpy = vi.spyOn(csrfSession, "refresh");
    const clearSpy = vi.spyOn(csrfSession, "clear");

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "new-csrf-token" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        logoutCallCount++;
        if (logoutCallCount === 1) {
          // First call: CSRF token is stale
          return Promise.resolve(
            new Response(
              JSON.stringify({
                type: "about:blank",
                title: "CSRF không hợp lệ",
                status: 403,
                detail: "CSRF token không hợp lệ hoặc đã hết hạn.",
                code: "CSRF_INVALID",
                traceId: "trace-csrf",
                fieldErrors: [],
              }),
              {
                status: 403,
                headers: { "Content-Type": "application/problem+json" },
              },
            ),
          );
        }
        // Second call (retry): returns 204
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    // Preset existing token to avoid initial refresh
    (csrfSession as unknown as { token: string | null }).token = "stale-token";

    await authApi.logout();

    expect(logoutCallCount).toBe(2);
    expect(refreshSpy).toHaveBeenCalledTimes(1);
    expect(clearSpy).toHaveBeenCalled();
  });

  it("throws ApiError when server returns 500 so client does NOT fake success", async () => {
    const clearSpy = vi.spyOn(csrfSession, "clear");

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-1" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              type: "about:blank",
              title: "Lỗi máy chủ",
              status: 500,
              detail: "Lỗi nội bộ máy chủ khi hủy session.",
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

    await expect(authApi.logout()).rejects.toThrow(ApiError);
    // CSRF should not be cleared when logout fails
    expect(clearSpy).not.toHaveBeenCalled();
  });

  it("throws ApiError on network error so client does NOT fake success", async () => {
    const clearSpy = vi.spyOn(csrfSession, "clear");

    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/api/v1/auth/csrf")) {
        return Promise.resolve(
          new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-token-1" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      if (url.includes("/api/v1/auth/logout")) {
        return Promise.reject(new TypeError("Failed to fetch"));
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    await expect(authApi.logout()).rejects.toThrow(ApiError);
    expect(clearSpy).not.toHaveBeenCalled();
  });

  it("R2 regression: retrying login after CSRF_INVALID does not keep sending the stale token", async () => {
    let csrfRequests = 0;
    const userFixture = {
      id: "A",
      username: "account-a",
      fullName: "Account A",
      timeZone: "Europe/London",
      role: "LEARNER",
    };

    globalThis.fetch = vi.fn().mockImplementation(async (url, options) => {
      if (String(url).endsWith("/auth/csrf")) {
        return new Response(
          JSON.stringify({
            token: ++csrfRequests === 1 ? "stale" : "fresh",
            headerName: "X-CSRF-TOKEN",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const token = new Headers(options?.headers).get("X-CSRF-TOKEN");
      if (token === "stale") {
        return new Response(
          JSON.stringify({
            status: 403,
            code: "CSRF_INVALID",
            detail: "CSRF token invalid",
            title: "CSRF_INVALID",
            fieldErrors: [],
            traceId: "review",
          }),
          { status: 403, headers: { "Content-Type": "application/problem+json" } },
        );
      }
      return new Response(JSON.stringify(userFixture), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    await expect(
      authApi.login({ username: "account-a", password: "fixture" }),
    ).rejects.toThrow();
    await expect(
      authApi.login({ username: "account-a", password: "fixture" }),
    ).resolves.toMatchObject({ id: "A" });
  });

  it("R2 regression: PATCH receiving CSRF_INVALID does not auto-replay mutation, but clears token for retry", async () => {
    let csrfRequests = 0;
    let patchCallCount = 0;
    const prefFixture = {
      dailyTargetMinutes: 45,
      focusedEnrollmentId: null,
    };

    globalThis.fetch = vi.fn().mockImplementation(async (url, options) => {
      if (String(url).endsWith("/auth/csrf")) {
        return new Response(
          JSON.stringify({
            token: ++csrfRequests === 1 ? "stale-patch" : "fresh-patch",
            headerName: "X-CSRF-TOKEN",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (String(url).includes("/me/preferences") && options?.method === "PATCH") {
        patchCallCount++;
        const token = new Headers(options?.headers).get("X-CSRF-TOKEN");
        if (token === "stale-patch") {
          return new Response(
            JSON.stringify({
              status: 403,
              code: "CSRF_INVALID",
              detail: "CSRF token invalid",
              title: "CSRF_INVALID",
              fieldErrors: [],
              traceId: "review-patch",
            }),
            { status: 403, headers: { "Content-Type": "application/problem+json" } },
          );
        }
        return new Response(JSON.stringify(prefFixture), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      return Promise.reject(new Error("Unexpected URL"));
    });

    // First attempt fails: must throw and NOT auto-replay
    await expect(
      authApi.updatePreferences({ dailyTargetMinutes: 45 }),
    ).rejects.toThrow();
    expect(patchCallCount).toBe(1); // Exactly 1 call: NO auto-replay!

    // Second attempt (user retry): fetches fresh token and succeeds
    await expect(
      authApi.updatePreferences({ dailyTargetMinutes: 45 }),
    ).resolves.toMatchObject({ dailyTargetMinutes: 45 });
    expect(patchCallCount).toBe(2);
    expect(csrfRequests).toBe(2);
  });
});
