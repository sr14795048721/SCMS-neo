import { proxyAdminStudentBinary } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_EXPORT_FAILED",
  failureMessage: "admin student export failed",
  unavailableCode: "ADMIN_STUDENT_EXPORT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student export backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminStudentBinary(request, "/api/v1/admin/students/export", failure);
}
