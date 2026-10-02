import { proxyManagerPassword } from "../../../../lib/api/managerProxy";

const failure = {
  failureCode: "MANAGER_PASSWORD_REQUEST_FAILED",
  failureMessage: "manager password request failed",
  unavailableCode: "MANAGER_PASSWORD_BACKEND_UNREACHABLE",
  unavailableMessage: "manager password backend unavailable"
} as const;

export async function POST(request: Request) {
  return proxyManagerPassword(request, failure);
}
