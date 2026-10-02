export type AdminClubStatus = "ACTIVE" | "INACTIVE";

export type AdminClubListItem = {
  id: number;
  name: string;
  type: string;
  status: AdminClubStatus;
  description: string;
  memberCount: number;
  managerCount: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminClubManager = {
  userId: number;
  username: string;
  displayName: string;
  managerNo: string;
  phone: string;
};

export type AdminClubStudent = {
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  grade: string;
  className: string;
  dutyId: number | null;
  dutyName: string;
  dutyPermissions: string[];
};

export type AdminClubDetail = AdminClubListItem & {
  managers: AdminClubManager[];
  students: AdminClubStudent[];
};

export type AdminClubDuty = {
  id: number;
  name: string;
  permissions: string[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminClubPage = {
  items: AdminClubListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type AdminClubMutationPayload = {
  name: string;
  type: string;
  status: AdminClubStatus;
  description: string;
  managerUserIds: number[];
};

export type AdminManagerOption = {
  userId: number;
  username: string;
  displayName: string;
  managerNo: string;
};

export type AdminClubOption = {
  id: number;
  name: string;
  type: string;
  description: string;
  memberCount: number;
};
