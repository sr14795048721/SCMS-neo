import { SysAdminBannerConfig } from "../../../components/sys-admin/SysAdminBannerConfig";

export default function SysAdminLoginCarouselPage() {
  return (
    <SysAdminBannerConfig
      namespace="adminLoginConfig"
      adminApiBase="/api/admin/login-banners"
      previewTarget="/login"
      headerIconClass="fas fa-images"
    />
  );
}
