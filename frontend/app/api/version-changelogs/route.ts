import { proxyVersionChangelogs } from "../../../lib/api/versionChangelogProxy";

const failure = {
  failureCode: "VERSION_CHANGELOGS_FAILED",
  failureMessage: "version changelogs request failed",
  unavailableCode: "VERSION_CHANGELOGS_UNAVAILABLE",
  unavailableMessage: "version changelogs service unavailable"
} as const;

export async function GET(request: Request) {
  return proxyVersionChangelogs(request, failure);
}
