import { proxyAdminSystemLogout } from "../../../../../lib/api/adminSystemProxy";

const failure = {
  failureCode: "ADMIN_SYSTEM_LOGOUT_FAILED",
  failureMessage: "admin system logout request failed",
  unavailableCode: "ADMIN_SYSTEM_LOGOUT_BACKEND_UNREACHABLE",
  unavailableMessage: "admin system logout backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyAdminSystemLogout(request, failure);
}
