import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  createProxyHeaders,
  fetchWithCoreFallback,
  parseJsonSafely
} from "./backend";
import { normalizeStudentClassName, normalizeStudentGrade } from "../student/fieldConstraints";

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

type BackendStudentListItem = {
  userId?: number;
  username?: string;
  email?: string;
  enabled?: boolean;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  phone?: string;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendStudentDetail = BackendStudentListItem & {
  bio?: string;
  role?: string;
  hasAvatar?: boolean;
  avatarUpdatedAt?: string | null;
};

type BackendManagerListItem = {
  userId?: number;
  username?: string;
  email?: string;
  enabled?: boolean;
  displayName?: string;
  managerNo?: string;
  phone?: string;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendManagerDetail = BackendManagerListItem & {
  bio?: string;
  role?: string;
  hasAvatar?: boolean;
  avatarUpdatedAt?: string | null;
};

type BackendManagerCreateResult = {
  manager?: BackendManagerDetail | null;
  defaultPassword?: string;
};

type BackendPage<T> = {
  items?: T[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendImportFailure = {
  rowNumber?: number;
  email?: string;
  reasonCode?: string;
  reasonMessage?: string;
};

type BackendImportResult = {
  totalRows?: number;
  successCount?: number;
  failureCount?: number;
  failures?: BackendImportFailure[];
};

type BackendResetResult = {
  resetCount?: number;
  defaultPassword?: string;
};

type BackendDeleteResult = {
  deletedUserId?: number;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeStudentListItem(item?: BackendStudentListItem | null) {
  return {
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    email: String(item?.email || ""),
    enabled: Boolean(item?.enabled),
    displayName: String(item?.displayName || ""),
    studentNo: String(item?.studentNo || ""),
    grade: normalizeStudentGrade(item?.grade),
    className: normalizeStudentClassName(item?.className),
    phone: String(item?.phone || ""),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeStudentDetail(item?: BackendStudentDetail | null) {
  return {
    ...normalizeStudentListItem(item),
    bio: String(item?.bio || ""),
    role: String(item?.role || ""),
    hasAvatar: Boolean(item?.hasAvatar),
    avatarUpdatedAt: item?.avatarUpdatedAt ? String(item.avatarUpdatedAt) : null
  };
}

function normalizeManagerListItem(item?: BackendManagerListItem | null) {
  return {
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    email: String(item?.email || ""),
    enabled: Boolean(item?.enabled),
    displayName: String(item?.displayName || ""),
    managerNo: String(item?.managerNo || ""),
    phone: String(item?.phone || ""),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeManagerDetail(item?: BackendManagerDetail | null) {
  return {
    ...normalizeManagerListItem(item),
    bio: String(item?.bio || ""),
    role: String(item?.role || ""),
    hasAvatar: Boolean(item?.hasAvatar),
    avatarUpdatedAt: item?.avatarUpdatedAt ? String(item.avatarUpdatedAt) : null
  };
}

function normalizeImportResult(item?: BackendImportResult | null) {
  return {
    totalRows: Number(item?.totalRows || 0),
    successCount: Number(item?.successCount || 0),
    failureCount: Number(item?.failureCount || 0),
    failures: Array.isArray(item?.failures)
      ? item.failures.map((failure) => ({
          rowNumber: Number(failure?.rowNumber || 0),
          email: String(failure?.email || ""),
          reasonCode: String(failure?.reasonCode || ""),
          reasonMessage: String(failure?.reasonMessage || "")
        }))
      : []
  };
}

function normalizeResetResult(item?: BackendResetResult | null) {
  return {
    resetCount: Number(item?.resetCount || 0),
    defaultPassword: String(item?.defaultPassword || "")
  };
}

function normalizeManagerCreateResult(item?: BackendManagerCreateResult | null) {
  return {
    manager: normalizeManagerDetail(item?.manager),
    defaultPassword: String(item?.defaultPassword || "")
  };
}

export async function proxyAdminStudentCollection(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/students${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPage<BackendStudentListItem>>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(data?.data?.items) ? data.data.items.map((item) => normalizeStudentListItem(item)) : [],
          page: Number(data?.data?.page || 1),
          pageSize: Number(data?.data?.pageSize || 10),
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

export async function proxyAdminStudentDetail(
  request: Request,
  targetPath: string,
  method: "GET" | "PATCH" | "DELETE",
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
    const data = await parseJsonSafely<BackendResponse<BackendStudentDetail | BackendDeleteResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      const deleted = data?.data as BackendDeleteResult | undefined;
      return NextResponse.json(
        {
          success: true,
          data: {
            deletedUserId: Number(deleted?.deletedUserId || 0)
          }
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeStudentDetail(data?.data as BackendStudentDetail | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminStudentImport(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const contentType = request.headers.get("content-type");
  const body = await request.arrayBuffer();

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/students/import", {
      method: "POST",
      headers: createProxyHeaders(authorization, contentType),
      body,
      cache: "no-store",
      duplex: "half"
    } as RequestInit & { duplex: "half" });
    const data = await parseJsonSafely<BackendResponse<BackendImportResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeImportResult(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminStudentBinary(
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
    const contentDisposition = response.headers.get("content-disposition");
    if (contentLength) {
      headers.set("Content-Length", contentLength);
    }
    if (contentDisposition) {
      headers.set("Content-Disposition", contentDisposition);
    }

    return new NextResponse(response.body, {
      status: response.status,
      headers
    });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminStudentResetPasswords(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/students/reset-passwords", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendResetResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeResetResult(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminSingleStudentResetPassword(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendResetResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeResetResult(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminManagerCollection(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/managers${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPage<BackendManagerListItem>>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(data?.data?.items) ? data.data.items.map((item) => normalizeManagerListItem(item)) : [],
          page: Number(data?.data?.page || 1),
          pageSize: Number(data?.data?.pageSize || 10),
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

export async function proxyAdminManagerCreate(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/managers", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendManagerCreateResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeManagerCreateResult(data?.data)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminManagerDetail(
  request: Request,
  targetPath: string,
  method: "GET" | "PATCH" | "DELETE",
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
    const data = await parseJsonSafely<BackendResponse<BackendManagerDetail | BackendDeleteResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      const deleted = data?.data as BackendDeleteResult | undefined;
      return NextResponse.json(
        {
          success: true,
          data: {
            deletedUserId: Number(deleted?.deletedUserId || 0)
          }
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeManagerDetail(data?.data as BackendManagerDetail | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminManagerResetPasswords(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/managers/reset-passwords", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendResetResult>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeResetResult(data?.data)
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
