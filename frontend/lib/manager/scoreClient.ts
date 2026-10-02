"use client";

import { authorizedFetch } from "../auth/client";
import { CreateScoreRecordPayload } from "./scoreTypes";

export async function fetchManagerScoreRulesRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/score-rules`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchManagerScoreRecordsRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/score-records`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerScoreRecordRequest(clubId: number, payload: CreateScoreRecordPayload) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/score-records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchManagerScoreRankingsRequest(clubId: number, range: string) {
  const suffix = range ? `?range=${encodeURIComponent(range)}` : "";
  return authorizedFetch(`/api/manager/clubs/${clubId}/score-rankings${suffix}`, {
    method: "GET",
    cache: "no-store"
  });
}
