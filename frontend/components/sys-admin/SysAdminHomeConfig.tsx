"use client";

import { SysAdminBannerConfig } from "./SysAdminBannerConfig";

export function SysAdminHomeConfig() {
  return (
    <SysAdminBannerConfig
      namespace="adminHomeConfig"
      adminApiBase="/api/admin/home-banners"
      previewTarget="/"
      headerIconClass="fas fa-home"
    />
  );
}
