import { proxyAdminActivityAction } from "../../../../../../lib/api/adminActivityProxy";

const failure = {
  failureCode: "ADMIN_ACTIVITY_PUBLISH_FAILED",
  failureMessage: "admin activity publish request failed",
  unavailableCode: "ADMIN_ACTIVITY_PUBLISH_BACKEND_UNREACHABLE",
  unavailableMessage: "admin activity publish backend unavailable"
} as const;

export async function POST(request: Request, context: { params: { activityId: string } }) {
  return proxyAdminActivityAction(request, `/api/v1/admin/activities/${context.params.activityId}/publish`, failure);
}
