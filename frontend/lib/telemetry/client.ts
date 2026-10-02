"use client";

import { getPersistedAuthTokens } from "../auth/session";

export const VISITOR_ID_STORAGE_KEY = "scms.visitor.id";
export const SESSION_ID_STORAGE_KEY = "scms.session.id";
const LAST_PAGE_VIEW_STORAGE_KEY = "scms.telemetry.lastPageView";

type LastPageViewState = {
  path: string;
  timestamp: number;
};

export function getOrCreateVisitorId(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  const current = window.localStorage.getItem(VISITOR_ID_STORAGE_KEY);
  if (current) {
    return current;
  }

  const next = createId();
  window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, next);
  return next;
}

export function getOrCreateSessionId(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  const current = window.sessionStorage.getItem(SESSION_ID_STORAGE_KEY);
  if (current) {
    return current;
  }

  const next = createId();
  window.sessionStorage.setItem(SESSION_ID_STORAGE_KEY, next);
  return next;
}

export function getTelemetryAuthorizationHeader(): string | null {
  const accessToken = getPersistedAuthTokens()?.accessToken;
  return accessToken ? `Bearer ${accessToken}` : null;
}

export function shouldSkipDuplicatePageView(path: string, now: number): boolean {
  if (typeof window === "undefined") {
    return false;
  }

  const raw = window.sessionStorage.getItem(LAST_PAGE_VIEW_STORAGE_KEY);
  if (!raw) {
    return false;
  }

  try {
    const parsed = JSON.parse(raw) as LastPageViewState;
    return parsed.path === path && now - parsed.timestamp < 1500;
  } catch {
    return false;
  }
}

export function markPageView(path: string, now: number): void {
  if (typeof window === "undefined") {
    return;
  }

  const payload: LastPageViewState = {
    path,
    timestamp: now
  };
  window.sessionStorage.setItem(LAST_PAGE_VIEW_STORAGE_KEY, JSON.stringify(payload));
}

function createId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}
