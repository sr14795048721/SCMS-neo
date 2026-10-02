import { proxyAdminActivityAction } from "../../../../../../lib/api/adminActivityProxy";

const failure = {
  failureCode: "ADMIN_ACTIVITY_CLOSE_FAILED",
  failureMessage: "admin activity close request failed",
  unavailableCode: "ADMIN_ACTIVITY_CLOSE_BACKEND_UNREACHABLE",
  unavailableMessage: "admin activity close backend unavailable"
} as const;

export async function POST(request: Request, context: { params: { activityId: string } }) {
  return proxyAdminActivityAction(request, `/api/v1/admin/activities/${context.params.activityId}/close`, failure);
}
