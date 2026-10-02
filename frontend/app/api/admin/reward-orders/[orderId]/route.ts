import { proxyAdminRewardOrderMutation } from "../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_ORDER_DETAIL_FAILED",
  failureMessage: "admin reward order detail request failed",
  unavailableCode: "ADMIN_REWARD_ORDER_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward order detail backend unavailable"
} as const;

export async function DELETE(
  request: Request,
  context: { params: Promise<{ orderId: string }> }
) {
  const { orderId } = await context.params;
  return proxyAdminRewardOrderMutation(request, `/api/v1/admin/reward-orders/${orderId}`, "DELETE", failure);
}
