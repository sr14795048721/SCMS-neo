export type PublicAttendanceSession = {
  clubName: string;
  title: string;
  status: "OPEN" | "COMPLETED";
  scoreRuleName: string;
  scoreDelta: number;
  startedAt: string | null;
};

export type PublicAttendanceSignResult = {
  displayName: string;
  grade: string;
  className: string;
  role: string;
  checkInAt: string | null;
};
