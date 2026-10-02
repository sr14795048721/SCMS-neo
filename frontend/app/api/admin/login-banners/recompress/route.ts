import { proxyAdminBannerAction } from "../../../../../lib/api/bannerProxy";

export async function POST(request: Request) {
  return proxyAdminBannerAction(request, "/api/v1/admin/login-banners/recompress", {
    failureCode: "ADMIN_LOGIN_BANNERS_RECOMPRESS_FAILED",
    failureMessage: "admin login banners recompress failed",
    unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "admin login banners backend unavailable"
  });
}
