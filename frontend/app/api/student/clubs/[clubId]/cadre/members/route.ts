import { proxyStudentCadreClubMembers } from "../../../../../../../lib/api/studentCadreClubProxy";

const failure = {
  failureCode: "STUDENT_CADRE_CLUB_MEMBERS_FAILED",
  failureMessage: "student cadre club members request failed",
  unavailableCode: "STUDENT_CADRE_CLUB_MEMBERS_UNAVAILABLE",
  unavailableMessage: "student cadre club members service unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: Promise<{ clubId: string }> }
) {
  const { clubId } = await context.params;
  return proxyStudentCadreClubMembers(request, clubId, failure);
}
