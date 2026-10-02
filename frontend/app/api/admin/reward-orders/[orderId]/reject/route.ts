import { proxyAdminRewardOrderMutation } from "../../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_ORDER_REJECT_FAILED",
  failureMessage: "admin reward order reject request failed",
  unavailableCode: "ADMIN_REWARD_ORDER_REJECT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward order reject backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: {
    params: Promise<{ orderId: string }>;
  }
) {
  const { orderId } = await context.params;
  return proxyAdminRewardOrderMutation(request, `/api/v1/admin/reward-orders/${orderId}/reject`, "POST", failure);
}
