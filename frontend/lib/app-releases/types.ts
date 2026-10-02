export type AppReleaseStatus = "DRAFT" | "PUBLISHED" | "RETIRED";

export type AppReleaseItem = {
  id: number;
  versionName: string;
  buildNumber: number;
  releaseNotes: string;
  forceUpdate: boolean;
  androidUrl: string;
  harmonyUrl: string;
  status: AppReleaseStatus;
  publishedAt: string | null;
  createdBy: number;
  updatedBy: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AppReleaseFormValue = {
  versionName: string;
  buildNumber: number;
  releaseNotes: string;
  forceUpdate: boolean;
  androidUrl: string;
  harmonyUrl: string;
};
