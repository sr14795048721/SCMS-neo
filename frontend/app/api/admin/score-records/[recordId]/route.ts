import { proxyAdminScoreRecordDelete } from "../../../../../lib/api/adminScoreRewardProxy";

const failure = {
  failureCode: "ADMIN_SCORE_RECORD_DELETE_FAILED",
  failureMessage: "admin score record delete request failed",
  unavailableCode: "ADMIN_SCORE_RECORD_DELETE_BACKEND_UNREACHABLE",
  unavailableMessage: "admin score record backend unavailable"
} as const;

export async function DELETE(
  request: Request,
  context: { params: Promise<{ recordId: string }> }
) {
  const { recordId } = await context.params;
  return proxyAdminScoreRecordDelete(request, recordId, failure);
}
