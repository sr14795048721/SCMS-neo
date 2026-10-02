import { proxyAdminBannerAction } from "../../../../../lib/api/bannerProxy";

export async function POST(request: Request) {
  return proxyAdminBannerAction(request, "/api/v1/admin/home-banners/recompress", {
    failureCode: "ADMIN_HOME_BANNERS_RECOMPRESS_FAILED",
    failureMessage: "admin home banners recompress failed",
    unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "admin home banners backend unavailable"
  });
}
