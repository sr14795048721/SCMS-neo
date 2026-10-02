import { proxyManagerClubDuties } from "../../../../../../lib/api/managerClubProxy";

const failure = {
  failureCode: "MANAGER_CLUB_DUTIES_FAILED",
  failureMessage: "manager club duties request failed",
  unavailableCode: "MANAGER_CLUB_DUTIES_UNAVAILABLE",
  unavailableMessage: "manager club duties backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: Promise<{ clubId: string }> }
) {
  const { clubId } = await context.params;
  return proxyManagerClubDuties(request, clubId, "GET", failure);
}

export async function POST(
  request: Request,
  context: { params: Promise<{ clubId: string }> }
) {
  const { clubId } = await context.params;
  return proxyManagerClubDuties(request, clubId, "POST", failure);
}
