export const STUDENT_GRADE_VALUES = ["HIGH_1", "HIGH_2", "HIGH_3"] as const;
export type StudentGrade = (typeof STUDENT_GRADE_VALUES)[number];

export type StudentInfo = {
  userId: number;
  username: string;
  role: string;
  email: string;
  displayName: string;
  studentNo: string;
  grade: StudentGrade | "";
  className: string;
  phone: string;
  bio: string;
  hasAvatar: boolean;
  avatarUpdatedAt: string | null;
  profileCompleted: boolean;
  missingRequiredFields: string[];
};

export type UpdateStudentInfoPayload = {
  displayName: string;
  studentNo: string;
  grade: StudentGrade | "";
  className: string;
  phone: string;
  bio: string;
};

export type ChangeStudentPasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export type StudentActivityItem = {
  id: number;
  clubId: number;
  title: string;
  description: string;
  location: string;
  startTime: string | null;
  endTime: string | null;
  capacity: number;
  status: string;
  createdBy: number;
  createdAt: string | null;
};

export type StudentActivityMutationPayload = {
  clubId: number;
  title: string;
  description: string;
  location: string;
  startTime: string;
  endTime: string;
  capacity: number;
};

export type StudentDiscoverClub = {
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  memberCount: number;
  dutyId: number | null;
  dutyName: string;
  dutyPermissions: string[];
  canManage: boolean;
  createdAt: string | null;
};

export type StudentPendingClubJoinRequest = {
  requestId: number;
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  memberCount: number;
  reason: string;
  status: string;
  createdAt: string | null;
};

export type StudentDiscoverClubsState = {
  joinedClub: StudentDiscoverClub | null;
  pendingRequest: StudentPendingClubJoinRequest | null;
  clubs: StudentDiscoverClub[];
};

export type StudentScoreSummaryClub = {
  clubId: number;
  clubName: string;
  score: number;
};

export type StudentScoreSummary = {
  totalScore: number;
  redeemedScore: number;
  balanceScore: number;
  clubs: StudentScoreSummaryClub[];
};

export type StudentClubRankingItem = {
  rank: number;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  score: number;
};

export type StudentSchoolRankingItem = {
  rank: number;
  clubId: number;
  clubName: string;
  memberCount: number;
  totalScore: number;
  avgScore: number;
};

export type StudentRewardStatus = "ACTIVE" | "INACTIVE";
export type StudentRewardOrderStatus = "PENDING" | "COMPLETED" | "REJECTED";

export type StudentRewardItem = {
  id: number;
  name: string;
  scoreCost: number;
  stock: number;
  status: StudentRewardStatus;
  hasImage: boolean;
  imageUrl?: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type StudentRewardOrder = {
  id: number;
  rewardId: number;
  rewardName: string;
  scoreCost: number;
  status: StudentRewardOrderStatus;
  createdAt: string | null;
  completedAt: string | null;
  rejectedAt: string | null;
};
