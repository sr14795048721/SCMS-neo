import { proxyManagerInfo } from "../../../../lib/api/managerProxy";

const failure = {
  failureCode: "MANAGER_INFO_REQUEST_FAILED",
  failureMessage: "manager info request failed",
  unavailableCode: "MANAGER_INFO_BACKEND_UNREACHABLE",
  unavailableMessage: "manager info backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerInfo(request, "GET", failure);
}

export async function PATCH(request: Request) {
  return proxyManagerInfo(request, "PATCH", failure);
}
