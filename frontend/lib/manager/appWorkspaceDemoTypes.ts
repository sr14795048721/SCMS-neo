export type ManagerClubAppWorkspaceProjectDemoStep = {
  id: number | null;
  title: string;
  description: string;
  triggerType: string;
  targetSubsystem: string;
  actionKey: string | null;
  sortOrder: number;
  enabled: boolean;
};

export type ManagerClubAppWorkspaceProjectDemo = {
  projectId: number;
  projectKey: string;
  title: string;
  subtitle: string;
  coverUrl: string;
  overviewTitle: string;
  overviewBody: string;
  bridgeMode: string;
  enabled: boolean;
  steps: ManagerClubAppWorkspaceProjectDemoStep[];
  mockSnapshot: unknown;
};

export type ManagerClubAppWorkspaceProjectDemoPayload = {
  overviewTitle: string;
  overviewBody: string;
  enabled: boolean;
  steps: Array<{
    id?: number;
    title: string;
    description: string;
    triggerType: string;
    targetSubsystem: string;
    actionKey?: string;
    enabled: boolean;
  }>;
};
