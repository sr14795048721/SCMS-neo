"use client";

import { authorizedFetch } from "../auth/client";
import {
  ClubDutyMutationPayload,
  CreateClubJoinRequestPayload,
  UpdateClubMemberDutyPayload
} from "./clubTypes";
import { ManagerClubAppWorkspacePayload } from "./appWorkspaceTypes";
import { ManagerClubAppWorkspaceProjectDemoPayload } from "./appWorkspaceDemoTypes";

export async function fetchManagerClubsRequest() {
  return authorizedFetch("/api/manager/clubs", {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchManagerClubDetailRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchManagerClubAppWorkspaceRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/app-workspace`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function saveManagerClubAppWorkspaceRequest(
  clubId: number,
  payload: ManagerClubAppWorkspacePayload
) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/app-workspace`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchManagerClubAppWorkspaceProjectDemoRequest(clubId: number, projectKey: string) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/app-workspace/${encodeURIComponent(projectKey)}/demo`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function saveManagerClubAppWorkspaceProjectDemoRequest(
  clubId: number,
  projectKey: string,
  payload: ManagerClubAppWorkspaceProjectDemoPayload
) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/app-workspace/${encodeURIComponent(projectKey)}/demo`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function fetchManagerClubMembersRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/members`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function fetchManagerClubJoinRequestsRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/join-requests`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function approveManagerClubJoinRequest(clubId: number, requestId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/join-requests/${requestId}/approve`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function rejectManagerClubJoinRequest(clubId: number, requestId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/join-requests/${requestId}/reject`, {
    method: "POST",
    cache: "no-store"
  });
}

export async function fetchManagerClubDutiesRequest(clubId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/duties`, {
    method: "GET",
    cache: "no-store"
  });
}

export async function createManagerClubDutyRequest(clubId: number, payload: ClubDutyMutationPayload) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/duties`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function updateManagerClubDutyRequest(clubId: number, dutyId: number, payload: ClubDutyMutationPayload) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/duties/${dutyId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function deleteManagerClubDutyRequest(clubId: number, dutyId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/duties/${dutyId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function updateManagerClubMemberDutyRequest(
  clubId: number,
  studentUserId: number,
  payload: UpdateClubMemberDutyPayload
) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/members/${studentUserId}/duty`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}

export async function removeManagerClubMemberRequest(clubId: number, studentUserId: number) {
  return authorizedFetch(`/api/manager/clubs/${clubId}/members/${studentUserId}`, {
    method: "DELETE",
    cache: "no-store"
  });
}

export async function createStudentClubJoinRequestRequest(payload: CreateClubJoinRequestPayload) {
  return authorizedFetch("/api/student/club-join-requests", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload),
    cache: "no-store"
  });
}
