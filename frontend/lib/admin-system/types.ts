export type ChangeAdminPasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export type AdminDirtyDataCleanupResult = {
  scoreRecordsRemoved: number;
  rewardOrdersRemoved: number;
  clubJoinRequestsRemoved: number;
  registrationsRemoved: number;
  clubMembersRemoved: number;
  notificationsRemoved: number;
  studentProfilesRemoved: number;
  managerProfilesRemoved: number;
  clubManagerBindingsRemoved: number;
  clubDutiesRemoved: number;
  rewardTargetClubsRemoved: number;
  totalRemoved: number;
};

export type AdminStatisticsOverview = {
  totalUsers: number;
  activeUsers: number;
  totalClubs: number;
  totalActivities: number;
  activeRegistrations: number;
  uniqueVisitors: number;
  todayVisitCount: number;
  onlineSessions: number;
};

export type AdminRoleDistribution = {
  studentCount: number;
  managerCount: number;
  adminCount: number;
};

export type AdminClubRanking = {
  rank: number;
  clubId: number;
  clubName: string;
  memberCount: number;
  activityCount: number;
};

export type AdminStatistics = {
  overview: AdminStatisticsOverview;
  roleDistribution: AdminRoleDistribution;
  clubRankings: AdminClubRanking[];
};
