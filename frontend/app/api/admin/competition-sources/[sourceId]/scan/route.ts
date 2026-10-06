import { proxyAdminCompetitionSourceScan } from "../../../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITION_SOURCES_FAILED",
  failureMessage: "admin competition sources request failed",
  unavailableCode: "ADMIN_COMPETITION_SOURCES_UNAVAILABLE",
  unavailableMessage: "admin competition sources service unavailable"
} as const;

type Params = { params: Promise<{ sourceId: string }> };

export async function POST(request: Request, context: Params) {
  const { sourceId } = await context.params;
  return proxyAdminCompetitionSourceScan(request, sourceId, failure);
}
