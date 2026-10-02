import { proxyAdminAppReleaseMutation } from "../../../../../../lib/api/appReleaseProxy";

const FAILURE = {
  failureCode: "ADMIN_APP_RELEASE_PUBLISH_FAILED",
  failureMessage: "admin app release publish failed",
  unavailableCode: "ADMIN_APP_RELEASE_PUBLISH_UNAVAILABLE",
  unavailableMessage: "admin app release publish service unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: Promise<{ releaseId: string }> }
) {
  const { releaseId } = await context.params;
  return proxyAdminAppReleaseMutation(request, `/api/v1/admin/app-releases/${releaseId}/publish`, "POST", FAILURE);
}
