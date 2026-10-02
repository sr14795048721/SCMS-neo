import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminActivityManage"]);

  return {
    title: messages.adminActivityManage?.["meta.title"] || "SCMS Plus",
    description: messages.adminActivityManage?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminActivityManageLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
