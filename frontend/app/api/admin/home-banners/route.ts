import { proxyAdminBannerCollection } from "../../../../lib/api/bannerProxy";

const failure = {
  failureCode: "ADMIN_HOME_BANNERS_FAILED",
  failureMessage: "admin home banners request failed",
  unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin home banners backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminBannerCollection(request, "/api/v1/admin/home-banners", "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminBannerCollection(request, "/api/v1/admin/home-banners", "POST", failure);
}
