import { proxyAdminBannerUpload } from "../../../../../../lib/api/bannerProxy";

export async function POST(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerUpload(
    request,
    `/api/v1/admin/login-banners/${context.params.bannerId}/file`,
    {
      failureCode: "ADMIN_LOGIN_BANNER_UPLOAD_FAILED",
      failureMessage: "admin login banner upload failed",
      unavailableCode: "ADMIN_LOGIN_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin login banners backend unavailable"
    }
  );
}
