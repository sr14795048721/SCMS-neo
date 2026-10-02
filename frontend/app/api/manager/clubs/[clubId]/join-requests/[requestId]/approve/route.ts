import { proxyManagerClubJoinRequests } from "../../../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_JOIN_APPROVE_FAILED",
  failureMessage: "manager club join approve failed",
  unavailableCode: "MANAGER_CLUB_JOIN_APPROVE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club join approve backend unavailable"
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
  return proxyManagerClubJoinRequests(request, clubId, "approve", requestId, failure);
}
