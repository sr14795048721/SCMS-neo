import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["studentClubCadre"]);

  return {
    title: messages.studentClubCadre?.["meta.title"] || "SCMS Plus",
    description: messages.studentClubCadre?.["meta.description"] || "SCMS Plus"
  };
}

export default function StudentClubManageLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
