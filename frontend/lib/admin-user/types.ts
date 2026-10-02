import { StudentGrade } from "../student/types";

export type AdminUserTab = "students" | "managers";

export type AdminUserSortBy = "username" | "displayName" | "identity" | "contact" | "status";

export type AdminSortDirection = "asc" | "desc";

export type AdminStudentListItem = {
  userId: number;
  username: string;
  email: string;
  enabled: boolean;
  displayName: string;
  studentNo: string;
  grade: StudentGrade | "";
  className: string;
  phone: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminStudentDetail = AdminStudentListItem & {
  bio: string;
  role: string;
  hasAvatar: boolean;
  avatarUpdatedAt: string | null;
};

export type AdminManagerListItem = {
  userId: number;
  username: string;
  email: string;
  enabled: boolean;
  displayName: string;
  managerNo: string;
  phone: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminManagerDetail = AdminManagerListItem & {
  bio: string;
  role: string;
  hasAvatar: boolean;
  avatarUpdatedAt: string | null;
};

export type AdminStudentPage = {
  items: AdminStudentListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type AdminManagerPage = {
  items: AdminManagerListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type UpdateAdminStudentPayload = {
  username: string;
  email: string;
  enabled: boolean;
  displayName: string;
  studentNo: string;
  grade: StudentGrade | "";
  className: string;
  phone: string;
  bio: string;
};

export type UpdateAdminManagerPayload = {
  username: string;
  email: string;
  enabled: boolean;
  displayName: string;
  managerNo: string;
  phone: string;
  bio: string;
};

export type CreateAdminManagerPayload = UpdateAdminManagerPayload;

export type AdminStudentImportFailure = {
  rowNumber: number;
  email: string;
  reasonCode: string;
  reasonMessage: string;
};

export type AdminStudentImportResult = {
  totalRows: number;
  successCount: number;
  failureCount: number;
  failures: AdminStudentImportFailure[];
};

export type AdminBulkResetPasswordResult = {
  resetCount: number;
  defaultPassword: string;
};

export type AdminManagerCreateResult = {
  manager: AdminManagerDetail;
  defaultPassword: string;
};
