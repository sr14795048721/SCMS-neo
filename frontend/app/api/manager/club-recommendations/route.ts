import { proxyManagerRecommendations } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_CLUB_RECOMMENDATIONS_FAILED",
  failureMessage: "manager club recommendations request failed",
  unavailableCode: "MANAGER_CLUB_RECOMMENDATIONS_UNAVAILABLE",
  unavailableMessage: "manager club recommendations unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerRecommendations(request, "GET", FAILURE);
}

export async function PUT(request: Request) {
  return proxyManagerRecommendations(request, "PUT", FAILURE);
}
