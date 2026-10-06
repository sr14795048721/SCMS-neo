import { proxyAdminCompetitionLeadConfirm } from "../../../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITION_LEADS_FAILED",
  failureMessage: "admin competition leads request failed",
  unavailableCode: "ADMIN_COMPETITION_LEADS_UNAVAILABLE",
  unavailableMessage: "admin competition leads service unavailable"
} as const;

type Params = { params: Promise<{ leadId: string }> };

export async function POST(request: Request, context: Params) {
  const { leadId } = await context.params;
  return proxyAdminCompetitionLeadConfirm(request, leadId, failure);
}
