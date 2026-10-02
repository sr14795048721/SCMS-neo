export type UserRole = "ADMIN" | "SUPER_ADMIN" | "CLUB_MANAGER" | "STUDENT";

export type StoredAuthTokens = {
  accessToken: string;
  refreshToken?: string;
  accessExpiresInSeconds?: number;
  refreshExpiresInSeconds?: number;
};

export type PersistedAuthStorage = "localStorage" | "sessionStorage";

export type PersistedAuthState = {
  tokens: StoredAuthTokens;
  storage: PersistedAuthStorage;
  remember: boolean;
};

export const TOKEN_STORAGE_KEY = "scms.auth.tokens";

export const ROLE_REDIRECT_MAP: Record<UserRole, string> = {
  ADMIN: "/sys-admin",
  SUPER_ADMIN: "/sys-admin",
  CLUB_MANAGER: "/club-admin",
  STUDENT: "/student"
};

const ROLE_SET: ReadonlySet<UserRole> = new Set(["ADMIN", "SUPER_ADMIN", "CLUB_MANAGER", "STUDENT"]);

export function resolveRoleRedirect(role: UserRole | null | undefined): string {
  if (!role) {
    return "/";
  }
  return ROLE_REDIRECT_MAP[role];
}

export function extractRoleFromAccessToken(accessToken: string | null | undefined): UserRole | null {
  if (!accessToken) {
    return null;
  }
  const payload = parseJwtPayload(accessToken);
  const role = payload?.role;
  return typeof role === "string" && ROLE_SET.has(role as UserRole) ? (role as UserRole) : null;
}

export function persistAuthTokens(tokens: StoredAuthTokens, remember: boolean): void {
  persistAuthTokensToStorage(tokens, remember ? "localStorage" : "sessionStorage");
}

export function persistAuthTokensToStorage(
  tokens: StoredAuthTokens,
  storage: PersistedAuthStorage
): void {
  if (typeof window === "undefined" || !tokens?.accessToken) {
    return;
  }

  const content = JSON.stringify(tokens);
  if (storage === "localStorage") {
    window.sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    window.localStorage.setItem(TOKEN_STORAGE_KEY, content);
  } else {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, content);
  }
}

export function getPersistedAuthTokens(): StoredAuthTokens | null {
  return getPersistedAuthState()?.tokens ?? null;
}

export function getPersistedAuthState(): PersistedAuthState | null {
  if (typeof window === "undefined") {
    return null;
  }
  return readTokens(window.localStorage, "localStorage") || readTokens(window.sessionStorage, "sessionStorage");
}

export function clearPersistedAuthTokens(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  window.sessionStorage.removeItem(TOKEN_STORAGE_KEY);
}

function readTokens(storage: Storage, storageType: PersistedAuthStorage): PersistedAuthState | null {
  const raw = storage.getItem(TOKEN_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as StoredAuthTokens;
    if (parsed?.accessToken && typeof parsed.accessToken === "string") {
      return {
        tokens: parsed,
        storage: storageType,
        remember: storageType === "localStorage"
      };
    }
  } catch {
    return null;
  }
  return null;
}

export function parseJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }
  try {
    const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const decoded = atob(padded);
    return JSON.parse(decoded) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function extractTokenExpirationMs(token: string | null | undefined): number | null {
  if (!token) {
    return null;
  }

  const payload = parseJwtPayload(token);
  const exp = payload?.exp;
  const seconds = typeof exp === "number" ? exp : Number.parseInt(String(exp || ""), 10);
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return null;
  }
  return seconds * 1000;
}

export function isTokenExpired(token: string | null | undefined, leewayMs = 0): boolean {
  const expirationMs = extractTokenExpirationMs(token);
  if (expirationMs == null) {
    return true;
  }
  return Date.now() + Math.max(0, leewayMs) >= expirationMs;
}
