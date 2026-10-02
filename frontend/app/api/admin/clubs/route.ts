import { proxyAdminClubCollection } from "../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_COLLECTION_FAILED",
  failureMessage: "admin club request failed",
  unavailableCode: "ADMIN_CLUB_BACKEND_UNREACHABLE",
  unavailableMessage: "admin club backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminClubCollection(request, "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminClubCollection(request, "POST", failure);
}
