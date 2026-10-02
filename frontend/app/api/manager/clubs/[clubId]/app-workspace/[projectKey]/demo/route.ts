import { proxyManagerClubAppWorkspaceProjectDemo } from "../../../../../../../../lib/api/managerClubAppWorkspaceDemoProxy";

const failure = {
  failureCode: "MANAGER_CLUB_APP_WORKSPACE_DEMO_REQUEST_FAILED",
  failureMessage: "manager club app workspace demo request failed",
  unavailableCode: "MANAGER_CLUB_APP_WORKSPACE_DEMO_BACKEND_UNREACHABLE",
  unavailableMessage: "manager club app workspace demo backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      projectKey: string;
    }>;
  }
) {
  const { clubId, projectKey } = await context.params;
  return proxyManagerClubAppWorkspaceProjectDemo(request, clubId, projectKey, "GET", failure);
}

export async function PUT(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
      projectKey: string;
    }>;
  }
) {
  const { clubId, projectKey } = await context.params;
  return proxyManagerClubAppWorkspaceProjectDemo(request, clubId, projectKey, "PUT", failure);
}
