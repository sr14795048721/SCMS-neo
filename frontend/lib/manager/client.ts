"use client";

import { authorizedFetch } from "../auth/client";
import { ChangeManagerPasswordPayload, UpdateManagerInfoPayload } from "./types";

export async function fetchManagerInfoRequest() {
  return authorizedFetch("/api/manager/info", {
    method: "GET",
    cache: "no-store"
  });
}

export async function updateManagerInfoRequest(payload: UpdateManagerInfoPayload) {
  return authorizedFetch("/api/manager/info", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchManagerAvatarRequest(version?: string | null) {
  const suffix = version ? `?v=${encodeURIComponent(version)}` : "";
  return authorizedFetch(`/api/manager/avatar${suffix}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function uploadManagerAvatarRequest(formData: FormData) {
  return authorizedFetch("/api/manager/avatar", {
    method: "POST",
    body: formData,
    cache: "no-store"
  });
}

export async function changeManagerPasswordRequest(payload: ChangeManagerPasswordPayload) {
  return authorizedFetch("/api/manager/password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}
