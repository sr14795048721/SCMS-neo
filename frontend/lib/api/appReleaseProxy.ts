import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  fetchWithCoreFallback,
  parseJsonSafely
} from "./backend";

type BackendAppRelease = {
  id?: number;
  versionName?: string;
  buildNumber?: number;
  releaseNotes?: string;
  forceUpdate?: boolean;
  androidUrl?: string | null;
  harmonyUrl?: string | null;
  status?: "DRAFT" | "PUBLISHED" | "RETIRED";
  publishedAt?: string | null;
  createdBy?: number;
  updatedBy?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
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

function normalizeAppRelease(item?: BackendAppRelease | null) {
  return {
    id: Number(item?.id || 0),
    versionName: String(item?.versionName || ""),
    buildNumber: Number(item?.buildNumber || 0),
    releaseNotes: String(item?.releaseNotes || ""),
    forceUpdate: Boolean(item?.forceUpdate),
    androidUrl: String(item?.androidUrl || ""),
    harmonyUrl: String(item?.harmonyUrl || ""),
    status: item?.status === "PUBLISHED" || item?.status === "RETIRED" ? item.status : "DRAFT",
    publishedAt: item?.publishedAt || null,
    createdBy: Number(item?.createdBy || 0),
    updatedBy: Number(item?.updatedBy || 0),
    createdAt: item?.createdAt || null,
    updatedAt: item?.updatedAt || null
  };
}

export async function proxyAdminAppReleaseCollection(
  request: Request,
  targetPath: string,
  method: "GET" | "POST",
  failure: FailureConfig
) {
  const authorization = request.headers.get("authorization");
  const body = method === "POST" ? await request.text() : undefined;

  try {
    const response = await fetchWithCoreFallback(targetPath, {
      method,
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAppRelease[] | BackendAppRelease>>(response);

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
        items: data.data.map((item) => normalizeAppRelease(item))
      });
    }

    return NextResponse.json({
      success: true,
      item: normalizeAppRelease(data?.data as BackendAppRelease | undefined)
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

export async function proxyAdminAppReleaseMutation(
  request: Request,
  targetPath: string,
  method: "PATCH" | "POST",
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
    const data = await parseJsonSafely<BackendResponse<BackendAppRelease>>(response);

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
      item: normalizeAppRelease(data?.data)
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
