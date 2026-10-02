import { NextResponse } from "next/server";
import { createForwardHeaders, fetchWithCoreFallback, parseJsonSafely } from "../../../../lib/api/backend";

type BackendResponse<T> = {
  code?: string;
  message?: string;
  data?: T;
};

type BackendAiCommitUsage = {
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
};

type BackendAiCommitResponse = {
  result?: string;
  provider?: string;
  model?: string;
  finishReason?: string;
  usage?: BackendAiCommitUsage | null;
};

const NO_STORE_HEADERS = {
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  Pragma: "no-cache",
  Expires: "0"
} as const;

export async function POST(request: Request) {
  const authorization = request.headers.get("authorization");
  const body = await request.text();

  try {
    const response = await fetchWithCoreFallback("/api/v1/ai/commit", {
      method: "POST",
      headers: createForwardHeaders("application/json", authorization),
      body,
      cache: "no-store"
    });
    const data = await parseJsonSafely<BackendResponse<BackendAiCommitResponse>>(response);

    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "AI_COMMIT_REQUEST_FAILED",
          message: data?.message || "ai commit request failed"
        },
        {
          status: response.status || 500,
          headers: NO_STORE_HEADERS
        }
      );
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          result: String(data?.data?.result || ""),
          provider: String(data?.data?.provider || "siliconflow"),
          model: String(data?.data?.model || ""),
          finishReason: String(data?.data?.finishReason || ""),
          usage: {
            promptTokens: Number(data?.data?.usage?.promptTokens || 0),
            completionTokens: Number(data?.data?.usage?.completionTokens || 0),
            totalTokens: Number(data?.data?.usage?.totalTokens || 0)
          }
        }
      },
      { headers: NO_STORE_HEADERS }
    );
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "AI_COMMIT_BACKEND_UNREACHABLE",
        message: "ai commit backend unavailable"
      },
      {
        status: 503,
        headers: NO_STORE_HEADERS
      }
    );
  }
}
