export type ManagerScoreRule = {
  id: number;
  name: string;
  scoreDelta: number;
  scopeType: string;
  clubId: number | null;
  clubName: string;
  status: string;
  createdBy: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ManagerScoreRecord = {
  id: number;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  ruleId: number | null;
  ruleName: string;
  scoreDelta: number;
  reason: string;
  operatorUserId: number;
  operatorUsername: string;
  createdAt: string | null;
};

export type ManagerScoreRanking = {
  rank: number;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  score: number;
};

export type CreateScoreRecordPayload = {
  userId: number;
  ruleId: number | null;
  scoreDelta?: number | null;
  reason: string;
};
