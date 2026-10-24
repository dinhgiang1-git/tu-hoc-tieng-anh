import { createContext } from "react";
import type { LoginPayload } from "../api/auth.api";
import type { User } from "../types";

export interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  isError: boolean;
  authError: Error | null;
  isAuthenticated: boolean;
  isLoggingOut: boolean;
  logoutError: string | null;
  clearLogoutError: () => void;
  login: (payload: LoginPayload) => Promise<void>;
  logout: () => Promise<void>;
  refetchUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
