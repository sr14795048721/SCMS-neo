import { NextResponse } from "next/server";
import { fetchWithCoreFallback, parseJsonSafely } from "./backend";
import { PublicAttendanceSession, PublicAttendanceSignResult } from "../attendance/publicAttendanceTypes";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

type BackendPublicAttendanceSession = {
  clubName?: string;
  title?: string;
  status?: string;
  scoreRuleName?: string;
  scoreDelta?: number;
  startedAt?: string | null;
};

type BackendPublicAttendanceSignResult = {
  displayName?: string;
  grade?: string;
  className?: string;
  role?: string;
  checkInAt?: string | null;
};

type FailureConfig = {
  failureCode: string;
  failureMessage: string;
  unavailableCode: string;
  unavailableMessage: string;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeSession(value?: BackendPublicAttendanceSession | null): PublicAttendanceSession {
  return {
    clubName: String(value?.clubName || ""),
    title: String(value?.title || ""),
    status: value?.status === "COMPLETED" ? "COMPLETED" : "OPEN",
    scoreRuleName: String(value?.scoreRuleName || ""),
    scoreDelta: Number(value?.scoreDelta || 0),
    startedAt: value?.startedAt ? String(value.startedAt) : null
  };
}

function normalizeSignResult(value?: BackendPublicAttendanceSignResult | null): PublicAttendanceSignResult {
  return {
    displayName: String(value?.displayName || ""),
    grade: String(value?.grade || ""),
    className: String(value?.className || ""),
    role: String(value?.role || ""),
    checkInAt: value?.checkInAt ? String(value.checkInAt) : null
  };
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

export async function proxyPublicAttendanceSession(shareToken: string, failure: FailureConfig) {
  try {
    const response = await fetchWithCoreFallback(
      `/api/v1/public/attendance/sessions/${encodeURIComponent(shareToken)}`,
      {
        method: "GET",
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<BackendResponse<BackendPublicAttendanceSession>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeSession(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyPublicAttendanceSign(
  request: Request,
  shareToken: string,
  failure: FailureConfig
) {
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(
      `/api/v1/public/attendance/sessions/${encodeURIComponent(shareToken)}/sign`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body,
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<BackendResponse<BackendPublicAttendanceSignResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeSignResult(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}
