import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["studentProfile"]);

  return {
    title: messages.studentProfile?.["meta.title"] || "SCMS Plus",
    description: messages.studentProfile?.["meta.description"] || "SCMS Plus"
  };
}

export default function StudentProfileLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}

