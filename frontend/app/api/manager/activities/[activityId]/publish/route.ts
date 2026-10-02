import { proxyManagerActivityDetail } from "../../../../../../lib/api/managerActivityProxy";

const failure = {
  failureCode: "MANAGER_ACTIVITY_PUBLISH_FAILED",
  failureMessage: "manager activity publish failed",
  unavailableCode: "MANAGER_ACTIVITY_PUBLISH_BACKEND_UNREACHABLE",
  unavailableMessage: "manager activity publish backend unavailable"
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
  return proxyManagerActivityDetail(request, `/api/v1/managers/me/activities/${activityId}/publish`, "POST", failure);
}
