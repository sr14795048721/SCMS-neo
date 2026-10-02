"use client";

import { authorizedFetch } from "../auth/client";
import { ManagerActivityMutationPayload } from "./activityTypes";

export async function fetchManagerActivitiesRequest() {
  return authorizedFetch("/api/manager/activities", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerActivityRequest(payload: ManagerActivityMutationPayload) {
  return authorizedFetch("/api/manager/activities", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateManagerActivityRequest(activityId: number, payload: ManagerActivityMutationPayload) {
  return authorizedFetch(`/api/manager/activities/${activityId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function publishManagerActivityRequest(activityId: number) {
  return authorizedFetch(`/api/manager/activities/${activityId}/publish`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function closeManagerActivityRequest(activityId: number) {
  return authorizedFetch(`/api/manager/activities/${activityId}/close`, {
    method: "POST",
    cache: "no-store"
  });
}
