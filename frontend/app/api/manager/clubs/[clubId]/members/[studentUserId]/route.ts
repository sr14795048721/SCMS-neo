import { proxyManagerClubMemberRemove } from "../../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_MEMBER_REMOVE_FAILED",
  failureMessage: "manager club member remove request failed",
  unavailableCode: "MANAGER_CLUB_MEMBER_REMOVE_UNAVAILABLE",
  unavailableMessage: "manager club member remove backend unavailable"
} as const;

export async function DELETE(
  request: Request,
  context: { params: Promise<{ clubId: string; studentUserId: string }> }
) {
  const { clubId, studentUserId } = await context.params;
  return proxyManagerClubMemberRemove(request, clubId, studentUserId, failure);
}
