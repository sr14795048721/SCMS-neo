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
  clubName?: string;
  title?: string;
  description?: string;
  location?: string;
  startTime?: string | null;
  endTime?: string | null;
  capacity?: number;
  status?: string;
  registrationCount?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
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
    clubName: String(item?.clubName || ""),
    title: String(item?.title || ""),
    description: String(item?.description || ""),
    location: String(item?.location || ""),
    startTime: item?.startTime ? String(item.startTime) : null,
    endTime: item?.endTime ? String(item.endTime) : null,
    capacity: Number(item?.capacity || 0),
    status: String(item?.status || ""),
    registrationCount: Number(item?.registrationCount || 0),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

export async function proxyManagerActivityCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback("/api/v1/managers/me/activities", {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendActivity[] | BackendActivity>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "GET") {
      return NextResponse.json(
        {
          success: true,
          data: Array.isArray(data?.data) ? data.data.map((item) => normalizeActivity(item)) : []
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeActivity(data?.data as BackendActivity | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerActivityDetail(
  request: Request,
  targetPath: string,
  method: "PATCH" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PATCH" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendActivity>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeActivity(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

function createFailureResponse(
  status: number,
  code: string | undefined,
  message: string | undefined,
  failure: FailureConfig
) {
  return NextResponse.json(
    {
      success: false,
      code: code || failure.failureCode,
      message: message || failure.failureMessage
    },
    {
      status: status || 500,
      headers: NO_STORE_HEADERS
    }
  );
}

function createUnavailableResponse(failure: FailureConfig) {
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
