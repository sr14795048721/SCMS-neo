export type VersionChangelogItem = {
  id: number;
  version: string;
  title: string;
  content: string;
  releasedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type VersionChangelogMutationPayload = {
  version: string;
  title: string;
  content: string;
  releasedAt: string;
};
