/**
 * In-memory CSRF session management.
 * Uses dedicated fetch to bootstrap and rotate tokens, avoiding circular calls with apiRequest.
 * Adheres to FE_CODE_CONVENTION.md Section 4.3 & 5.1.
 */

interface CsrfTokenResponse {
  headerName: string;
  token: string;
}

class CsrfSession {
  private token: string | null = null;
  private headerName = "X-CSRF-TOKEN";
  private inflightPromise: Promise<string> | null = null;

  async getToken(): Promise<string> {
    if (this.token) {
      return this.token;
    }
    return this.refresh();
  }

  getHeaderName(): string {
    return this.headerName;
  }

  async refresh(): Promise<string> {
    if (this.inflightPromise) {
      return this.inflightPromise;
    }

    this.inflightPromise = (async () => {
      try {
        const response = await fetch("/api/v1/auth/csrf", {
          method: "GET",
          credentials: "include",
          headers: {
            Accept: "application/json",
          },
          cache: "no-store",
        });

        if (!response.ok) {
          throw new Error(`Failed to fetch CSRF token: status ${response.status}`);
        }

        const data = (await response.json()) as CsrfTokenResponse;
        if (data && typeof data.token === "string") {
          this.token = data.token;
          if (typeof data.headerName === "string") {
            this.headerName = data.headerName;
          }
          return this.token;
        }

        throw new Error("Invalid CSRF token response payload");
      } finally {
        this.inflightPromise = null;
      }
    })();

    return this.inflightPromise;
  }

  clear(): void {
    this.token = null;
  }
}

export const csrfSession = new CsrfSession();
