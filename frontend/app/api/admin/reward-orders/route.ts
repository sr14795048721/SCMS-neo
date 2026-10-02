import { proxyAdminRewardOrders } from "../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_ORDERS_FAILED",
  failureMessage: "admin reward orders request failed",
  unavailableCode: "ADMIN_REWARD_ORDERS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward orders backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminRewardOrders(request, failure);
}
