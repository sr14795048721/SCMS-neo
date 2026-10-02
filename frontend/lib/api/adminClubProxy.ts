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

type BackendClubListItem = {
  id?: number;
  name?: string;
  type?: string;
  status?: string;
  description?: string;
  memberCount?: number;
  managerCount?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendClubManager = {
  userId?: number;
  username?: string;
  displayName?: string;
  managerNo?: string;
  phone?: string;
};

type BackendClubStudent = {
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  dutyId?: number | null;
  dutyName?: string;
  dutyPermissions?: string[];
};

type BackendClubDuty = {
  id?: number;
  name?: string;
  permissions?: string[];
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendClubDetail = BackendClubListItem & {
  managers?: BackendClubManager[];
  students?: BackendClubStudent[];
};

type BackendPage<T> = {
  items?: T[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendManagerOption = {
  userId?: number;
  username?: string;
  displayName?: string;
  managerNo?: string;
};

type BackendClubOption = {
  id?: number;
  name?: string;
  type?: string;
  description?: string;
  memberCount?: number;
};

type BackendRecommendedClub = {
  slotNo?: number;
  clubId?: number;
  name?: string;
  type?: string;
  description?: string;
  memberCount?: number;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeClubListItem(item?: BackendClubListItem | null) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    type: String(item?.type || ""),
    status: String(item?.status || "ACTIVE"),
    description: String(item?.description || ""),
    memberCount: Number(item?.memberCount || 0),
    managerCount: Number(item?.managerCount || 0),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeClubDetail(item?: BackendClubDetail | null) {
  return {
    ...normalizeClubListItem(item),
    managers: Array.isArray(item?.managers)
      ? item.managers.map((manager) => ({
          userId: Number(manager?.userId || 0),
          username: String(manager?.username || ""),
          displayName: String(manager?.displayName || ""),
          managerNo: String(manager?.managerNo || ""),
          phone: String(manager?.phone || "")
        }))
      : [],
    students: Array.isArray(item?.students)
      ? item.students.map((student) => ({
          userId: Number(student?.userId || 0),
          username: String(student?.username || ""),
          displayName: String(student?.displayName || ""),
          studentNo: String(student?.studentNo || ""),
          grade: String(student?.grade || ""),
          className: String(student?.className || ""),
          dutyId: student?.dutyId == null ? null : Number(student.dutyId),
          dutyName: String(student?.dutyName || ""),
          dutyPermissions: Array.isArray(student?.dutyPermissions) ? student.dutyPermissions.map((permission) => String(permission || "")) : []
        }))
      : []
  };
}

function normalizeClubDuty(item?: BackendClubDuty | null) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    permissions: Array.isArray(item?.permissions) ? item.permissions.map((permission) => String(permission || "")) : [],
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeManagerOption(item?: BackendManagerOption | null) {
  return {
    userId: Number(item?.userId || 0),
    username: String(item?.username || ""),
    displayName: String(item?.displayName || ""),
    managerNo: String(item?.managerNo || "")
  };
}

function normalizeClubOption(item?: BackendClubOption | null) {
  return {
    id: Number(item?.id || 0),
    name: String(item?.name || ""),
    type: String(item?.type || ""),
    description: String(item?.description || ""),
    memberCount: Number(item?.memberCount || 0)
  };
}

function normalizeRecommendedClub(item?: BackendRecommendedClub | null) {
  return {
    slotNo: Number(item?.slotNo || 0),
    clubId: Number(item?.clubId || 0),
    name: String(item?.name || ""),
    type: String(item?.type || ""),
    description: String(item?.description || ""),
    memberCount: Number(item?.memberCount || 0)
  };
}

export async function proxyAdminClubCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/clubs${method === "GET" ? query : ""}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPage<BackendClubListItem> | BackendClubDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "POST") {
      return NextResponse.json(
        {
          success: true,
          data: normalizeClubDetail(data?.data as BackendClubDetail | undefined)
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    const page = data?.data as BackendPage<BackendClubListItem> | undefined;
    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(page?.items) ? page.items.map((item) => normalizeClubListItem(item)) : [],
          page: Number(page?.page || 1),
          pageSize: Number(page?.pageSize || 10),
          total: Number(page?.total || 0),
          totalPages: Number(page?.totalPages || 0)
        }
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubDetail(
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
    const data = await parseJsonSafely<BackendResponse<BackendClubDetail | null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubDetail(data?.data as BackendClubDetail | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubDutyMutation(
  request: Request,
  targetPath: string,
  method: "GET" | "POST" | "PATCH" | "DELETE",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" || method === "PATCH" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubDuty[] | BackendClubDuty | null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "GET") {
      return NextResponse.json(
        {
          success: true,
          data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubDuty(item)) : []
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeClubDuty(data?.data as BackendClubDuty | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubMemberDuty(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "PATCH",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubMemberRemove(
  request: Request,
  targetPath: string,
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method: "DELETE",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminManagerOptions(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/managers/options${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendManagerOption[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeManagerOption(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubOptions(request: Request, failure: FailureConfig) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/clubs/options${query}`, {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubOption[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeClubOption(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminClubRecommendations(
  request: Request,
  method: "GET" | "PUT",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "PUT" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback("/api/v1/admin/club-recommendations", {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendRecommendedClub[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data) ? data.data.map((item) => normalizeRecommendedClub(item)) : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyPublicRecommendedClubs(failure: FailureConfig) {
  try {
    const response = await fetchWithCoreFallback("/api/v1/home/recommended-clubs", {
      method: "GET",
      headers: createForwardHeaders("application/json"),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubOption[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        clubs: Array.isArray(data?.data)
          ? data.data.map((item) => ({
              id: String(item?.id || ""),
              name: String(item?.name || ""),
              type: String(item?.type || ""),
              description: String(item?.description || ""),
              memberCount: Number(item?.memberCount || 0)
            }))
          : []
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyPublicClubList(failure: FailureConfig) {
  try {
    const response = await fetchWithCoreFallback("/api/v1/clubs", {
      method: "GET",
      headers: createForwardHeaders("application/json"),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendClubListItem[]>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        clubs: Array.isArray(data?.data)
          ? data.data.map((item) => ({
              id: String(item?.id || ""),
              name: String(item?.name || ""),
              type: String(item?.type || ""),
              description: String(item?.description || ""),
              memberCount: Number(item?.memberCount || 0)
            }))
          : []
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
