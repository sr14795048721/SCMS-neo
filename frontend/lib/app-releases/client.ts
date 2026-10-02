"use client";

import { authorizedFetch } from "../auth/client";
import { AppReleaseFormValue } from "./types";

export async function fetchAdminAppReleasesRequest() {
  return authorizedFetch("/api/admin/app-releases", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createAdminAppReleaseRequest(payload: AppReleaseFormValue) {
  return authorizedFetch("/api/admin/app-releases", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateAdminAppReleaseRequest(releaseId: number, payload: AppReleaseFormValue) {
  return authorizedFetch(`/api/admin/app-releases/${releaseId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function publishAdminAppReleaseRequest(releaseId: number) {
  return authorizedFetch(`/api/admin/app-releases/${releaseId}/publish`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function retireAdminAppReleaseRequest(releaseId: number) {
  return authorizedFetch(`/api/admin/app-releases/${releaseId}/retire`, {
    method: "POST",
    cache: "no-store"
  });
}
