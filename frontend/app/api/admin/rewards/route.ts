import { proxyAdminRewardCollection } from "../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARDS_FAILED",
  failureMessage: "admin rewards request failed",
  unavailableCode: "ADMIN_REWARDS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin rewards backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminRewardCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminRewardCollection(request, "POST", failure);
}
