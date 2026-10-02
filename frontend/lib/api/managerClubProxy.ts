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

type BackendClubSummary = {
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  memberCount?: number;
  pendingJoinRequestCount?: number;
  draftActivityCount?: number;
  createdAt?: string | null;
};

type BackendClubDetail = {
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  status?: string;
  memberCount?: number;
  pendingJoinRequestCount?: number;
  totalJoinRequestCount?: number;
  activityCount?: number;
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

type BackendClubDuty = {
  id?: number;
  name?: string;
  permissions?: string[];
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendClubJoinRequest = {
  requestId?: number;
  clubId?: number;
  studentUserId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  reason?: string;
  status?: string;
  reviewedBy?: number | null;
  reviewedByName?: string;
  reviewedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeClubSummary(item?: BackendClubSummary | null) {
  return {
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    clubType: String(item?.clubType || ""),
    description: String(item?.description || ""),
    memberCount: Number(item?.memberCount || 0),
    pendingJoinRequestCount: Number(item?.pendingJoinRequestCount || 0),
    draftActivityCount: Number(item?.draftActivityCount || 0),
    createdAt: item?.createdAt ? String(item.createdAt) : null
  };
}

function normalizeClubDetail(item?: BackendClubDetail | null) {
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

function normalizeClubDuty(item?: BackendClubDuty | null) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    permissions: Array.isArray(item?.permissions) ? item.permissions.map((permission) => String(permission || "")) : [],
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeClubJoinRequest(item?: BackendClubJoinRequest | null) {
  return {
    requestId: Number(item?.requestId || 0),
    clubId: Number(item?.clubId || 0),
    studentUserId: Number(item?.studentUserId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    grade: String(item?.grade || ""),
    className: String(item?.className || ""),
    reason: String(item?.reason || ""),
    status: String(item?.status || ""),
    reviewedBy: item?.reviewedBy == null ? null : Number(item.reviewedBy),
    reviewedByName: String(item?.reviewedByName || ""),
    reviewedAt: item?.reviewedAt ? String(item.reviewedAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

export async function proxyManagerClubCollection(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/managers/me/clubs", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubSummary[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubSummary(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerClubDetail(request: Request, clubId: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubDetail(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerClubMembers(request: Request, clubId: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}/members`, {
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

export async function proxyManagerClubJoinRequests(
  request: Request,
  clubId: string,
  action: "list" | "approve" | "reject",
  requestId: string | null,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const suffix =
    action === "list"
      ? `/api/v1/managers/me/clubs/${clubId}/join-requests`
      : `/api/v1/managers/me/clubs/${clubId}/join-requests/${requestId}/${action}`;

  try {
    const response = await fetchWithCoreFallback(suffix, {
      method: action === "list" ? "GET" : "POST",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubJoinRequest[] | BackendClubJoinRequest>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (action === "list") {
      return NextResponse.json(
        {
          success: true,
          data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubJoinRequest(item)) : []
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubJoinRequest(data?.data as BackendClubJoinRequest | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerClubDuties(
  request: Request,
  clubId: string,
  method: "GET" | "POST" | "PATCH" | "DELETE",
  failure: FailureConfig,
  dutyId?: string
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" || method === "PATCH" ? await request.text() : undefined;
  const targetPath = dutyId
    ? `/api/v1/managers/me/clubs/${clubId}/duties/${dutyId}`
    : `/api/v1/managers/me/clubs/${clubId}/duties`;

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubDuty[] | BackendClubDuty>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "GET") {
      return NextResponse.json(
        {
          success: true,
          data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubDuty(item)) : []
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubDuty(data?.data as BackendClubDuty | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerClubMemberDuty(
  request: Request,
  clubId: string,
  studentUserId: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}/members/${studentUserId}/duty`, {
      method: "PATCH",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerClubMemberRemove(
  request: Request,
  clubId: string,
  studentUserId: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}/members/${studentUserId}`, {
      method: "DELETE",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyStudentClubJoinRequest(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/students/me/club-join-requests", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubJoinRequest>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubJoinRequest(data?.data)
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
