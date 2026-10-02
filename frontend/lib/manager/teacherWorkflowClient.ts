"use client";

import { authorizedFetch } from "../auth/client";

export async function fetchManagerNewsRequest(page = 1, pageSize = 10) {
  return authorizedFetch(`/api/manager/news?page=${page}&pageSize=${pageSize}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerNewsDraftRequest() {
  return authorizedFetch("/api/manager/news", {
    method: "POST",
    cache: "no-store"
  });
}

export async function fetchManagerNewsDetailRequest(newsId: number) {
  return authorizedFetch(`/api/manager/news/${newsId}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function updateManagerNewsRequest(newsId: number, payload: unknown) {
  return authorizedFetch(`/api/manager/news/${newsId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function deleteManagerNewsRequest(newsId: number) {
  return authorizedFetch(`/api/manager/news/${newsId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function uploadManagerNewsCoverRequest(newsId: number, formData: FormData) {
  return authorizedFetch(`/api/manager/news/${newsId}/cover`, {
    method: "POST",
    body: formData,
    cache: "no-store"
  });
}

export async function deleteManagerNewsCoverRequest(newsId: number) {
  return authorizedFetch(`/api/manager/news/${newsId}/cover`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function uploadManagerNewsBodyImageRequest(newsId: number, formData: FormData) {
  return authorizedFetch(`/api/manager/news/${newsId}/assets/image`, {
    method: "POST",
    body: formData,
    cache: "no-store"
  });
}

export async function fetchManagerRecommendationsRequest() {
  return authorizedFetch("/api/manager/club-recommendations", {
    method: "GET",
    cache: "no-store"
  });
}

export async function updateManagerRecommendationsRequest(clubIds: number[]) {
  return authorizedFetch("/api/manager/club-recommendations", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ clubIds }),
    cache: "no-store"
  });
}

export async function fetchManagerClubCreationRequests() {
  return authorizedFetch("/api/manager/club-creation-requests", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerClubCreationRequest(payload: unknown) {
  return authorizedFetch("/api/manager/club-creation-requests", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
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
