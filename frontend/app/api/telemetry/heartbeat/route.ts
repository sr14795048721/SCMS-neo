import { NextResponse } from "next/server";
import { createForwardHeaders, parseJsonSafely, resolveBackendBaseUrl } from "../../../../lib/api/backend";

type TelemetryRequestPayload = {
  visitorId?: string;
  sessionId?: string;
  path?: string;
};

type BackendResponse = {
  code?: string;
  message?: string;
};

export async function POST(request: Request) {
  const payload = (await request.json().catch(() => null)) as TelemetryRequestPayload | null;
  const backendBase = resolveBackendBaseUrl();
  const authorization = request.headers.get("authorization");

  try {
    const response = await fetch(`${backendBase}/api/v1/telemetry/heartbeat`, {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body: JSON.stringify({
        visitorId: String(payload?.visitorId || "").trim(),
        sessionId: String(payload?.sessionId || "").trim(),
        path: String(payload?.path || "/").trim() || "/"
      }),
      cache: "no-store"
    });

    const data = await parseJsonSafely<BackendResponse>(response);
    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "TELEMETRY_HEARTBEAT_FAILED",
          message: data?.message || "telemetry heartbeat failed"
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({ success: true });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "TELEMETRY_BACKEND_UNREACHABLE",
        message: "telemetry backend unavailable"
      },
      { status: 503 }
    );
  }
}
