import {
  proxyAdminVersionChangelogCreate,
  proxyVersionChangelogs
} from "../../../../lib/api/versionChangelogProxy";

const failure = {
  failureCode: "ADMIN_VERSION_CHANGELOGS_FAILED",
  failureMessage: "admin version changelogs request failed",
  unavailableCode: "ADMIN_VERSION_CHANGELOGS_UNAVAILABLE",
  unavailableMessage: "admin version changelogs service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyVersionChangelogs(request, failure);
}

export async function POST(request: Request) {
  return proxyAdminVersionChangelogCreate(request, failure);
}
