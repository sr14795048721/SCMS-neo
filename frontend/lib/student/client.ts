"use client";

import { authorizedFetch, getValidAccessToken } from "../auth/client";
import {
  ChangeStudentPasswordPayload,
  StudentActivityItem,
  StudentActivityMutationPayload,
  StudentClubRankingItem,
  StudentDiscoverClubsState,
  StudentRewardOrder,
  StudentRewardItem,
  StudentSchoolRankingItem,
  StudentScoreRecord,
  StudentScoreSummary,
  UpdateStudentInfoPayload
} from "./types";

export async function fetchStudentInfoRequest() {
  return authorizedFetch("/api/student/info", {
    method: "GET",
    cache: "no-store"
  });
}

export async function updateStudentInfoRequest(payload: UpdateStudentInfoPayload) {
  return authorizedFetch("/api/student/info", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchStudentAvatarRequest(version?: string | null) {
  const suffix = version ? `?v=${encodeURIComponent(version)}` : "";
  return authorizedFetch(`/api/student/avatar${suffix}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function uploadStudentAvatarRequest(formData: FormData) {
  return authorizedFetch("/api/student/avatar", {
    method: "POST",
    body: formData,
    cache: "no-store"
  });
}

export async function changeStudentPasswordRequest(payload: ChangeStudentPasswordPayload) {
  return authorizedFetch("/api/student/password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchActivitiesRequest() {
  const headers = new Headers();
  const accessToken = await getValidAccessToken().catch(() => null);
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return fetch("/api/activities", {
    method: "GET",
    headers,
    cache: "no-store"
  });
}

export async function fetchStudentActivitiesRequest() {
  return authorizedFetch("/api/student/activities", {
    method: "GET",
    cache: "no-store"
  });
}

export async function updateStudentActivityRequest(activityId: number, payload: StudentActivityMutationPayload) {
  return authorizedFetch(`/api/student/activities/${activityId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchMyRegistrationIdsRequest() {
  return authorizedFetch("/api/activities/registrations/me", {
    method: "GET",
    cache: "no-store"
  });
}

export async function registerActivityRequest(activityId: number) {
  return authorizedFetch(`/api/activities/${activityId}/registrations`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function cancelActivityRegistrationRequest(activityId: number) {
  return authorizedFetch(`/api/activities/${activityId}/registrations/me`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function createStudentClubJoinRequestRequest(payload: {
  clubId: number;
  reason: string;
}) {
  return authorizedFetch("/api/student/club-join-requests", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchStudentDiscoverClubsRequest() {
  return authorizedFetch("/api/student/discover-clubs", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentCadreClubDetailRequest(clubId: number) {
  return authorizedFetch(`/api/student/clubs/${clubId}/cadre`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentCadreClubMembersRequest(clubId: number) {
  return authorizedFetch(`/api/student/clubs/${clubId}/cadre/members`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentScoreSummaryRequest() {
  return authorizedFetch("/api/student/score-summary", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentScoreRecordsRequest() {
  return authorizedFetch("/api/student/score-records", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentClubRankingsRequest(clubId: number, range = "all") {
  const query = new URLSearchParams({
    clubId: String(clubId),
    range
  });

  return authorizedFetch(`/api/student/club-rankings?${query.toString()}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentSchoolRankingsRequest() {
  return authorizedFetch("/api/student/school-rankings", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentRewardsRequest() {
  return authorizedFetch("/api/student/rewards", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchStudentRewardOrdersRequest() {
  return authorizedFetch("/api/student/reward-orders", {
    method: "GET",
    cache: "no-store"
  });
}

export async function createStudentRewardOrderRequest(rewardId: number) {
  return authorizedFetch("/api/student/reward-orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ rewardId }),
    cache: "no-store"
  });
}

export async function fetchStudentRewardImageRequest(rewardId: number, version?: string | null) {
  const suffix = version ? `?v=${encodeURIComponent(version)}` : "";
  return authorizedFetch(`/api/student/rewards/${rewardId}/image/content${suffix}`, {
    method: "GET",
    cache: "no-store"
  });
}

export type StudentDiscoverClubsResponse = {
  success?: boolean;
  data?: StudentDiscoverClubsState;
  message?: string;
};

export type StudentScoreSummaryResponse = {
  success?: boolean;
  data?: StudentScoreSummary;
  message?: string;
};

export type StudentScoreRecordsResponse = {
  success?: boolean;
  data?: StudentScoreRecord[];
  message?: string;
};

export type StudentClubRankingsResponse = {
  success?: boolean;
  data?: StudentClubRankingItem[];
  message?: string;
};

export type StudentSchoolRankingsResponse = {
  success?: boolean;
  data?: StudentSchoolRankingItem[];
  message?: string;
};

export type StudentRewardsResponse = {
  success?: boolean;
  data?: StudentRewardItem[];
  message?: string;
};

export type StudentRewardOrdersResponse = {
  success?: boolean;
  data?: StudentRewardOrder[];
  message?: string;
};

export function sortActivitiesByStartTime(items: StudentActivityItem[]): StudentActivityItem[] {
  return [...items].sort((left, right) => {
    const leftTime = left.startTime ? new Date(left.startTime).getTime() : Number.MAX_SAFE_INTEGER;
    const rightTime = right.startTime ? new Date(right.startTime).getTime() : Number.MAX_SAFE_INTEGER;
    return leftTime - rightTime;
  });
}
