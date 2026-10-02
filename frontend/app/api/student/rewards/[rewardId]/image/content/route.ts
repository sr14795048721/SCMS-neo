import { proxyStudentRewardImageBinary } from "../../../../../../../lib/api/studentScoreRewardProxy";

const failure = {
  failureCode: "STUDENT_REWARD_IMAGE_FAILED",
  failureMessage: "student reward image request failed",
  unavailableCode: "STUDENT_REWARD_IMAGE_UNAVAILABLE",
  unavailableMessage: "student reward image service unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{ rewardId: string }>;
  }
) {
  const { rewardId } = await context.params;
  return proxyStudentRewardImageBinary(
    request,
    `/api/v1/students/me/rewards/${encodeURIComponent(rewardId)}/image/content`,
    failure
  );
}
