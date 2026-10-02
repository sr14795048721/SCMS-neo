"use client";

import zhApiMessages from "../../locales/zh-CN/api.json";
import {
  clearPersistedAuthTokens,
  getPersistedAuthState,
  isTokenExpired,
  persistAuthTokensToStorage,
  PersistedAuthState,
  StoredAuthTokens
} from "./session";

type RefreshRouteResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: StoredAuthTokens;
};

type AuthorizedFetchOptions = {
  minValidityMs?: number;
  retryUnauthorized?: boolean;
};

const ACCESS_TOKEN_REFRESH_LEEWAY_MS = 60 * 1000;
const DEFAULT_RETRYABLE_MESSAGE = zhApiMessages.auth.serviceUnavailable;

let refreshPromise: Promise<PersistedAuthState | null> | null = null;

export class AuthRequestError extends Error {
  constructor(
    message: string,
    options?: {
      isRetryable?: boolean;
    }
  ) {
    super(message);
    this.name = "AuthRequestError";
    this.isRetryable = options?.isRetryable === true;
  }

  readonly isRetryable: boolean;
}

export async function getValidAccessToken(
  options?: {
    minValidityMs?: number;
  }
): Promise<string | null> {
  const authState = getPersistedAuthState();
  if (!authState?.tokens.accessToken) {
    return null;
  }

  const minValidityMs = options?.minValidityMs ?? ACCESS_TOKEN_REFRESH_LEEWAY_MS;
  const accessToken = authState.tokens.accessToken;
  if (!isTokenExpired(accessToken, minValidityMs)) {
    return accessToken;
  }

  const accessExpired = isTokenExpired(accessToken, 0);
  try {
    const refreshedState = await refreshPersistedAuthTokens();
    return refreshedState?.tokens.accessToken ?? null;
  } catch (error) {
    if (error instanceof AuthRequestError && error.isRetryable && !accessExpired) {
      return accessToken;
    }
    throw error;
  }
}

export async function authorizedFetch(
  input: RequestInfo | URL,
  init?: RequestInit,
  options?: AuthorizedFetchOptions
): Promise<Response | null> {
  const minValidityMs = options?.minValidityMs ?? ACCESS_TOKEN_REFRESH_LEEWAY_MS;
  const retryUnauthorized = options?.retryUnauthorized ?? true;

  let accessToken: string | null;
  try {
    accessToken = await getValidAccessToken({ minValidityMs });
  } catch (error) {
    if (error instanceof AuthRequestError && error.isRetryable) {
      return createRetryableResponse(error.message);
    }
    return null;
  }

  if (!accessToken) {
    return null;
  }

  const firstResponse = await fetchWithBearerToken(input, init, accessToken);
  if (firstResponse instanceof AuthRequestError) {
    return createRetryableResponse(firstResponse.message);
  }
  if (firstResponse.status !== 401 || !retryUnauthorized) {
    return firstResponse;
  }

  try {
    const refreshedState = await refreshPersistedAuthTokens();
    if (!refreshedState?.tokens.accessToken) {
      return null;
    }

    const retryResponse = await fetchWithBearerToken(
      input,
      init,
      refreshedState.tokens.accessToken
    );
    if (retryResponse instanceof AuthRequestError) {
      return createRetryableResponse(retryResponse.message);
    }
    if (retryResponse.status === 401) {
      clearPersistedAuthTokens();
      return null;
    }
    return retryResponse;
  } catch (error) {
    if (error instanceof AuthRequestError && error.isRetryable) {
      return createRetryableResponse(error.message);
    }
    return null;
  }
}

export async function refreshPersistedAuthTokens(): Promise<PersistedAuthState | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = refreshPersistedAuthTokensInternal();
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

async function refreshPersistedAuthTokensInternal(): Promise<PersistedAuthState | null> {
  const authState = getPersistedAuthState();
  const refreshToken = authState?.tokens.refreshToken?.trim() ?? "";
  if (!authState || !refreshToken) {
    clearPersistedAuthTokens();
    return null;
  }

  if (isTokenExpired(refreshToken, 0)) {
    clearPersistedAuthTokens();
    return null;
  }

  let response: Response;
  try {
    response = await fetch("/api/auth/refresh", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        refreshToken
      }),
      cache: "no-store"
    });
  } catch {
    throw new AuthRequestError(DEFAULT_RETRYABLE_MESSAGE, {
      isRetryable: true
    });
  }

  const payload = (await response.json().catch(() => null)) as RefreshRouteResponse | null;
  const nextTokens = payload?.data;
  if (!response.ok || payload?.success !== true || !nextTokens?.accessToken) {
    if (response.status === 401 || response.status === 403) {
      clearPersistedAuthTokens();
      return null;
    }

    if (response.status >= 500 || response.status === 408) {
      throw new AuthRequestError(payload?.message || DEFAULT_RETRYABLE_MESSAGE, {
        isRetryable: true
      });
    }

    clearPersistedAuthTokens();
    return null;
  }

  persistAuthTokensToStorage(nextTokens, authState.storage);
  return {
    tokens: nextTokens,
    storage: authState.storage,
    remember: authState.remember
  };
}

async function fetchWithBearerToken(
  input: RequestInfo | URL,
  init: RequestInit | undefined,
  accessToken: string
): Promise<Response | AuthRequestError> {
  const headers = new Headers(init?.headers || {});
  headers.set("Authorization", `Bearer ${accessToken}`);

  try {
    return await fetch(input, {
      ...init,
      headers
    });
  } catch {
    return new AuthRequestError(DEFAULT_RETRYABLE_MESSAGE, {
      isRetryable: true
    });
  }
}

function createRetryableResponse(message: string): Response {
  return new Response(
    JSON.stringify({
      success: false,
      message: message || DEFAULT_RETRYABLE_MESSAGE
    }),
    {
      status: 503,
      headers: {
        "Content-Type": "application/json"
      }
    }
  );
}
