import { proxyAdminScoreRuleCollection } from "../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_SCORE_RULES_FAILED",
  failureMessage: "admin score rules request failed",
  unavailableCode: "ADMIN_SCORE_RULES_BACKEND_UNREACHABLE",
  unavailableMessage: "admin score rules backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminScoreRuleCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminScoreRuleCollection(request, "POST", failure);
}
