import { proxyAdminClubRecommendations } from "../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_RECOMMENDATIONS_FAILED",
  failureMessage: "admin club recommendations request failed",
  unavailableCode: "ADMIN_CLUB_RECOMMENDATIONS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin club recommendations backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminClubRecommendations(request, "GET", failure);
}

export async function PUT(request: Request) {
  return proxyAdminClubRecommendations(request, "PUT", failure);
}
