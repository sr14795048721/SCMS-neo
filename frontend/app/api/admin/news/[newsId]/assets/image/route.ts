import { proxyAdminNewsUpload } from "../../../../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_ASSET_UPLOAD_FAILED",
  failureMessage: "admin news asset upload failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsUpload(request, `/api/v1/admin/news/${context.params.newsId}/assets/image`, failure, "asset");
}
