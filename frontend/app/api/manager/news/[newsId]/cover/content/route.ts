import { proxyManagerNewsBinary } from "../../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_COVER_CONTENT_FAILED",
  failureMessage: "manager news cover content failed",
  unavailableCode: "MANAGER_NEWS_COVER_CONTENT_UNAVAILABLE",
  unavailableMessage: "manager news cover content unavailable"
} as const;

export async function GET(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsBinary(request, `/api/v1/managers/me/news/${newsId}/cover/content`, FAILURE);
}
