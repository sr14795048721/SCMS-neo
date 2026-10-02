import { proxyAdminNewsBinary } from "../../../../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_COVER_CONTENT_FAILED",
  failureMessage: "admin news cover content request failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsBinary(request, `/api/v1/admin/news/${context.params.newsId}/cover/content`, failure);
}
