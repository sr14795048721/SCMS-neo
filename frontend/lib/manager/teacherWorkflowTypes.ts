import type { AdminNewsDetail, AdminNewsPage } from "../news/types";

export type TeacherNewsPage = AdminNewsPage;
export type TeacherNewsDetail = AdminNewsDetail;

export type TeacherRecommendationItem = {
  orderNo: number;
  clubId: number;
  clubName: string;
  clubType: string;
  description: string;
  memberCount: number;
  remark: string;
};

export type TeacherRecommendationGroup = {
  managerUserId: number;
  managerName: string;
  managerNo: string;
  recommendations: TeacherRecommendationItem[];
};

export type TeacherNotification = {
  id: number;
  category: string;
  title: string;
  content: string;
  status: "UNREAD" | "READ";
  createdAt: string | null;
  targetPath: string | null;
};

export type ClubCreationRequestItem = {
  id: number;
  applicantManagerUserId: number;
  applicantName: string;
  applicantManagerNo: string;
  name: string;
  type: string;
  description: string;
  applyReason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reviewedBy: number | null;
  reviewedByName: string;
  reviewedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};
