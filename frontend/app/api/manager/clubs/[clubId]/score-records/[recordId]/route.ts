import { proxyManagerScoreRequest } from "../../../../../../../lib/api/managerScoreProxy";

const failure = {
  failureCode: "MANAGER_SCORE_RECORD_DELETE_REQUEST_FAILED",
  failureMessage: "manager score record delete request failed",
  unavailableCode: "MANAGER_SCORE_RECORD_DELETE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager score record delete backend unavailable"
} as const;

export async function DELETE(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      recordId: string;
    }>;
  }
) {
  const { clubId, recordId } = await context.params;
  return proxyManagerScoreRequest(
    request,
    `/api/v1/managers/me/clubs/${clubId}/score-records/${recordId}`,
    "DELETE",
    failure
  );
}
