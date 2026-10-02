import { proxyPublicNewsHome } from "../../../../lib/api/newsProxy";

const failure = {
  failureCode: "PUBLIC_NEWS_HOME_FAILED",
  failureMessage: "public news home request failed",
  unavailableCode: "PUBLIC_NEWS_BACKEND_UNREACHABLE",
  unavailableMessage: "public news backend unavailable"
} as const;

export async function GET() {
  return proxyPublicNewsHome(failure);
}
