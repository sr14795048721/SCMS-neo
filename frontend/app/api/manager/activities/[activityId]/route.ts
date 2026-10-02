import { proxyManagerActivityDetail } from "../../../../../lib/api/managerActivityProxy";

const failure = {
  failureCode: "MANAGER_ACTIVITY_UPDATE_FAILED",
  failureMessage: "manager activity update failed",
  unavailableCode: "MANAGER_ACTIVITY_UPDATE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager activity update backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: {
    params: Promise<{
      activityId: string;
    }>;
  }
) {
  const { activityId } = await context.params;
  return proxyManagerActivityDetail(request, `/api/v1/managers/me/activities/${activityId}`, "PATCH", failure);
}
