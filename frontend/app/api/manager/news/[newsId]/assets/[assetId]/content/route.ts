import { proxyManagerNewsBinary } from "../../../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_ASSET_CONTENT_FAILED",
  failureMessage: "manager news asset content failed",
  unavailableCode: "MANAGER_NEWS_ASSET_CONTENT_UNAVAILABLE",
  unavailableMessage: "manager news asset content unavailable"
} as const;

export async function GET(request: Request, context: { params: Promise<{ newsId: string; assetId: string }> }) {
  const { newsId, assetId } = await context.params;
  return proxyManagerNewsBinary(request, `/api/v1/managers/me/news/${newsId}/assets/${assetId}/content`, FAILURE);
}
