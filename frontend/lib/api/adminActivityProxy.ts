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

type BackendPage<T> = {
  items?: T[];
  page?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
};

type BackendActivityListItem = {
  id?: number;
  clubId?: number;
  clubName?: string;
  title?: string;
  description?: string;
  location?: string;
  startTime?: string | null;
  endTime?: string | null;
  capacity?: number;
  status?: string;
  registrationCount?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendActivityRegistration = {
  registrationId?: number;
  userId?: number;
  username?: string;
  displayName?: string;
  studentNo?: string;
  grade?: string;
  className?: string;
  registeredAt?: string | null;
};

type BackendActivityDetail = BackendActivityListItem & {
  registrations?: BackendActivityRegistration[];
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

function normalizeActivityListItem(item?: BackendActivityListItem | null) {
  return {
    id: Number(item?.id || 0),
    clubId: Number(item?.clubId || 0),
    clubName: String(item?.clubName || ""),
    title: String(item?.title || ""),
    description: String(item?.description || ""),
    location: String(item?.location || ""),
    startTime: item?.startTime ? String(item.startTime) : null,
    endTime: item?.endTime ? String(item.endTime) : null,
    capacity: Number(item?.capacity || 0),
    status: String(item?.status || "DRAFT"),
    registrationCount: Number(item?.registrationCount || 0),
    createdAt: item?.createdAt ? String(item.createdAt) : null,
    updatedAt: item?.updatedAt ? String(item.updatedAt) : null
  };
}

function normalizeActivityDetail(item?: BackendActivityDetail | null) {
  return {
    ...normalizeActivityListItem(item),
    registrations: Array.isArray(item?.registrations)
      ? item.registrations.map((registration) => ({
          registrationId: Number(registration?.registrationId || 0),
          userId: Number(registration?.userId || 0),
          username: String(registration?.username || ""),
          displayName: String(registration?.displayName || ""),
          studentNo: String(registration?.studentNo || ""),
          grade: String(registration?.grade || ""),
          className: String(registration?.className || ""),
          registeredAt: registration?.registeredAt ? String(registration.registeredAt) : null
        }))
      : []
  };
}

export async function proxyAdminActivityCollection(
  request: Request,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const sourceUrl = new URL(request.url);
  const query = sourceUrl.search || "";
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(`/api/v1/admin/activities${method === "GET" ? query : ""}`, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendPage<BackendActivityListItem> | BackendActivityDetail>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "POST") {
      return NextResponse.json(
        {
          success: true,
          data: normalizeActivityDetail(data?.data as BackendActivityDetail | undefined)
        },
        { headers: NO_STORE_HEADERS }
      );
    }

    const page = data?.data as BackendPage<BackendActivityListItem> | undefined;
    return NextResponse.json(
      {
        success: true,
        data: {
          items: Array.isArray(page?.items) ? page.items.map((item) => normalizeActivityListItem(item)) : [],
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

export async function proxyAdminActivityDetail(
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
    const data = await parseJsonSafely<BackendResponse<BackendActivityDetail | null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    if (method === "DELETE") {
      return NextResponse.json({ success: true }, { headers: NO_STORE_HEADERS });
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeActivityDetail(data?.data as BackendActivityDetail | undefined)
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return createUnavailableResponse(failure);
  }
}

export async function proxyAdminActivityAction(
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
    const data = await parseJsonSafely<BackendResponse<BackendActivityDetail | null>>(response);

    if (!response.ok) {
      return createFailureResponse(response.status, data?.code, data?.message, failure);
    }

    return NextResponse.json(
      {
        success: true,
        data: normalizeActivityDetail(data?.data as BackendActivityDetail | undefined)
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
