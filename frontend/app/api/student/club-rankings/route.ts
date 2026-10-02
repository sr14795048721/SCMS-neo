import { proxyStudentClubRankings } from "../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_CLUB_RANKINGS_FAILED",
  failureMessage: "student club rankings request failed",
  unavailableCode: "STUDENT_CLUB_RANKINGS_UNAVAILABLE",
  unavailableMessage: "student club rankings service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentClubRankings(request, failure);
}
