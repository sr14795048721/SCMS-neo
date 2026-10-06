import {
  proxyAdminCompetitionSourceDelete,
  proxyAdminCompetitionSourceUpdate
} from "../../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITION_SOURCES_FAILED",
  failureMessage: "admin competition sources request failed",
  unavailableCode: "ADMIN_COMPETITION_SOURCES_UNAVAILABLE",
  unavailableMessage: "admin competition sources service unavailable"
} as const;

type Params = { params: Promise<{ sourceId: string }> };

export async function PATCH(request: Request, context: Params) {
  const { sourceId } = await context.params;
  return proxyAdminCompetitionSourceUpdate(request, sourceId, failure);
}

export async function DELETE(request: Request, context: Params) {
  const { sourceId } = await context.params;
  return proxyAdminCompetitionSourceDelete(request, sourceId, failure);
}
