import { proxyManagerClubDetail } from "../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_DETAIL_REQUEST_FAILED",
  failureMessage: "manager club detail request failed",
  unavailableCode: "MANAGER_CLUB_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club detail backend unavailable"
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
  return proxyManagerClubDetail(request, clubId, failure);
}
