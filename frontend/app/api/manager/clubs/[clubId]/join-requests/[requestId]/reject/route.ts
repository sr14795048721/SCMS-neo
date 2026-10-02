import { proxyManagerClubJoinRequests } from "../../../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_JOIN_REJECT_FAILED",
  failureMessage: "manager club join reject failed",
  unavailableCode: "MANAGER_CLUB_JOIN_REJECT_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club join reject backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      requestId: string;
    }>;
  }
) {
  const { clubId, requestId } = await context.params;
  return proxyManagerClubJoinRequests(request, clubId, "reject", requestId, failure);
}
