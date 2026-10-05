import { proxyStudentScoreRecords } from "../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_SCORE_RECORDS_FAILED",
  failureMessage: "student score records request failed",
  unavailableCode: "STUDENT_SCORE_RECORDS_UNAVAILABLE",
  unavailableMessage: "student score records service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentScoreRecords(request, failure);
}
