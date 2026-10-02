import { proxyStudentDiscoverClubs } from "../../../../lib/api/studentClubProxy";

const failure = {
  failureCode: "STUDENT_DISCOVER_CLUBS_FAILED",
  failureMessage: "student discover clubs request failed",
  unavailableCode: "STUDENT_DISCOVER_CLUBS_UNAVAILABLE",
  unavailableMessage: "student discover clubs service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentDiscoverClubs(request, failure);
}
