import { proxyAdminNewsCollection } from "../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_REQUEST_FAILED",
  failureMessage: "admin news request failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminNewsCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminNewsCollection(request, "POST", failure);
}
