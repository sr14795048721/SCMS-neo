import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminClubManage"]);

  return {
    title: messages.adminClubManage?.["meta.title"] || "SCMS Plus",
    description: messages.adminClubManage?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminClubManageLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
