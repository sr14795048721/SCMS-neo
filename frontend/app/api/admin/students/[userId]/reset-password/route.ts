import { proxyAdminSingleStudentResetPassword } from "../../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_SINGLE_STUDENT_PASSWORD_RESET_FAILED",
  failureMessage: "admin single student password reset failed",
  unavailableCode: "ADMIN_SINGLE_STUDENT_PASSWORD_RESET_BACKEND_UNREACHABLE",
  unavailableMessage: "admin single student password reset backend unavailable"
} as const;

export async function POST(request: Request, context: { params: { userId: string } }) {
  return proxyAdminSingleStudentResetPassword(
    request,
    `/api/v1/admin/students/${context.params.userId}/reset-password`,
    failure
  );
}
