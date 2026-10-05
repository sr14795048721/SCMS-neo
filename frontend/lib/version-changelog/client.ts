"use client";

import { authorizedFetch } from "../auth/client";
import { VersionChangelogMutationPayload } from "./types";

export async function fetchVersionChangelogsRequest() {
  return authorizedFetch("/api/version-changelogs", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createVersionChangelogRequest(payload: VersionChangelogMutationPayload) {
  return authorizedFetch("/api/admin/version-changelogs", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateVersionChangelogRequest(changelogId: number, payload: VersionChangelogMutationPayload) {
  return authorizedFetch(`/api/admin/version-changelogs/${changelogId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function deleteVersionChangelogRequest(changelogId: number) {
  return authorizedFetch(`/api/admin/version-changelogs/${changelogId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}
