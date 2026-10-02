import { proxyManagerClubMembers } from "../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_MEMBERS_REQUEST_FAILED",
  failureMessage: "manager club members request failed",
  unavailableCode: "MANAGER_CLUB_MEMBERS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club members backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerClubMembers(request, clubId, failure);
}
