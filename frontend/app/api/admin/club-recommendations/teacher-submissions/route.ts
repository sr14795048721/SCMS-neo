import { proxyTeacherRecommendationSubmissions } from "../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "ADMIN_TEACHER_RECOMMENDATIONS_FAILED",
  failureMessage: "teacher recommendation submissions failed",
  unavailableCode: "ADMIN_TEACHER_RECOMMENDATIONS_UNAVAILABLE",
  unavailableMessage: "teacher recommendation submissions unavailable"
} as const;

export async function GET(request: Request) {
  return proxyTeacherRecommendationSubmissions(request, FAILURE);
}
