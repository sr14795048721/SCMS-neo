import { NextResponse } from "next/server";
import { createForwardHeaders, fetchWithCoreFallback, parseJsonSafely } from "../../../lib/api/backend";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

type ClubItem = {
  id?: number;
  name?: string;
  type?: string;
  description?: string;
  memberCount?: number;
  createdAt?: string | null;
};

const HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
};

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization");
  try {
    const response = await fetchWithCoreFallback("/api/v1/clubs", {
      method: "GET",
      headers: createForwardHeaders("application/json", authorization),
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<ClubItem[]>>(response);
    if (!response.ok) {
      return NextResponse.json(
        { success: false, code: data?.code || "CLUBS_FAILED", message: data?.message || "club list request failed" },
        { status: response.status || 500, headers: HEADERS }
      );
    }
    return NextResponse.json(
      {
        success: true,
        data: Array.isArray(data?.data)
          ? data.data.map((item) => ({
              id: Number(item?.id || 0),
              name: String(item?.name || ""),
              type: String(item?.type || ""),
              description: String(item?.description || ""),
              memberCount: Number(item?.memberCount || 0),
              createdAt: item?.createdAt ? String(item.createdAt) : null
            }))
          : []
      },
      { headers: HEADERS }
    );
  } catch {
    return NextResponse.json(
      { success: false, code: "CLUBS_UNAVAILABLE", message: "club list service unavailable" },
      { status: 503, headers: HEADERS }
    );
  }
}
