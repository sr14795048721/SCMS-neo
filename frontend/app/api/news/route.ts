import { proxyPublicNewsList } from "../../../lib/api/newsProxy";

const failure = {
  failureCode: "PUBLIC_NEWS_LIST_FAILED",
  failureMessage: "public news list request failed",
  unavailableCode: "PUBLIC_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "public news backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyPublicNewsList(request, failure);
}
