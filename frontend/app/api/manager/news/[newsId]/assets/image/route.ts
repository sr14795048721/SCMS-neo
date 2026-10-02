import { proxyManagerNewsUpload } from "../../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_ASSET_UPLOAD_FAILED",
  failureMessage: "manager news asset upload failed",
  unavailableCode: "MANAGER_NEWS_ASSET_UPLOAD_UNAVAILABLE",
  unavailableMessage: "manager news asset upload unavailable"
} as const;

export async function POST(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsUpload(request, `/api/v1/managers/me/news/${newsId}/assets/image`, FAILURE, "asset");
}
