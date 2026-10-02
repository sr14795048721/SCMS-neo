import { proxyClubCreationRequests } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "ADMIN_CLUB_CREATION_REQUESTS_FAILED",
  failureMessage: "admin club creation requests failed",
  unavailableCode: "ADMIN_CLUB_CREATION_REQUESTS_UNAVAILABLE",
  unavailableMessage: "admin club creation requests unavailable"
} as const;

export async function GET(request: Request) {
  return proxyClubCreationRequests(request, "/api/v1/admin/club-creation-requests", "GET", FAILURE);
}
