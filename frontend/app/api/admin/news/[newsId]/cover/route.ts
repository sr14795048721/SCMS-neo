import { proxyAdminNewsMutation, proxyAdminNewsUpload } from "../../../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_COVER_FAILED",
  failureMessage: "admin news cover request failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function POST(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsUpload(request, `/api/v1/admin/news/${context.params.newsId}/cover`, failure, "detail");
}

export async function DELETE(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsMutation(
    request,
    `/api/v1/admin/news/${context.params.newsId}/cover`,
    "DELETE",
    failure,
    { deleteReturnsDetail: true }
  );
}
