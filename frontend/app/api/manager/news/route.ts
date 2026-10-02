import { proxyManagerNewsCollection } from "../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_REQUEST_FAILED",
  failureMessage: "manager news request failed",
  unavailableCode: "MANAGER_NEWS_SERVICE_UNAVAILABLE",
  unavailableMessage: "manager news service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerNewsCollection(request, "GET", FAILURE);
}

export async function POST(request: Request) {
  return proxyManagerNewsCollection(request, "POST", FAILURE);
}
