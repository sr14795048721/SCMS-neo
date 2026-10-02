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

type BackendProjectDemoStep = {
  id?: number;
  title?: string;
  description?: string;
  triggerType?: string;
  targetSubsystem?: string;
  actionKey?: string | null;
  sortOrder?: number;
  enabled?: boolean;
};

type BackendProjectDemo = {
  projectId?: number;
  projectKey?: string;
  title?: string;
  subtitle?: string;
  coverUrl?: string | null;
  overviewTitle?: string;
  overviewBody?: string;
  bridgeMode?: string;
  enabled?: boolean;
  steps?: BackendProjectDemoStep[];
  mockSnapshot?: unknown;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeStep(item?: BackendProjectDemoStep | null) {
  return {
    id: item?.id == null ? null : Number(item.id),
    title: String(item?.title || ""),
    description: String(item?.description || ""),
    triggerType: String(item?.triggerType || "MANUAL"),
    targetSubsystem: String(item?.targetSubsystem || "APP"),
    actionKey: item?.actionKey == null ? null : String(item.actionKey),
    sortOrder: Number(item?.sortOrder || 0),
    enabled: item?.enabled !== false
  };
}

function normalizeProjectDemo(item?: BackendProjectDemo | null) {
  return {
    projectId: Number(item?.projectId || 0),
    projectKey: String(item?.projectKey || ""),
    title: String(item?.title || ""),
    subtitle: String(item?.subtitle || ""),
    coverUrl: item?.coverUrl ? String(item.coverUrl) : "",
    overviewTitle: String(item?.overviewTitle || ""),
    overviewBody: String(item?.overviewBody || ""),
    bridgeMode: String(item?.bridgeMode || "APP_HUB"),
    enabled: item?.enabled !== false,
    steps: Array.isArray(item?.steps) ? item.steps.map((step) => normalizeStep(step)) : [],
    mockSnapshot:
      item && typeof item.mockSnapshot === "object" && item.mockSnapshot !== null ? item.mockSnapshot : {}
  };
}

export async function proxyManagerClubAppWorkspaceProjectDemo(
  request: Request,
  clubId: string,
  projectKey: string,
  method: "GET" | "PUT",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PUT" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(
      `/api/v1/managers/me/clubs/${clubId}/app-workspace/projects/${encodeURIComponent(projectKey)}/demo`,
      {
        method,
        headers: createForwardHeaders("application/json", authorization),
        body,
        cache: "no-store"
      }
    );
    const data = await parseJsonSafely<BackendResponse<BackendProjectDemo>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeProjectDemo(data?.data)
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
