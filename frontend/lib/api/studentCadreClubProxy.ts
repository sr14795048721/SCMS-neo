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

type BackendClubWorkspace = {
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  status?: string;
  memberCount?: number;
  pendingJoinRequestCount?: number;
  totalJoinRequestCount?: number;
  activityCount?: number;
  dutyId?: number | null;
  dutyName?: string;
  permissions?: string[];
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendClubMember = {
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  role?: string;
  dutyId?: number | null;
  dutyName?: string;
  dutyPermissions?: string[];
  joinedAt?: string | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeClubDetail(item?: BackendClubWorkspace | null) {
  return {
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    clubType: String(item?.clubType || ""),
    description: String(item?.description || ""),
    status: String(item?.status || ""),
    memberCount: Number(item?.memberCount || 0),
    pendingJoinRequestCount: Number(item?.pendingJoinRequestCount || 0),
    totalJoinRequestCount: Number(item?.totalJoinRequestCount || 0),
    activityCount: Number(item?.activityCount || 0),
    dutyId: item?.dutyId == null ? null : Number(item.dutyId),
    dutyName: String(item?.dutyName || ""),
    permissions: Array.isArray(item?.permissions) ? item.permissions.map((permission) => String(permission || "")) : [],
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeClubMember(item?: BackendClubMember | null) {
  return {
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    grade: String(item?.grade || ""),
    className: String(item?.className || ""),
    role: String(item?.role || ""),
    dutyId: item?.dutyId == null ? null : Number(item.dutyId),
    dutyName: String(item?.dutyName || ""),
    dutyPermissions: Array.isArray(item?.dutyPermissions) ? item.dutyPermissions.map((permission) => String(permission || "")) : [],
    joinedAt: item?.joinedAt ? String(item.joinedAt) : null
  };
}

export async function proxyStudentCadreClubDetail(request: Request, clubId: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(`/api/v1/students/me/clubs/${clubId}/cadre`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubWorkspace>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true, data: normalizeClubDetail(data?.data) }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyStudentCadreClubMembers(request: Request, clubId: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(`/api/v1/students/me/clubs/${clubId}/cadre/members`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubMember[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubMember(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

function createFailureResponse(status: number, code: string | undefined, message: string | undefined, failure: FailureConfig) {
  return NextResponse.json(
    {
      success: false,
      code: code || failure.failureCode,
      message: message || failure.failureMessage
    },
    { status: status || 500, headers: NO_STORE_HEADERS }
  );
}

function createUnavailableResponse(failure: FailureConfig) {
  return NextResponse.json(
    {
      success: false,
      code: failure.unavailableCode,
      message: failure.unavailableMessage
    },
    { status: 503, headers: NO_STORE_HEADERS }
  );
}
