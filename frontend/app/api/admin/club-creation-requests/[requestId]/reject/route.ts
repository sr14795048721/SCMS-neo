import { proxyClubCreationRequests } from "../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "ADMIN_CLUB_CREATION_REJECT_FAILED",
  failureMessage: "club creation reject failed",
  unavailableCode: "ADMIN_CLUB_CREATION_REJECT_UNAVAILABLE",
  unavailableMessage: "club creation reject unavailable"
} as const;

export async function POST(request: Request, context: { params: Promise<{ requestId: string }> }) {
  const { requestId } = await context.params;
  return proxyClubCreationRequests(request, `/api/v1/admin/club-creation-requests/${requestId}/reject`, "POST", FAILURE);
}
