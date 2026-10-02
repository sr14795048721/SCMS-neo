import type { Metadata } from "next";
import { loadInitialMessages, resolveRequestLocale } from "../../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["studentActivities"]);

  return {
    title: messages.studentActivities?.["meta.title"] || "SCMS Plus",
    description: messages.studentActivities?.["meta.description"] || "SCMS Plus"
  };
}

export default function StudentActivitiesLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
