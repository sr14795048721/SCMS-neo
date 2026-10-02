import { proxyAdminAppReleaseMutation } from "../../../../../lib/api/appReleaseProxy";

const FAILURE = {
  failureCode: "ADMIN_APP_RELEASE_MUTATION_FAILED",
  failureMessage: "admin app release mutation failed",
  unavailableCode: "ADMIN_APP_RELEASE_MUTATION_UNAVAILABLE",
  unavailableMessage: "admin app release mutation service unavailable"
} as const;

export async function PATCH(
  request: Request,
  context: { params: Promise<{ releaseId: string }> }
) {
  const { releaseId } = await context.params;
  return proxyAdminAppReleaseMutation(request, `/api/v1/admin/app-releases/${releaseId}`, "PATCH", FAILURE);
}
