export type ScoreRuleScope = "GLOBAL" | "CLUB";
export type ScoreRuleStatus = "ACTIVE" | "INACTIVE";
export type RewardStatus = "ACTIVE" | "INACTIVE";
export type RewardVisibilityScope = "GLOBAL" | "CLUB" | "UNASSIGNED";
export type RewardOrderStatus = "PENDING" | "COMPLETED" | "REJECTED";

export type AdminScoreRule = {
  id: number;
  name: string;
  scoreDelta: number;
  scopeType: ScoreRuleScope;
  clubId: number | null;
  clubName: string;
  status: ScoreRuleStatus;
  createdBy: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminScoreRuleMutationPayload = {
  name: string;
  scoreDelta: number;
  scopeType: ScoreRuleScope;
  clubId: number | null;
  status: ScoreRuleStatus;
};

export type AdminScoreRecord = {
  id: number;
  clubId: number;
  clubName: string;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  ruleId: number | null;
  ruleName: string;
  scoreDelta: number;
  reason: string;
  operatorUserId: number;
  operatorName: string;
  createdAt: string | null;
};

export type AdminScoreRecordMutationPayload = {
  clubId: number;
  userId: number;
  ruleId: number | null;
  scoreDelta: number;
  reason: string;
};

export type AdminRewardItem = {
  id: number;
  name: string;
  scoreCost: number;
  stock: number;
  status: RewardStatus;
  visibilityScope: RewardVisibilityScope;
  clubIds: number[];
  clubNames: string[];
  hasImage: boolean;
  imageUrl?: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminRewardMutationPayload = {
  name: string;
  scoreCost: number;
  stock: number;
  visibilityScope: Exclude<RewardVisibilityScope, "UNASSIGNED"> | "UNASSIGNED";
  clubIds: number[];
  status: RewardStatus;
};

export type AdminRewardOrder = {
  id: number;
  rewardId: number;
  rewardName: string;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  scoreCost: number;
  status: RewardOrderStatus;
  createdAt: string | null;
  completedAt: string | null;
  completedBy: number | null;
  completedByName: string;
  rejectedAt: string | null;
  rejectedBy: number | null;
  rejectedByName: string;
};
