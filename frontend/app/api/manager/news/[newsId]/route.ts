import { proxyManagerNewsMutation } from "../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_DETAIL_FAILED",
  failureMessage: "manager news detail request failed",
  unavailableCode: "MANAGER_NEWS_DETAIL_UNAVAILABLE",
  unavailableMessage: "manager news detail service unavailable"
} as const;

export async function GET(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsMutation(request, `/api/v1/managers/me/news/${newsId}`, "GET", FAILURE);
}

export async function PATCH(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsMutation(request, `/api/v1/managers/me/news/${newsId}`, "PATCH", FAILURE);
}

export async function DELETE(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsMutation(request, `/api/v1/managers/me/news/${newsId}`, "DELETE", FAILURE);
}
