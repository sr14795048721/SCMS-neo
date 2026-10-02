import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  fetchWithCoreFallback,
  parseJsonSafely
} from "./backend";

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

type BackendDirtyDataCleanup = {
  scoreRecordsRemoved?: number;
  rewardOrdersRemoved?: number;
  clubJoinRequestsRemoved?: number;
  registrationsRemoved?: number;
  clubMembersRemoved?: number;
  notificationsRemoved?: number;
  studentProfilesRemoved?: number;
  managerProfilesRemoved?: number;
  clubManagerBindingsRemoved?: number;
  clubDutiesRemoved?: number;
  rewardTargetClubsRemoved?: number;
  totalRemoved?: number;
};

type BackendAdminStatisticsOverview = {
  totalUsers?: number;
  activeUsers?: number;
  totalClubs?: number;
  totalActivities?: number;
  activeRegistrations?: number;
  uniqueVisitors?: number;
  todayVisitCount?: number;
  onlineSessions?: number;
};

type BackendAdminRoleDistribution = {
  studentCount?: number;
  managerCount?: number;
  adminCount?: number;
};

type BackendAdminClubRanking = {
  rank?: number;
  clubId?: number;
  clubName?: string;
  memberCount?: number;
  activityCount?: number;
};

type BackendAdminStatistics = {
  overview?: BackendAdminStatisticsOverview | null;
  roleDistribution?: BackendAdminRoleDistribution | null;
  clubRankings?: BackendAdminClubRanking[] | null;
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

function normalizeAdminStatistics(item?: BackendAdminStatistics | null) {
  const overview = item?.overview;
  const roleDistribution = item?.roleDistribution;
  const clubRankings = Array.isArray(item?.clubRankings) ? item.clubRankings : [];

  return {
    overview: {
      totalUsers: Number(overview?.totalUsers || 0),
      activeUsers: Number(overview?.activeUsers || 0),
      totalClubs: Number(overview?.totalClubs || 0),
      totalActivities: Number(overview?.totalActivities || 0),
      activeRegistrations: Number(overview?.activeRegistrations || 0),
      uniqueVisitors: Number(overview?.uniqueVisitors || 0),
      todayVisitCount: Number(overview?.todayVisitCount || 0),
      onlineSessions: Number(overview?.onlineSessions || 0)
    },
    roleDistribution: {
      studentCount: Number(roleDistribution?.studentCount || 0),
      managerCount: Number(roleDistribution?.managerCount || 0),
      adminCount: Number(roleDistribution?.adminCount || 0)
    },
    clubRankings: clubRankings.map((item, index) => ({
      rank: Number(item?.rank || index + 1),
      clubId: Number(item?.clubId || 0),
      clubName: String(item?.clubName || ""),
      memberCount: Number(item?.memberCount || 0),
      activityCount: Number(item?.activityCount || 0)
    }))
  };
}

function normalizeDirtyDataCleanup(item?: BackendDirtyDataCleanup | null) {
  return {
    scoreRecordsRemoved: Number(item?.scoreRecordsRemoved || 0),
    rewardOrdersRemoved: Number(item?.rewardOrdersRemoved || 0),
    clubJoinRequestsRemoved: Number(item?.clubJoinRequestsRemoved || 0),
    registrationsRemoved: Number(item?.registrationsRemoved || 0),
    clubMembersRemoved: Number(item?.clubMembersRemoved || 0),
    notificationsRemoved: Number(item?.notificationsRemoved || 0),
    studentProfilesRemoved: Number(item?.studentProfilesRemoved || 0),
    managerProfilesRemoved: Number(item?.managerProfilesRemoved || 0),
    clubManagerBindingsRemoved: Number(item?.clubManagerBindingsRemoved || 0),
    clubDutiesRemoved: Number(item?.clubDutiesRemoved || 0),
    rewardTargetClubsRemoved: Number(item?.rewardTargetClubsRemoved || 0),
    totalRemoved: Number(item?.totalRemoved || 0)
  };
}

export async function proxyAdminSystemPassword(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/me/password", {
      method: "POST",
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

export async function proxyAdminSystemLogout(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/auth/logout", {
      method: "POST",
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

export async function proxyAdminStatistics(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/reports/admin-statistics", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAdminStatistics>>(response);

    if (!response.ok || !data?.data) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeAdminStatistics(data.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminSystemDirtyDataCleanup(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/me/dirty-data", {
      method: "DELETE",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendDirtyDataCleanup>>(response);

    if (!response.ok || !data?.data) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeDirtyDataCleanup(data.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}
