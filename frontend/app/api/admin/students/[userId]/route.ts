import { proxyAdminStudentDetail } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_STUDENT_DETAIL_REQUEST_FAILED",
  failureMessage: "admin student detail request failed",
  unavailableCode: "ADMIN_STUDENT_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin student detail backend unavailable"
} as const;

export async function GET(request: Request, context: { params: { userId: string } }) {
  return proxyAdminStudentDetail(request, `/api/v1/admin/students/${context.params.userId}`, "GET", failure);
}

export async function PATCH(request: Request, context: { params: { userId: string } }) {
  return proxyAdminStudentDetail(request, `/api/v1/admin/students/${context.params.userId}`, "PATCH", failure);
}

export async function DELETE(request: Request, context: { params: { userId: string } }) {
  return proxyAdminStudentDetail(request, `/api/v1/admin/students/${context.params.userId}`, "DELETE", failure);
}
