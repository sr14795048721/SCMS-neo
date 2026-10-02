import { NextResponse } from "next/server";
import { createForwardHeaders, fetchWithCoreFallback, parseJsonSafely } from "./backend";

type FailureConfig = {
  failureCode: string;
  failureMessage: string;
  unavailableCode: string;
  unavailableMessage: string;
};

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

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

export async function proxyStudentActivities(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/students/me/activities", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendActivity[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
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
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}

export async function proxyStudentActivityMutation(
  request: Request,
  targetPath: string,
  method: "PATCH",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendActivity>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
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
        data: normalizeActivity(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}
