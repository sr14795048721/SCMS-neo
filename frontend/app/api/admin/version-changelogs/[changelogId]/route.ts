import {
  proxyAdminVersionChangelogDelete,
  proxyAdminVersionChangelogUpdate
} from "../../../../../lib/api/versionChangelogProxy";

const failure = {
  failureCode: "ADMIN_VERSION_CHANGELOGS_FAILED",
  failureMessage: "admin version changelogs request failed",
  unavailableCode: "ADMIN_VERSION_CHANGELOGS_UNAVAILABLE",
  unavailableMessage: "admin version changelogs service unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ changelogId: string }> }
) {
  const { changelogId } = await context.params;
  return proxyAdminVersionChangelogUpdate(request, changelogId, failure);
}

export async function DELETE(
  request: Request,
  context: { params: Promise<{ changelogId: string }> }
) {
  const { changelogId } = await context.params;
  return proxyAdminVersionChangelogDelete(request, changelogId, failure);
}
