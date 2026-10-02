import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
  fetchWithCoreFallback,
  parseJsonSafely,
  resolveBackendBaseUrl
} from "./backend";
import { ManagerInfo } from "../manager/types";

type BackendManagerInfo = {
  userId?: number;
  username?: string;
  role?: string;
  email?: string;
  displayName?: string;
  managerNo?: string;
  phone?: string;
  bio?: string;
  hasAvatar?: boolean;
  avatarUpdatedAt?: string | null;
};

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
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

function normalizeManagerInfo(item?: BackendManagerInfo | null): ManagerInfo {
  return {
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    role: String(item?.role || ""),
    email: String(item?.email || ""),
    displayName: String(item?.displayName || ""),
    managerNo: String(item?.managerNo || ""),
    phone: String(item?.phone || ""),
    bio: String(item?.bio || ""),
    hasAvatar: Boolean(item?.hasAvatar),
    avatarUpdatedAt: item?.avatarUpdatedAt ? String(item.avatarUpdatedAt) : null
  };
}

export async function proxyManagerInfo(
  request: Request,
  method: "GET" | "PATCH",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PATCH" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/info`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendManagerInfo>>(response);

    if (!response.ok || !data?.data) {
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

    return NextResponse.json(
      {
        success: true,
        data: normalizeManagerInfo(data.data)
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
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}

export async function proxyManagerPassword(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/password`, {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<null>>(response);

    if (!response.ok) {
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

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
  } catch {
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
}

export async function proxyManagerAvatarUpload(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const contentType = request.headers.get("content-type");
  const body = await request.arrayBuffer();

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/avatar`, {
      method: "POST",
      headers: createProxyHeaders(authorization, contentType),
      body,
      cache: "no-store",
      duplex: "half"
    } as RequestInit & { duplex: "half" });
    const data = await parseJsonSafely<BackendResponse<BackendManagerInfo>>(response);

    if (!response.ok || !data?.data) {
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

    return NextResponse.json(
      {
        success: true,
        data: normalizeManagerInfo(data.data)
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
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}

export async function proxyManagerAvatarGet(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const targetUrl = new URL(`${resolveBackendBaseUrl()}/api/v1/managers/me/avatar`);

  for (const [key, value] of sourceUrl.searchParams.entries()) {
    targetUrl.searchParams.set(key, value);
  }

  try {
    const response = await fetchWithCoreFallback(`${targetUrl.pathname}${targetUrl.search}`, {
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
}
