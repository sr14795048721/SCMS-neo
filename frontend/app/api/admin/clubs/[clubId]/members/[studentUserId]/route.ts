import { proxyAdminClubMemberRemove } from "../../../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_MEMBER_REMOVE_FAILED",
  failureMessage: "admin club member remove request failed",
  unavailableCode: "ADMIN_CLUB_MEMBER_REMOVE_UNAVAILABLE",
  unavailableMessage: "admin club member remove backend unavailable"
} as const;

export async function DELETE(
  request: Request,
  context: { params: Promise<{ clubId: string; studentUserId: string }> }
) {
  const { clubId, studentUserId } = await context.params;
  return proxyAdminClubMemberRemove(
    request,
    `/api/v1/admin/clubs/${clubId}/members/${studentUserId}`,
    failure
  );
}
