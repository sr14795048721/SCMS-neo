import { proxyStudentCadreClubDetail } from "../../../../../../lib/api/studentCadreClubProxy";

const failure = {
  failureCode: "STUDENT_CADRE_CLUB_DETAIL_FAILED",
  failureMessage: "student cadre club detail request failed",
  unavailableCode: "STUDENT_CADRE_CLUB_DETAIL_UNAVAILABLE",
  unavailableMessage: "student cadre club detail service unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: Promise<{ clubId: string }> }
) {
  const { clubId } = await context.params;
  return proxyStudentCadreClubDetail(request, clubId, failure);
}
