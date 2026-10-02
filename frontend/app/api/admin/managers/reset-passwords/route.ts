import { proxyAdminManagerResetPasswords } from "../../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_MANAGER_PASSWORD_RESET_FAILED",
  failureMessage: "admin manager password reset failed",
  unavailableCode: "ADMIN_MANAGER_PASSWORD_RESET_BACKEND_UNREACHABLE",
  unavailableMessage: "admin manager password reset backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyAdminManagerResetPasswords(request, failure);
}
