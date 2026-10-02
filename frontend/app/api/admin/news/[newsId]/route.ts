import { proxyAdminNewsMutation } from "../../../../../lib/api/newsProxy";

const failure = {
  failureCode: "ADMIN_NEWS_MUTATION_FAILED",
  failureMessage: "admin news mutation failed",
  unavailableCode: "ADMIN_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin news backend unavailable"
} as const;

export async function GET(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsMutation(request, `/api/v1/admin/news/${context.params.newsId}`, "GET", failure);
}

export async function PATCH(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsMutation(request, `/api/v1/admin/news/${context.params.newsId}`, "PATCH", failure);
}

export async function DELETE(
  request: Request,
  context: { params: { newsId: string } }
) {
  return proxyAdminNewsMutation(request, `/api/v1/admin/news/${context.params.newsId}`, "DELETE", failure);
}
