import { NextResponse } from "next/server";
import { createForwardHeaders, fetchWithCoreFallback, parseJsonSafely } from "./backend";

type FailureConfig = {
  failureCode: string;
  failureMessage: string;
  unavailableCode: string;
  unavailableMessage: string;
};

type BackendChangelog = {
  id?: number;
  version?: string;
  title?: string;
  content?: string;
  releasedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeChangelog(item?: BackendChangelog | null) {
  return {
    id: Number(item?.id || 0),
    version: String(item?.version || ""),
    title: String(item?.title || ""),
    content: String(item?.content || ""),
    releasedAt: item?.releasedAt ? String(item.releasedAt) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

export async function proxyVersionChangelogs(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/version-changelogs", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<{ success?: boolean; code?: string; message?: string; data?: BackendChangelog[] }>(
      response
    );

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeChangelog(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminVersionChangelogCreate(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/version-changelogs", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<{ code?: string; message?: string; data?: BackendChangelog }>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeChangelog(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminVersionChangelogUpdate(
  request: Request,
  changelogId: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(
      `/api/v1/admin/version-changelogs/${encodeURIComponent(changelogId)}`,
      {
        method: "PATCH",
        headers: createForwardHeaders("application/json", authorization),
        body,
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<{ code?: string; message?: string; data?: BackendChangelog }>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeChangelog(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminVersionChangelogDelete(
  request: Request,
  changelogId: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(
      `/api/v1/admin/version-changelogs/${encodeURIComponent(changelogId)}`,
      {
        method: "DELETE",
        headers: createForwardHeaders("application/json", authorization),
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<{ code?: string; message?: string }>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
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
