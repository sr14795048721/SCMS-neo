import { proxyManagerClubDuties } from "../../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_DUTY_DETAIL_FAILED",
  failureMessage: "manager club duty mutation failed",
  unavailableCode: "MANAGER_CLUB_DUTY_DETAIL_UNAVAILABLE",
  unavailableMessage: "manager club duty backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ clubId: string; dutyId: string }> }
) {
  const { clubId, dutyId } = await context.params;
  return proxyManagerClubDuties(request, clubId, "PATCH", failure, dutyId);
}

export async function DELETE(
  request: Request,
  context: { params: Promise<{ clubId: string; dutyId: string }> }
) {
  const { clubId, dutyId } = await context.params;
  return proxyManagerClubDuties(request, clubId, "DELETE", failure, dutyId);
}
