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

type BackendAttendanceSessionSummary = {
  sessionId?: number;
  clubId?: number;
  title?: string;
  scoreRuleId?: number;
  scoreRuleName?: string;
  scoreDelta?: number;
  status?: string;
  totalMembers?: number;
  checkedInCount?: number;
  checkedOutCount?: number;
  settledCount?: number;
  startedAt?: string | null;
  endedAt?: string | null;
  settledAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendAttendanceSessionMember = {
  studentUserId?: number;
  displayName?: string;
  grade?: string;
  className?: string;
  status?: string;
  checkInAt?: string | null;
  checkOutAt?: string | null;
  settled?: boolean;
};

type BackendAttendanceSessionDetail = {
  session?: BackendAttendanceSessionSummary | null;
  members?: BackendAttendanceSessionMember[] | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

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

function normalizeSummary(item?: BackendAttendanceSessionSummary | null) {
  return {
    sessionId: Number(item?.sessionId || 0),
    clubId: Number(item?.clubId || 0),
    title: String(item?.title || ""),
    scoreRuleId: Number(item?.scoreRuleId || 0),
    scoreRuleName: String(item?.scoreRuleName || ""),
    scoreDelta: Number(item?.scoreDelta || 0),
    status: String(item?.status || ""),
    totalMembers: Number(item?.totalMembers || 0),
    checkedInCount: Number(item?.checkedInCount || 0),
    checkedOutCount: Number(item?.checkedOutCount || 0),
    settledCount: Number(item?.settledCount || 0),
    startedAt: item?.startedAt ? String(item.startedAt) : null,
    endedAt: item?.endedAt ? String(item.endedAt) : null,
    settledAt: item?.settledAt ? String(item.settledAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeMember(item?: BackendAttendanceSessionMember | null) {
  return {
    studentUserId: Number(item?.studentUserId || 0),
    displayName: String(item?.displayName || ""),
    grade: String(item?.grade || ""),
    className: String(item?.className || ""),
    status: String(item?.status || ""),
    checkInAt: item?.checkInAt ? String(item.checkInAt) : null,
    checkOutAt: item?.checkOutAt ? String(item.checkOutAt) : null,
    settled: Boolean(item?.settled)
  };
}

function normalizeDetail(item?: BackendAttendanceSessionDetail | null) {
  return {
    session: normalizeSummary(item?.session),
    members: Array.isArray(item?.members) ? item.members.map((member) => normalizeMember(member)) : []
  };
}

export async function proxyManagerAttendanceSessions(
  request: Request,
  clubId: string,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}/attendance-sessions`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<
      BackendResponse<BackendAttendanceSessionSummary[] | BackendAttendanceSessionDetail>
    >(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "GET") {
      return NextResponse.json(
        {
          success: true,
          data: Array.isArray(data?.data) ? data.data.map((item) => normalizeSummary(item)) : []
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeDetail(data?.data as BackendAttendanceSessionDetail | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerAttendanceSessionDetail(
  request: Request,
  clubId: string,
  sessionId: string,
  action: "detail" | "check-in" | "check-out" | "bulk-check-in" | "bulk-check-out" | "settle",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = action === "check-in" || action === "check-out" || action === "bulk-check-in" || action === "bulk-check-out"
    ? await request.text()
    : undefined;
  const suffix =
    action === "detail"
      ? `/api/v1/managers/me/clubs/${clubId}/attendance-sessions/${sessionId}`
      : `/api/v1/managers/me/clubs/${clubId}/attendance-sessions/${sessionId}/${action}`;

  try {
    const response = await fetchWithCoreFallback(suffix, {
      method: action === "detail" ? "GET" : "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAttendanceSessionDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeDetail(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}
