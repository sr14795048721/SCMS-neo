import { proxyAdminManagerCollection } from "../../../../lib/api/adminUserProxy";
import { proxyAdminManagerCreate } from "../../../../lib/api/adminUserProxy";

const failure = {
  failureCode: "ADMIN_MANAGER_REQUEST_FAILED",
  failureMessage: "admin manager request failed",
  unavailableCode: "ADMIN_MANAGER_BACKEND_UNREACHABLE",
  unavailableMessage: "admin manager backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminManagerCollection(request, failure);
}

export async function POST(request: Request) {
  return proxyAdminManagerCreate(request, failure);
}
