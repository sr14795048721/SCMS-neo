"use client";

import { authorizedFetch } from "../auth/client";

export async function fetchNotificationSummaryRequest() {
  return authorizedFetch("/api/notifications/summary", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchNotificationFeedRequest(status: "UNREAD" | "READ", page = 0, size = 20) {
  const query = new URLSearchParams({
    status,
    page: String(page),
    size: String(size)
  });

  return authorizedFetch(`/api/notifications/feed?${query.toString()}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchNotificationsRequest(status: "UNREAD" | "READ") {
  return authorizedFetch(`/api/notifications/me?status=${status}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function markNotificationReadRequest(notificationId: number) {
  return authorizedFetch(`/api/notifications/${notificationId}/read`, {
    method: "POST",
    cache: "no-store"
  });
}
