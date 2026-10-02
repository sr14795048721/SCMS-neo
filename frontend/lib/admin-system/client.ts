"use client";

import { authorizedFetch } from "../auth/client";
import { AdminDirtyDataCleanupResult, AdminStatistics, ChangeAdminPasswordPayload } from "./types";

export async function changeAdminPasswordRequest(payload: ChangeAdminPasswordPayload) {
  return authorizedFetch("/api/admin/system/password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchAdminStatisticsRequest() {
  return authorizedFetch("/api/admin/statistics", {
    method: "GET",
    cache: "no-store"
  });
}

export async function cleanAdminDirtyDataRequest() {
  return authorizedFetch("/api/admin/system/dirty-data/cleanup", {
    method: "DELETE",
    cache: "no-store"
  });
}

export type AdminStatisticsResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: AdminStatistics;
};

export type AdminDirtyDataCleanupResponse = {
  success?: boolean;
  code?: string;
  message?: string;
  data?: AdminDirtyDataCleanupResult;
};
