import { NextResponse } from "next/server";
import {
  createForwardHeaders,
  parseJsonSafely,
  resolveBackendBaseUrl,
  resolveCoreServiceBaseUrl
} from "../../../../lib/api/backend";

type BackendStatsPayload = {
  totalUsers?: number;
  uniqueVisitors?: number;
  todayVisitCount?: number;
  onlineSessions?: number;
};

type BackendResponse = {
  code?: string;
  message?: string;
  data?: BackendStatsPayload;
};

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization");
  const targets = [
    `${resolveBackendBaseUrl()}/api/v1/reports/admin-dashboard`,
    `${resolveCoreServiceBaseUrl()}/api/v1/reports/admin-dashboard`
  ];

  let lastFailure: { status: number; code: string; message: string } | null = null;

  for (const target of targets) {
    try {
      const response = await fetch(target, {
        method: "GET",
        headers: createForwardHeaders("application/json", authorization),
        cache: "no-store"
      });

      const data = await parseJsonSafely<BackendResponse>(response);
      if (response.ok && data?.data) {
        return NextResponse.json({
          success: true,
          data: {
            totalUsers: Number(data.data.totalUsers || 0),
            uniqueVisitors: Number(data.data.uniqueVisitors || 0),
            todayVisitCount: Number(data.data.todayVisitCount || 0),
            onlineSessions: Number(data.data.onlineSessions || 0)
          }
        });
      }

      const failure = {
        status: response.status || 500,
        code: data?.code || "ADMIN_STATS_FAILED",
        message: data?.message || "admin stats unavailable"
      };

      if (response.status === 401 || response.status === 403 || response.status < 500) {
        return NextResponse.json(
          {
            success: false,
            code: failure.code,
            message: failure.message
          },
          { status: failure.status }
        );
      }

      lastFailure = failure;
    } catch {
      lastFailure = {
        status: 503,
        code: "ADMIN_STATS_BACKEND_UNREACHABLE",
        message: "admin stats backend unavailable"
      };
    }
  }

  return NextResponse.json(
    {
      success: false,
      code: lastFailure?.code || "ADMIN_STATS_BACKEND_UNREACHABLE",
      message: lastFailure?.message || "admin stats backend unavailable"
    },
    { status: lastFailure?.status || 503 }
  );
}
