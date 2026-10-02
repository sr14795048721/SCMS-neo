export const CLUB_DUTY_PERMISSION_VALUES = [
  "SCORE_MANAGEMENT",
  "ACTIVITY_MANAGEMENT",
  "JOIN_APPROVAL",
  "NEWS_PUBLISH"
] as const;

export type ClubDutyPermission = (typeof CLUB_DUTY_PERMISSION_VALUES)[number];

export type ClubDuty = {
  id: number;
  name: string;
  permissions: ClubDutyPermission[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type ManagerClubSummary = {
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  memberCount: number;
  pendingJoinRequestCount: number;
  draftActivityCount: number;
  createdAt: string | null;
};

export type ManagerClubDetail = {
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  status: string;
  memberCount: number;
  pendingJoinRequestCount: number;
  totalJoinRequestCount: number;
  activityCount: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ManagerClubMember = {
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  grade: string;
  className: string;
  role: string;
  dutyId: number | null;
  dutyName: string;
  dutyPermissions: ClubDutyPermission[];
  joinedAt: string | null;
};

export type ManagerClubJoinRequest = {
  requestId: number;
  clubId: number;
  studentUserId: number;
  username: string;
  displayName: string;
  studentNo: string;
  grade: string;
  className: string;
  reason: string;
  status: string;
  reviewedBy: number | null;
  reviewedByName: string;
  reviewedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ClubDutyMutationPayload = {
  name: string;
  permissions: ClubDutyPermission[];
};

export type UpdateClubMemberDutyPayload = {
  dutyId: number | null;
};

export type CreateClubJoinRequestPayload = {
  clubId: number;
  reason: string;
};
