import { proxyManagerActivityDetail } from "../../../../../../lib/api/managerActivityProxy";

const failure = {
  failureCode: "MANAGER_ACTIVITY_CLOSE_FAILED",
  failureMessage: "manager activity close failed",
  unavailableCode: "MANAGER_ACTIVITY_CLOSE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager activity close backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: {
    params: Promise<{
      activityId: string;
    }>;
  }
) {
  const { activityId } = await context.params;
  return proxyManagerActivityDetail(request, `/api/v1/managers/me/activities/${activityId}/close`, "POST", failure);
}
