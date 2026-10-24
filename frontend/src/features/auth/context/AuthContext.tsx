import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/api-error";
import { csrfSession } from "@/lib/api/csrf-session";
import { authApi, type LoginPayload } from "../api/auth.api";
import { authKeys } from "../api/auth.keys";
import type { User } from "../types";
import { AuthContext, type AuthContextValue } from "./auth-context";

function removePersonalQueries(queryClient: QueryClient) {
  const currentUserQuery = queryClient.getQueryCache().find({
    queryKey: authKeys.me(),
    exact: true,
  });
  // Preferences also use the auth prefix. Preserve only the exact /me query.
  queryClient.removeQueries({ predicate: (query) => query !== currentUserQuery });
}

/**
 * AuthProvider wraps the application and manages the complete auth and session lifecycle.
 * Adheres to F2 & F3 of G1-03 review and FE_CODE_CONVENTION.md Section 4.3 & 6.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const isExpiringRef = useRef(false);

  // 1. Fetch current user. Distinguish 401 (anonymous) from 500/network errors (F3)
  const {
    data: user,
    isLoading,
    isError,
    error: authError,
    refetch,
  } = useQuery<User | null, Error>({
    queryKey: authKeys.me(),
    queryFn: async ({ signal }) => {
      try {
        return await authApi.getMe(signal);
      } catch (err) {
        if (err instanceof ApiError && err.isUnauthorized()) {
          // 401 strictly maps to anonymous/unauthenticated
          return null;
        }
        // 500, network errors, etc. MUST be thrown to display error/retry UI
        throw err;
      }
    },
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, err) => {
      if (err instanceof ApiError && err.isUnauthorized()) {
        return false;
      }
      return failureCount < 2;
    },
  });

  // Centralized session expired handler: cancels pending queries, purges all cached personal data, resets user and CSRF
  const handleSessionExpired = useCallback(async () => {
    if (isExpiringRef.current) return;
    isExpiringRef.current = true;
    try {
      await queryClient.cancelQueries();
      removePersonalQueries(queryClient);
      queryClient.setQueryData(authKeys.me(), null);
      csrfSession.clear();
    } finally {
      isExpiringRef.current = false;
    }
  }, [queryClient]);

  // 2. Global session expired listener: monitors BOTH QueryCache and MutationCache for 401 AUTHENTICATION_REQUIRED
  useEffect(() => {
    const checkUnauthorized = (error: unknown) => {
      if (
        error instanceof ApiError &&
        error.isUnauthorized() &&
        error.code === "AUTHENTICATION_REQUIRED"
      ) {
        const cachedUser = queryClient.getQueryData<User | null>(authKeys.me());
        if (user !== null || (cachedUser !== null && cachedUser !== undefined)) {
          void handleSessionExpired();
        }
      }
    };

    const unsubscribeQuery = queryClient.getQueryCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") {
        checkUnauthorized(event.action.error);
      }
    });

    const unsubscribeMutation = queryClient.getMutationCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") {
        checkUnauthorized(event.action.error);
      }
    });

    return () => {
      unsubscribeQuery();
      unsubscribeMutation();
    };
  }, [queryClient, user, handleSessionExpired]);

  // 3. Login
  const login = useCallback(
    async (payload: LoginPayload) => {
      const loggedInUser = await authApi.login(payload);
      queryClient.setQueryData(authKeys.me(), loggedInUser);
      await queryClient.invalidateQueries({ queryKey: authKeys.all });
    },
    [queryClient],
  );

  // 4. Logout adhering to F2: prevents double-clicks, verifies server confirmation, no fake success
  const logout = useCallback(async () => {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setLogoutError(null);

    try {
      await authApi.logout();
      // Server confirmed logout! Cancel running queries and clear client cache
      await queryClient.cancelQueries();
      removePersonalQueries(queryClient);
      queryClient.setQueryData(authKeys.me(), null);
      csrfSession.clear();
    } catch (err) {
      if (err instanceof ApiError) {
        setLogoutError(
          err.problem.detail || "Không thể hoàn tất đăng xuất trên máy chủ.",
        );
      } else {
        setLogoutError(
          "Lỗi kết nối mạng khi đăng xuất. Phiên của bạn vẫn đang mở.",
        );
      }
      throw err;
    } finally {
      setIsLoggingOut(false);
    }
  }, [isLoggingOut, queryClient]);

  const clearLogoutError = useCallback(() => {
    setLogoutError(null);
  }, []);

  const refetchUser = useCallback(async () => {
    await refetch();
  }, [refetch]);

  const value: AuthContextValue = {
    user: user ?? null,
    isLoading,
    isError,
    authError,
    isAuthenticated: !!user,
    isLoggingOut,
    logoutError,
    clearLogoutError,
    login,
    logout,
    refetchUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
