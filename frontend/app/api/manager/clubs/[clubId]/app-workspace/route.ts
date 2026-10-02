import { proxyManagerClubAppWorkspace } from "../../../../../../lib/api/managerClubAppWorkspaceProxy";

const failure = {
  failureCode: "MANAGER_CLUB_APP_WORKSPACE_REQUEST_FAILED",
  failureMessage: "manager club app workspace request failed",
  unavailableCode: "MANAGER_CLUB_APP_WORKSPACE_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club app workspace backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerClubAppWorkspace(request, clubId, "GET", failure);
}

export async function PUT(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyManagerClubAppWorkspace(request, clubId, "PUT", failure);
}
