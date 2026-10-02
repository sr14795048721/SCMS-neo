import { proxyStudentSchoolRankings } from "../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_SCHOOL_RANKINGS_FAILED",
  failureMessage: "student school rankings request failed",
  unavailableCode: "STUDENT_SCHOOL_RANKINGS_UNAVAILABLE",
  unavailableMessage: "student school rankings service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentSchoolRankings(request, failure);
}
