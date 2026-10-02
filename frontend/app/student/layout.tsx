import type { Metadata } from "next";
import { StudentProfileCompletionGuard } from "../../components/student/StudentProfileCompletionGuard";
import { loadInitialMessages, resolveRequestLocale } from "../../lib/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const locale = resolveRequestLocale();
  const messages = await loadInitialMessages(locale, ["studentDashboard"]);

  return {
    title: messages.studentDashboard?.["meta.title"] || "SCMS Plus",
    description: messages.studentDashboard?.["meta.description"] || "SCMS Plus"
  };
}

export default function StudentLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <StudentProfileCompletionGuard>{children}</StudentProfileCompletionGuard>;
}
