import { proxyAdminNewsBinary } from "../../../../../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_ASSET_CONTENT_FAILED",
  failureMessage: "admin news asset content request failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: { newsId: string; assetId: string } }
) {
  return proxyAdminNewsBinary(
    request,
    `/api/v1/admin/news/${context.params.newsId}/assets/${context.params.assetId}/content`,
    failure
  );
}
