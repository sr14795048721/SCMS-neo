"use client";

import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import {
  getOrCreateSessionId,
  getOrCreateVisitorId,
  getTelemetryAuthorizationHeader,
  markPageView,
  shouldSkipDuplicatePageView
} from "../../lib/telemetry/client";

const HEARTBEAT_INTERVAL_MS = 60000;

export function TelemetryTracker() {
  const pathname = usePathname();
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    if (typeof document === "undefined") {
      return;
    }

    const syncVisibility = () => {
      setVisible(document.visibilityState === "visible");
    };

    syncVisibility();
    document.addEventListener("visibilitychange", syncVisibility);
    return () => {
      document.removeEventListener("visibilitychange", syncVisibility);
    };
  }, []);

  useEffect(() => {
    if (!pathname) {
      return;
    }

    const now = Date.now();
    if (shouldSkipDuplicatePageView(pathname, now)) {
      return;
    }

    markPageView(pathname, now);
    void sendTelemetry("page-view", pathname);
  }, [pathname]);

  useEffect(() => {
    const activePath = pathname || "/";

    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    if (!visible) {
      return;
    }

    void sendTelemetry("heartbeat", activePath);
    intervalRef.current = setInterval(() => {
      void sendTelemetry("heartbeat", activePath);
    }, HEARTBEAT_INTERVAL_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [pathname, visible]);

  return null;
}

async function sendTelemetry(type: "page-view" | "heartbeat", path: string) {
  const visitorId = getOrCreateVisitorId();
  const sessionId = getOrCreateSessionId();
  if (!visitorId || !sessionId) {
    return;
  }

  const headers: HeadersInit = {
    "Content-Type": "application/json"
  };
  const authorization = getTelemetryAuthorizationHeader();
  if (authorization) {
    headers.Authorization = authorization;
  }

  try {
    await fetch(`/api/telemetry/${type}`, {
      method: "POST",
      headers,
      body: JSON.stringify({
        visitorId,
        sessionId,
        path: path || "/"
      }),
      cache: "no-store",
      keepalive: true
    });
  } catch {
    // Ignore telemetry failures to avoid disturbing the user flow.
  }
}
