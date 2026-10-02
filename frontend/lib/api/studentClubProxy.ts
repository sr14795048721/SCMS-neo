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

type BackendDiscoverClub = {
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  memberCount?: number;
  dutyId?: number | null;
  dutyName?: string;
  dutyPermissions?: string[];
  canManage?: boolean;
  createdAt?: string | null;
};

type BackendPendingRequest = {
  requestId?: number;
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  memberCount?: number;
  reason?: string;
  status?: string;
  createdAt?: string | null;
};

type BackendDiscoverPayload = {
  joinedClub?: BackendDiscoverClub | null;
  pendingRequest?: BackendPendingRequest | null;
  clubs?: BackendDiscoverClub[];
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeDiscoverClub(item?: BackendDiscoverClub | null) {
  if (!item) {
    return null;
  }
  return {
    clubId: Number(item.clubId || 0),
    clubName: String(item.clubName || ""),
    clubType: String(item.clubType || ""),
    description: String(item.description || ""),
    memberCount: Number(item.memberCount || 0),
    dutyId: item.dutyId == null ? null : Number(item.dutyId),
    dutyName: String(item.dutyName || ""),
    dutyPermissions: Array.isArray(item.dutyPermissions)
      ? item.dutyPermissions.map((permission) => String(permission || ""))
      : [],
    canManage: Boolean(item.canManage),
    createdAt: item.createdAt ? String(item.createdAt) : null
  };
}

function normalizePendingRequest(item?: BackendPendingRequest | null) {
  if (!item) {
    return null;
  }
  return {
    requestId: Number(item.requestId || 0),
    clubId: Number(item.clubId || 0),
    clubName: String(item.clubName || ""),
    clubType: String(item.clubType || ""),
    description: String(item.description || ""),
    memberCount: Number(item.memberCount || 0),
    reason: String(item.reason || ""),
    status: String(item.status || ""),
    createdAt: item.createdAt ? String(item.createdAt) : null
  };
}

export async function proxyStudentDiscoverClubs(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/students/me/discover-clubs", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendDiscoverPayload>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500, headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          joinedClub: normalizeDiscoverClub(data?.data?.joinedClub),
          pendingRequest: normalizePendingRequest(data?.data?.pendingRequest),
          clubs: Array.isArray(data?.data?.clubs) ? data.data.clubs.map((item) => normalizeDiscoverClub(item)).filter(Boolean) : []
        }
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
      { status: 503, headers: NO_STORE_HEADERS }
    );
  }
}
