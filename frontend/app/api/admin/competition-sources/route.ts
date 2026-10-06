import {
  proxyAdminCompetitionSourceCreate,
  proxyAdminCompetitionSources
} from "../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITION_SOURCES_FAILED",
  failureMessage: "admin competition sources request failed",
  unavailableCode: "ADMIN_COMPETITION_SOURCES_UNAVAILABLE",
  unavailableMessage: "admin competition sources service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminCompetitionSources(request, failure);
}

export async function POST(request: Request) {
  return proxyAdminCompetitionSourceCreate(request, failure);
}
