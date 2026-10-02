import { proxyAdminBannerUpload } from "../../../../../../lib/api/bannerProxy";

export async function POST(
  request: Request,
  context: { params: { bannerId: string } }
) {
  return proxyAdminBannerUpload(
    request,
    `/api/v1/admin/home-banners/${context.params.bannerId}/file`,
    {
      failureCode: "ADMIN_HOME_BANNER_UPLOAD_FAILED",
      failureMessage: "admin home banner upload failed",
      unavailableCode: "ADMIN_HOME_BANNERS_BACKEND_UNREACHABLE",
      unavailableMessage: "admin home banners backend unavailable"
    }
  );
}
