import { proxyManagerScoreRequest } from "../../../../../../lib/api/managerScoreProxy";

const failure = {
  failureCode: "MANAGER_SCORE_RECORDS_REQUEST_FAILED",
  failureMessage: "manager score records request failed",
  unavailableCode: "MANAGER_SCORE_RECORDS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager score records backend unavailable"
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
  return proxyManagerScoreRequest(request, `/api/v1/managers/me/clubs/${clubId}/score-records`, "GET", failure);
}

export async function POST(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerScoreRequest(request, `/api/v1/managers/me/clubs/${clubId}/score-records`, "POST", failure);
}
