import { proxyAdminBannerReorder } from "../../../../../lib/api/bannerProxy";

export async function PUT(request: Request) {
  return proxyAdminBannerReorder(request, "/api/v1/admin/login-banners/order", {
    failureCode: "ADMIN_LOGIN_BANNERS_REORDER_FAILED",
    failureMessage: "admin login banners reorder failed",
    unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "admin login banners backend unavailable"
  });
}
