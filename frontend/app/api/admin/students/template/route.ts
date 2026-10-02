import { proxyAdminStudentBinary } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_TEMPLATE_FAILED",
  failureMessage: "admin student template failed",
  unavailableCode: "ADMIN_STUDENT_TEMPLATE_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student template backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminStudentBinary(request, "/api/v1/admin/students/template", failure);
}
