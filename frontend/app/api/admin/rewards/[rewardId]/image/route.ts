import { proxyAdminRewardImageUpload } from "../../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_REWARD_IMAGE_UPLOAD_FAILED",
  failureMessage: "admin reward image upload failed",
  unavailableCode: "ADMIN_REWARD_IMAGE_UPLOAD_BACKEND_UNREACHABLE",
  unavailableMessage: "admin reward image backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: Promise<{ rewardId: string }> }
) {
  const { rewardId } = await context.params;
  return proxyAdminRewardImageUpload(request, `/api/v1/admin/rewards/${rewardId}/image`, failure);
}
