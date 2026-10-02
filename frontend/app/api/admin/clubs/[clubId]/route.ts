import { proxyAdminClubDetail } from "../../../../../lib/api/adminClubProxy";

const failure = {
  failureCode: "ADMIN_CLUB_DETAIL_FAILED",
  failureMessage: "admin club detail request failed",
  unavailableCode: "ADMIN_CLUB_DETAIL_BACKEND_UNREACHABLE",
  unavailableMessage: "admin club detail backend unavailable"
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
  return proxyAdminClubDetail(request, `/api/v1/admin/clubs/${clubId}`, "GET", failure);
}

export async function PATCH(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyAdminClubDetail(request, `/api/v1/admin/clubs/${clubId}`, "PATCH", failure);
}

export async function DELETE(
  request: Request,
  context: {
    params: Promise<{
      clubId: string;
    }>;
  }
) {
  const { clubId } = await context.params;
  return proxyAdminClubDetail(request, `/api/v1/admin/clubs/${clubId}`, "DELETE", failure);
}
