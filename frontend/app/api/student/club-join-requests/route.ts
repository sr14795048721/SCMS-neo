import { proxyStudentClubJoinRequest } from "../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "STUDENT_CLUB_JOIN_REQUEST_FAILED",
  failureMessage: "student club join request failed",
  unavailableCode: "STUDENT_CLUB_JOIN_BACKEND_UNREACHABLE",
  unavailableMessage: "student club join backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyStudentClubJoinRequest(request, failure);
}
