import { proxyAdminSystemDirtyDataCleanup } from "../../../../../../lib/api/adminSystemProxy";

const failure = {
  failureCode: "ADMIN_SYSTEM_DIRTY_DATA_CLEANUP_FAILED",
  failureMessage: "admin dirty data cleanup request failed",
  unavailableCode: "ADMIN_SYSTEM_DIRTY_DATA_CLEANUP_BACKEND_UNREACHABLE",
  unavailableMessage: "admin dirty data cleanup backend unavailable"
} as const;

export async function DELETE(request: Request) {
  return proxyAdminSystemDirtyDataCleanup(request, failure);
}
