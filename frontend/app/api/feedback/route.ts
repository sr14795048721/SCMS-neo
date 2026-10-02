import { NextResponse } from "next/server";
import { loadApiMessages } from "../../../lib/i18n/apiMessages";
import { FeedbackMailerConfigError, sendFeedbackEmail } from "../../../lib/server/feedbackMailer";

type FeedbackPayload = {
  name?: string;
  email?: string;
  content?: string;
};

export const runtime = "nodejs";

export async function POST(request: Request) {
  const body = (await request.json()) as FeedbackPayload;
  const messages = await loadApiMessages(request.headers.get("accept-language"));

  if (!body?.content || !body.content.trim()) {
    return NextResponse.json(
      {
        success: false,
        code: "FEEDBACK_CONTENT_REQUIRED",
        message: messages.feedback.contentRequired
      },
      { status: 400 }
    );
  }

  const receivedAt = new Date().toISOString();

  try {
    await sendFeedbackEmail({
      name: String(body.name || "").trim() || undefined,
      email: String(body.email || "").trim() || undefined,
      content: body.content.trim(),
      submittedAt: receivedAt,
      sourceUrl: request.headers.get("origin") || request.headers.get("referer") || undefined,
      userAgent: request.headers.get("user-agent") || undefined,
      forwardedFor: request.headers.get("x-forwarded-for") || undefined
    }, messages.feedback.mail);

    return NextResponse.json({
      success: true,
      receivedAt
    });
  } catch (error) {
    const configError = error instanceof FeedbackMailerConfigError;

    return NextResponse.json(
      {
        success: false,
        code: configError ? "FEEDBACK_SERVICE_UNAVAILABLE" : "FEEDBACK_SEND_FAILED",
        message: configError ? messages.feedback.serviceUnavailable : messages.feedback.sendFailed
      },
      { status: configError ? 503 : 502 }
    );
  }
}
