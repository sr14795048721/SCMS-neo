import { proxyAdminAppReleaseCollection } from "../../../../lib/api/appReleaseProxy";

const FAILURE = {
  failureCode: "ADMIN_APP_RELEASES_FAILED",
  failureMessage: "admin app releases request failed",
  unavailableCode: "ADMIN_APP_RELEASES_UNAVAILABLE",
  unavailableMessage: "admin app releases service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminAppReleaseCollection(request, "/api/v1/admin/app-releases", "GET", FAILURE);
}

export async function POST(request: Request) {
  return proxyAdminAppReleaseCollection(request, "/api/v1/admin/app-releases", "POST", FAILURE);
}
