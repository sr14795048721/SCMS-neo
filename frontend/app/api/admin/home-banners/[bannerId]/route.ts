import { proxyAdminBannerMutation } from "../../../../../lib/api/bannerProxy";

export async function PATCH(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerMutation(
    request,
    `/api/v1/admin/home-banners/${context.params.bannerId}`,
    "PATCH",
    {
      failureCode: "ADMIN_HOME_BANNER_UPDATE_FAILED",
      failureMessage: "admin home banner update failed",
      unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin home banners backend unavailable"
    }
  );
}

export async function DELETE(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerMutation(
    request,
    `/api/v1/admin/home-banners/${context.params.bannerId}`,
    "DELETE",
    {
      failureCode: "ADMIN_HOME_BANNER_DELETE_FAILED",
      failureMessage: "admin home banner delete failed",
      unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin home banners backend unavailable"
    }
  );
}
