import { proxyStudentRewards } from "../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_REWARDS_FAILED",
  failureMessage: "student rewards request failed",
  unavailableCode: "STUDENT_REWARDS_UNAVAILABLE",
  unavailableMessage: "student rewards service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentRewards(request, failure);
}
