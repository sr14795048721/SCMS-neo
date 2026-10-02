import { NextResponse } from "next/server";
import { createProxyHeaders, parseJsonSafely, resolveBackendBaseUrl } from "../../../../../lib/api/backend";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}/api/v1/activities/registrations/me`, {
      method: "GET",
      headers: createProxyHeaders(authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<number[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "MY_ACTIVITY_REGISTRATIONS_REQUEST_FAILED",
          message: data?.message || "my activity registrations request failed"
        },
        {
          status: response.status || 500,
          headers: NO_STORE_HEADERS
        }
      );
    }

    return NextResponse.json(
      {
        success: true,
        activityIds: Array.isArray(data?.data) ? data.data.map((item) => Number(item || 0)).filter(Boolean) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "MY_ACTIVITY_REGISTRATIONS_BACKEND_UNREACHABLE",
        message: "my activity registrations backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}
