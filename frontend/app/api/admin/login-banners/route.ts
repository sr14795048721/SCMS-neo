import { proxyAdminBannerCollection } from "../../../../lib/api/bannerProxy";

const failure = {
  failureCode: "ADMIN_LOGIN_BANNERS_FAILED",
  failureMessage: "admin login banners request failed",
  unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
  unavailableMessage: "admin login banners backend unavailable"
} as const;

export async function GET(request: Request) {
  return proxyAdminBannerCollection(request, "/api/v1/admin/login-banners", "GET", failure);
}

export async function POST(request: Request) {
  return proxyAdminBannerCollection(request, "/api/v1/admin/login-banners", "POST", failure);
}
