import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminHomeConfig"]);

  return {
    title: messages.adminHomeConfig?.["meta.title"] || "SCMS Plus",
    description: messages.adminHomeConfig?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminHomeConfigLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
