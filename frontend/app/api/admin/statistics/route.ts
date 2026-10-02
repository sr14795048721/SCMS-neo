import { proxyAdminStatistics } from "../../../../lib/api/adminSystemProxy";

const failure = {
  failureCode: "ADMIN_STATISTICS_FAILED",
  failureMessage: "admin statistics request failed",
  unavailableCode: "ADMIN_STATISTICS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin statistics backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminStatistics(request, failure);
}
