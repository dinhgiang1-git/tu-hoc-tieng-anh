import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/api-error";

/**
 * Global TanStack Query client configuration adhering to FE_CODE_CONVENTION.md Section 6.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes default
      gcTime: 10 * 60 * 1000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Never retry client errors (400, 401, 403, 404, 409, 429)
        if (error instanceof ApiError) {
          if (error.status >= 400 && error.status < 500) {
            return false;
          }
        }
        return failureCount < 1;
      },
    },
    mutations: {
      retry: 0, // Never retry mutations automatically
    },
  },
});
