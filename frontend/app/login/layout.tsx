import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["login"]);

  return {
    title: messages.login?.["meta.title"] || "SCMS Plus",
    description: messages.login?.["meta.description"] || "SCMS Plus"
  };
}

export default function LoginLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}

