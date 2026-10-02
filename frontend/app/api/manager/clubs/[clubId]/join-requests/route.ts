import { proxyManagerClubJoinRequests } from "../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_JOIN_REQUESTS_FAILED",
  failureMessage: "manager club join requests failed",
  unavailableCode: "MANAGER_CLUB_JOIN_REQUESTS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club join requests backend unavailable"
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
  return proxyManagerClubJoinRequests(request, clubId, "list", null, failure);
}
