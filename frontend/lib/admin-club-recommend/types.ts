export type RecommendedClubItem = {
  slotNo: number;
  clubId: number;
  name: string;
  type: string;
  description: string;
  memberCount: number;
};

export type RecommendedClubOption = {
  id: number;
  name: string;
  type: string;
  description: string;
  memberCount: number;
};

export type UpdateRecommendedClubsPayload = {
  clubIds: number[];
};
