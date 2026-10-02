import { proxyPublicBannerList } from "../../../lib/api/bannerProxy";

export async function GET() {
  return proxyPublicBannerList("/api/v1/public/login-banners", {
    failureCode: "PUBLIC_LOGIN_BANNERS_FAILED",
    failureMessage: "public login banners unavailable",
    unavailableCode: "PUBLIC_LOGIN_BANNERS_BACKEND_UNREACHABLE",
    unavailableMessage: "public login banners backend unavailable"
  });
}
