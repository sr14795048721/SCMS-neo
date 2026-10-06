import {
  proxyAdminCompetitionDelete,
  proxyAdminCompetitionUpdate
} from "../../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITIONS_FAILED",
  failureMessage: "admin competitions request failed",
  unavailableCode: "ADMIN_COMPETITIONS_UNAVAILABLE",
  unavailableMessage: "admin competitions service unavailable"
} as const;

type Params = { params: Promise<{ competitionId: string }> };

export async function PATCH(request: Request, context: Params) {
  const { competitionId } = await context.params;
  return proxyAdminCompetitionUpdate(request, competitionId, failure);
}

export async function DELETE(request: Request, context: Params) {
  const { competitionId } = await context.params;
  return proxyAdminCompetitionDelete(request, competitionId, failure);
}
