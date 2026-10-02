import { proxyAdminClubDutyMutation } from "../../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_DUTIES_FAILED",
  failureMessage: "admin club duties request failed",
  unavailableCode: "ADMIN_CLUB_DUTIES_UNAVAILABLE",
  unavailableMessage: "admin club duties backend unavailable"
} as const;

export async function GET(request: Request, context: { params: Promise<{ clubId: string }> }) {
  const { clubId } = await context.params;
  return proxyAdminClubDutyMutation(request, `/api/v1/admin/clubs/${clubId}/duties`, "GET", failure);
}

export async function POST(request: Request, context: { params: Promise<{ clubId: string }> }) {
  const { clubId } = await context.params;
  return proxyAdminClubDutyMutation(request, `/api/v1/admin/clubs/${clubId}/duties`, "POST", failure);
}
