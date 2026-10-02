import { proxyStudentScoreSummary } from "../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_SCORE_SUMMARY_FAILED",
  failureMessage: "student score summary request failed",
  unavailableCode: "STUDENT_SCORE_SUMMARY_UNAVAILABLE",
  unavailableMessage: "student score summary service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentScoreSummary(request, failure);
}
