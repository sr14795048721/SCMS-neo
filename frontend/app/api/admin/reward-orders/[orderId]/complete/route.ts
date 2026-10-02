import { proxyAdminRewardOrderMutation } from "../../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_ORDER_COMPLETE_FAILED",
  failureMessage: "admin reward order complete request failed",
  unavailableCode: "ADMIN_REWARD_ORDER_COMPLETE_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward order complete backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: Promise<{ orderId: string }> }
) {
  const { orderId } = await context.params;
  return proxyAdminRewardOrderMutation(request, `/api/v1/admin/reward-orders/${orderId}/complete`, "POST", failure);
}
