export type ManagerAttendanceSessionSummary = {
  sessionId: number;
  clubId: number;
  title: string;
  scoreRuleId: number;
  scoreRuleName: string;
  scoreDelta: number;
  status: string;
  totalMembers: number;
  checkedInCount: number;
  checkedOutCount: number;
  settledCount: number;
  startedAt: string | null;
  endedAt: string | null;
  settledAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ManagerAttendanceSessionMember = {
  studentUserId: number;
  displayName: string;
  grade: string;
  className: string;
  status: string;
  checkInAt: string | null;
  checkOutAt: string | null;
  settled: boolean;
};

export type ManagerAttendanceSessionDetail = {
  session: ManagerAttendanceSessionSummary;
  members: ManagerAttendanceSessionMember[];
};

export type CreateAttendanceSessionPayload = {
  title: string;
  scoreRuleId: number;
};

export type MarkAttendancePayload = {
  studentUserId: number;
};

export type BulkMarkAttendancePayload = {
  studentUserIds: number[];
};
