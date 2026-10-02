export type AdminActivityStatus = "DRAFT" | "PUBLISHED" | "CLOSED";

export type AdminActivityListItem = {
  id: number;
  clubId: number;
  clubName: string;
  title: string;
  description: string;
  location: string;
  startTime: string | null;
  endTime: string | null;
  capacity: number;
  status: AdminActivityStatus;
  registrationCount: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AdminActivityRegistration = {
  registrationId: number;
  userId: number;
  username: string;
  displayName: string;
  studentNo: string;
  grade: string;
  className: string;
  registeredAt: string | null;
};

export type AdminActivityDetail = AdminActivityListItem & {
  registrations: AdminActivityRegistration[];
};

export type AdminActivityPage = {
  items: AdminActivityListItem[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type AdminActivityMutationPayload = {
  clubId: number;
  title: string;
  description: string;
  location: string;
  startTime: string;
  endTime: string;
  capacity: number;
};

export type AdminActivityClubOption = {
  id: number;
  name: string;
  type: string;
  description: string;
  memberCount: number;
};
