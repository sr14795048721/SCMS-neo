import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminUserManage"]);

  return {
    title: messages.adminUserManage?.["meta.title"] || "SCMS Plus",
    description: messages.adminUserManage?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminUserManageLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
