export type NewsStatus = "DRAFT" | "PUBLISHED";

export type AdminNewsListItem = {
  id: number;
  title: string;
  status: NewsStatus;
  hasCover: boolean;
  cover?: string;
  authorName: string;
  authorRole?: string;
  viewCount: number;
  publishedAt: string | null;
  updatedAt: string | null;
};

export type AdminNewsDetail = {
  id: number;
  title: string;
  status: NewsStatus;
  markdownContent: string;
  cover?: string;
  authorName: string;
  authorRole?: string;
  viewCount: number;
  publishedAt: string | null;
  coverUpdatedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminNewsPage = {
  items: AdminNewsListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type PublicHomeNewsItem = {
  id: number;
  title: string;
  cover?: string;
  authorName: string;
  publishedAt: string | null;
  viewCount: number;
};

export type PublicNewsHome = {
  carousel: PublicHomeNewsItem[];
  textList: PublicHomeNewsItem[];
};

export type PublicNewsListItem = {
  id: number;
  title: string;
  cover?: string;
  authorName: string;
  publishedAt: string | null;
  viewCount: number;
};

export type PublicNewsPage = {
  items: PublicNewsListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type PublicNewsDetail = {
  id: number;
  title: string;
  cover?: string;
  authorName: string;
  publishedAt: string | null;
  viewCount: number;
  markdownContent: string;
};

export type UploadedNewsAsset = {
  assetId: number;
  url: string;
  markdown: string;
};

export type AdminNewsPreviewPayload = {
  previewId: string;
  newsId: number;
  title: string;
  markdownContent: string;
  cover?: string;
  coverUpdatedAt: string | null;
  updatedAt: string | null;
  authorName: string;
  createdAt: string;
};
