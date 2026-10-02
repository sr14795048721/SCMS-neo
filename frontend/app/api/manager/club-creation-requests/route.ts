import { proxyClubCreationRequests } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_CLUB_CREATION_REQUESTS_FAILED",
  failureMessage: "manager club creation requests failed",
  unavailableCode: "MANAGER_CLUB_CREATION_REQUESTS_UNAVAILABLE",
  unavailableMessage: "manager club creation requests unavailable"
} as const;

export async function GET(request: Request) {
  return proxyClubCreationRequests(request, "/api/v1/managers/me/club-creation-requests", "GET", FAILURE);
}

export async function POST(request: Request) {
  return proxyClubCreationRequests(request, "/api/v1/managers/me/club-creation-requests", "POST", FAILURE);
}
