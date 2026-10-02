import { NextResponse } from "next/server";
import { loadApiMessages } from "../../../../lib/i18n/apiMessages";
import {
  isStrongPassword,
  isValidEmail,
  USERNAME_MAX_LENGTH,
  USERNAME_MIN_LENGTH
} from "../../../../lib/auth/validation";

type RegisterRequestPayload = {
  username?: string;
  email?: string;
  password?: string;
};

type BackendResponse = {
  code?: string;
  message?: string;
};

export async function POST(request: Request) {
  const payload = (await request.json()) as RegisterRequestPayload;
  const messages = await loadApiMessages(request.headers.get("accept-language"));
  const username = String(payload?.username || "").trim();
  const email = String(payload?.email || "").trim();
  const password = String(payload?.password || "");

  if (!username || !email || !password) {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_REGISTER_INVALID_INPUT",
        message: messages.auth.registerRequired
      },
      { status: 400 }
    );
  }

  if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_REGISTER_USERNAME_INVALID",
        message: messages.auth.usernameInvalid
      },
      { status: 400 }
    );
  }

  if (!isValidEmail(email)) {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_REGISTER_INVALID_EMAIL",
        message: messages.auth.emailInvalid
      },
      { status: 400 }
    );
  }

  if (!isStrongPassword(password)) {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_REGISTER_WEAK_PASSWORD",
        message: messages.auth.passwordWeak
      },
      { status: 400 }
    );
  }

  const backendBase = process.env.BACKEND_BASE_URL || "http://localhost:8080";

  try {
    const response = await fetch(`${backendBase}/api/v1/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        username,
        email,
        password
      }),
      cache: "no-store"
    });

    const data = (await response.json().catch(() => null)) as BackendResponse | null;
    if (!response.ok) {
      return NextResponse.json(
        {
          success: false,
          code: data?.code || "AUTH_REGISTER_FAILED",
          message: data?.message || messages.auth.registerFailed
        },
        { status: response.status || 500 }
      );
    }

    return NextResponse.json({
      success: true
    });
  } catch {
    return NextResponse.json(
      {
        success: false,
        code: "AUTH_BACKEND_UNREACHABLE",
        message: messages.auth.serviceUnavailable
      },
      { status: 503 }
    );
  }
}
