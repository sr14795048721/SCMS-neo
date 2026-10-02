export type ManagerActivityItem = {
  id: number;
  clubId: number;
  clubName: string;
  title: string;
  description: string;
  location: string;
  startTime: string | null;
  endTime: string | null;
  capacity: number;
  status: string;
  registrationCount: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ManagerActivityMutationPayload = {
  clubId: number;
  title: string;
  description: string;
  location: string;
  startTime: string;
  endTime: string;
  capacity: number;
};
