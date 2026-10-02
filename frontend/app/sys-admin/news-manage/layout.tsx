import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["adminNews"]);

  return {
    title: messages.adminNews?.["meta.title"] || "SCMS Plus",
    description: messages.adminNews?.["meta.description"] || "SCMS Plus"
  };
}

export default function SysAdminNewsLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
