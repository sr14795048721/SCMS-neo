import { proxyStudentPassword } from "../../../../lib/api/studentProxy";

const failure = {
  failureCode: "STUDENT_PASSWORD_REQUEST_FAILED",
  failureMessage: "student password request failed",
  unavailableCode: "STUDENT_PASSWORD_BACKEND_UNREACHABLE",
  unavailableMessage: "student password backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyStudentPassword(request, failure);
}

