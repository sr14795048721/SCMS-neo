"use client";

import { clearPersistedAuthTokens, getPersistedAuthTokens } from "../auth/session";

type LogoutResult = {
  ok: boolean;
  status: number;
};

export async function logoutAdminSession(): Promise<LogoutResult> {
  const tokens = getPersistedAuthTokens();

  try {
    if (tokens?.accessToken) {
      const response = await fetch("/api/admin/system/logout", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${tokens.accessToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          refreshToken: tokens.refreshToken || ""
        }),
        cache: "no-store"
      });

      return {
        ok: response.ok,
        status: response.status
      };
    }

    return {
      ok: true,
      status: 200
    };
  } catch {
    return {
      ok: false,
      status: 503
    };
  } finally {
    clearPersistedAuthTokens();
  }
}
