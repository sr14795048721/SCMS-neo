import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminClubRecommend"]);

  return {
    title: messages.adminClubRecommend?.["meta.title"] || "SCMS Plus",
    description: messages.adminClubRecommend?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminClubRecommendLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
