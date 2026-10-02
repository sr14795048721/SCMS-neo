import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
  fetchWithCoreFallback,
  parseJsonSafely,
  resolveGatewayAssetUrl
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

type BackendManagerRecommendationItem = {
  orderNo?: number;
  clubId?: number;
  clubName?: string;
  clubType?: string;
  description?: string;
  memberCount?: number;
  remark?: string;
};

type BackendManagerRecommendationGroup = {
  managerUserId?: number;
  managerName?: string;
  managerNo?: string;
  recommendations?: BackendManagerRecommendationItem[];
};

type BackendNotificationItem = {
  id?: number;
  category?: string;
  title?: string;
  content?: string;
  status?: string;
  createdAt?: string | null;
  targetPath?: string | null;
};

type BackendNotificationSummary = {
  unreadCount?: number;
  latestUnread?: BackendNotificationItem[];
};

type BackendNotificationFeed = {
  items?: BackendNotificationItem[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendClubCreationRequest = {
  id?: number;
  applicantManagerUserId?: number;
  applicantName?: string;
  applicantManagerNo?: string;
  name?: string;
  type?: string;
  description?: string;
  applyReason?: string;
  status?: string;
  reviewedBy?: number | null;
  reviewedByName?: string;
  reviewedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendAdminNewsListItem = {
  id?: number;
  title?: string;
  status?: string;
  hasCover?: boolean;
  cover?: string | null;
  authorName?: string;
  authorRole?: string;
  viewCount?: number;
  publishedAt?: string | null;
  updatedAt?: string | null;
};

type BackendAdminNewsDetail = {
  id?: number;
  title?: string;
  status?: string;
  markdownContent?: string;
  cover?: string | null;
  authorName?: string;
  authorRole?: string;
  viewCount?: number;
  publishedAt?: string | null;
  coverUpdatedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendAdminNewsPage = {
  items?: BackendAdminNewsListItem[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendUploadedAsset = {
  assetId?: number;
  url?: string;
  markdown?: string;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeNewsStatus(value?: string | null): "DRAFT" | "PUBLISHED" {
  return value === "PUBLISHED" ? "PUBLISHED" : "DRAFT";
}

function rewriteMarkdownContent(newsId: number, markdown: string) {
  return markdown.replace(
    /(^|[\s(])(?:https?:\/\/[^\s)]+)?\/api(?:\/v1)?\/(?:public\/news|managers\/me\/news|manager\/news|admin\/news)\/(\d+)\/assets\/(\d+)(?:\/content)?(\?[^\s)]*)?/g,
    (_match, prefix, matchedNewsId, assetId) => {
      const absoluteUrl =
        resolveGatewayAssetUrl(`/api/manager/news/${Number(matchedNewsId || newsId)}/assets/${Number(assetId || 0)}/content`) || "";
      return `${prefix}${absoluteUrl}`;
    }
  );
}

function withAssetVersion(url?: string, version?: string | null) {
  if (!url || !version) {
    return url;
  }
  const parsed = /^https?:\/\//i.test(url) ? new URL(url) : new URL(url, "http://localhost");
  parsed.searchParams.set("v", version);
  return /^https?:\/\//i.test(url) ? parsed.toString() : `${parsed.pathname}${parsed.search}${parsed.hash}`;
}

function normalizeNewsListItem(item?: BackendAdminNewsListItem | null) {
  return {
    id: Number(item?.id || 0),
    title: String(item?.title || ""),
    status: normalizeNewsStatus(item?.status),
    hasCover: Boolean(item?.hasCover),
    cover: withAssetVersion(
      item?.cover ? resolveGatewayAssetUrl(`/api/manager/news/${Number(item.id || 0)}/cover/content`) : undefined,
      item?.updatedAt
    ),
    authorName: String(item?.authorName || ""),
    authorRole: String(item?.authorRole || ""),
    viewCount: Number(item?.viewCount || 0),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeNewsDetail(item?: BackendAdminNewsDetail | null) {
  const newsId = Number(item?.id || 0);
  return {
    id: newsId,
    title: String(item?.title || ""),
    status: normalizeNewsStatus(item?.status),
    markdownContent: rewriteMarkdownContent(newsId, String(item?.markdownContent || "")),
    cover: withAssetVersion(
      newsId > 0 && item?.cover ? resolveGatewayAssetUrl(`/api/manager/news/${newsId}/cover/content`) : undefined,
      item?.coverUpdatedAt || item?.updatedAt
    ),
    authorName: String(item?.authorName || ""),
    authorRole: String(item?.authorRole || ""),
    viewCount: Number(item?.viewCount || 0),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    coverUpdatedAt: item?.coverUpdatedAt ? String(item.coverUpdatedAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeRecommendationItem(item?: BackendManagerRecommendationItem | null) {
  return {
    orderNo: Number(item?.orderNo || 0),
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    clubType: String(item?.clubType || ""),
    description: String(item?.description || ""),
    memberCount: Number(item?.memberCount || 0),
    remark: String(item?.remark || "")
  };
}

function normalizeRecommendationGroup(item?: BackendManagerRecommendationGroup | null) {
  return {
    managerUserId: Number(item?.managerUserId || 0),
    managerName: String(item?.managerName || ""),
    managerNo: String(item?.managerNo || ""),
    recommendations: Array.isArray(item?.recommendations)
      ? item.recommendations.map((entry) => normalizeRecommendationItem(entry))
      : []
  };
}

function normalizeNotification(item?: BackendNotificationItem | null) {
  return {
    id: Number(item?.id || 0),
    category: String(item?.category || ""),
    title: String(item?.title || ""),
    content: String(item?.content || ""),
    status: item?.status === "READ" ? "READ" : "UNREAD",
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    targetPath: item?.targetPath ? String(item.targetPath) : null
  };
}

function normalizeNotificationSummary(item?: BackendNotificationSummary | null) {
  return {
    unreadCount: Number(item?.unreadCount || 0),
    latestUnread: Array.isArray(item?.latestUnread) ? item.latestUnread.map((entry) => normalizeNotification(entry)) : []
  };
}

function normalizeNotificationFeed(item?: BackendNotificationFeed | null) {
  return {
    items: Array.isArray(item?.items) ? item.items.map((entry) => normalizeNotification(entry)) : [],
    page: Number(item?.page || 0),
    pageSize: Number(item?.pageSize || 20),
    total: Number(item?.total || 0),
    totalPages: Number(item?.totalPages || 0)
  };
}

function normalizeClubCreationRequest(item?: BackendClubCreationRequest | null) {
  return {
    id: Number(item?.id || 0),
    applicantManagerUserId: Number(item?.applicantManagerUserId || 0),
    applicantName: String(item?.applicantName || ""),
    applicantManagerNo: String(item?.applicantManagerNo || ""),
    name: String(item?.name || ""),
    type: String(item?.type || ""),
    description: String(item?.description || ""),
    applyReason: String(item?.applyReason || ""),
    status: item?.status === "APPROVED" ? "APPROVED" : item?.status === "REJECTED" ? "REJECTED" : "PENDING",
    reviewedBy: item?.reviewedBy == null ? null : Number(item.reviewedBy),
    reviewedByName: String(item?.reviewedByName || ""),
    reviewedAt: item?.reviewedAt ? String(item.reviewedAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

export async function proxyManagerNewsCollection(request: Request, method: "GET" | "POST", failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/news${query}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAdminNewsPage | BackendAdminNewsDetail>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    if (method === "POST") {
      return NextResponse.json({ success: true, data: normalizeNewsDetail(data?.data as BackendAdminNewsDetail) }, { headers: NO_STORE_HEADERS });
    }
    const pageData = data?.data as BackendAdminNewsPage | undefined;
    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(pageData?.items) ? pageData.items.map((item) => normalizeNewsListItem(item)) : [],
          page: Number(pageData?.page || 1),
          pageSize: Number(pageData?.pageSize || 10),
          total: Number(pageData?.total || 0),
          totalPages: Number(pageData?.totalPages || 0)
        }
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerNewsMutation(
  request: Request,
  targetPath: string,
  method: "GET" | "PATCH" | "DELETE",
  failure: FailureConfig,
  options?: { deleteReturnsDetail?: boolean }
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
    const data = await parseJsonSafely<BackendResponse<BackendAdminNewsDetail>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    if (method === "DELETE" && !options?.deleteReturnsDetail) {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }
    return NextResponse.json({ success: true, data: normalizeNewsDetail(data?.data) }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerNewsUpload(
  request: Request,
  targetPath: string,
  failure: FailureConfig,
  responseType: "detail" | "asset"
) {
  const authorization = request.headers.get("authorization");
  const contentType = request.headers.get("content-type");
  const body = await request.arrayBuffer();

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "POST",
      headers: createProxyHeaders(authorization, contentType),
      body,
      cache: "no-store",
      duplex: "half"
    } as RequestInit & { duplex: "half" });
    const data = await parseJsonSafely<BackendResponse<BackendAdminNewsDetail | BackendUploadedAsset>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    if (responseType === "asset") {
      const asset = data?.data as BackendUploadedAsset | undefined;
      const rawUrl = String(asset?.url || "");
      const absoluteUrl = resolveGatewayAssetUrl(rawUrl) || rawUrl;
      return NextResponse.json(
        {
          success: true,
          data: {
            assetId: Number(asset?.assetId || 0),
            url: absoluteUrl,
            markdown: String(asset?.markdown || "").replace(rawUrl, absoluteUrl)
          }
        },
        { headers: NO_STORE_HEADERS }
      );
    }
    return NextResponse.json({ success: true, data: normalizeNewsDetail(data?.data as BackendAdminNewsDetail) }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerNewsBinary(request: Request, targetPath: string, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
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
    return new NextResponse(response.body, { status: 200, headers });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyManagerRecommendations(request: Request, method: "GET" | "PUT", failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = method === "PUT" ? await request.text() : undefined;
  try {
    const response = await fetchWithCoreFallback("/api/v1/managers/me/club-recommendations", {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendManagerRecommendationItem[]>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    return NextResponse.json(
      { success: true, data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRecommendationItem(item)) : [] },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyTeacherRecommendationSubmissions(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/club-recommendations/teacher-submissions", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendManagerRecommendationGroup[]>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    return NextResponse.json(
      { success: true, data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRecommendationGroup(item)) : [] },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyClubCreationRequests(request: Request, targetPath: string, method: "GET" | "POST", failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" && targetPath.endsWith("/club-creation-requests") ? await request.text() : undefined;
  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubCreationRequest[] | BackendClubCreationRequest>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    if (method === "GET") {
      return NextResponse.json(
        { success: true, data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubCreationRequest(item)) : [] },
        { headers: NO_STORE_HEADERS }
      );
    }
    return NextResponse.json(
      { success: true, data: normalizeClubCreationRequest(data?.data as BackendClubCreationRequest) },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyNotifications(request: Request, targetPath: string, method: "GET" | "POST", failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendNotificationItem[] | BackendNotificationItem>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    if (method === "GET") {
      return NextResponse.json(
        { success: true, data: Array.isArray(data?.data) ? data.data.map((item) => normalizeNotification(item)) : [] },
        { headers: NO_STORE_HEADERS }
      );
    }
    return NextResponse.json(
      { success: true, data: normalizeNotification(data?.data as BackendNotificationItem) },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyNotificationSummary(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  try {
    const response = await fetchWithCoreFallback("/api/v1/notifications/me/summary", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendNotificationSummary>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    return NextResponse.json(
      { success: true, data: normalizeNotificationSummary(data?.data) },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyNotificationFeed(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";
  try {
    const response = await fetchWithCoreFallback(`/api/v1/notifications/me/feed${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendNotificationFeed>>(response);
    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }
    return NextResponse.json(
      { success: true, data: normalizeNotificationFeed(data?.data) },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

function createFailureResponse(status: number, code: string | undefined, message: string | undefined, failure: FailureConfig) {
  return NextResponse.json(
    { success: false, code: code || failure.failureCode, message: message || failure.failureMessage },
    { status: status || 500, headers: NO_STORE_HEADERS }
  );
}

function createUnavailableResponse(failure: FailureConfig) {
  return NextResponse.json(
    { success: false, code: failure.unavailableCode, message: failure.unavailableMessage },
    { status: 503, headers: NO_STORE_HEADERS }
  );
}
