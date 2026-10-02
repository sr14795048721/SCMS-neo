import { proxyManagerNewsMutation, proxyManagerNewsUpload } from "../../../../../../lib/api/managerTeacherProxy";

const FAILURE = {
  failureCode: "MANAGER_NEWS_COVER_FAILED",
  failureMessage: "manager news cover request failed",
  unavailableCode: "MANAGER_NEWS_COVER_UNAVAILABLE",
  unavailableMessage: "manager news cover service unavailable"
} as const;

export async function POST(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsUpload(request, `/api/v1/managers/me/news/${newsId}/cover`, FAILURE, "detail");
}

export async function DELETE(request: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params;
  return proxyManagerNewsMutation(request, `/api/v1/managers/me/news/${newsId}/cover`, "DELETE", FAILURE, { deleteReturnsDetail: true });
}
