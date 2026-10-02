export type BannerMediaType = "image" | "video";

export type PublicBannerItem = {
  type: BannerMediaType;
  src: string;
  title?: string;
  desc?: string;
  poster?: string;
};

export type AdminBannerItem = PublicBannerItem & {
  id: number;
  sortOrder: number;
  hasMedia: boolean;
};

export type AdminBannerRecompressFailure = {
  bannerId: number;
  reason: string;
};

export type AdminBannerRecompressResult = {
  processedCount: number;
  skippedCount: number;
  failedCount: number;
  failures: AdminBannerRecompressFailure[];
};
