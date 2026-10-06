import {
  proxyAdminCompetitionCreate,
  proxyAdminCompetitions
} from "../../../../lib/api/competitionProxy";

const failure = {
  failureCode: "ADMIN_COMPETITIONS_FAILED",
  failureMessage: "admin competitions request failed",
  unavailableCode: "ADMIN_COMPETITIONS_UNAVAILABLE",
  unavailableMessage: "admin competitions service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminCompetitions(request, failure);
}

export async function POST(request: Request) {
  return proxyAdminCompetitionCreate(request, failure);
}
