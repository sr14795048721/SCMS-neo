import { NextResponse } from "next/server";
import { createForwardHeaders, fetchWithCoreFallback, parseJsonSafely } from "./backend";

type FailureConfig = {
  failureCode: string;
  failureMessage: string;
  unavailableCode: string;
  unavailableMessage: string;
};

type BackendItem = Record<string, unknown> | null;

type ApiEnvelope = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: unknown;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeSource(item?: BackendItem) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    url: item?.url ? String(item.url) : null,
    sourceType: String(item?.sourceType || "OFFICIAL_WEBSITE"),
    wechatName: item?.wechatName ? String(item.wechatName) : null,
    enabled: Boolean(item?.enabled),
    scanEnabled: Boolean(item?.scanEnabled),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeLead(item?: BackendItem) {
  return {
    id: Number(item?.id || 0),
    sourceId: item?.sourceId != null ? Number(item.sourceId) : null,
    sourceName: item?.sourceName ? String(item.sourceName) : null,
    title: String(item?.title || ""),
    url: item?.url ? String(item.url) : null,
    snippet: item?.snippet ? String(item.snippet) : null,
    detectedAt: item?.detectedAt ? String(item.detectedAt) : null,
    status: String(item?.status || "PENDING"),
    createdAt: item?.createdAt ? String(item.createdAt) : null
  };
}

function normalizeCompetition(item?: BackendItem) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    category: item?.category ? String(item.category) : null,
    participantScope: item?.participantScope ? String(item.participantScope) : null,
    applyDeadline: item?.applyDeadline ? String(item.applyDeadline) : null,
    startDate: item?.startDate ? String(item.startDate) : null,
    endDate: item?.endDate ? String(item.endDate) : null,
    url: item?.url ? String(item.url) : null,
    sourceId: item?.sourceId != null ? Number(item.sourceId) : null,
    sourceName: item?.sourceName ? String(item.sourceName) : null,
    status: String(item?.status || "UPCOMING"),
    note: item?.note ? String(item.note) : null,
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

type Normalizer = (item?: BackendItem) => Record<string, unknown>;

async function proxyJson(
  request: Request,
  path: string,
  method: string,
  failure: FailureConfig,
  normalizer: Normalizer | null,
  hasBody: boolean
) {
  const authorization = request.headers.get("authorization");
  const body = hasBody ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(path, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      ...(hasBody ? { body } : {}),
      cache: "no-store"
    });
    const data = await parseJsonSafely<ApiEnvelope>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    const payload: Record<string, unknown> = { success: true };
    if (data?.data !== undefined) {
      if (Array.isArray(data.data) && normalizer) {
        payload.data = data.data.map((item) => normalizer(item as BackendItem));
      } else if (normalizer) {
        payload.data = normalizer(data.data as BackendItem);
      } else {
        payload.data = data.data;
      }
    }
    return NextResponse.json(payload, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminCompetitionSources(request: Request, failure: FailureConfig) {
  return proxyJson(request, "/api/v1/admin/competition-sources", "GET", failure, normalizeSource, false);
}

export async function proxyAdminCompetitionSourceCreate(request: Request, failure: FailureConfig) {
  return proxyJson(request, "/api/v1/admin/competition-sources", "POST", failure, normalizeSource, true);
}

export async function proxyAdminCompetitionSourceUpdate(
  request: Request,
  sourceId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competition-sources/${encodeURIComponent(sourceId)}`,
    "PATCH",
    failure,
    normalizeSource,
    true
  );
}

export async function proxyAdminCompetitionSourceDelete(
  request: Request,
  sourceId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competition-sources/${encodeURIComponent(sourceId)}`,
    "DELETE",
    failure,
    null,
    false
  );
}

export async function proxyAdminCompetitionSourceScan(
  request: Request,
  sourceId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competition-sources/${encodeURIComponent(sourceId)}/scan`,
    "POST",
    failure,
    null,
    false
  );
}

export async function proxyAdminCompetitionLeads(request: Request, failure: FailureConfig) {
  const url = new URL(request.url);
  const status = url.searchParams.get("status");
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return proxyJson(request, `/api/v1/admin/competition-leads${query}`, "GET", failure, normalizeLead, false);
}

export async function proxyAdminCompetitionLeadConfirm(
  request: Request,
  leadId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competition-leads/${encodeURIComponent(leadId)}/confirm`,
    "POST",
    failure,
    normalizeCompetition,
    true
  );
}

export async function proxyAdminCompetitionLeadIgnore(
  request: Request,
  leadId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competition-leads/${encodeURIComponent(leadId)}/ignore`,
    "POST",
    failure,
    null,
    false
  );
}

export async function proxyAdminCompetitions(request: Request, failure: FailureConfig) {
  return proxyJson(request, "/api/v1/admin/competitions", "GET", failure, normalizeCompetition, false);
}

export async function proxyAdminCompetitionCreate(request: Request, failure: FailureConfig) {
  return proxyJson(request, "/api/v1/admin/competitions", "POST", failure, normalizeCompetition, true);
}

export async function proxyAdminCompetitionUpdate(
  request: Request,
  competitionId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competitions/${encodeURIComponent(competitionId)}`,
    "PATCH",
    failure,
    normalizeCompetition,
    true
  );
}

export async function proxyAdminCompetitionDelete(
  request: Request,
  competitionId: string,
  failure: FailureConfig
) {
  return proxyJson(
    request,
    `/api/v1/admin/competitions/${encodeURIComponent(competitionId)}`,
    "DELETE",
    failure,
    null,
    false
  );
}

export async function proxyAdminCompetitionImportArticle(request: Request, failure: FailureConfig) {
  return proxyJson(
    request,
    "/api/v1/admin/competitions/import-article",
    "POST",
    failure,
    null,
    true
  );
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
