import { proxyStudentInfo } from "../../../../lib/api/studentProxy";

const failure = {
  failureCode: "STUDENT_INFO_REQUEST_FAILED",
  failureMessage: "student info request failed",
  unavailableCode: "STUDENT_INFO_BACKEND_UNREACHABLE",
  unavailableMessage: "student info backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyStudentInfo(request, "GET", failure);
}

export async function PATCH(request: Request) {
  return proxyStudentInfo(request, "PATCH", failure);
}

