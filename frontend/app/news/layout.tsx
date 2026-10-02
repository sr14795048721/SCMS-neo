import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["news"]);

  return {
    title: messages.news?.["list.meta.title"] || "SCMS Plus",
    description: messages.news?.["list.meta.description"] || "SCMS Plus"
  };
}

export default function NewsLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
