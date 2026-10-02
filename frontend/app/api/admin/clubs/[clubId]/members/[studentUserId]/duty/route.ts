import { proxyAdminClubMemberDuty } from "../../../../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_MEMBER_DUTY_FAILED",
  failureMessage: "admin club member duty request failed",
  unavailableCode: "ADMIN_CLUB_MEMBER_DUTY_UNAVAILABLE",
  unavailableMessage: "admin club member duty backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ clubId: string; studentUserId: string }> }
) {
  const { clubId, studentUserId } = await context.params;
  return proxyAdminClubMemberDuty(
    request,
    `/api/v1/admin/clubs/${clubId}/members/${studentUserId}/duty`,
    failure
  );
}
