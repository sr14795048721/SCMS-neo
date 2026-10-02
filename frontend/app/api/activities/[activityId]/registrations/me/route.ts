import { NextResponse } from "next/server";
import { createProxyHeaders, parseJsonSafely, resolveBackendBaseUrl } from "../../../../../../lib/api/backend";

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

export async function DELETE(
  request: Request,
  context: {
    params: Promise<{ activityId: string }>;
  }
) {
  const { activityId } = await context.params;
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(
      `${resolveBackendBaseUrl()}/api/v1/activities/${encodeURIComponent(activityId)}/registrations/me`,
      {
        method: "DELETE",
        headers: createProxyHeaders(authorization),
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<BackendResponse<unknown>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "ACTIVITY_REGISTRATION_CANCEL_FAILED",
          message: data?.message || "activity registration cancel failed"
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
        data: data?.data || null
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "ACTIVITY_REGISTRATION_CANCEL_UNREACHABLE",
        message: "activity registration cancel backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}
