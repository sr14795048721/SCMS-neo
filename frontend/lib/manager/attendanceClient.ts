"use client";

import { authorizedFetch } from "../auth/client";
import { BulkMarkAttendancePayload, CreateAttendanceSessionPayload, MarkAttendancePayload } from "./attendanceTypes";

export async function fetchManagerAttendanceSessionsRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerAttendanceSessionRequest(clubId: number, payload: CreateAttendanceSessionPayload) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchManagerAttendanceSessionDetailRequest(clubId: number, sessionId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions/${sessionId}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function markManagerAttendanceRequest(
  clubId: number,
  sessionId: number,
  action: "check-in" | "check-out",
  payload: MarkAttendancePayload
) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions/${sessionId}/${action}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function settleManagerAttendanceSessionRequest(clubId: number, sessionId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions/${sessionId}/settle`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function bulkMarkManagerAttendanceRequest(
  clubId: number,
  sessionId: number,
  action: "bulk-check-in" | "bulk-check-out",
  payload: BulkMarkAttendancePayload
) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions/${sessionId}/${action}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}
