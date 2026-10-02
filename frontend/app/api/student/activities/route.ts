import { proxyStudentActivities } from "../../../../lib/api/studentActivityProxy";

const failure = {
  failureCode: "STUDENT_ACTIVITIES_FAILED",
  failureMessage: "student activities request failed",
  unavailableCode: "STUDENT_ACTIVITIES_UNAVAILABLE",
  unavailableMessage: "student activities service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentActivities(request, failure);
}
