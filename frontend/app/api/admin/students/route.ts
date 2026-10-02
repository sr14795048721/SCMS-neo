import { proxyAdminStudentCollection } from "../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_REQUEST_FAILED",
  failureMessage: "admin student request failed",
  unavailableCode: "ADMIN_STUDENT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminStudentCollection(request, failure);
}
