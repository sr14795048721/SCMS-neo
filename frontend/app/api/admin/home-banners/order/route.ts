import { proxyAdminBannerReorder } from "../../../../../lib/api/bannerProxy";

export async function PUT(request: Request) {
  return proxyAdminBannerReorder(request, "/api/v1/admin/home-banners/order", {
    failureCode: "ADMIN_HOME_BANNERS_REORDER_FAILED",
    failureMessage: "admin home banners reorder failed",
    unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "admin home banners backend unavailable"
  });
}
