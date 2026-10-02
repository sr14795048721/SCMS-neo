import { proxyManagerClubMemberDuty } from "../../../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_MEMBER_DUTY_FAILED",
  failureMessage: "manager club member duty request failed",
  unavailableCode: "MANAGER_CLUB_MEMBER_DUTY_UNAVAILABLE",
  unavailableMessage: "manager club member duty backend unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ clubId: string; studentUserId: string }> }
) {
  const { clubId, studentUserId } = await context.params;
  return proxyManagerClubMemberDuty(request, clubId, studentUserId, failure);
}
