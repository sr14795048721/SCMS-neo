import { proxyAdminStudentImport } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_IMPORT_FAILED",
  failureMessage: "admin student import failed",
  unavailableCode: "ADMIN_STUDENT_IMPORT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student import backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyAdminStudentImport(request, failure);
}
