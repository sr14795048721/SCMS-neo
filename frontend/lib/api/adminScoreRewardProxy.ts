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

type BackendScoreRule = {
  id?: number;
  name?: string;
  scoreDelta?: number;
  scopeType?: string;
  clubId?: number | null;
  clubName?: string;
  status?: string;
  createdBy?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendRewardItem = {
  id?: number;
  name?: string;
  scoreCost?: number;
  stock?: number;
  status?: string;
  visibilityScope?: string;
  clubIds?: number[];
  clubNames?: string[];
  hasImage?: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendRewardOrder = {
  id?: number;
  rewardId?: number;
  rewardName?: string;
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  scoreCost?: number;
  status?: string;
  createdAt?: string | null;
  completedAt?: string | null;
  completedBy?: number | null;
  completedByName?: string;
  rejectedAt?: string | null;
  rejectedBy?: number | null;
  rejectedByName?: string;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeScoreRule(item?: BackendScoreRule | null) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    scoreDelta: Number(item?.scoreDelta || 0),
    scopeType: item?.scopeType === "CLUB" ? "CLUB" : "GLOBAL",
    clubId: item?.clubId == null ? null : Number(item.clubId),
    clubName: String(item?.clubName || ""),
    status: item?.status === "INACTIVE" ? "INACTIVE" : "ACTIVE",
    createdBy: item?.createdBy == null ? null : Number(item.createdBy),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function buildRewardImageUrl(id: number, updatedAt?: string | null) {
  if (!id) {
    return undefined;
  }
  const suffix = updatedAt ? `?v=${encodeURIComponent(updatedAt)}` : "";
  return `/api/admin/rewards/${id}/image/content${suffix}`;
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
    visibilityScope:
      item?.visibilityScope === "CLUB"
        ? "CLUB"
        : item?.visibilityScope === "UNASSIGNED"
          ? "UNASSIGNED"
          : "GLOBAL",
    clubIds: Array.isArray(item?.clubIds) ? item.clubIds.map((clubId) => Number(clubId || 0)).filter((clubId) => clubId > 0) : [],
    clubNames: Array.isArray(item?.clubNames) ? item.clubNames.map((name) => String(name || "")) : [],
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
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    scoreCost: Number(item?.scoreCost || 0),
    status: normalizedStatus,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    completedAt: item?.completedAt ? String(item.completedAt) : null,
    completedBy: item?.completedBy == null ? null : Number(item.completedBy),
    completedByName: String(item?.completedByName || ""),
    rejectedAt: item?.rejectedAt ? String(item.rejectedAt) : null,
    rejectedBy: item?.rejectedBy == null ? null : Number(item.rejectedBy),
    rejectedByName: String(item?.rejectedByName || "")
  };
}

export async function proxyAdminScoreRuleCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/score-rules", {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendScoreRule[] | BackendScoreRule>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "POST") {
      return NextResponse.json(
        {
          success: true,
          data: normalizeScoreRule(data?.data as BackendScoreRule | undefined)
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeScoreRule(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminScoreRuleDetail(
  request: Request,
  targetPath: string,
  method: "PATCH" | "DELETE",
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
    const data = await parseJsonSafely<BackendResponse<BackendScoreRule>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeScoreRule(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminRewardCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/rewards", {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendRewardItem[] | BackendRewardItem>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "POST") {
      return NextResponse.json(
        {
          success: true,
          data: normalizeRewardItem(data?.data as BackendRewardItem | undefined)
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRewardItem(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminRewardDetail(
  request: Request,
  targetPath: string,
  method: "PATCH" | "DELETE",
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
    const data = await parseJsonSafely<BackendResponse<BackendRewardItem>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeRewardItem(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminRewardImageUpload(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const contentType = request.headers.get("content-type");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "POST",
      headers: createProxyHeaders(authorization, contentType),
      body: request.body,
      cache: "no-store",
      duplex: "half"
    } as RequestInit & { duplex: "half" });
    const data = await parseJsonSafely<BackendResponse<BackendRewardItem>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeRewardItem(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminRewardImageBinary(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
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

export async function proxyAdminRewardOrders(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/reward-orders", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendRewardOrder[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRewardOrder(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminRewardOrderMutation(
  request: Request,
  targetPath: string,
  method: "POST" | "DELETE",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendRewardOrder>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
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
