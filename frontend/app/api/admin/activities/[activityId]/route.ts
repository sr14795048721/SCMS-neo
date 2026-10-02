import { proxyAdminActivityDetail } from "../../../../../lib/api/adminActivityProxy";

const failure = {
  failureCode: "ADMIN_ACTIVITY_DETAIL_FAILED",
  failureMessage: "admin activity detail request failed",
  unavailableCode: "ADMIN_ACTIVITY_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin activity detail backend unavailable"
} as const;

export async function GET(request: Request, context: { params: { activityId: string } }) {
  return proxyAdminActivityDetail(request, `/api/v1/admin/activities/${context.params.activityId}`, "GET", failure);
}

export async function PATCH(request: Request, context: { params: { activityId: string } }) {
  return proxyAdminActivityDetail(request, `/api/v1/admin/activities/${context.params.activityId}`, "PATCH", failure);
}

export async function DELETE(request: Request, context: { params: { activityId: string } }) {
  return proxyAdminActivityDetail(request, `/api/v1/admin/activities/${context.params.activityId}`, "DELETE", failure);
}
