export type CompetitionSourceType = "OFFICIAL_WEBSITE" | "WECHAT_OFFICIAL";
export type CompetitionLeadStatus = "PENDING" | "CONFIRMED" | "IGNORED";
export type CompetitionStatus = "UPCOMING" | "ONGOING" | "ENDED";

export type CompetitionSource = {
  id: number;
  name: string;
  url: string | null;
  sourceType: CompetitionSourceType;
  wechatName: string | null;
  enabled: boolean;
  scanEnabled: boolean;
  createdAt: string | null;
  updatedAt: string | null;
};

export type CompetitionSourcePayload = {
  name: string;
  url?: string | null;
  sourceType: CompetitionSourceType;
  wechatName?: string | null;
  enabled: boolean;
  scanEnabled: boolean;
};

export type CompetitionLead = {
  id: number;
  sourceId: number | null;
  sourceName: string | null;
  title: string;
  url: string | null;
  snippet: string | null;
  detectedAt: string | null;
  status: CompetitionLeadStatus;
  createdAt: string | null;
};

export type CompetitionItem = {
  id: number;
  name: string;
  category: string | null;
  participantScope: string | null;
  applyDeadline: string | null;
  startDate: string | null;
  endDate: string | null;
  url: string | null;
  sourceId: number | null;
  sourceName: string | null;
  status: CompetitionStatus;
  note: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type CompetitionPayload = {
  name: string;
  category?: string | null;
  participantScope?: string | null;
  applyDeadline?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  url?: string | null;
  sourceId?: number | null;
  status: CompetitionStatus;
  note?: string | null;
};

export type CompetitionImportPayload = {
  url: string;
  title?: string | null;
  content?: string | null;
};

export type CompetitionScanResult = {
  created: number;
};
