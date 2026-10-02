import { proxyAdminAppReleaseMutation } from "../../../../../../lib/api/appReleaseProxy";

const FAILURE = {
  failureCode: "ADMIN_APP_RELEASE_RETIRE_FAILED",
  failureMessage: "admin app release retire failed",
  unavailableCode: "ADMIN_APP_RELEASE_RETIRE_UNAVAILABLE",
  unavailableMessage: "admin app release retire service unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: Promise<{ releaseId: string }> }
) {
  const { releaseId } = await context.params;
  return proxyAdminAppReleaseMutation(request, `/api/v1/admin/app-releases/${releaseId}/retire`, "POST", FAILURE);
}
