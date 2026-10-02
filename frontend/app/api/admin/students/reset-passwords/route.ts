import { proxyAdminStudentResetPasswords } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_PASSWORD_RESET_FAILED",
  failureMessage: "admin student password reset failed",
  unavailableCode: "ADMIN_STUDENT_PASSWORD_RESET_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student password reset backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyAdminStudentResetPasswords(request, failure);
}
