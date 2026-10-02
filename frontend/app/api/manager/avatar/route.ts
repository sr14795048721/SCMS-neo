import { proxyManagerAvatarGet, proxyManagerAvatarUpload } from "../../../../lib/api/managerProxy";

const failure = {
  failureCode: "MANAGER_AVATAR_REQUEST_FAILED",
  failureMessage: "manager avatar request failed",
  unavailableCode: "MANAGER_AVATAR_BACKEND_UNREACHABLE",
  unavailableMessage: "manager avatar backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyManagerAvatarGet(request, failure);
}

export async function POST(request: Request) {
  return proxyManagerAvatarUpload(request, failure);
}
