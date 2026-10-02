import { proxyPublicNewsDetail } from "../../../../lib/api/newsProxy";

const failure = {
  failureCode: "PUBLIC_NEWS_DETAIL_FAILED",
  failureMessage: "public news detail request failed",
  unavailableCode: "PUBLIC_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "public news backend unavailable"
} as const;

export async function GET(
  _request: Request,
  context: { params: { newsId: string } }
) {
  return proxyPublicNewsDetail(`/api/v1/public/news/${context.params.newsId}`, failure);
}
