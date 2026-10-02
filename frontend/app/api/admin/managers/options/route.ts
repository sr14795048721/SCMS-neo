import { proxyAdminManagerOptions } from "../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_MANAGER_OPTIONS_FAILED",
  failureMessage: "admin manager options request failed",
  unavailableCode: "ADMIN_MANAGER_OPTIONS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin manager options backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminManagerOptions(request, failure);
}
