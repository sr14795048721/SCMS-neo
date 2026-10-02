export type ManagerInfo = {
  userId: number;
  username: string;
  role: string;
  email: string;
  displayName: string;
  managerNo: string;
  phone: string;
  bio: string;
  hasAvatar: boolean;
  avatarUpdatedAt: string | null;
};

export type UpdateManagerInfoPayload = {
  displayName: string;
  managerNo: string;
  phone: string;
  bio: string;
};

export type ChangeManagerPasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};
