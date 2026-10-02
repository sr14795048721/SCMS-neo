import { proxyPublicBannerList } from "../../../lib/api/bannerProxy";

export async function GET() {
  return proxyPublicBannerList("/api/v1/public/home-banners", {
    failureCode: "PUBLIC_BANNERS_FAILED",
    failureMessage: "public banners unavailable",
    unavailableCode: "PUBLIC_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "public banners backend unavailable"
  });
}
