import { proxyAdminActivityCollection } from "../../../../lib/api/adminActivityProxy";

const failure = {
  failureCode: "ADMIN_ACTIVITY_COLLECTION_FAILED",
  failureMessage: "admin activity request failed",
  unavailableCode: "ADMIN_ACTIVITY_BACKEND_UNREACHABLE",
  unavailableMessage: "admin activity backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminActivityCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminActivityCollection(request, "POST", failure);
}
