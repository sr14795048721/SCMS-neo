export type ManagerClubAppWorkspaceMaterial = {
  id: number | null;
  sectionKey: string;
  title: string;
  storagePath: string;
  sortOrder: number;
  enabled: boolean;
};

export type ManagerClubAppWorkspaceProject = {
  id: number | null;
  projectKey: string;
  title: string;
  subtitle: string;
  summary: string;
  coverUrl: string;
  sortOrder: number;
  enabled: boolean;
  materials: ManagerClubAppWorkspaceMaterial[];
};

export type ManagerClubAppWorkspaceGroup = {
  id: number | null;
  title: string;
  subtitle: string;
  sortOrder: number;
  enabled: boolean;
  projects: ManagerClubAppWorkspaceProject[];
};

export type ManagerClubAppWorkspace = {
  clubId: number;
  clubName: string;
  groups: ManagerClubAppWorkspaceGroup[];
};

export type ManagerClubAppWorkspacePayload = {
  groups: Array<{
    id?: number;
    title: string;
    subtitle: string;
    enabled: boolean;
    projects: Array<{
      id?: number;
      projectKey: string;
      title: string;
      subtitle: string;
      summary: string;
      coverUrl: string;
      enabled: boolean;
      materials: Array<{
        id?: number;
        sectionKey: string;
        title: string;
        storagePath: string;
        enabled: boolean;
      }>;
    }>;
  }>;
};
