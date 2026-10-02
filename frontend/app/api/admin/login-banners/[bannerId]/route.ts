import { proxyAdminBannerMutation } from "../../../../../lib/api/bannerProxy";

export async function PATCH(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerMutation(
    request,
    `/api/v1/admin/login-banners/${context.params.bannerId}`,
    "PATCH",
    {
      failureCode: "ADMIN_LOGIN_BANNER_UPDATE_FAILED",
      failureMessage: "admin login banner update failed",
      unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin login banners backend unavailable"
    }
  );
}

export async function DELETE(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerMutation(
    request,
    `/api/v1/admin/login-banners/${context.params.bannerId}`,
    "DELETE",
    {
      failureCode: "ADMIN_LOGIN_BANNER_DELETE_FAILED",
      failureMessage: "admin login banner delete failed",
      unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin login banners backend unavailable"
    }
  );
}
