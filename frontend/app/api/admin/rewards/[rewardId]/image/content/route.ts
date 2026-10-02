import { proxyAdminRewardImageBinary } from "../../../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_IMAGE_CONTENT_FAILED",
  failureMessage: "admin reward image content request failed",
  unavailableCode: "ADMIN_REWARD_IMAGE_CONTENT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward image content backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: Promise<{ rewardId: string }> }
) {
  const { rewardId } = await context.params;
  return proxyAdminRewardImageBinary(request, `/api/v1/admin/rewards/${rewardId}/image/content`, failure);
}
