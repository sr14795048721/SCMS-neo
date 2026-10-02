import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
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

type BackendScoreSummaryClub = {
  clubId?: number;
  clubName?: string;
  score?: number;
};

type BackendScoreSummary = {
  totalScore?: number;
  redeemedScore?: number;
  balanceScore?: number;
  clubs?: BackendScoreSummaryClub[];
};

type BackendClubRanking = {
  rank?: number;
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  score?: number;
};

type BackendSchoolRanking = {
  rank?: number;
  clubId?: number;
  clubName?: string;
  memberCount?: number;
  totalScore?: number;
  avgScore?: number;
};

type BackendRewardItem = {
  id?: number;
  name?: string;
  scoreCost?: number;
  stock?: number;
  status?: string;
  hasImage?: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendRewardOrder = {
  id?: number;
  rewardId?: number;
  rewardName?: string;
  scoreCost?: number;
  status?: string;
  createdAt?: string | null;
  completedAt?: string | null;
  rejectedAt?: string | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeSummary(data?: BackendScoreSummary | null) {
  return {
    totalScore: Number(data?.totalScore || 0),
    redeemedScore: Number(data?.redeemedScore || 0),
    balanceScore: Number(data?.balanceScore || 0),
    clubs: Array.isArray(data?.clubs)
      ? data.clubs.map((item) => ({
          clubId: Number(item?.clubId || 0),
          clubName: String(item?.clubName || ""),
          score: Number(item?.score || 0)
        }))
      : []
  };
}

function normalizeClubRanking(item?: BackendClubRanking | null) {
  return {
    rank: Number(item?.rank || 0),
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    score: Number(item?.score || 0)
  };
}

function normalizeSchoolRanking(item?: BackendSchoolRanking | null) {
  return {
    rank: Number(item?.rank || 0),
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    memberCount: Number(item?.memberCount || 0),
    totalScore: Number(item?.totalScore || 0),
    avgScore: Number(item?.avgScore || 0)
  };
}

function buildRewardImageUrl(id: number, updatedAt?: string | null) {
  if (!id) {
    return undefined;
  }
  const suffix = updatedAt ? `?v=${encodeURIComponent(updatedAt)}` : "";
  return `/api/student/rewards/${id}/image/content${suffix}`;
}

function normalizeRewardItem(item?: BackendRewardItem | null) {
  const id = Number(item?.id || 0);
  const updatedAt = item?.updatedAt ? String(item.updatedAt) : null;
  const hasImage = Boolean(item?.hasImage);

  return {
    id,
    name: String(item?.name || ""),
    scoreCost: Number(item?.scoreCost || 0),
    stock: Number(item?.stock || 0),
    status: item?.status === "INACTIVE" ? "INACTIVE" : "ACTIVE",
    hasImage,
    imageUrl: hasImage ? buildRewardImageUrl(id, updatedAt) : undefined,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt
  };
}

function normalizeRewardOrder(item?: BackendRewardOrder | null) {
  const normalizedStatus = item?.status === "COMPLETED"
    ? "COMPLETED"
    : item?.status === "REJECTED"
      ? "REJECTED"
      : "PENDING";

  return {
    id: Number(item?.id || 0),
    rewardId: Number(item?.rewardId || 0),
    rewardName: String(item?.rewardName || ""),
    scoreCost: Number(item?.scoreCost || 0),
    status: normalizedStatus,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    completedAt: item?.completedAt ? String(item.completedAt) : null,
    rejectedAt: item?.rejectedAt ? String(item.rejectedAt) : null
  };
}

export async function proxyStudentScoreSummary(request: Request, failure: FailureConfig) {
  return proxyCollectionRequest<BackendScoreSummary, ReturnType<typeof normalizeSummary>>(
    request,
    "/api/v1/students/me/score-summary",
    failure,
    (data) => normalizeSummary(data)
  );
}

export async function proxyStudentClubRankings(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/students/me/club-rankings${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubRanking[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubRanking(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyStudentSchoolRankings(request: Request, failure: FailureConfig) {
  return proxyCollectionRequest<BackendSchoolRanking[], ReturnType<typeof normalizeSchoolRanking>[]>(
    request,
    "/api/v1/students/me/school-rankings",
    failure,
    (data) => Array.isArray(data) ? data.map((item) => normalizeSchoolRanking(item)) : []
  );
}

export async function proxyStudentRewards(request: Request, failure: FailureConfig) {
  return proxyCollectionRequest<BackendRewardItem[], ReturnType<typeof normalizeRewardItem>[]>(
    request,
    "/api/v1/students/me/rewards",
    failure,
    (data) => Array.isArray(data) ? data.map((item) => normalizeRewardItem(item)) : []
  );
}

export async function proxyStudentRewardOrders(request: Request, failure: FailureConfig) {
  return proxyCollectionRequest<BackendRewardOrder[], ReturnType<typeof normalizeRewardOrder>[]>(
    request,
    "/api/v1/students/me/reward-orders",
    failure,
    (data) => Array.isArray(data) ? data.map((item) => normalizeRewardOrder(item)) : []
  );
}

export async function proxyStudentRewardOrderCreate(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/students/me/reward-orders", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendRewardOrder>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeRewardOrder(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyStudentRewardImageBinary(request: Request, targetPath: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`${targetPath}${query}`, {
      method: "GET",
      headers: createProxyHeaders(authorization),
      cache: "no-store"
    });
    const contentType = response.headers.get("content-type") || "";

    if (!response.ok || contentType.includes("application/json")) {
      const data = await parseJsonSafely<BackendResponse<null>>(response);
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    const headers = new Headers(NO_STORE_HEADERS);
    headers.set("Content-Type", contentType || "application/octet-stream");
    const contentLength = response.headers.get("content-length");
    if (contentLength) {
      headers.set("Content-Length", contentLength);
    }

    return new NextResponse(response.body, {
      status: response.status,
      headers
    });
  } catch {
    return createUnavailableResponse(failure);
  }
}

async function proxyCollectionRequest<TBackend, TData>(
  request: Request,
  targetPath: string,
  failure: FailureConfig,
  normalize: (data: TBackend | undefined | null) => TData
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<TBackend>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalize(data?.data)
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
