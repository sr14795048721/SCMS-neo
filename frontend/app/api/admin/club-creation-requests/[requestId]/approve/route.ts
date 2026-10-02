import { proxyClubCreationRequests } from "../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "ADMIN_CLUB_CREATION_APPROVE_FAILED",
  failureMessage: "club creation approve failed",
  unavailableCode: "ADMIN_CLUB_CREATION_APPROVE_UNAVAILABLE",
  unavailableMessage: "club creation approve unavailable"
} as const;

export async function POST(request: Request, context: { params: Promise<{ requestId: string }> }) {
  const { requestId } = await context.params;
  return proxyClubCreationRequests(request, `/api/v1/admin/club-creation-requests/${requestId}/approve`, "POST", FAILURE);
}
