import { proxyAdminManagerDetail } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_MANAGER_DETAIL_REQUEST_FAILED",
  failureMessage: "admin manager detail request failed",
  unavailableCode: "ADMIN_MANAGER_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin manager detail backend unavailable"
} as const;

export async function GET(request: Request, context: { params: { userId: string } }) {
  return proxyAdminManagerDetail(request, `/api/v1/admin/managers/${context.params.userId}`, "GET", failure);
}

export async function PATCH(request: Request, context: { params: { userId: string } }) {
  return proxyAdminManagerDetail(request, `/api/v1/admin/managers/${context.params.userId}`, "PATCH", failure);
}

export async function DELETE(request: Request, context: { params: { userId: string } }) {
  return proxyAdminManagerDetail(request, `/api/v1/admin/managers/${context.params.userId}`, "DELETE", failure);
}
