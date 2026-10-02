import { proxyManagerScoreRequest } from "../../../../../../lib/api/managerScoreProxy";

const failure = {
  failureCode: "MANAGER_SCORE_RANKINGS_REQUEST_FAILED",
  failureMessage: "manager score rankings request failed",
  unavailableCode: "MANAGER_SCORE_RANKINGS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager score rankings backend unavailable"
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
  return proxyManagerScoreRequest(request, `/api/v1/managers/me/clubs/${clubId}/score-rankings`, "GET", failure);
}
