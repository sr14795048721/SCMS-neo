import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
  parseJsonSafely,
  resolveBackendBaseUrl,
  resolveGatewayAssetUrl
} from "./backend";

type BackendBannerItem = {
  id?: number;
  type?: "image" | "video";
  title?: string;
  desc?: string;
  src?: string;
  poster?: string;
  sortOrder?: number;
  hasMedia?: boolean;
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

export function normalizeBannerItem(item: BackendBannerItem) {
  return {
    id: Number(item.id || 0),
    type: item.type === "video" ? "video" : "image",
    title: String(item.title || ""),
    desc: String(item.desc || ""),
    src: resolveGatewayAssetUrl(item.src),
    poster: resolveGatewayAssetUrl(item.poster),
    sortOrder: Number(item.sortOrder || 0),
    hasMedia: Boolean(item.hasMedia)
  };
}

export async function proxyAdminBannerCollection(
  request: Request,
  targetPath: string,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendBannerItem | BackendBannerItem[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500 }
      );
    }

    if (Array.isArray(data?.data)) {
      return NextResponse.json({
        success: true,
        items: data.data.map(normalizeBannerItem)
      });
    }

    return NextResponse.json({
      success: true,
      item: data?.data ? normalizeBannerItem(data.data as BackendBannerItem) : null
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      { status: 503 }
    );
  }
}

export async function proxyAdminBannerMutation(
  request: Request,
  targetPath: string,
  method: "PATCH" | "DELETE",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PATCH" ? await request.text() : undefined;

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendBannerItem>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500 }
      );
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true });
    }

    return NextResponse.json({
      success: true,
      item: data?.data ? normalizeBannerItem(data.data) : null
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      { status: 503 }
    );
  }
}

export async function proxyAdminBannerUpload(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const contentType = request.headers.get("content-type");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method: "POST",
      headers: createProxyHeaders(authorization, contentType),
      body: request.body,
      cache: "no-store",
      duplex: "half"
    } as RequestInit & { duplex: "half" });
    const data = await parseJsonSafely<BackendResponse<BackendBannerItem>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({
      success: true,
      item: data?.data ? normalizeBannerItem(data.data) : null
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      { status: 503 }
    );
  }
}

export async function proxyAdminBannerReorder(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method: "PUT",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendBannerItem[]>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({
      success: true,
      items: Array.isArray(data?.data) ? data.data.map(normalizeBannerItem) : []
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      { status: 503 }
    );
  }
}

export async function proxyAdminBannerAction<T>(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<T>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || failure.failureCode,
          message: data?.message || failure.failureMessage
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({
      success: true,
      data: data?.data ?? null
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: failure.unavailableCode,
        message: failure.unavailableMessage
      },
      { status: 503 }
    );
  }
}

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

export async function proxyPublicBannerList(targetPath: string, failure: FailureConfig) {
  try {
    const response = await fetch(`${resolveBackendBaseUrl()}${targetPath}`, {
      method: "GET",
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<{ banners?: BackendBannerItem[] }>>(response);

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

    return NextResponse.json(
      {
        success: true,
        banners: Array.isArray(data?.data?.banners)
          ? data.data.banners.map((item) => ({
              type: item.type === "video" ? "video" : "image",
              title: String(item.title || ""),
              desc: String(item.desc || ""),
              src: resolveGatewayAssetUrl(item.src) || "",
              poster: resolveGatewayAssetUrl(item.poster)
            }))
          : []
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
