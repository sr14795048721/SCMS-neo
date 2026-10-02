import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminDashboard"]);

  return {
    title: messages.adminDashboard?.["meta.title"] || "SCMS Plus",
    description: messages.adminDashboard?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
