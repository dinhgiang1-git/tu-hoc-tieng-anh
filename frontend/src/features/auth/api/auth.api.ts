import { apiRequest, voidDecoder } from "@/lib/api/api-client";
import { ApiError } from "@/lib/api/api-error";
import { csrfSession } from "@/lib/api/csrf-session";
import type { LearnerPreferences, User } from "../types";

export interface LoginPayload {
  username: string;
  password: string;
}

export interface UpdatePreferencesPayload {
  dailyTargetMinutes?: number;
  focusedEnrollmentId?: string | null;
}

export const authApi = {
  async login(payload: LoginPayload): Promise<User> {
    try {
      const user = await apiRequest<User>("/auth/login", {
        method: "POST",
        json: payload,
      });
      // Rotate CSRF token after login as session ID changed
      await csrfSession.refresh();
      return user;
    } catch (error) {
      if (error instanceof ApiError && error.isCsrfInvalid()) {
        csrfSession.clear();
      }
      throw error;
    }
  },

  /**
   * Performs server-side logout adhering to F2.
   * If server returns 401, session is already invalid (considered successfully logged out).
   * If server returns 403 CSRF_INVALID, refreshes token and retries once.
   * Throws on failure (network/500/etc.) so client does NOT fake success.
   */
  async logout(): Promise<void> {
    try {
      await apiRequest<void>(
        "/auth/logout",
        {
          method: "POST",
        },
        voidDecoder,
      );
    } catch (error) {
      if (error instanceof ApiError) {
        // 401 means session is already expired or dead on server: logout succeeded
        if (error.isUnauthorized()) {
          csrfSession.clear();
          return;
        }

        // 403 CSRF_INVALID: refresh token once and retry logout request
        if (error.isCsrfInvalid()) {
          await csrfSession.refresh();
          await apiRequest<void>(
            "/auth/logout",
            {
              method: "POST",
            },
            voidDecoder,
          );
          csrfSession.clear();
          return;
        }
      }
      // Re-throw so caller knows logout failed
      throw error;
    }

    csrfSession.clear();
  },

  async getMe(signal?: AbortSignal): Promise<User> {
    return apiRequest<User>("/me", {
      method: "GET",
      signal,
    });
  },

  async getPreferences(signal?: AbortSignal): Promise<LearnerPreferences> {
    return apiRequest<LearnerPreferences>("/me/preferences", {
      method: "GET",
      signal,
    });
  },

  async updatePreferences(
    payload: UpdatePreferencesPayload,
  ): Promise<LearnerPreferences> {
    return apiRequest<LearnerPreferences>("/me/preferences", {
      method: "PATCH",
      json: payload,
    });
  },
};
