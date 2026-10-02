import { proxyAdminClubDutyMutation } from "../../../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_DUTY_DETAIL_FAILED",
  failureMessage: "admin club duty mutation failed",
  unavailableCode: "ADMIN_CLUB_DUTY_DETAIL_UNAVAILABLE",
  unavailableMessage: "admin club duty backend unavailable"
} as const;

export async function PATCH(request: Request, context: { params: Promise<{ clubId: string; dutyId: string }> }) {
  const { clubId, dutyId } = await context.params;
  return proxyAdminClubDutyMutation(
    request,
    `/api/v1/admin/clubs/${clubId}/duties/${dutyId}`,
    "PATCH",
    failure
  );
}

export async function DELETE(request: Request, context: { params: Promise<{ clubId: string; dutyId: string }> }) {
  const { clubId, dutyId } = await context.params;
  return proxyAdminClubDutyMutation(
    request,
    `/api/v1/admin/clubs/${clubId}/duties/${dutyId}`,
    "DELETE",
    failure
  );
}
