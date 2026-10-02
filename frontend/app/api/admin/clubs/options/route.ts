import { proxyAdminClubOptions } from "../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_OPTIONS_FAILED",
  failureMessage: "admin club options request failed",
  unavailableCode: "ADMIN_CLUB_OPTIONS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin club options backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminClubOptions(request, failure);
}
