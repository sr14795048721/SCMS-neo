import { proxyAdminSystemPassword } from "../../../../../lib/api/adminSystemProxy";

const failure = {
  failureCode: "ADMIN_SYSTEM_PASSWORD_FAILED",
  failureMessage: "admin system password request failed",
  unavailableCode: "ADMIN_SYSTEM_PASSWORD_BACKEND_UNREACHABLE",
  unavailableMessage: "admin system password backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyAdminSystemPassword(request, failure);
}
