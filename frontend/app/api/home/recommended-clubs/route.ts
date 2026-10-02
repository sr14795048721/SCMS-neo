import { proxyPublicRecommendedClubs } from "../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "HOME_RECOMMENDED_CLUBS_FAILED",
  failureMessage: "home recommended clubs request failed",
  unavailableCode: "HOME_RECOMMENDED_CLUBS_BACKEND_UNREACHABLE",
  unavailableMessage: "home recommended clubs backend unavailable"
} as const;

export async function GET() {
  return proxyPublicRecommendedClubs(failure);
}
