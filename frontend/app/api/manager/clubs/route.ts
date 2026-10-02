import { proxyManagerClubCollection } from "../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUBS_REQUEST_FAILED",
  failureMessage: "manager clubs request failed",
  unavailableCode: "MANAGER_CLUBS_BACKEND_UNREACHABLE",
  unavailableMessage: "manager clubs backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerClubCollection(request, failure);
}
