import { proxyAdminRewardDetail } from "../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_DETAIL_FAILED",
  failureMessage: "admin reward detail request failed",
  unavailableCode: "ADMIN_REWARD_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward detail backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ rewardId: string }> }
) {
  const { rewardId } = await context.params;
  return proxyAdminRewardDetail(request, `/api/v1/admin/rewards/${rewardId}`, "PATCH", failure);
}

export async function DELETE(
  request: Request,
  context: { params: Promise<{ rewardId: string }> }
) {
  const { rewardId } = await context.params;
  return proxyAdminRewardDetail(request, `/api/v1/admin/rewards/${rewardId}`, "DELETE", failure);
}
