import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["teacherProfile"]);

  return {
    title: messages.teacherProfile?.["meta.title"] || "SCMS Plus",
    description: messages.teacherProfile?.["meta.description"] || "SCMS Plus"
  };
}

export default function ClubAdminProfileLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
