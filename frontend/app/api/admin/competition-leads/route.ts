import { proxyAdminCompetitionLeads } from "../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITION_LEADS_FAILED",
  failureMessage: "admin competition leads request failed",
  unavailableCode: "ADMIN_COMPETITION_LEADS_UNAVAILABLE",
  unavailableMessage: "admin competition leads service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminCompetitionLeads(request, failure);
}
