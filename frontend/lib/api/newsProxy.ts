import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
  fetchWithCoreFallback,
  parseJsonSafely,
  resolveGatewayAssetUrl
} from "./backend";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
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

type BackendPublicNewsCard = {
  id?: number;
  title?: string;
  cover?: string | null;
  authorName?: string;
  publishedAt?: string | null;
  viewCount?: number;
};

type BackendPublicNewsHome = {
  carousel?: BackendPublicNewsCard[];
  textList?: BackendPublicNewsCard[];
};

type BackendPublicNewsPage = {
  items?: BackendPublicNewsCard[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendPublicNewsDetail = {
  id?: number;
  title?: string;
  cover?: string | null;
  authorName?: string;
  publishedAt?: string | null;
  viewCount?: number;
  markdownContent?: string;
};

type BackendUploadedAsset = {
  assetId?: number;
  url?: string;
  markdown?: string;
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

function normalizeStatus(value?: string | null): "DRAFT" | "PUBLISHED" {
  return value === "PUBLISHED" ? "PUBLISHED" : "DRAFT";
}

function normalizeAdminListItem(item?: BackendAdminNewsListItem | null) {
  return {
    id: Number(item?.id || 0),
    title: String(item?.title || ""),
    status: normalizeStatus(item?.status),
    hasCover: Boolean(item?.hasCover),
    cover: withAssetVersion(resolveGatewayAssetUrl(item?.cover), item?.updatedAt),
    authorName: String(item?.authorName || ""),
    authorRole: String(item?.authorRole || ""),
    viewCount: Number(item?.viewCount || 0),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeAdminDetail(item?: BackendAdminNewsDetail | null) {
  const newsId = Number(item?.id || 0);
  return {
    id: newsId,
    title: String(item?.title || ""),
    status: normalizeStatus(item?.status),
    markdownContent: rewriteMarkdownContent(newsId, String(item?.markdownContent || "")),
    cover: withAssetVersion(resolveGatewayAssetUrl(item?.cover), item?.coverUpdatedAt || item?.updatedAt),
    authorName: String(item?.authorName || ""),
    authorRole: String(item?.authorRole || ""),
    viewCount: Number(item?.viewCount || 0),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    coverUpdatedAt: item?.coverUpdatedAt ? String(item.coverUpdatedAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizePublicCard(item?: BackendPublicNewsCard | null) {
  return {
    id: Number(item?.id || 0),
    title: String(item?.title || ""),
    cover: resolveGatewayAssetUrl(item?.cover),
    authorName: String(item?.authorName || ""),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    viewCount: Number(item?.viewCount || 0)
  };
}

function normalizePublicDetail(item?: BackendPublicNewsDetail | null) {
  return {
    id: Number(item?.id || 0),
    title: String(item?.title || ""),
    cover: resolveGatewayAssetUrl(item?.cover),
    authorName: String(item?.authorName || ""),
    publishedAt: item?.publishedAt ? String(item.publishedAt) : null,
    viewCount: Number(item?.viewCount || 0),
    markdownContent: rewriteMarkdownContent(Number(item?.id || 0), String(item?.markdownContent || ""))
  };
}

function rewriteMarkdownContent(newsId: number, markdown: string) {
  return markdown.replace(
    /(^|[\s(])(?:https?:\/\/[^\s)]+)?\/api(?:\/v1)?\/(?:public\/news|managers\/me\/news|manager\/news|admin\/news)\/(\d+)\/assets\/(\d+)(?:\/content)?(\?[^\s)]*)?/g,
    (_match, prefix, matchedNewsId, assetId) => {
      const absoluteUrl =
        resolveGatewayAssetUrl(`/api/admin/news/${Number(matchedNewsId || newsId)}/assets/${Number(assetId || 0)}/content`) || "";
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

export async function proxyAdminNewsCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/news${query}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAdminNewsPage | BackendAdminNewsDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "POST") {
      return NextResponse.json(
        {
          success: true,
          data: normalizeAdminDetail(data?.data as BackendAdminNewsDetail)
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    const pageData = data?.data as BackendAdminNewsPage | undefined;
    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(pageData?.items) ? pageData.items.map((item) => normalizeAdminListItem(item)) : [],
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

export async function proxyAdminNewsMutation(
  request: Request,
  targetPath: string,
  method: "GET" | "PATCH" | "DELETE",
  failure: FailureConfig,
  options?: {
    deleteReturnsDetail?: boolean;
  }
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

    return NextResponse.json(
      {
        success: true,
        data: normalizeAdminDetail(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminNewsUpload(
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

    return NextResponse.json(
      {
        success: true,
        data: normalizeAdminDetail(data?.data as BackendAdminNewsDetail)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminNewsBinary(
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
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        {
          status: response.status || 500,
          headers: NO_STORE_HEADERS
        }
      );
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

export async function proxyPublicNewsHome(failure: FailureConfig) {
  try {
    const response = await fetchWithCoreFallback("/api/v1/public/news/home", {
      method: "GET",
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPublicNewsHome>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          carousel: Array.isArray(data?.data?.carousel)
            ? data.data.carousel.map((item) => normalizePublicCard(item))
            : [],
          textList: Array.isArray(data?.data?.textList)
            ? data.data.textList.map((item) => normalizePublicCard(item))
            : []
        }
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyPublicNewsList(request: Request, failure: FailureConfig) {
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/public/news${query}`, {
      method: "GET",
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPublicNewsPage>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(data?.data?.items) ? data.data.items.map((item) => normalizePublicCard(item)) : [],
          page: Number(data?.data?.page || 1),
          pageSize: Number(data?.data?.pageSize || 9),
          total: Number(data?.data?.total || 0),
          totalPages: Number(data?.data?.totalPages || 0)
        }
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyPublicNewsDetail(targetPath: string, failure: FailureConfig) {
  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "GET",
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPublicNewsDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizePublicDetail(data?.data)
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
