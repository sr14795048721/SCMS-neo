import { NextResponse } from "next/server";
import { createProxyHeaders, parseJsonSafely, resolveBackendBaseUrl } from "../../../lib/api/backend";

type BackendActivity = {
  id?: number;
  clubId?: number;
  title?: string;
  description?: string;
  location?: string;
  startTime?: string | null;
  endTime?: string | null;
  capacity?: number;
  status?: string;
  createdBy?: number;
  createdAt?: string | null;
};

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

function normalizeActivity(item?: BackendActivity | null) {
  return {
    id: Number(item?.id || 0),
    clubId: Number(item?.clubId || 0),
    title: String(item?.title || ""),
    description: String(item?.description || ""),
    location: String(item?.location || ""),
    startTime: item?.startTime ? String(item.startTime) : null,
    endTime: item?.endTime ? String(item.endTime) : null,
    capacity: Number(item?.capacity || 0),
    status: String(item?.status || ""),
    createdBy: Number(item?.createdBy || 0),
    createdAt: item?.createdAt ? String(item.createdAt) : null
  };
}

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}/api/v1/activities`, {
      method: "GET",
      headers: createProxyHeaders(authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendActivity[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "ACTIVITY_LIST_REQUEST_FAILED",
          message: data?.message || "activity list request failed"
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
        activities: Array.isArray(data?.data) ? data.data.map((item) => normalizeActivity(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "ACTIVITY_LIST_BACKEND_UNREACHABLE",
        message: "activity list backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}

