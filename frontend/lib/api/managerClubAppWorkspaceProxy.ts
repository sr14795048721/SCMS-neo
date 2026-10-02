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

type BackendWorkspaceProject = {
  id?: number;
  projectKey?: string;
  title?: string;
  subtitle?: string;
  summary?: string;
  coverUrl?: string | null;
  sortOrder?: number;
  enabled?: boolean;
  materials?: BackendWorkspaceMaterial[];
};

type BackendWorkspaceMaterial = {
  id?: number;
  sectionKey?: string;
  title?: string;
  storagePath?: string;
  sortOrder?: number;
  enabled?: boolean;
};

type BackendWorkspaceGroup = {
  id?: number;
  title?: string;
  subtitle?: string;
  sortOrder?: number;
  enabled?: boolean;
  projects?: BackendWorkspaceProject[];
};

type BackendWorkspace = {
  clubId?: number;
  clubName?: string;
  groups?: BackendWorkspaceGroup[];
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeProject(item?: BackendWorkspaceProject | null) {
  return {
    id: item?.id == null ? null : Number(item.id),
    projectKey: String(item?.projectKey || ""),
    title: String(item?.title || ""),
    subtitle: String(item?.subtitle || ""),
    summary: String(item?.summary || ""),
    coverUrl: item?.coverUrl ? String(item.coverUrl) : "",
    sortOrder: Number(item?.sortOrder || 0),
    enabled: item?.enabled !== false,
    materials: Array.isArray(item?.materials) ? item.materials.map((material) => normalizeMaterial(material)) : []
  };
}

function normalizeMaterial(item?: BackendWorkspaceMaterial | null) {
  return {
    id: item?.id == null ? null : Number(item.id),
    sectionKey: String(item?.sectionKey || "OTHER"),
    title: String(item?.title || ""),
    storagePath: String(item?.storagePath || ""),
    sortOrder: Number(item?.sortOrder || 0),
    enabled: item?.enabled !== false
  };
}

function normalizeGroup(item?: BackendWorkspaceGroup | null) {
  return {
    id: item?.id == null ? null : Number(item.id),
    title: String(item?.title || ""),
    subtitle: String(item?.subtitle || ""),
    sortOrder: Number(item?.sortOrder || 0),
    enabled: item?.enabled !== false,
    projects: Array.isArray(item?.projects) ? item.projects.map((project) => normalizeProject(project)) : []
  };
}

function normalizeWorkspace(item?: BackendWorkspace | null) {
  return {
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    groups: Array.isArray(item?.groups) ? item.groups.map((group) => normalizeGroup(group)) : []
  };
}

export async function proxyManagerClubAppWorkspace(
  request: Request,
  clubId: string,
  method: "GET" | "PUT",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PUT" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(`/api/v1/managers/me/clubs/${clubId}/app-workspace`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendWorkspace>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeWorkspace(data?.data)
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
