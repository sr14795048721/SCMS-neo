import { NextResponse } from "next/server";
import { loadApiMessages } from "../../../../lib/i18n/apiMessages";

type RefreshRequestPayload = {
  refreshToken?: string;
};

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

export async function POST(request: Request) {
  const payload = (await request.json().catch(() => null)) as RefreshRequestPayload | null;
  const messages = await loadApiMessages(request.headers.get("accept-language"));
  const refreshToken = String(payload?.refreshToken || "").trim();

  if (!refreshToken) {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_REFRESH_INVALID_INPUT",
        message: messages.auth.loginRequired
      },
      { status: 400 }
    );
  }

  const backendBase = process.env.BACKEND_BASE_URL || "http://localhost:8080";

  try {
    const response = await fetch(`${backendBase}/api/v1/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        refreshToken
      }),
      cache: "no-store"
    });

    const data = (await response.json().catch(() => null)) as BackendResponse<Record<string, unknown>> | null;
    const backendPayload = data?.data;
    if (!response.ok || !backendPayload) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "AUTH_REFRESH_FAILED",
          message: data?.message || messages.auth.serviceUnavailable
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({
      success: true,
      data: {
        accessToken: backendPayload.accessToken,
        refreshToken: backendPayload.refreshToken,
        accessExpiresInSeconds: backendPayload.accessExpiresInSeconds,
        refreshExpiresInSeconds: backendPayload.refreshExpiresInSeconds
      }
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_BACKEND_UNREACHABLE",
        message: messages.auth.serviceUnavailable
      },
      { status: 503 }
    );
  }
}
