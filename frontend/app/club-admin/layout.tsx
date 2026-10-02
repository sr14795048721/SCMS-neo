import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["teacherDashboard"]);

  return {
    title: messages.teacherDashboard?.["meta.title"] || "SCMS Plus",
    description: messages.teacherDashboard?.["meta.description"] || "SCMS Plus"
  };
}

export default function ClubAdminLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
