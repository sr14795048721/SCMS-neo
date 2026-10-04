"use client";

import { authorizedFetch } from "../auth/client";
import { CreateAttendanceSessionPayload } from "./attendanceTypes";

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

export async function settleManagerAttendanceSessionRequest(clubId: number, sessionId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/attendance-sessions/${sessionId}/settle`, {
    method: "POST",
    cache: "no-store"
  });
}
