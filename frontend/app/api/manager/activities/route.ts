import { proxyManagerActivityCollection } from "../../../../lib/api/managerActivityProxy";

const failure = {
  failureCode: "MANAGER_ACTIVITIES_REQUEST_FAILED",
  failureMessage: "manager activities request failed",
  unavailableCode: "MANAGER_ACTIVITIES_BACKEND_UNREACHABLE",
  unavailableMessage: "manager activities backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerActivityCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyManagerActivityCollection(request, "POST", failure);
}
