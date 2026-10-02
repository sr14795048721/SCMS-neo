import { NextResponse } from "next/server";
import { createProxyHeaders, parseJsonSafely, resolveBackendBaseUrl } from "../../../../../lib/api/backend";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

type BackendRegistration = {
  registrationId?: number;
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  registeredAt?: string | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeRegistration(item?: BackendRegistration | null) {
  return {
    registrationId: Number(item?.registrationId || 0),
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    grade: String(item?.grade || ""),
    className: String(item?.className || ""),
    registeredAt: item?.registeredAt ? String(item.registeredAt) : null
  };
}

export async function GET(
  request: Request,
  context: {
    params: Promise<{ activityId: string }>;
  }
) {
  const { activityId } = await context.params;
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(
      `${resolveBackendBaseUrl()}/api/v1/activities/${encodeURIComponent(activityId)}/registrations`,
      {
        method: "GET",
        headers: createProxyHeaders(authorization),
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<BackendResponse<BackendRegistration[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "ACTIVITY_REGISTRATION_LIST_FAILED",
          message: data?.message || "activity registration list failed"
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
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRegistration(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "ACTIVITY_REGISTRATION_LIST_UNREACHABLE",
        message: "activity registration list backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}

export async function POST(
  request: Request,
  context: {
    params: Promise<{ activityId: string }>;
  }
) {
  const { activityId } = await context.params;
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}/api/v1/activities/${encodeURIComponent(activityId)}/registrations`, {
      method: "POST",
      headers: createProxyHeaders(authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<unknown>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "ACTIVITY_REGISTRATION_REQUEST_FAILED",
          message: data?.message || "activity registration request failed"
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
        code: "ACTIVITY_REGISTRATION_BACKEND_UNREACHABLE",
        message: "activity registration backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}
